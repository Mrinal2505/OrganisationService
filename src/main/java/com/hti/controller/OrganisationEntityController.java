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

import com.hti.request.OrganisationEntityRequest;
import com.hti.request.OrganisationEntityUpdateRequest;
import com.hti.service.OrganisationEntityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Organisation Entity", description = "APIs for managing organisation entities")
@RestController
@RequestMapping("/entities")
@RequiredArgsConstructor
public class OrganisationEntityController {

    private final OrganisationEntityService service;

    @Operation(summary = "Create entity", description = "Creates a new organisation entity — validates attributes strictly against metadata template")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody OrganisationEntityRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Get all entities", description = "Fetch paginated list of entities with optional filters")
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String sortBy,
            @RequestParam(required = false)    String sortDirection,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    Integer priority,
            @RequestParam(required = false)    UUID organisationId,
            @RequestParam(required = false)    Boolean isActive
    ) {
        return service.getAll(page, size, sortBy, sortDirection, search, priority, organisationId, isActive);
    }

    @Operation(summary = "Get entity by ID", description = "Fetch a single entity by ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @Operation(summary = "Get entities by organisation", description = "Fetch all entities belonging to an organisation")
    @GetMapping("/org/{organisationId}")
    public ResponseEntity<?> getByOrganisation(@PathVariable UUID organisationId) {
        return service.getByOrganisation(organisationId);
    }

    @Operation(summary = "Search by attribute", description = "Search entities by a specific attribute key-value pair")
    @GetMapping("/org/{organisationId}/search")
    public ResponseEntity<?> searchByAttribute(
            @PathVariable UUID organisationId,
            @RequestParam String key,
            @RequestParam String value) {
        return service.searchByAttribute(organisationId, key, value);
    }

    @Operation(summary = "Update entity", description = "Update an existing entity — re-validates attributes against metadata template if provided")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(
            @PathVariable UUID id,
            @Valid @RequestBody OrganisationEntityUpdateRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete entity", description = "Delete an entity by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        return service.delete(id);
    }
}