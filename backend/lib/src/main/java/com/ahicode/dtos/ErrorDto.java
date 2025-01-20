package com.ahicode.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorDto {

    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy  HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonCreator
    public ErrorDto(
            @JsonProperty("message") String message,
            @JsonProperty("timestamp") LocalDateTime timestamp
    ) {
        this.message = message;
        this.timestamp = timestamp;
    }
}
