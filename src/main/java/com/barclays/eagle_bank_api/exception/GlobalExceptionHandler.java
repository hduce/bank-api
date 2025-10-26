package com.barclays.eagle_bank_api.exception;

import com.barclays.eagle_bank_api.model.BadRequestErrorResponse;
import com.barclays.eagle_bank_api.model.BadRequestErrorResponseDetailsInner;
import com.barclays.eagle_bank_api.model.ErrorResponse;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
    var error = new ErrorResponse();
    error.setMessage(ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(UserEmailAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleUserEmailAlreadyExists(
      UserEmailAlreadyExistsException ex) {
    var error = new ErrorResponse();
    error.setMessage(ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<BadRequestErrorResponse> handleValidationErrors(
      MethodArgumentNotValidException ex) {
    var details = new ArrayList<BadRequestErrorResponseDetailsInner>();

    ex.getBindingResult()
        .getFieldErrors()
        .forEach(
            error -> {
              var detail = new BadRequestErrorResponseDetailsInner();
              detail.setField(error.getField());
              detail.setMessage(error.getDefaultMessage());
              detail.setType("validation_error");
              details.add(detail);
            });

    var response = new BadRequestErrorResponse();
    response.setMessage("Request validation failed");
    response.setDetails(details);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<BadRequestErrorResponse> handleMessageNotReadable(
      HttpMessageNotReadableException ex) {
    var response = new BadRequestErrorResponse();
    response.setMessage("Malformed JSON request");
    var detail = new BadRequestErrorResponseDetailsInner();
    detail.setField("request_body");
    detail.setMessage(ex.getMostSpecificCause().getMessage());
    detail.setType("malformed_json");
    response.getDetails().add(detail);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(AccountNumberGenerationException.class)
  public ResponseEntity<ErrorResponse> handleAccountNumberGeneration(
      AccountNumberGenerationException ex) {
    var error = new ErrorResponse();
    error.setMessage(ex.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  @ExceptionHandler(AccountNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
    var error = new ErrorResponse();
    error.setMessage(ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(AccountAccessForbiddenException.class)
  public ResponseEntity<ErrorResponse> handleAccountAccessForbidden(
      AccountAccessForbiddenException ex) {
    var error = new ErrorResponse();
    error.setMessage(ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentials() {
    var error = new ErrorResponse();
    error.setMessage("Invalid username or password");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAuthorizationDenied() {
    var error = new ErrorResponse();
    error.setMessage("You do not have permission to access this resource");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @ExceptionHandler(CannotDeleteUserWithAccountsException.class)
  public ResponseEntity<ErrorResponse> handleCannotDeleteUserWithAccounts(
      CannotDeleteUserWithAccountsException ex) {
    var error = new ErrorResponse();
    error.setMessage(ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException() {
    var error = new ErrorResponse();
    error.setMessage("An unexpected error occurred");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
