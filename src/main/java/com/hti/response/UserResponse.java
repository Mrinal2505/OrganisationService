package com.hti.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

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
    private String role;          
    private String status;        
    private LocalDateTime lastLoginAt;  
    private LocalDateTime updatedAt;
}