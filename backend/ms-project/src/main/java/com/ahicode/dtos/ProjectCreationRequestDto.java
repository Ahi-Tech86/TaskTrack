package com.ahicode.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCreationRequestDto {
    @NotBlank(message = "Project name is mandatory")
    @Size(min = 3, max = 50, message = "Project name must be between 3 and 50 characters")
    @Schema(description = "Project name", example = "My project")
    private String name;

    @NotBlank(message = "Project description is mandatory")
    @Size(min = 3, max = 500, message = "Project description must be between 3 and 500 characters")
    @Schema(description = "Project description", example = "Project description")
    private String description;
    @JsonProperty("start_date")
    @Schema(description = "Date when project start", example = "2023-10-01T12:34:56Z")
    private Instant startDate;
}
