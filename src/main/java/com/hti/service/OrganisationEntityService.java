package com.hti.service;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import com.hti.request.OrganisationEntityRequest;
import com.hti.request.OrganisationEntityUpdateRequest;

public interface OrganisationEntityService {
    ResponseEntity<?> create(OrganisationEntityRequest request);
    ResponseEntity<?> update(UUID id, OrganisationEntityUpdateRequest request);
    ResponseEntity<?> delete(UUID id);
    ResponseEntity<?> getById(UUID id);
    ResponseEntity<?> getAll(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            String search,
            String entityType,
            Integer priority,
            UUID organisationId
    );
    ResponseEntity<?> getByOrganisation(UUID organisationId);
    ResponseEntity<?> getByEntityType(String entityType);
    ResponseEntity<?> searchByAttribute(UUID organisationId, String key, String value);
}