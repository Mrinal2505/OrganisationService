package com.hti.controller;

import java.util.UUID;

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

import com.hti.request.OrganisationRequest;
import com.hti.request.OrganisationUpdateRequest;
import com.hti.service.OrganisationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Organisation", description = "APIs for managing organisations")
@RestController
@RequestMapping("/organisations")
@RequiredArgsConstructor
public class OrganisationController {

    private final OrganisationService service;

    @Operation(summary = "Create organisation", description = "Creates a new organisation")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody OrganisationRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Get all organisations", description = "Fetch paginated list with optional filters")
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String sortBy,
            @RequestParam(required = false)    String sortDirection,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    String organizationType,
            @RequestParam(required = false)    String industryType,
            @RequestParam(required = false)    String city,
            @RequestParam(required = false)    String state,
            @RequestParam(required = false)    String country
    ) {
        return service.getAll(page, size, sortBy, sortDirection, search,
                organizationType, industryType, city, state, country);
    }

    @Operation(summary = "Get organisation by ID", description = "Fetch a single organisation by ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @Operation(summary = "Update organisation", description = "Update an existing organisation by ID")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody OrganisationUpdateRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete organisation", description = "Delete an organisation by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        return service.delete(id);
    }
}