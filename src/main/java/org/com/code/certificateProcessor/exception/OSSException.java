package org.com.code.certificateProcessor.exception;

public class OSSException extends RuntimeException {
    public OSSException(String message) {
        super(message);
    }
    public OSSException(String message,Throwable cause) {
        super(message, cause);
    }
}
