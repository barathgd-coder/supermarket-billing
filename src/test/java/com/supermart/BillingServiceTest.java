package com.supermart;

import com.supermart.dto.CartItemRequest;
import com.supermart.dto.CheckoutRequest;
import com.supermart.exception.InsufficientStockException;
import com.supermart.model.*;
import com.supermart.repository.CategoryRepository;
import com.supermart.repository.InventoryTransactionRepository;
import com.supermart.repository.ProductRepository;
import com.supermart.service.BillingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.upload-dir=./target/test-uploads"
})
@Transactional
class BillingServiceTest {

    @Autowired BillingService billingService;
    @Autowired ProductRepository productRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired InventoryTransactionRepository transactionRepository;

    Product product;

    @BeforeEach
    void setUp() {
        Category c = categoryRepository.save(new Category("Test category"));
        Product p = new Product();
        p.setBarcode("TEST-0001");
        p.setName("Test Rice 1kg");
        p.setCategory(c);
        p.setPurchasePrice(new BigDecimal("40.00"));
        p.setSellingPrice(new BigDecimal("50.00"));
        p.setGstPercent(new BigDecimal("5"));
        p.setQuantity(10);
        product = productRepository.save(p);
    }

    private CheckoutRequest request(int qty) {
        CheckoutRequest r = new CheckoutRequest();
        r.setPaymentMethod(PaymentMethod.CASH);
        CartItemRequest item = new CartItemRequest();
        item.setProductId(product.getId());
        item.setQuantity(qty);
        r.getItems().add(item);
        return r;
    }

    @Test
    void checkoutReducesStockAndWritesInventoryLog() {
        long logsBefore = transactionRepository.countByProductId(product.getId());

        Sale sale = billingService.checkout(request(3), "cashier");

        assertThat(productRepository.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(7);
        assertThat(sale.getItems()).hasSize(1);
        assertThat(sale.getSubtotal()).isEqualByComparingTo("150.00");
        assertThat(sale.getTaxTotal()).isEqualByComparingTo("7.50");
        assertThat(sale.getGrandTotal()).isEqualByComparingTo("157.50");
        assertThat(sale.getPayment().getAmount()).isEqualByComparingTo("157.50");
        assertThat(sale.getTotalCost()).isEqualByComparingTo("120.00");
        assertThat(transactionRepository.countByProductId(product.getId())).isEqualTo(logsBefore + 1);
    }

    @Test
    void sellingMoreThanAvailableStockIsBlockedAndStockIsUntouched() {
        assertThatThrownBy(() -> billingService.checkout(request(11), "cashier"))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
    }

    @Test
    void invoiceNumbersFollowTheFormatAndAreUnique() {
        Sale a = billingService.checkout(request(1), "cashier");
        Sale b = billingService.checkout(request(1), "cashier");
        assertThat(a.getInvoiceNumber()).matches("INV-\\d{8}-\\d{4}");
        assertThat(b.getInvoiceNumber()).matches("INV-\\d{8}-\\d{4}");
        assertThat(a.getInvoiceNumber()).isNotEqualTo(b.getInvoiceNumber());
    }
}
