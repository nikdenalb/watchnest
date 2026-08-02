package dev.watchnest.plannerapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.watchnest.plannerapp.api.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        boolean csrfFailure = accessDeniedException instanceof InvalidCsrfTokenException
                || accessDeniedException instanceof MissingCsrfTokenException
                || accessDeniedException.getCause() instanceof InvalidCsrfTokenException
                || accessDeniedException.getCause() instanceof MissingCsrfTokenException;

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse body = csrfFailure
                ? new ApiErrorResponse("csrf_invalid", "CSRF token is missing or invalid")
                : new ApiErrorResponse("access_denied", "Access is denied");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
