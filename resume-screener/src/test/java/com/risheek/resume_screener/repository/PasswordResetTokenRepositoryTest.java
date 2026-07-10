package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.PasswordResetToken;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.util.RepositoryTestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class PasswordResetTokenRepositoryTest extends RepositoryTestHelper {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldFindByToken() {

        User currentUser = createUser(
                "Risheek",
                "risheekshrestha@gmail.com"
        );

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(currentUser);
        token.setToken("resettoken123");
        token.setExpiryDate(Instant.now().plusSeconds(3600));

        passwordResetTokenRepository.save(token);

        entityManager.flush();
        entityManager.clear();

        var found = passwordResetTokenRepository.findByToken("resettoken123");

        assertTrue(found.isPresent());
        assertEquals("resettoken123", found.get().getToken());
    }

    @Test
    void shouldReturnEmptyWhenTokenNotFound() {

        var found = passwordResetTokenRepository.findByToken("doesnotexist");

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldDeleteByUser() {

        User currentUser = createUser(
                "Risheek",
                "risheekshrestha@gmail.com"
        );

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(currentUser);
        token.setToken("resettoken456");
        token.setExpiryDate(Instant.now().plusSeconds(3600));

        passwordResetTokenRepository.save(token);

        entityManager.flush();
        entityManager.clear();

        User managedUser =
                userRepository.findById(currentUser.getId()).orElseThrow();

        passwordResetTokenRepository.deleteByUser(managedUser);

        entityManager.flush();
        entityManager.clear();

        var remaining =
                passwordResetTokenRepository.findByToken("resettoken456");

        assertTrue(remaining.isEmpty());
    }
}