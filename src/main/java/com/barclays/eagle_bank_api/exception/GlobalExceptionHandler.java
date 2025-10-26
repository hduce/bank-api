package com.barclays.eagle_bank_api.exception;

import com.barclays.eagle_bank_api.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
    var error = new ErrorResponse();
    error.setMessage(ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }
}
