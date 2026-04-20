package org.com.code.certificateProcessor.pojo.dto.response.adminResponse;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminInfoResponse{
    private String username;
    private String fullName;
    private String auth;
    private Instant createdAt;
    private Instant updatedAt;
}
