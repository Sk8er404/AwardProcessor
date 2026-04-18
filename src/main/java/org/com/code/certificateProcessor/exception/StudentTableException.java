package org.com.code.certificateProcessor.exception;

public class StudentTableException extends RuntimeException {
    public StudentTableException(String message) {
        super(message);
    }
    public StudentTableException(String message, Throwable cause) {
        super(message, cause);
    }
}
