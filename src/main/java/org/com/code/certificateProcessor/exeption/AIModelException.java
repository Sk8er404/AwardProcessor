package org.com.code.certificateProcessor.exeption;

public class AIModelException extends RuntimeException {
    public AIModelException(String message) {
        super(message);
    }
    public AIModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
