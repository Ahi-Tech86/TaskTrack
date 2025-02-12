package com.ahicode.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    private String name;
    private String description;
    @JsonProperty("create_at")
    private Instant createAt;
    @JsonProperty("start_date")
    private Instant startDate;
}
