package org.com.code.certificateProcessor.exception.elastic;

public class ElasticGeneralException extends RuntimeException {
    public ElasticGeneralException(String message) {
        super(message);
    }
    public ElasticGeneralException(String message, Throwable cause) {
        super(message, cause);
    }
}
