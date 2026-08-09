package dev.watchnest.plannerapp.persistence;

import dev.watchnest.identity.domain.DuplicateUsernameException;
import dev.watchnest.identity.domain.UserAccount;
import dev.watchnest.identity.domain.Username;
import dev.watchnest.identity.port.UserAccountRepository;
import dev.watchnest.plannerapp.persistence.jpa.UserAccountEntity;
import dev.watchnest.plannerapp.persistence.jpa.UserAccountJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Profile("persistent")
public class JpaUserAccountRepository implements UserAccountRepository {

    private static final String USERNAME_UK = "uk_user_account_username";

    private final UserAccountJpaRepository jpa;

    public JpaUserAccountRepository(UserAccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void insert(UserAccount account) {
        UserAccountEntity entity = new UserAccountEntity(
                account.id(),
                account.username().value(),
                account.passwordHash(),
                account.createdAt()
        );
        try {
            jpa.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            if (isUsernameUniqueViolation(ex)) {
                throw new DuplicateUsernameException(account.username().value());
            }
            throw ex;
        }
    }

    @Override
    public Optional<UserAccount> findByUsername(Username username) {
        return jpa.findByUsername(username.value()).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        return new UserAccount(
                entity.getId(),
                Username.parse(entity.getUsername()),
                entity.getPasswordHash(),
                entity.getCreatedAt()
        );
    }

    private static boolean isUsernameUniqueViolation(DataIntegrityViolationException ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null && message.toLowerCase().contains(USERNAME_UK)) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }
}
