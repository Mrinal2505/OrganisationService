package com.hti.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hti.entity.PasswordHistory;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, UUID> {

  
    List<PasswordHistory> findByUserIdOrderByChangedAtDesc(UUID userId);
}