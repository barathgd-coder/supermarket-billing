package com.supermart.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_name", columnList = "name"),
        @Index(name = "idx_product_category", columnList = "category_id")
})
@Getter @Setter @NoArgsConstructor
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Product ID / barcode. Unique across all products (including deactivated ones). */
    @Column(nullable = false, unique = true, length = 50)
    private String barcode;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "selling_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "gst_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstPercent = BigDecimal.ZERO;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel = 10;

    /** Soft-delete flag so that sales history keeps its references. */
    @Column(nullable = false)
    private boolean active = true;

    public boolean isLowStock() { return quantity <= reorderLevel; }

    public boolean isOutOfStock() { return quantity <= 0; }

    public boolean isExpired() { return expiryDate != null && expiryDate.isBefore(LocalDate.now()); }

    public boolean isNearExpiry() {
        return expiryDate != null && !isExpired() && !expiryDate.isAfter(LocalDate.now().plusDays(7));
    }
}
