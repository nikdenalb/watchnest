package dev.watchnest.plannerapp.api;

import dev.watchnest.identity.domain.DuplicateUsernameException;
import dev.watchnest.identity.domain.InvalidCredentialsException;
import dev.watchnest.identity.domain.InvalidPasswordException;
import dev.watchnest.identity.domain.InvalidUsernameException;
import dev.watchnest.plannerapp.api.dto.ApiErrorResponse;
import dev.watchnest.plannerapp.library.LibraryResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidUsernameException.class)
    public ResponseEntity<ApiErrorResponse> invalidUsername(InvalidUsernameException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("validation_failed", exception.getMessage()));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiErrorResponse> invalidPassword(InvalidPasswordException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("validation_failed", exception.getMessage()));
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ApiErrorResponse> duplicateUsername(DuplicateUsernameException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("username_already_exists", "Username is already taken"));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> invalidCredentials(InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse("invalid_credentials", "Invalid credentials"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> validationFailed(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Validation failed");
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("validation_failed", message));
    }

    @ExceptionHandler(LibraryResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> libraryResourceNotFound(LibraryResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("not_found", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> illegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("validation_failed", exception.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> missingRequestParameter(
            MissingServletRequestParameterException exception
    ) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("validation_failed", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> typeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("validation_failed", exception.getMessage()));
    }
}
