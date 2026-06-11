package com.hti.service;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import com.hti.request.Entitymetadatarequest;
import com.hti.request.Entitymetadataupdaterequest;

public interface Entitymetadataservice {
    ResponseEntity<?> create(Entitymetadatarequest request);
    ResponseEntity<?> update(UUID id, Entitymetadataupdaterequest request);
    ResponseEntity<?> delete(UUID id);
    ResponseEntity<?> getById(UUID id);
    ResponseEntity<?> getAll(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            String search,
            UUID organisationId,
            String entityType,
            Boolean isActive
    );
    ResponseEntity<?> getByOrganisationId(UUID organisationId);
    ResponseEntity<?> getByEntityType(String entityType);
}