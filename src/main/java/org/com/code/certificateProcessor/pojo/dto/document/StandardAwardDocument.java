package org.com.code.certificateProcessor.pojo.dto.document;

import lombok.*;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class StandardAwardDocument{
    private long id;
    private String standardAwardId;
    private String name;
    private String category;
    private String level;
    private double score;
    private Boolean isActive;
    private String createdBy;
    private String updatedBy;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    private float[] nameVector;
}
