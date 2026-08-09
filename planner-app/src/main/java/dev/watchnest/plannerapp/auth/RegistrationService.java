package dev.watchnest.plannerapp.auth;

import dev.watchnest.identity.domain.AuthenticatedUser;
import dev.watchnest.identity.service.IdentityService;
import dev.watchnest.plannerapp.library.PersonalLibraryStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class RegistrationService {

    private final IdentityService identityService;
    private final PersonalLibraryStore personalLibraryStore;
    private final ObjectProvider<PlatformTransactionManager> transactionManagers;

    public RegistrationService(
            IdentityService identityService,
            PersonalLibraryStore personalLibraryStore,
            ObjectProvider<PlatformTransactionManager> transactionManagers
    ) {
        this.identityService = identityService;
        this.personalLibraryStore = personalLibraryStore;
        this.transactionManagers = transactionManagers;
    }

    public AuthenticatedUser register(String username, String password) {
        PlatformTransactionManager transactionManager = transactionManagers.getIfAvailable();
        if (transactionManager == null) {
            return doRegister(username, password);
        }
        return new TransactionTemplate(transactionManager)
                .execute(status -> doRegister(username, password));
    }

    private AuthenticatedUser doRegister(String username, String password) {
        AuthenticatedUser user = identityService.register(username, password);
        personalLibraryStore.getOrCreateProfile(user.id(), user.username());
        return user;
    }
}
