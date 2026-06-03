package com.hti.service;


import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.hti.request.ResetPasswordRequest;
import com.hti.request.UserRequest;
import com.hti.request.UserUpdateRequest;
import com.hti.request.VerifyOtpRequest;

public interface UserService {

    ResponseEntity<?> create(UserRequest request);

    ResponseEntity<?> update(UUID id, UserUpdateRequest request);

    ResponseEntity<?> delete(UUID id);

    ResponseEntity<?> getById(UUID id);

    ResponseEntity<?> getAll(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            String search,
            UUID organisationId,
            UUID entityId
    );

    ResponseEntity<?> getByOrganisation(UUID organisationId);

    ResponseEntity<?> getByEntity(UUID entityId);
    
    ResponseEntity<?> checkUsernameAvailability(String username); 
    ResponseEntity<?> login(String encryptedData);
    
    ResponseEntity<?> changePassword(UUID id);
    ResponseEntity<?> verifyLink(String encryptData);
    ResponseEntity<?> verifyOtp(String encryptData, String otp);
    ResponseEntity<?> resetPassword(String encryptData, ResetPasswordRequest request);
    
}