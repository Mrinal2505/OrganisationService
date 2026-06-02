package com.hti.controller;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hti.request.LoginRequest;
import com.hti.request.UserRequest;
import com.hti.request.UserUpdateRequest;
import com.hti.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "User", description = "APIs for managing users")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @Operation(summary = "Create user", description = "Creates a new user")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
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
    
    @Operation(summary = "Check username availability")     // ✅ NEW
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        return service.checkUsernameAvailability(username);
    }

    @Operation(summary = "Login")                           // ✅ NEW
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }
}