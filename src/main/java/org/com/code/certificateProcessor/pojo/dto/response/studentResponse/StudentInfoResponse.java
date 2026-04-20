package org.com.code.certificateProcessor.pojo.dto.response.studentResponse;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class StudentInfoResponse{
    private String studentId;
    private String name;
    private String college;
    private String major;
    private Instant createdAt;
    private Instant updatedAt;
    private Double sumOfScore;
}
