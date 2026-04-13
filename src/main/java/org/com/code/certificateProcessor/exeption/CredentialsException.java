package org.com.code.certificateProcessor.exeption;

public class CredentialsException extends RuntimeException {
    public CredentialsException(String message) {
        super(message);
    }
    public CredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
