package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.PasswordResetToken;
import com.risheek.resume_screener.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
    Optional<PasswordResetToken> findByToken(String token);

    @Transactional
    void deleteByUser(User user);
}
