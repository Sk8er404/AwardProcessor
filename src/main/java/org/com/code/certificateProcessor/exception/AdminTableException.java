package org.com.code.certificateProcessor.exception;

public class AdminTableException extends RuntimeException {
    public AdminTableException(String message) {
        super(message);
    }
    public AdminTableException(String message, Throwable cause) {
        super(message, cause);
    }
}
