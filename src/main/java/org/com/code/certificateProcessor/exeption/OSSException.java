package org.com.code.certificateProcessor.exeption;

public class OSSException extends RuntimeException {
    public OSSException(String message) {
        super(message);
    }
    public OSSException(String message,Throwable cause) {
        super(message, cause);
    }
}
