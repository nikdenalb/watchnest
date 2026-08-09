package dev.watchnest.plannerapp.identity;

import dev.watchnest.identity.adapter.memory.InMemoryUserAccountRepository;
import dev.watchnest.identity.port.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("memory")
public class MemoryIdentityConfig {

    @Bean
    UserAccountRepository userAccountRepository() {
        return new InMemoryUserAccountRepository();
    }
}
