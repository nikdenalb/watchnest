package dev.watchnest.plannerapp.identity;

import dev.watchnest.identity.adapter.memory.InMemoryUserAccountRepository;
import dev.watchnest.identity.port.IdentityEventPublisher;
import dev.watchnest.identity.port.PasswordHasher;
import dev.watchnest.identity.port.UserAccountRepository;
import dev.watchnest.identity.service.IdentityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

@Configuration
public class IdentityConfig {

    @Bean
    UserAccountRepository userAccountRepository() {
        return new InMemoryUserAccountRepository();
    }

    @Bean
    IdentityService identityService(
            UserAccountRepository userAccountRepository,
            PasswordHasher passwordHasher,
            IdentityEventPublisher identityEventPublisher,
            Clock clock
    ) {
        Supplier<UUID> idGenerator = UUID::randomUUID;
        return new IdentityService(
                userAccountRepository,
                passwordHasher,
                identityEventPublisher,
                clock,
                idGenerator
        );
    }
}
