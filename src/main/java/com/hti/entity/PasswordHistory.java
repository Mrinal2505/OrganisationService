package com.hti.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "password_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "old_password", nullable = false)
    private String oldPassword;

    @Column(name = "new_password", nullable = false)
    private String newPassword;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}