package dev.watchnest.plannerapp.cms.api;

import dev.watchnest.catalog.domain.CatalogTitleNotFoundException;
import dev.watchnest.catalog.domain.DuplicateCatalogTitleException;
import dev.watchnest.catalog.domain.InvalidCatalogTitleException;
import dev.watchnest.plannerapp.api.dto.ApiErrorResponse;
import dev.watchnest.plannerapp.cms.api.dto.CatalogTitleResponse;
import dev.watchnest.plannerapp.cms.api.dto.DuplicateCatalogTitleResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "dev.watchnest.plannerapp.cms")
public class CmsApiExceptionHandler {

    @ExceptionHandler(CmsDemoAccountException.class)
    public ResponseEntity<ApiErrorResponse> demoAccount(CmsDemoAccountException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse("demo_account", exception.getMessage()));
    }

    @ExceptionHandler(InvalidCatalogTitleException.class)
    public ResponseEntity<ApiErrorResponse> invalidTitle(InvalidCatalogTitleException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("validation_failed", exception.getMessage()));
    }

    @ExceptionHandler(CatalogTitleNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> titleNotFound(CatalogTitleNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("not_found", "Title not found"));
    }

    @ExceptionHandler(DuplicateCatalogTitleException.class)
    public ResponseEntity<DuplicateCatalogTitleResponse> duplicateTitle(DuplicateCatalogTitleException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new DuplicateCatalogTitleResponse(
                        "title_already_exists",
                        "A title with the same English name, year, and type already exists",
                        CatalogTitleResponse.from(exception.existingTitle())
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> unreadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("validation_failed", "Request body is invalid"));
    }
}
