package com.supermart.dto;

import java.math.BigDecimal;

public record CheckoutResponse(Long saleId, String invoiceNumber, BigDecimal grandTotal, String invoiceUrl) { }
