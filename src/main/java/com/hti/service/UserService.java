package com.hti.service;


import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.hti.request.UserRequest;
import com.hti.request.UserUpdateRequest;

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
}