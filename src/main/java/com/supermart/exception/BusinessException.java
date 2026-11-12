package com.supermart.exception;

/** A rule of the business was violated (shown to the user as a clear message). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
