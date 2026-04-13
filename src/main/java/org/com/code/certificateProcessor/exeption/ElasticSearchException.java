package org.com.code.certificateProcessor.exeption;

public class ElasticSearchException extends RuntimeException {
    public ElasticSearchException(String message) {
        super(message);
    }
    public ElasticSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
