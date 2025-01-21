package com.ahicode.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorDto {
    private String message;
    private Map<String, String> errors;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy  HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonCreator
    public ValidationErrorDto(
            @JsonProperty("message") String message,
            @JsonProperty("timestamp") LocalDateTime timestamp,
            @JsonProperty("errors") Map<String, String> errors
    ) {
        this.message = message;
        this.errors = errors;
        this.timestamp = timestamp;
    }
}
