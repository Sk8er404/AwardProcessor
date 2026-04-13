package org.com.code.certificateProcessor.exeption;

public class RocketmqException extends RuntimeException {
    public RocketmqException(String message) {
        super(message);
    }
    public RocketmqException(String message, Throwable cause) {
        super(message, cause);
    }
}
