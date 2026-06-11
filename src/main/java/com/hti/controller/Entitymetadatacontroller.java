package com.hti.controller;

import java.util.UUID;

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

import com.hti.request.Entitymetadatarequest;
import com.hti.request.Entitymetadataupdaterequest;
import com.hti.service.Entitymetadataservice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Entity Metadata", description = "APIs for managing entity metadata")
@RestController
@RequestMapping("/entity-metadata")
@RequiredArgsConstructor
public class Entitymetadatacontroller {

    private final Entitymetadataservice service;

    @Operation(summary = "Create entity metadata", description = "Creates a new entity metadata record")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Entitymetadatarequest request) {
        return service.create(request);
    }

    @Operation(summary = "Get all entity metadata", description = "Fetch paginated list of entity metadata with optional filters")
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String sortBy,
            @RequestParam(required = false)    String sortDirection,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    UUID organisationId,
            @RequestParam(required = false)    String entityType,
            @RequestParam(required = false)    Boolean isActive
    ) {
        return service.getAll(page, size, sortBy, sortDirection, search, organisationId, entityType, isActive);
    }

    @Operation(summary = "Get entity metadata by ID", description = "Fetch a single entity metadata record by ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @Operation(summary = "Get metadata by organisation", description = "Fetch all metadata records belonging to a specific organisation")
    @GetMapping("/org/{organisationId}")
    public ResponseEntity<?> getByOrganisationId(@PathVariable UUID organisationId) {
        return service.getByOrganisationId(organisationId);
    }

    @Operation(summary = "Get metadata by entity type", description = "Fetch all metadata records of a specific entity type")
    @GetMapping("/type/{entityType}")
    public ResponseEntity<?> getByEntityType(@PathVariable String entityType) {
        return service.getByEntityType(entityType);
    }

    @Operation(summary = "Update entity metadata", description = "Update an existing entity metadata record by ID")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable UUID id,
            @Valid @RequestBody Entitymetadataupdaterequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete entity metadata", description = "Delete an entity metadata record by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        return service.delete(id);
    }
}