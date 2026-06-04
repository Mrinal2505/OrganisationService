package com.hti.controller;


import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hti.request.ResetPasswordRequest;
import com.hti.request.UserRequest;
import com.hti.request.UserUpdateRequest;
import com.hti.service.UserService;
import com.hti.util.CryptoUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "User", description = "APIs for managing users")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor

public class UserController {
	 @Autowired
	    private CryptoUtil cryptoUtil;

	
    private final UserService service;

    @Operation(summary = "Create user", description = "Creates a new user")
    @PostMapping()
    public ResponseEntity<?> create(@Valid @RequestBody UserRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Get all users", description = "Fetch paginated list of users with optional filters")
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String sortBy,
            @RequestParam(required = false)    String sortDirection,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    UUID organisationId,  
            @RequestParam(required = false)    UUID entityId         
    ) {
        return service.getAll(page, size, sortBy, sortDirection, search, organisationId, entityId);
    }

    @Operation(summary = "Get user by ID", description = "Fetch a single user by ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@RequestParam UUID id) {        
        return service.getById(id);
    }

    @Operation(summary = "Get users by organisation", description = "Fetch all users belonging to an organisation")
    @GetMapping("/org/{organisationId}")
    public ResponseEntity<?> getByOrganisation(@RequestParam UUID organisationId) {  
        return service.getByOrganisation(organisationId);
    }

    @Operation(summary = "Get users by entity", description = "Fetch all users belonging to a specific entity")
    @GetMapping("/entity/{entityId}")
    public ResponseEntity<?> getByEntity(@RequestParam UUID entityId) { 
        return service.getByEntity(entityId);
    }

    @Operation(summary = "Update user", description = "Update an existing user by ID")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(
    		@RequestParam UUID id,                                   
            @Valid @RequestBody UserUpdateRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete user", description = "Delete a user by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestParam UUID id) {       
        return service.delete(id);
    }
    
    @Operation(summary = "Check username availability")     
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        return service.checkUsernameAvailability(username);
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", 
               description = "Accepts AES encrypted JSON containing username and password. Decrypts and authenticates the user.")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        return service.login(body.get("encryptedData"));
    }

    @GetMapping("/{id}/change-password")
    @Operation(summary = "Request Password Reset", 
               description = "Generates a password reset link and sends it to the user's registered email address.")
    public ResponseEntity<?> changePassword(@PathVariable UUID id) {
        return service.changePassword(id);
    }

    @GetMapping("/verify-link")
    @Operation(summary = "Verify Reset Link", 
               description = "Validates the reset link from email. Generates a 6-digit OTP and sends it to the user's email.")
    public ResponseEntity<?> verifyLink(@RequestParam String encryptData) {
        return service.verifyLink(encryptData);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", 
               description = "Verifies the 6-digit OTP received on email. Must be called after verify-link.")
    public ResponseEntity<?> verifyOtp(
            @RequestParam String encryptData,
            @RequestParam String otp) {
        return service.verifyOtp(encryptData, otp);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset Password", 
               description = "Resets the user password after successful OTP verification. Requires newPassword and confirmPassword to match.")
    public ResponseEntity<?> resetPassword(
            @RequestParam String encryptData,
            @RequestBody @Valid ResetPasswordRequest request) {
        return service.resetPassword(encryptData, request);
    }

    
    @PostMapping("/encrypt")
    @Operation(summary = "Encrypt Data", 
               description = "Utility endpoint to encrypt plain text using AES. Use this to generate encryptedData for login and other encrypted APIs. For testing purposes only.")
    public ResponseEntity<?> encrypt(@RequestBody Map<String, String> body) {
        String encrypted = cryptoUtil.encrypt(body.get("text"));
        return ResponseEntity.ok(Map.of("encryptedData", encrypted));
    }
  
}