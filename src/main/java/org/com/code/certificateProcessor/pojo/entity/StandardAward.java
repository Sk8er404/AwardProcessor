package org.com.code.certificateProcessor.pojo.entity;

import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class StandardAward {

  private long id;
  private String standardAwardId;
  private String name;
  private String category;
  private String level;
  private Double score;
  private Boolean isActive;
  private String createdBy;
  private String updatedBy;
  private Instant createdAt;
  private Instant updatedAt;

  public Map<String, Object> toMap() {
    Map<String, Object> map = new HashMap<>();
    if(standardAwardId != null)
      map.put("standardAwardId", standardAwardId);
    if(name != null)
      map.put("name", name);
    if(category != null)
      map.put("category", category);
    if(level != null)
      map.put("level", level);
    if(score != null)
      map.put("score", score);
    if(isActive != null)
      map.put("isActive", isActive);
    if(createdBy != null)
      map.put("createdBy", createdBy);
    if(updatedBy != null)
      map.put("updatedBy", updatedBy);
    if(createdAt != null)
      map.put("createdAt", createdAt);
    if(updatedAt != null)
      map.put("updatedAt", updatedAt);
    return map;
  }
}
