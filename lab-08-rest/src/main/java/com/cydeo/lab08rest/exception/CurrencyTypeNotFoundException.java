package com.cydeo.lab08rest.exception;

public class CurrencyTypeNotFoundException extends RuntimeException {
    public CurrencyTypeNotFoundException(String message) {
        super(message);
    }
}
