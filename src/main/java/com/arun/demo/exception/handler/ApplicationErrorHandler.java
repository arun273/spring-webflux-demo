package com.arun.demo.exception.handler;

import com.arun.demo.exception.ServerException;
import com.arun.demo.exception.StudentNotFoundException;
import com.arun.demo.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestControllerAdvice
@Slf4j
public class ApplicationErrorHandler {

    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStudentNotFoundException(StudentNotFoundException e) {
        var errorResponse = this.buildErrorResponse(101, String.format("Student id %s is not found", e.getStudentId()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    @ExceptionHandler(ServerException.class)
    public ResponseEntity<ErrorResponse> handleBackendError(ServerException e) {
        var errorResponse = this.buildErrorResponse(101, "Unable to fetch students from Backend");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }


    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<String> handleWebClientResponseException(WebClientResponseException ex) {
        log.error("Error from WebClient - Status {}, Body {}", ex.getRawStatusCode(), ex.getResponseBodyAsString(), ex);
        return ResponseEntity.status(ex.getRawStatusCode()).body(ex.getResponseBodyAsString());
    }


    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        var errorResponse = this.buildErrorResponse(100, "Unable to fetch students");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }




    private ErrorResponse buildErrorResponse(int code, String message) {
        return new ErrorResponse(code, message);
    }

}