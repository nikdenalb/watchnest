package dev.watchnest.plannerapp.cms.api;

import dev.watchnest.plannerapp.api.dto.CsrfTokenResponse;
import dev.watchnest.plannerapp.cms.api.dto.CmsCredentialsRequest;
import dev.watchnest.plannerapp.cms.api.dto.CmsUserResponse;
import dev.watchnest.plannerapp.cms.auth.CmsAuthService;
import dev.watchnest.plannerapp.cms.auth.CmsUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cms/api/v1")
@Tag(name = "CMS Auth", description = "CMS login, session, and CSRF")
public class CmsAuthApiController {

    private final CmsAuthService cmsAuthService;

    public CmsAuthApiController(CmsAuthService cmsAuthService) {
        this.cmsAuthService = cmsAuthService;
    }

    @GetMapping("/csrf")
    @Operation(summary = "Obtain CMS CSRF header name and token for unsafe requests")
    public ResponseEntity<CsrfTokenResponse> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken()));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a provisioned CMS account")
    public CmsUserResponse login(
            @Valid @RequestBody CmsCredentialsRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        CmsUser user = cmsAuthService.login(request.username(), request.password(), httpRequest, httpResponse);
        return new CmsUserResponse(user.id(), user.getUsername());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke the current CMS token if present (idempotent)")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        cmsAuthService.logout(request, response);
    }

    @GetMapping("/me")
    @Operation(summary = "Return the authenticated CMS user")
    public CmsUserResponse me(@AuthenticationPrincipal CmsUser user) {
        return new CmsUserResponse(user.id(), user.getUsername());
    }
}
