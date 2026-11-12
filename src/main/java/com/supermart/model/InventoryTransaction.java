package com.supermart.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions", indexes = {
        @Index(name = "idx_invtxn_created", columnList = "created_at"),
        @Index(name = "idx_invtxn_product", columnList = "product_id")
})
@Getter @Setter @NoArgsConstructor
public class InventoryTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false, length = 20)
    private TransactionType type;

    /** Signed change: positive = stock added, negative = stock removed. */
    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Column(length = 100)
    private String reference;

    @Column(length = 255)
    private String note;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
