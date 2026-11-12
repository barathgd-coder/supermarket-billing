package com.supermart.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row per day (yyyyMMdd). Locked with a pessimistic write lock to guarantee unique invoice numbers. */
@Entity
@Table(name = "invoice_counters")
@Getter @Setter @NoArgsConstructor
public class InvoiceCounter {
    @Id
    @Column(name = "date_key", length = 8)
    private String dateKey;

    @Column(name = "last_number", nullable = false)
    private int lastNumber;

    public InvoiceCounter(String dateKey, int lastNumber) {
        this.dateKey = dateKey;
        this.lastNumber = lastNumber;
    }
}
