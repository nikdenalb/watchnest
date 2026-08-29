package dev.watchnest.plannerapp.api;

import dev.watchnest.identity.domain.AuthenticatedUser;
import dev.watchnest.plannerapp.api.dto.AuthCredentialsRequest;
import dev.watchnest.plannerapp.api.dto.CsrfTokenResponse;
import dev.watchnest.plannerapp.api.dto.CurrentUserResponse;
import dev.watchnest.plannerapp.auth.AuthSessionService;
import dev.watchnest.plannerapp.auth.WatchNestUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Registration, login, session, and CSRF")
public class AuthApiController {

    private final AuthSessionService authSessionService;

    public AuthApiController(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    @GetMapping("/csrf")
    @Operation(summary = "Obtain CSRF header name and token for unsafe requests")
    public ResponseEntity<CsrfTokenResponse> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken()));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a username/password account and create a session")
    public CurrentUserResponse register(
            @Valid @RequestBody AuthCredentialsRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthenticatedUser user = authSessionService.register(
                request.username(),
                request.password(),
                httpRequest,
                httpResponse
        );
        return new CurrentUserResponse(user.id(), user.username());
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and create a session")
    public CurrentUserResponse login(
            @Valid @RequestBody AuthCredentialsRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthenticatedUser user = authSessionService.login(
                request.username(),
                request.password(),
                httpRequest,
                httpResponse
        );
        return new CurrentUserResponse(user.id(), user.username());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Invalidate the current session if present (idempotent)")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.setInvalidateHttpSession(true);
        logoutHandler.setClearAuthentication(true);
        logoutHandler.logout(request, response, authentication);
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("JSESSIONID".equals(cookie.getName())) {
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    cookie.setValue("");
                    response.addCookie(cookie);
                }
            }
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Return the authenticated user")
    public CurrentUserResponse me(@AuthenticationPrincipal WatchNestUser user) {
        return new CurrentUserResponse(user.id(), user.getUsername());
    }
}
