package org.com.code.certificateProcessor.pojo.dto.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class StandardAwardDocument{
    // 映射到 ES 索引中的 standard_award_id
    @JsonProperty("standard_award_id")
    private String standardAwardId;

    @JsonProperty("award_name")
    private String name;

    @JsonProperty("category")
    private String category;

    @JsonProperty("level")
    private String level;

    @JsonProperty("score")
    private double score;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("updated_by")
    private String updatedBy;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss Z", timezone = "UTC")
    private Instant createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss Z", timezone = "UTC")
    private Instant updatedAt;

    @JsonProperty("name_vector")
    private float[] nameVector;
}
