package com.supermart.exception;

public class InsufficientStockException extends BusinessException {
    public InsufficientStockException(String productName, int available, int requested) {
        super("Insufficient stock for '" + productName + "': requested " + requested + ", available " + available);
    }
}
