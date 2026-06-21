package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class UserRepositoryTest {

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
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldRegisterandRetrieveUser(){

        User user = new User();
        user.setUsername("Risheek");
        user.setEmail("risheekshrestha@gmail.com");
        user.setPasswordHash("risheek@1234");
        user.setRole(User.Role.USER);

        User currentUser =  userRepository.save(user);

        var found = userRepository.findById(currentUser.getId());

        entityManager.flush();
        entityManager.clear();

        assertTrue(found.isPresent());
        assertEquals("Risheek", found.get().getUsername());
        assertEquals("risheekshrestha@gmail.com", found.get().getEmail());
        assertEquals(User.Role.USER, found.get().getRole());
    }

    @Test
    void shouldFindByEmail(){

        User user = new User();
        user.setUsername("Risheek");
        user.setEmail("risheekshrestha@gmail.com");
        user.setPasswordHash("risheek@1234");
        user.setRole(User.Role.USER);

        User currentUser =  userRepository.save(user);

        entityManager.flush();
        entityManager.clear();

        var found = userRepository.findByEmail(currentUser.getEmail());

        assertTrue(found.isPresent());
        assertEquals("Risheek", found.get().getUsername());
        assertEquals("risheekshrestha@gmail.com", found.get().getEmail());
        assertEquals(User.Role.USER, found.get().getRole());
    }

    @Test
    void shouldExistByEmail(){

        User user = new User();
        user.setUsername("Risheek");
        user.setEmail("risheekshrestha@gmail.com");
        user.setPasswordHash("risheek@1234");
        user.setRole(User.Role.USER);

        User currentUser =  userRepository.save(user);

        entityManager.flush();
        entityManager.clear();

        boolean found = userRepository.existsByEmail(currentUser.getEmail());
        assertTrue(found);
    }

}