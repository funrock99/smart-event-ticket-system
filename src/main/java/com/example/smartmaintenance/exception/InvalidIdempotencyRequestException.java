package com.example.smartmaintenance.exception;

public class InvalidIdempotencyRequestException extends RuntimeException {

    public InvalidIdempotencyRequestException(String message) {
        super(message);
    }
}