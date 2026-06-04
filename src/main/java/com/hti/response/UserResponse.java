package com.hti.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.hti.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private UUID organisationId;
    private UUID entityId;
    private LocalDateTime createdAt;
    private String username;
    private Role role;          
    private String status;        
    private LocalDateTime lastLoginAt;  
    private LocalDateTime updatedAt;
}