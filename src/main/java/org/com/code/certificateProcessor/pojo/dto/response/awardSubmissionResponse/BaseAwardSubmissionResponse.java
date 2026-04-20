package org.com.code.certificateProcessor.pojo.dto.response.awardSubmissionResponse;

import lombok.*;
import org.com.code.certificateProcessor.pojo.enums.AwardSubmissionStatus;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BaseAwardSubmissionResponse {
    protected String submissionId;
    protected String studentId;
    protected String temporaryImageURL;
    protected AwardSubmissionStatus status;
    protected Map<String, Object> ocrFullText;
    protected String matchedAwardId;
    protected Double finalScore;
    protected String reason;
    protected String duplicateCheckResult;
    protected String reviewedBy;
    protected Instant submittedAt;
    protected Instant completedAt;
}
