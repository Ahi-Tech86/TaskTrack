package com.ahicode.config;

import com.ahicode.dtos.ErrorDto;
import com.ahicode.dtos.ValidationErrorDto;
import com.ahicode.exceptions.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler {

    @ResponseBody
    @ExceptionHandler(value = {AppException.class})
    public ResponseEntity<ErrorDto> exceptionHandling(AppException exception) {
        return ResponseEntity
                .status(exception.getHttpStatus())
                .body(
                        ErrorDto.builder()
                                .message(exception.getMessage())
                                .timestamp(exception.getTimestamp())
                                .build()
                );
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorDto> handleValidationExceptions(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ValidationErrorDto validationErrorDto = ValidationErrorDto.builder()
                .message("Validation failed")
                .errors(errors)
                .build();

        return new ResponseEntity<>(validationErrorDto, HttpStatus.BAD_REQUEST);
    }
}
