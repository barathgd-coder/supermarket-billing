package com.supermart.dto;

import java.math.BigDecimal;

/** Lightweight product view returned to the POS screen. */
public record ProductLookup(Long id, String barcode, String name, String category,
                            BigDecimal price, BigDecimal gstPercent, int stock) { }
