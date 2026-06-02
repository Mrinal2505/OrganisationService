package com.hti.serviceimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hti.Repository.OrganisationEntityRepository;
import com.hti.Repository.OrganisationRepository;
import com.hti.Repository.UserRepository;
import com.hti.entity.User;
import com.hti.exception.BadRequestException;
import com.hti.exception.InternalServerException;
import com.hti.exception.NotFoundException;
import com.hti.request.LoginRequest;
import com.hti.request.UserRequest;
import com.hti.request.UserUpdateRequest;
import com.hti.response.PaginatedResponse;
import com.hti.response.UserResponse;
import com.hti.service.UserService;
import com.hti.util.CryptoUtil;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserImpl.class);
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9]{3,30}$");
    private final UserRepository repository;
    private final OrganisationRepository organisationRepository;          
    private final OrganisationEntityRepository organisationEntityRepository;
    private final CryptoUtil cryptoUtil;

    @Autowired
    private ObjectMapper objectMapper;
    @Override
    public ResponseEntity<?> create(UserRequest request) {
        logger.info("Creating user | email={}", request.getEmail());

        if (repository.existsByEmail(request.getEmail())) {
            logger.error("User already exists | email={}", request.getEmail());
            throw new BadRequestException("User with email '" + request.getEmail() + "' already exists");
        }
        
        if (repository.existsByUsername(request.getUsername().toLowerCase())) {
            logger.error("Username already taken | username={}", request.getUsername());
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }
        
        if (!organisationRepository.existsById(request.getOrganisationId())) {
            logger.error("Organisation not found | orgId={}", request.getOrganisationId());
            throw new NotFoundException("Organisation not found: " + request.getOrganisationId());
        }
        
        if (request.getEntityId() != null && !organisationEntityRepository.existsById(request.getEntityId())) {
            logger.error("Entity not found | entityId={}", request.getEntityId());
            throw new NotFoundException("Entity not found: " + request.getEntityId());
        }

        try {
            User user = User.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .password(request.getPassword())
                    .organisationId(request.getOrganisationId())
                    .entityId(request.getEntityId())
                    .role(request.getRole())       
                    .username(request.getUsername().toLowerCase())
                    .status(request.getStatus()) 
                    .build();

            user = repository.save(user);
            logger.info("User created successfully | id={} email={}", user.getId(), user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));

        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
        	logger.error("Error creating user | email={}", request.getEmail(), ex);
            throw new InternalServerException("Failed to create user: " + ex.getMessage());
        }
    }

  @Override
public ResponseEntity<?> update(UUID id, UserUpdateRequest request) {
    logger.info("Updating user | id={}", id);

 

    User user = repository.findById(id)
            .orElseThrow(() -> {
                logger.error("User not found | id={}", id);
                return new NotFoundException("User not found: " + id);
            });
    
    if (request.getEntityId() != null && 
            !organisationEntityRepository.existsById(request.getEntityId())) {
            throw new NotFoundException("Entity not found: " + request.getEntityId());
        }

    try {
        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());

        if (request.getLastName() != null)
            user.setLastName(request.getLastName());

        if (request.getPhone() != null)
            user.setPhone(request.getPhone());

        if (request.getEntityId() != null)
            user.setEntityId(request.getEntityId());

        if (request.getStatus() != null)
            user.setStatus(request.getStatus());

        user = repository.save(user);
        logger.info("User updated successfully | id={}", user.getId());
        return ResponseEntity.ok(toResponse(user));

    } catch (Exception ex) {
        logger.error("Error updating user | id={}", id, ex);
        throw new InternalServerException("Failed to update user: " + ex.getMessage());
    }
}

    @Override
    public ResponseEntity<?> delete(UUID id) {
        logger.info("Deleting user | id={}", id);

        if (!repository.existsById(id)) {
            logger.error("User not found | id={}", id);
            throw new NotFoundException("User not found: " + id);
        }

        try {
            repository.deleteById(id);
            logger.info("User deleted successfully | id={}", id);
            return ResponseEntity.ok("User deleted successfully");

        } catch (Exception ex) {
        	logger.error("Error deleting user | id={}", id, ex);
            throw new InternalServerException("Failed to delete user: " + ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getById(UUID id) {
        logger.info("Fetching user | id={}", id);

        User user = repository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found | id={}", id);
                    return new NotFoundException("User not found: " + id);
                });

        logger.info("User fetched successfully | id={}", id);
        return ResponseEntity.ok(toResponse(user));
    }

@Override
public ResponseEntity<?> getAll(int page, int size, String sortBy, String sortDirection,
                                  String search, UUID organisationId, UUID entityId) {
    logger.info("Fetching all users | page={}, size={}, sortBy={}, sortDir={}, search={}",
                page, size, sortBy, sortDirection, search);
    try {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 5), 100);

        Sort.Direction direction = (sortDirection != null && sortDirection.equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, sortField));

        Specification<User> spec = buildUserSpec(search, organisationId, entityId);

        Page<User> result = repository.findAll(spec, pageable);

        if (result.getTotalElements() == 0) {
            throw new NotFoundException("No User found.");
        }

        var content = result.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        PaginatedResponse<UserResponse> paginatedData = new PaginatedResponse<>(
                content,
                result.getNumber() + 1,
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        logger.info("Users fetched successfully | totalElements={}", result.getTotalElements());
        return ResponseEntity.ok(paginatedData);

    } catch (NotFoundException ex) {
        throw ex;
    } catch (Exception ex) {
        logger.error("Error fetching all users", ex);
        throw new InternalServerException("Failed to fetch users: " + ex.getMessage());
    }
}

