package com.siteguard.exception;

/** Thrown when the user enters a URL that cannot be monitored. */
public class InvalidUrlException extends RuntimeException {
    public InvalidUrlException(String message) {
        super(message);
    }
}
