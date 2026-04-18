package org.com.code.certificateProcessor.exception;

public class AwardSubmissionException extends RuntimeException {
    public AwardSubmissionException(String message) {
        super(message);
    }
    public AwardSubmissionException(String message, Throwable cause) {
        super(message, cause);
    }
}
