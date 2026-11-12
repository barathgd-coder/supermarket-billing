package com.supermart.service;

import com.supermart.dto.StockEntryForm;
import com.supermart.exception.BusinessException;
import com.supermart.exception.ResourceNotFoundException;
import com.supermart.model.InventoryTransaction;
import com.supermart.model.Product;
import com.supermart.model.TransactionType;
import com.supermart.repository.InventoryTransactionRepository;
import com.supermart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository transactionRepository;

    /** Applies a signed stock change to the product and writes the inventory log entry. */
    @Transactional
    public InventoryTransaction record(Product product, TransactionType type, int delta,
                                       String reference, String note, String user) {
        return record(product, type, delta, reference, note, user, LocalDateTime.now());
    }

    @Transactional
    public InventoryTransaction record(Product product, TransactionType type, int delta,
                                       String reference, String note, String user, LocalDateTime when) {
        int newQty = product.getQuantity() + delta;
        if (newQty < 0) {
            throw new BusinessException("Stock cannot go negative for '" + product.getName()
                    + "' (available " + product.getQuantity() + ")");
        }
        product.setQuantity(newQty);
        productRepository.save(product);

        InventoryTransaction t = new InventoryTransaction();
        t.setProduct(product);
        t.setType(type);
        t.setQuantityChange(delta);
        t.setBalanceAfter(newQty);
        t.setReference(reference);
        t.setNote(note);
        t.setCreatedBy(user);
        t.setCreatedAt(when);
        return transactionRepository.save(t);
    }

    @Transactional
    public void enterStock(StockEntryForm form, String user) {
        Product p = productRepository.findByIdForUpdate(form.getProductId())
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product", form.getProductId()));
        int qty = form.getQuantity();
        String ref = blankToNull(form.getReference());
        String note = blankToNull(form.getNote());
        switch (form.getType()) {
            case STOCK_IN -> record(p, TransactionType.STOCK_IN, qty, ref, note, user);
            case STOCK_OUT -> record(p, TransactionType.STOCK_OUT, -qty, ref, note, user);
            default -> throw new BusinessException("Only stock-in and stock-out entries can be added manually");
        }
    }

    @Transactional(readOnly = true)
    public Page<InventoryTransaction> history(String q, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size);
        return transactionRepository.search(q == null ? "" : q.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Product> lowStock() { return productRepository.findLowStock(); }

    @Transactional(readOnly = true)
    public List<Product> expired() {
        return productRepository.findByActiveTrueAndExpiryDateBeforeOrderByExpiryDateAsc(LocalDate.now());
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
