package com.hti.serviceimpl;

import java.security.SecureRandom;
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
import org.springframework.beans.factory.annotation.Value; // ← CORRECT
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hti.Repository.OrganisationEntityRepository;
import com.hti.Repository.OrganisationRepository;
import com.hti.Repository.PasswordResetTokenRepository;
import com.hti.Repository.UserRepository;
import com.hti.entity.PasswordResetToken;
import com.hti.entity.User;
import com.hti.exception.BadRequestException;
import com.hti.exception.InternalServerException;
import com.hti.exception.NotFoundException;
import com.hti.request.LoginRequest;
import com.hti.request.ResetPasswordRequest;
import com.hti.request.UserRequest;
import com.hti.request.UserUpdateRequest;
import com.hti.response.PaginatedResponse;
import com.hti.response.UserResponse;
import com.hti.service.EmailService;
import com.hti.service.UserService;
import com.hti.util.CryptoUtil;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
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
    
    @Value("${app.reset-password.expiry-minutes}")
    private int expiryMinutes;

    @Value("${app.reset-password.base-url}")
    private String baseUrl;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

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
    
//step -1 
   @Override
public ResponseEntity<?> changePassword(UUID id) {
    logger.info("changePassword | userId={}", id);

    User user = repository.findById(id)
            .orElseThrow(() -> new BadRequestException("User not found"));

    if (user.getEmail() == null || !user.getEmail().contains("@")) {
        throw new BadRequestException("No valid email found for this user");
    }

    tokenRepository.deleteByUser(user);

    // encryptData — user info encrypt karo
    String rawData = "{\"userId\":\"" + user.getId() + "\"}";
    String encryptData = cryptoUtil.encrypt(rawData);

    PasswordResetToken resetToken = PasswordResetToken.builder()
            .otp(null)
            .otpSent(false)
            .otpVerified(false)
            .user(user)
            .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
            .used(false)
            .createdAt(LocalDateTime.now())
            .build();

    tokenRepository.save(resetToken);

    // Link mein encryptData jaayega token ki jagah
    String resetLink = baseUrl + "/users/verify-link?encryptData=" + encryptData;
    emailService.sendPasswordResetLink(user.getEmail(), user.getUsername(), resetLink);

    return ResponseEntity.ok("Password reset link sent to your registered email.");
}

    // ── Step 2 ──────────────────────────────────────────────────────
  @Override
public ResponseEntity<?> verifyLink(String encryptData) {
    logger.info("verifyLink | encryptData={}", encryptData);

    // 1. Decrypt karke userId nikalo
    String json;
    try {
        json = cryptoUtil.decrypt(encryptData);
    } catch (Exception e) {
        throw new BadRequestException("Invalid or expired link");
    }

    UUID userId;
    try {
        JsonNode node = objectMapper.readTree(json);
        userId = UUID.fromString(node.get("userId").asText());
    } catch (Exception e) {
        throw new BadRequestException("Malformed link data");
    }

    // 2. User dhundo
    User user = repository.findById(userId)
            .orElseThrow(() -> new BadRequestException("User not found"));

    // 3. Token dhundo
    PasswordResetToken resetToken = tokenRepository.findByUser(user)
            .orElseThrow(() -> new BadRequestException("No reset request found"));

    // 4. Expiry check
    if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
        tokenRepository.delete(resetToken);
        throw new BadRequestException("Link expired. Please request a new one");
    }

    if (resetToken.isUsed()) {
        throw new BadRequestException("Link already used");
    }

    // 5. OTP generate karo
    String otp = String.format("%06d", new SecureRandom().nextInt(999999));
    resetToken.setOtp(otp);
    resetToken.setOtpSent(true);
    tokenRepository.save(resetToken);

    // 6. OTP email bhejo
    emailService.sendOtpEmail(user.getEmail(), user.getUsername(), otp);

    return ResponseEntity.ok("OTP sent to your registered email.");
}

    // ── Step 3 ──────────────────────────────────────────────────────
   @Override
public ResponseEntity<?> verifyOtp(String encryptData, String otp) {
    logger.info("verifyOtp");

    // 1. Decrypt
    String json;
    try {
        json = cryptoUtil.decrypt(encryptData);
    } catch (Exception e) {
        throw new BadRequestException("Invalid or expired link");
    }

    UUID userId;
    try {
        JsonNode node = objectMapper.readTree(json);
        userId = UUID.fromString(node.get("userId").asText());
    } catch (Exception e) {
        throw new BadRequestException("Malformed link data");
    }

    // 2. User aur token dhundo
    User user = repository.findById(userId)
            .orElseThrow(() -> new BadRequestException("User not found"));

    PasswordResetToken resetToken = tokenRepository.findByUser(user)
            .orElseThrow(() -> new BadRequestException("No reset request found"));

    // 3. Checks
    if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
        tokenRepository.delete(resetToken);
        throw new BadRequestException("Token expired. Please request a new one");
    }

    if (resetToken.isUsed()) {
        throw new BadRequestException("Token already used");
    }

    if (!resetToken.isOtpSent()) {
        throw new BadRequestException("OTP not generated. Please click the reset link first");
    }

    // 4. OTP match
    if (!resetToken.getOtp().equals(otp)) {
        logger.warn("Invalid OTP | userId={}", userId);
        throw new BadRequestException("Invalid OTP");
    }

    resetToken.setOtpVerified(true);
    tokenRepository.save(resetToken);

    return ResponseEntity.ok("OTP verified successfully.");
}

    // ── Step 4 ──────────────────────────────────────────────────────
  @Override
@Transactional
public ResponseEntity<?> resetPassword(String encryptData, ResetPasswordRequest request) {
    logger.info("resetPassword");

    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
        throw new BadRequestException("Passwords do not match");
    }

    // 1. Decrypt
    String json;
    try {
        json = cryptoUtil.decrypt(encryptData);
    } catch (Exception e) {
        throw new BadRequestException("Invalid or expired link");
    }

    UUID userId;
    try {
        JsonNode node = objectMapper.readTree(json);
        userId = UUID.fromString(node.get("userId").asText());
    } catch (Exception e) {
        throw new BadRequestException("Malformed link data");
    }

    // 2. User aur token dhundo
    User user = repository.findById(userId)
            .orElseThrow(() -> new BadRequestException("User not found"));

    PasswordResetToken resetToken = tokenRepository.findByUser(user)
            .orElseThrow(() -> new BadRequestException("No reset request found"));

    // 3. Checks
    if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
        tokenRepository.delete(resetToken);
        throw new BadRequestException("Token expired. Please request a new one");
    }

    if (!resetToken.isOtpVerified()) {
        throw new BadRequestException("OTP not verified. Please verify OTP first");
    }

    if (resetToken.isUsed()) {
        throw new BadRequestException("Token already used");
    }

    // 4. Password update
    user.setPassword(request.getNewPassword());
    user.setUpdatedAt(LocalDateTime.now());
    repository.save(user);

    resetToken.setUsed(true);
    tokenRepository.save(resetToken);

    logger.info("Password reset successful | userId={}", userId);
    return ResponseEntity.ok("Password reset successfully.");
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