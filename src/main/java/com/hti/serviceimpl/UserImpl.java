package com.hti.serviceimpl;

import org.springframework.data.domain.Pageable;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hti.Repository.OrganisationEntityRepository;
import com.hti.Repository.OrganisationRepository;
import com.hti.Repository.PasswordHistoryRepository;
import com.hti.Repository.PasswordResetTokenRepository;
import com.hti.Repository.UserRepository;
import com.hti.entity.PasswordHistory;
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

    // ── Loggers ──────────────────────────────────────────────────────────────
    private static final Logger logger   = LoggerFactory.getLogger("tracklogger");
    private static final Logger dbLogger = LoggerFactory.getLogger("dblogger");
    // ─────────────────────────────────────────────────────────────────────────

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9]{3,30}$");

    private final UserRepository repository;
    private final OrganisationRepository organisationRepository;
    private final OrganisationEntityRepository organisationEntityRepository;
    private final CryptoUtil cryptoUtil;
    private final PasswordHistoryRepository passwordHistoryRepository;

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

    // =========================================================================
    // CREATE
    // =========================================================================
    @Override
    public ResponseEntity<?> create(UserRequest request) {
        logger.info("Creating user | email={} username={}", request.getEmail(), request.getUsername());

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

            dbLogger.info("DB INSERT | table=users | email={} orgId={}", request.getEmail(), request.getOrganisationId());
            user = repository.save(user);
            dbLogger.info("DB INSERT success | table=users | id={}", user.getId());

            logger.info("User created successfully | id={} email={}", user.getId(), user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));

        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Error creating user | email={}", request.getEmail(), ex);
            throw new InternalServerException("Failed to create user: " + ex.getMessage());
        }
    }

    // =========================================================================
    // UPDATE
    // =========================================================================
    @Override
    public ResponseEntity<?> update(UUID id, UserUpdateRequest request) {
        logger.info("Updating user | id={}", id);

        User user = repository.findById(id).orElseThrow(() -> {
            logger.error("User not found | id={}", id);
            return new NotFoundException("User not found: " + id);
        });

        if (request.getEntityId() != null &&
                !organisationEntityRepository.existsById(request.getEntityId())) {
            throw new NotFoundException("Entity not found: " + request.getEntityId());
        }

        try {
            if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
            if (request.getLastName()  != null) user.setLastName(request.getLastName());
            if (request.getPhone()     != null) user.setPhone(request.getPhone());
            if (request.getEntityId()  != null) user.setEntityId(request.getEntityId());
            if (request.getStatus()    != null) user.setStatus(request.getStatus());

            dbLogger.info("DB UPDATE | table=users | id={}", id);
            user = repository.save(user);
            dbLogger.info("DB UPDATE success | table=users | id={}", user.getId());

            logger.info("User updated successfully | id={}", user.getId());
            return ResponseEntity.ok(toResponse(user));

        } catch (Exception ex) {
            logger.error("Error updating user | id={}", id, ex);
            throw new InternalServerException("Failed to update user: " + ex.getMessage());
        }
    }

    // =========================================================================
    // DELETE
    // =========================================================================
    @Override
    public ResponseEntity<?> delete(UUID id) {
        logger.info("Deleting user | id={}", id);

        if (!repository.existsById(id)) {
            logger.error("User not found | id={}", id);
            throw new NotFoundException("User not found: " + id);
        }

        try {
            dbLogger.info("DB DELETE | table=users | id={}", id);
            repository.deleteById(id);
            dbLogger.info("DB DELETE success | table=users | id={}", id);

            logger.info("User deleted successfully | id={}", id);
            return ResponseEntity.ok("User deleted successfully");

        } catch (Exception ex) {
            logger.error("Error deleting user | id={}", id, ex);
            throw new InternalServerException("Failed to delete user: " + ex.getMessage());
        }
    }

    // =========================================================================
    // GET BY ID
    // =========================================================================
    @Override
    public ResponseEntity<?> getById(UUID id) {
        logger.info("Fetching user | id={}", id);

        User user = repository.findById(id).orElseThrow(() -> {
            logger.error("User not found | id={}", id);
            return new NotFoundException("User not found: " + id);
        });

        logger.info("User fetched successfully | id={}", id);
        return ResponseEntity.ok(toResponse(user));
    }

    // =========================================================================
    // GET ALL
    // =========================================================================
    @Override
    public ResponseEntity<?> getAll(int page, int size, String sortBy, String sortDirection,
                                    String search, UUID organisationId, UUID entityId) {
        logger.info("Fetching all users | page={} size={} sortBy={} sortDir={} search={}",
                page, size, sortBy, sortDirection, search);
        try {
            int safePage = Math.max(page, 0);
            int safeSize = Math.min(Math.max(size, 5), 100);

            Sort.Direction direction = (sortDirection != null && sortDirection.equalsIgnoreCase("asc"))
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";

            Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, sortField));

            Specification<User> spec = buildUserSpec(search, organisationId, entityId);

            dbLogger.info("DB SELECT | table=users | page={} size={} search={}", safePage, safeSize, search);
            Page<User> result = repository.findAll(spec, pageable);
            dbLogger.info("DB SELECT success | table=users | totalElements={}", result.getTotalElements());

            if (result.getTotalElements() == 0) {
                throw new NotFoundException("No users found.");
            }
            if (safePage >= result.getTotalPages() && result.getTotalPages() > 0) {
                throw new NotFoundException(
                        String.format("Page %d not found. Total available pages: %d",
                                safePage + 1, result.getTotalPages()));
            }

            var content = result.getContent().stream().map(this::toResponse).toList();

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
            logger.error("Error fetching users", ex);
            throw new InternalServerException("Failed to fetch users: " + ex.getMessage());
        }
    }

    // =========================================================================
    // GET BY ORGANISATION
    // =========================================================================
    @Override
    public ResponseEntity<?> getByOrganisation(UUID organisationId) {
        logger.info("Fetching users by org | orgId={}", organisationId);

        dbLogger.info("DB SELECT | table=users | orgId={}", organisationId);
        List<UserResponse> list = repository.findByOrganisationId(organisationId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        dbLogger.info("DB SELECT success | table=users | orgId={} count={}", organisationId, list.size());

        if (list.isEmpty()) {
            logger.error("No users found | orgId={}", organisationId);
            throw new NotFoundException("No users found for organisation: " + organisationId);
        }

        logger.info("Users fetched successfully | orgId={} count={}", organisationId, list.size());
        return ResponseEntity.ok(list);
    }

    // =========================================================================
    // GET BY ENTITY
    // =========================================================================
    @Override
    public ResponseEntity<?> getByEntity(UUID entityId) {
        logger.info("Fetching users by entity | entityId={}", entityId);

        dbLogger.info("DB SELECT | table=users | entityId={}", entityId);
        List<UserResponse> list = repository.findByEntityId(entityId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        dbLogger.info("DB SELECT success | table=users | entityId={} count={}", entityId, list.size());

        if (list.isEmpty()) {
            logger.error("No users found | entityId={}", entityId);
            throw new NotFoundException("No users found for entity: " + entityId);
        }

        logger.info("Users fetched successfully | entityId={} count={}", entityId, list.size());
        return ResponseEntity.ok(list);
    }

    // =========================================================================
    // CHECK USERNAME
    // =========================================================================
    @Override
    public ResponseEntity<?> checkUsernameAvailability(String username) {
        logger.info("Checking username availability | username={}", username);

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            logger.error("Invalid username format | username={}", username);
            throw new BadRequestException("Username must be 3-30 alphanumeric characters");
        }

        boolean taken = repository.existsByUsername(username.toLowerCase());
        logger.info("Username check result | username={} taken={}", username, taken);
        return ResponseEntity.ok(Map.of("username", username, "available", !taken));
    }

    // =========================================================================
    // LOGIN
    // =========================================================================
    @Override
    public ResponseEntity<?> login(String encryptedData) {
        logger.info("Login attempt received");

        String json;
        try {
            json = cryptoUtil.decrypt(encryptedData);
        } catch (Exception e) {
            logger.error("Failed to decrypt login data");
            throw new BadRequestException("Invalid encrypted data");
        }

        LoginRequest loginRequest;
        try {
            loginRequest = objectMapper.readValue(json, LoginRequest.class);
        } catch (Exception e) {
            logger.error("Failed to parse login data");
            throw new BadRequestException("Invalid login format");
        }

        User user = repository.findByUsername(loginRequest.getUsername().toLowerCase())
                .orElseThrow(() -> {
                    logger.error("Login failed - user not found | username={}", loginRequest.getUsername());
                    return new BadRequestException("Invalid username or password");
                });

        if (!user.getPassword().equals(loginRequest.getPassword())) {
            logger.error("Login failed - wrong password | username={}", loginRequest.getUsername());
            throw new BadRequestException("Invalid username or password");
        }

        dbLogger.info("DB UPDATE | table=users | action=lastLoginAt | id={}", user.getId());
        user.setLastLoginAt(LocalDateTime.now());
        repository.save(user);
        dbLogger.info("DB UPDATE success | table=users | action=lastLoginAt | id={}", user.getId());

        logger.info("Login successful | userId={} username={}", user.getId(), user.getUsername());
        return ResponseEntity.ok(toResponse(user));
    }

    // =========================================================================
    // CHANGE PASSWORD (send reset link)
    // =========================================================================
    @Override
    public ResponseEntity<?> changePassword(UUID id) {
        logger.info("changePassword | userId={}", id);

        User user = repository.findById(id)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            throw new BadRequestException("No valid email found for this user");
        }

        tokenRepository.deleteByUser(user);

        String rawData    = "{\"userId\":\"" + user.getId() + "\"}";
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

        dbLogger.info("DB INSERT | table=password_reset_token | userId={}", user.getId());
        tokenRepository.save(resetToken);
        dbLogger.info("DB INSERT success | table=password_reset_token | userId={}", user.getId());

        String resetLink = baseUrl + "/users/verify-link?" + encryptData;
        emailService.sendPasswordResetLink(user.getEmail(), user.getUsername(), resetLink);

        logger.info("Password reset link sent | userId={}", id);
        return ResponseEntity.ok("Password reset link sent to your registered email.");
    }

    // =========================================================================
    // VERIFY LINK
    // =========================================================================
    @Override
    public ResponseEntity<?> verifyLink(String encryptData) {
        logger.info("verifyLink called");

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

        User user = repository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        PasswordResetToken resetToken = tokenRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("No reset request found"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(resetToken);
            throw new BadRequestException("Link expired. Please request a new one");
        }

        if (resetToken.isUsed()) {
            throw new BadRequestException("Link already used");
        }

        String otp = String.format("%06d", new SecureRandom().nextInt(999999));
        resetToken.setOtp(otp);
        resetToken.setOtpSent(true);

        dbLogger.info("DB UPDATE | table=password_reset_token | action=setOtp | userId={}", userId);
        tokenRepository.save(resetToken);
        dbLogger.info("DB UPDATE success | table=password_reset_token | userId={}", userId);

        emailService.sendOtpEmail(user.getEmail(), user.getUsername(), otp);

        logger.info("OTP sent successfully | userId={}", userId);
        return ResponseEntity.ok("OTP sent to your registered email.");
    }

    // =========================================================================
    // VERIFY OTP
    // =========================================================================
    @Override
    public ResponseEntity<?> verifyOtp(String encryptData, String otp) {
        logger.info("verifyOtp called");

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

        User user = repository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        PasswordResetToken resetToken = tokenRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("No reset request found"));

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

        if (!resetToken.getOtp().equals(otp)) {
            logger.warn("Invalid OTP | userId={}", userId);
            throw new BadRequestException("Invalid OTP");
        }

        resetToken.setOtpVerified(true);

        dbLogger.info("DB UPDATE | table=password_reset_token | action=otpVerified | userId={}", userId);
        tokenRepository.save(resetToken);
        dbLogger.info("DB UPDATE success | table=password_reset_token | userId={}", userId);

        logger.info("OTP verified successfully | userId={}", userId);
        return ResponseEntity.ok("OTP verified successfully.");
    }

    // =========================================================================
    // RESET PASSWORD
    // =========================================================================
    @Override
    @Transactional
    public ResponseEntity<?> resetPassword(String encryptData, ResetPasswordRequest request) {
        logger.info("resetPassword called");

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

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

        User user = repository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        PasswordResetToken resetToken = tokenRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("No reset request found"));

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

        String oldPassword = user.getPassword();

        dbLogger.info("DB UPDATE | table=users | action=resetPassword | userId={}", userId);
        user.setPassword(request.getNewPassword());
        user.setUpdatedAt(LocalDateTime.now());
        repository.save(user);
        dbLogger.info("DB UPDATE success | table=users | action=resetPassword | userId={}", userId);

        PasswordHistory history = PasswordHistory.builder()
                .userId(user.getId())
                .oldPassword(oldPassword)
                .newPassword(request.getNewPassword())
                .changedAt(LocalDateTime.now())
                .build();

        dbLogger.info("DB INSERT | table=password_history | userId={}", userId);
        passwordHistoryRepository.save(history);
        dbLogger.info("DB INSERT success | table=password_history | userId={}", userId);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        logger.info("Password reset successful | userId={}", userId);
        return ResponseEntity.ok("Password reset successfully.");
    }

    // =========================================================================
    // SPEC BUILDER
    // =========================================================================
    private Specification<User> buildUserSpec(String search, UUID organisationId, UUID entityId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("lastName")),  like),
                        cb.like(cb.lower(root.get("email")),     like),
                        cb.like(cb.lower(root.get("username")),  like),
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

    // =========================================================================
    // MAPPER
    // =========================================================================
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