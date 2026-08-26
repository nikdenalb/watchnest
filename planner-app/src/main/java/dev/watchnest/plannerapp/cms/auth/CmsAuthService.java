package dev.watchnest.plannerapp.cms.auth;

import dev.watchnest.identity.domain.InvalidCredentialsException;
import dev.watchnest.identity.domain.InvalidPasswordException;
import dev.watchnest.identity.domain.InvalidUsernameException;
import dev.watchnest.identity.domain.PasswordRules;
import dev.watchnest.identity.domain.Username;
import dev.watchnest.identity.port.PasswordHasher;
import dev.watchnest.plannerapp.cms.account.CmsAccount;
import dev.watchnest.plannerapp.cms.account.CmsAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CmsAuthService {

    private final CmsAccountRepository accounts;
    private final PasswordHasher passwordHasher;
    private final CmsSessionStore sessions;
    private final boolean cookieSecure;

    public CmsAuthService(
            CmsAccountRepository accounts,
            PasswordHasher passwordHasher,
            CmsSessionStore sessions,
            @Value("${watchnest.session.cookie.secure:false}") boolean cookieSecure
    ) {
        this.accounts = accounts;
        this.passwordHasher = passwordHasher;
        this.sessions = sessions;
        this.cookieSecure = cookieSecure;
    }

    public CmsUser login(String rawUsername, String rawPassword, HttpServletRequest request, HttpServletResponse response) {
        Username username;
        try {
            username = Username.parse(rawUsername);
            PasswordRules.requireValid(rawPassword);
        } catch (InvalidUsernameException | InvalidPasswordException ex) {
            throw new InvalidCredentialsException();
        }

        CmsAccount account = accounts.findByUsername(username.value())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordHasher.verify(rawPassword, account.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        String previous = CmsCookies.read(request, CmsCookies.SESSION_COOKIE);
        if (previous != null) {
            sessions.revoke(previous);
        }
        String token = sessions.create(account.id(), account.username());
        CmsCookies.writeSession(response, token, cookieSecure);
        return new CmsUser(account.id(), account.username());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String token = CmsCookies.read(request, CmsCookies.SESSION_COOKIE);
        if (token != null) {
            sessions.revoke(token);
        }
        CmsCookies.clearSession(response, cookieSecure);
        CmsCookies.clearCsrf(response, cookieSecure);
    }
}
