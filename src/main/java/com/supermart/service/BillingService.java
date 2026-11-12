package com.supermart.service;

import com.supermart.billing.BillingCalculator;
import com.supermart.billing.BillingCalculator.BillResult;
import com.supermart.billing.BillingCalculator.LineInput;
import com.supermart.billing.BillingCalculator.LineResult;
import com.supermart.dto.CartItemRequest;
import com.supermart.dto.CheckoutRequest;
import com.supermart.dto.ProductLookup;
import com.supermart.exception.BusinessException;
import com.supermart.exception.InsufficientStockException;
import com.supermart.exception.ResourceNotFoundException;
import com.supermart.model.*;
import com.supermart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final InvoiceCounterRepository counterRepository;
    private final InventoryService inventoryService;
    private final SettingService settingService;
    private final BillingCalculator calculator;

    /** POS search by name, barcode / product ID. Exact barcode matches are returned first. */
    @Transactional(readOnly = true)
    public List<ProductLookup> search(String q) {
        String term = q == null ? "" : q.trim();
        if (term.isEmpty()) return List.of();
        List<Product> found = new ArrayList<>(productRepository.searchForPos(term, PageRequest.of(0, 12)));
        productRepository.findByBarcode(term).filter(Product::isActive).ifPresent(exact -> {
            found.removeIf(p -> p.getId().equals(exact.getId()));
            found.add(0, exact);
        });
        List<ProductLookup> out = new ArrayList<>();
        for (Product p : found) {
            out.add(new ProductLookup(p.getId(), p.getBarcode(), p.getName(), p.getCategory().getName(),
                    p.getSellingPrice(), p.getGstPercent(), p.getQuantity()));
        }
        return out;
    }

    @Transactional
    public Sale checkout(CheckoutRequest request, String cashierUsername) {
        return checkoutAt(request, cashierUsername, LocalDateTime.now());
    }

    /**
     * The whole sale runs in ONE transaction: sale, sale items, payment, stock reduction and
     * inventory log are committed together or not at all.
     */
    @Transactional
    public Sale checkoutAt(CheckoutRequest request, String cashierUsername, LocalDateTime when) {
        // merge duplicate lines; TreeMap => products are locked in id order (prevents deadlocks)
        Map<Long, Integer> quantities = new TreeMap<>();
        for (CartItemRequest item : request.getItems()) {
            quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        if (quantities.isEmpty()) throw new BusinessException("Cart is empty");

        List<Product> products = new ArrayList<>();
        List<LineInput> inputs = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : quantities.entrySet()) {
            Product p = productRepository.findByIdForUpdate(e.getKey())
                    .filter(Product::isActive)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", e.getKey()));
            if (p.getExpiryDate() != null && p.getExpiryDate().isBefore(when.toLocalDate())) {
                throw new BusinessException("'" + p.getName() + "' is expired and cannot be sold");
            }
            if (e.getValue() > p.getQuantity()) {
                throw new InsufficientStockException(p.getName(), p.getQuantity(), e.getValue());
            }
            products.add(p);
            inputs.add(new LineInput(p.getSellingPrice(), e.getValue(), p.getGstPercent()));
        }

        BillResult result = calculator.calculate(inputs, request.getDiscountType(), request.getDiscountValue());

        User cashier = userRepository.findByUsername(cashierUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", cashierUsername));
        Customer customer = resolveCustomer(request.getCustomerName(), request.getCustomerPhone());

        Sale sale = new Sale();
        sale.setInvoiceNumber(nextInvoiceNumber(when));
        sale.setSaleDate(when);
        sale.setCashier(cashier);
        sale.setCustomer(customer);
        sale.setCustomerName(customer != null ? customer.getName() : blankToNull(request.getCustomerName()));
        sale.setCustomerPhone(customer != null ? customer.getPhone() : blankToNull(request.getCustomerPhone()));
        sale.setPaymentMethod(request.getPaymentMethod());
        sale.setSubtotal(result.subtotal());
        sale.setDiscountAmount(result.discount());
        sale.setTaxTotal(result.taxTotal());
        sale.setGrandTotal(result.grandTotal());

        BigDecimal totalCost = BigDecimal.ZERO;
        List<Integer> deltas = new ArrayList<>();
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            LineInput in = inputs.get(i);
            LineResult line = result.lines().get(i);

            SaleItem item = new SaleItem();
            item.setProduct(p);
            item.setProductName(p.getName());
            item.setQuantity(in.quantity());
            item.setUnitPrice(p.getSellingPrice());
            item.setPurchasePrice(p.getPurchasePrice());
            item.setGstPercent(p.getGstPercent());
            item.setDiscountAmount(line.discount());
            item.setTaxAmount(line.tax());
            item.setLineTotal(line.total());
            sale.addItem(item);

            totalCost = totalCost.add(BillingCalculator.money(p.getPurchasePrice().multiply(BigDecimal.valueOf(in.quantity()))));
            deltas.add(-in.quantity());
        }
        sale.setTotalCost(totalCost);

        Payment payment = new Payment();
        payment.setMethod(request.getPaymentMethod());
        payment.setAmount(result.grandTotal());
        payment.setPaidAt(when);
        payment.setReference(blankToNull(request.getPaymentReference()));
        sale.attachPayment(payment);

        sale = saleRepository.save(sale);

        // stock reduction + inventory log (same transaction)
        for (int i = 0; i < products.size(); i++) {
            inventoryService.record(products.get(i), TransactionType.SALE, deltas.get(i),
                    sale.getInvoiceNumber(), "Billing", cashierUsername, when);
        }
        return sale;
    }

    private Customer resolveCustomer(String name, String phone) {
        String n = blankToNull(name);
        String ph = blankToNull(phone);
        if (ph == null) return null;   // walk-in customer: keep the typed name on the bill only
        Customer c = customerRepository.findByPhone(ph).orElse(null);
        if (c == null) {
            c = new Customer();
            c.setPhone(ph);
            c.setName(n != null ? n : "Customer " + ph);
            return customerRepository.save(c);
        }
        return c;
    }

    /** INV-YYYYMMDD-0001: unique per day thanks to the locked counter row (+ unique DB constraint). */
    private String nextInvoiceNumber(LocalDateTime when) {
        String key = when.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE);
        InvoiceCounter counter = counterRepository.lockByDateKey(key)
                .orElseGet(() -> counterRepository.saveAndFlush(new InvoiceCounter(key, 0)));
        counter.setLastNumber(counter.getLastNumber() + 1);
        counterRepository.save(counter);
        return String.format("%s-%s-%04d", settingService.invoicePrefix(), key, counter.getLastNumber());
    }

    @Transactional(readOnly = true)
    public Optional<Customer> lookupCustomer(String phone) {
        return phone == null || phone.isBlank() ? Optional.empty() : customerRepository.findByPhone(phone.trim());
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
