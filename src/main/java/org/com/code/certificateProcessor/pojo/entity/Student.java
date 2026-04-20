package org.com.code.certificateProcessor.pojo.entity;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Student {
  private long id;
  private String studentId;
  private String password;
  private String name;
  private String college;
  private String major;
  private String auth;
  private Instant createdAt;
  private Instant updatedAt;

  private Double sumOfScore;
}
