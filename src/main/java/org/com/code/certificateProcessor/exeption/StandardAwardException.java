package org.com.code.certificateProcessor.exeption;

public class StandardAwardException extends RuntimeException {
    public StandardAwardException(String message) {
        super(message);
    }
    public StandardAwardException(String message, Throwable cause) {
        super(message, cause);
    }
}
