package org.com.code.certificateProcessor.pojo.entity;

import lombok.*;
import org.com.code.certificateProcessor.pojo.enums.AwardSubmissionStatus;
import org.com.code.certificateProcessor.pojo.enums.DuplicateCheckResult;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AwardSubmission {
  private long id;

  private String submissionId;
  private String studentId;
  private String imageObjectKey;
  private AwardSubmissionStatus status;
  private Map<String, Object> ocrFullText;
  private String matchedAwardId;
  private Double finalScore;
  private String reason;
  private List<Map<String, Object>> suggestion;
  private String duplicateSubmissionId;
  private DuplicateCheckResult duplicateCheckResult;
  private String reviewedBy;
  private Instant submittedAt;
  private Instant completedAt;
}
