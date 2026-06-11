package com.hti.service;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import com.hti.request.OrganisationRequest;
import com.hti.request.OrganisationUpdateRequest;

public interface OrganisationService {
    ResponseEntity<?> create(OrganisationRequest request);
    ResponseEntity<?> update(UUID id, OrganisationUpdateRequest request);
    ResponseEntity<?> delete(UUID id);
    ResponseEntity<?> getById(UUID id);
    ResponseEntity<?> getAll(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            String search,
            String organisationType,
            String industryType,
            String city,
            String state,
            String country,
            Boolean isActive
    );
}