package dev.watchnest.plannerapp.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/do-not-use-local-watchnest")
@AutoConfigureMockMvc
@ActiveProfiles("persistent")
@Testcontainers
public abstract class PostgresHttpTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected static String uniqueUsername(String prefix) {
        String candidate = prefix + UUID.randomUUID().toString().replace("-", "");
        return candidate.substring(0, Math.min(32, candidate.length()));
    }

    protected void deleteCmsAccountsAndCatalogTitles() {
        jdbcTemplate.update("delete from catalog_title");
        jdbcTemplate.update("delete from cms_account");
    }
}
