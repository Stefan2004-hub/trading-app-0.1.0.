package com.trading.exception;

public class OAuthEmailConflictException extends RuntimeException {

    public OAuthEmailConflictException() {
        super("Email already registered with local account. Please use email/password login.");
    }
}
