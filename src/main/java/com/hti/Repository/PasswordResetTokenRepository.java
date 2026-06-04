package com.hti.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hti.entity.PasswordResetToken;
import com.hti.entity.User;

import jakarta.transaction.Transactional;

@Repository
public interface PasswordResetTokenRepository 
        extends JpaRepository<PasswordResetToken, UUID> {

 
	@Transactional
    void deleteByUser(User user);
    Optional<PasswordResetToken> findByUser(User user);
}