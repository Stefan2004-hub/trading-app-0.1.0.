package com.trading.exception;

public record ApiFieldViolation(
    String field,
    String message
) {
}