private Specification<User> buildUserSpec(
        String search,
        UUID organisationId,
        UUID entityId) {

    return (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();

      
        if (search != null && !search.isBlank()) {
            String like = "%" + search.toLowerCase() + "%";
            predicates.add(cb.or(
                cb.like(cb.lower(root.get("firstName")), like),
                cb.like(cb.lower(root.get("lastName")),  like),
                cb.like(cb.lower(root.get("email")),     like),
                cb.like(cb.lower(root.get("phone")),     like)
            ));
        }
        if (organisationId != null)
            predicates.add(cb.equal(root.get("organisationId"), organisationId));

        if (entityId != null)
            predicates.add(cb.equal(root.get("entityId"), entityId));

        return cb.and(predicates.toArray(new Predicate[0]));
    };
}
    @Override
    public ResponseEntity<?> getByOrganisation(UUID organisationId) {
        logger.info("Fetching users by org | orgId={}", organisationId);

        List<UserResponse> list = repository.findByOrganisationId(organisationId)
                .stream().map(this::toResponse).collect(Collectors.toList());

        if (list.isEmpty()) {
            logger.error("No users found | orgId={}", organisationId);
            throw new NotFoundException("No users found for organisation: " + organisationId);
        }

        logger.info("Users fetched successfully | orgId={} count={}", organisationId, list.size());
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<?> getByEntity(UUID entityId) {
        logger.info("Fetching users by entity | entityId={}", entityId);

        List<UserResponse> list = repository.findByEntityId(entityId)
                .stream().map(this::toResponse).collect(Collectors.toList());

        if (list.isEmpty()) {
            logger.error("No users found | entityId={}", entityId);
            throw new NotFoundException("No users found for entity: " + entityId);
        }

        logger.info("Users fetched successfully | entityId={} count={}", entityId, list.size());
        return ResponseEntity.ok(list);
    }
    
    @Override
    public ResponseEntity<?> checkUsernameAvailability(String username) {
        logger.info("Checking username | username={}", username);

        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            logger.warn("Invalid username format | username={}", username);
            throw new BadRequestException(
                    "Username must be alphanumeric only, between 3 to 30 characters"
            );
        }

        boolean taken = repository.existsByUsername(username.toLowerCase());
        logger.info("Username check done | username={} available={}", username, !taken);

        return ResponseEntity.ok(Map.of(
                "username",  username,
                "available", !taken,
                "message",   taken ? "Username is already taken" : "Username is available"
        ));
    }
    
    @Override
    public ResponseEntity<?> login(String encryptedData) {
        
        // 1. Decrypt
        String json;
        try {
            json = cryptoUtil.decrypt(encryptedData);
            logger.info("Login attempt | decrypted json={}", json);
        } catch (Exception e) {
            logger.warn("Login decryption failed: {}", e.getMessage());
            throw new BadRequestException("Invalid encrypted request");
        }

        LoginRequest request;
        try {
            request = objectMapper.readValue(json, LoginRequest.class);
        } catch (Exception e) {
            throw new BadRequestException("Malformed login payload");
        }

        User user = repository
                .findByUsername(request.getUsername().toLowerCase())
                .orElseThrow(() -> {
                    logger.warn("Login failed - not found | username={}", request.getUsername());
                    return new BadRequestException("Invalid username or password");
                });

        if (!request.getPassword().equals(user.getPassword())) {
            logger.warn("Login failed - wrong password | username={}", request.getUsername());
            throw new BadRequestException("Invalid username or password");
        }

       
        user.setLastLoginAt(LocalDateTime.now());
        repository.save(user);

        logger.info("Login successful | username={} id={}", request.getUsername(), user.getId());
        return ResponseEntity.ok(toResponse(user));
    }
    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .organisationId(user.getOrganisationId())
                .entityId(user.getEntityId())
                .createdAt(user.getCreatedAt())
                .role(user.getRole())       
                .username(user.getUsername())
                .status(user.getStatus())          
                .lastLoginAt(user.getLastLoginAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}