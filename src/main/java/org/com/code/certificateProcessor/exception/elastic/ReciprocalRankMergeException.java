package org.com.code.certificateProcessor.exception.elastic;

public class ReciprocalRankMergeException extends RuntimeException {
    public ReciprocalRankMergeException(String errorMessage) {
        super(errorMessage);
    }
    public ReciprocalRankMergeException(String errorMessage, Throwable cause) {
        super(errorMessage,cause);
    }
}
