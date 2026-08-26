package dev.watchnest.plannerapp.persistence.jpa;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile("persistent")
@EnableJpaRepositories(basePackages = {
        "dev.watchnest.plannerapp.persistence.jpa",
        "dev.watchnest.plannerapp.cms.persistence.jpa",
        "dev.watchnest.plannerapp.catalog.persistence.jpa"
})
@EntityScan(basePackages = {
        "dev.watchnest.plannerapp.persistence.jpa",
        "dev.watchnest.plannerapp.cms.persistence.jpa",
        "dev.watchnest.plannerapp.catalog.persistence.jpa"
})
public class JpaPersistenceConfig {
}
