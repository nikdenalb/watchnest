package dev.watchnest.plannerapp.auth;

import dev.watchnest.identity.domain.AuthenticatedUser;
import dev.watchnest.identity.service.IdentityService;
import dev.watchnest.plannerapp.library.PersonalLibraryStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final IdentityService identityService;
    private final PersonalLibraryStore personalLibraryStore;

    public RegistrationService(
            IdentityService identityService,
            PersonalLibraryStore personalLibraryStore
    ) {
        this.identityService = identityService;
        this.personalLibraryStore = personalLibraryStore;
    }

    @Transactional
    public AuthenticatedUser register(String username, String password) {
        AuthenticatedUser user = identityService.register(username, password);
        personalLibraryStore.ensureProfile(user.id(), user.username());
        return user;
    }
}
