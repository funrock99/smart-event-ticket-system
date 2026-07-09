package com.example.smarteventticket.exception;

public class InvalidIdempotencyRequestException extends RuntimeException {

    public InvalidIdempotencyRequestException(String message) {
        super(message);
    }
}
