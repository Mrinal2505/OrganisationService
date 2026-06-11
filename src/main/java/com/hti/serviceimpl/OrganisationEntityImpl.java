package com.hti.serviceimpl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.hti.Repository.EntityMetadataRepository;
import com.hti.Repository.OrganisationEntityRepository;
import com.hti.Repository.OrganisationRepository;
import com.hti.entity.EntityMetadata;
import com.hti.entity.OrganisationEntity;
import com.hti.exception.BadRequestException;
import com.hti.exception.InternalServerException;
import com.hti.exception.NotFoundException;
import com.hti.mapper.OrganisationEntityMapper;
import com.hti.request.OrganisationEntityRequest;
import com.hti.request.OrganisationEntityUpdateRequest;
import com.hti.response.OrganisationEntityResponse;
import com.hti.response.PaginatedResponse;
import com.hti.service.OrganisationEntityService;
import com.hti.specification.OrganisationEntitySpecification;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganisationEntityImpl implements OrganisationEntityService {

    private static final Logger logger   = LoggerFactory.getLogger("tracklogger");
    private static final Logger dbLogger = LoggerFactory.getLogger("dblogger");

    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final String SORT_ASC           = "asc";

    private final OrganisationEntityRepository repository;
    private final OrganisationRepository organisationRepository;
    private final EntityMetadataRepository entityMetadataRepository;

    // ----------------------------------------------------------------
    // JSON Attribute Validation against Entity Metadata Template
    // ----------------------------------------------------------------
    private void validateAttributes(Map<String, Object> template, Map<String, Object> incoming) {

        if (template == null || template.isEmpty()) {
            logger.error("Metadata template has no defined keys");
            throw new BadRequestException("Metadata template has no defined keys to validate against");
        }

        if (incoming == null || incoming.isEmpty()) {
            logger.error("Attributes cannot be null or empty");
            throw new BadRequestException("Attributes cannot be null or empty");
        }

        Set<String> expectedKeys = template.keySet();
        Set<String> incomingKeys = incoming.keySet();

        // Check for missing keys
        Set<String> missingKeys = expectedKeys.stream()
                .filter(k -> !incomingKeys.contains(k))
                .collect(Collectors.toSet());
        if (!missingKeys.isEmpty()) {
            logger.error("Attributes validation failed – missing keys | missing={}", missingKeys);
            throw new BadRequestException("Missing required attribute keys: " + missingKeys);
        }

        // Check for extra keys
        Set<String> extraKeys = incomingKeys.stream()
                .filter(k -> !expectedKeys.contains(k))
                .collect(Collectors.toSet());
        if (!extraKeys.isEmpty()) {
            logger.error("Attributes validation failed – extra keys | extra={}", extraKeys);
            throw new BadRequestException("Unexpected attribute keys not defined in metadata template: " + extraKeys);
        }

        // Check for null or empty values
        List<String> emptyValueKeys = incomingKeys.stream()
                .filter(k -> {
                    Object val = incoming.get(k);
                    return val == null || val.toString().trim().isEmpty();
                })
                .collect(Collectors.toList());
        if (!emptyValueKeys.isEmpty()) {
            logger.error("Attributes validation failed – empty values | keys={}", emptyValueKeys);
            throw new BadRequestException("Attribute values cannot be null or empty for keys: " + emptyValueKeys);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> create(OrganisationEntityRequest request) {
        logger.info("Creating entity | orgId={} metaId={}", request.getOrganisationId(), request.getMetaId());
        dbLogger.info("DB check – organisation exists | orgId={}", request.getOrganisationId());

        if (!organisationRepository.existsById(request.getOrganisationId())) {
            logger.error("Organisation not found | orgId={}", request.getOrganisationId());
            dbLogger.error("DB check – organisation not found | orgId={}", request.getOrganisationId());
            throw new NotFoundException("Organisation not found: " + request.getOrganisationId());
        }

        dbLogger.info("DB check – entity metadata exists | metaId={}", request.getMetaId());
        EntityMetadata entityMetadata = entityMetadataRepository.findById(request.getMetaId())
                .orElseThrow(() -> {
                    logger.error("Entity metadata not found | metaId={}", request.getMetaId());
                    dbLogger.error("DB check – entity metadata not found | metaId={}", request.getMetaId());
                    return new NotFoundException("Entity metadata not found: " + request.getMetaId());
                });

        logger.info("Validating attributes against metadata template | metaId={}", request.getMetaId());
        validateAttributes(entityMetadata.getMetadata(), request.getAttributes());
        logger.info("Attributes validation passed | metaId={}", request.getMetaId());

        try {
            OrganisationEntity entity = OrganisationEntity.builder()
                    .organisationId(request.getOrganisationId())
                    .metaId(request.getMetaId())
                    .priority(request.getPriority())
                    .attributes(request.getAttributes())
                    .createdBy(request.getCreatedBy())
                    .build();

            dbLogger.info("DB insert – saving entity | orgId={} metaId={}", request.getOrganisationId(), request.getMetaId());
            entity = repository.save(entity);
            logger.info("Entity created successfully | id={}", entity.getId());
            dbLogger.info("DB insert – entity saved successfully | id={}", entity.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(OrganisationEntityMapper.toResponse(entity));

        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Error creating entity | orgId={}", request.getOrganisationId(), ex);
            dbLogger.error("DB insert failed | error={}", ex.getMessage());
            throw new InternalServerException("Failed to create entity: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> update(UUID id, OrganisationEntityUpdateRequest request) {
        logger.info("Updating entity | id={}", id);
        dbLogger.info("DB select – fetching entity for update | id={}", id);

        OrganisationEntity entity = repository.findById(id).orElseThrow(() -> {
            logger.error("Entity not found | id={}", id);
            dbLogger.error("DB select – entity not found | id={}", id);
            return new NotFoundException("Entity not found: " + id);
        });

        try {
            if (request.getAttributes() != null) {
                dbLogger.info("DB check – fetching metadata template for attribute validation | metaId={}", entity.getMetaId());
                EntityMetadata entityMetadata = entityMetadataRepository.findById(entity.getMetaId())
                        .orElseThrow(() -> new NotFoundException("Entity metadata not found: " + entity.getMetaId()));
                logger.info("Validating updated attributes against metadata template | metaId={}", entity.getMetaId());
                validateAttributes(entityMetadata.getMetadata(), request.getAttributes());
                logger.info("Attributes validation passed for update | metaId={}", entity.getMetaId());
                entity.setAttributes(request.getAttributes());
            }
            if (request.getPriority()  != null) entity.setPriority(request.getPriority());
            if (request.getIsActive()  != null) entity.setActive(request.getIsActive());
            if (request.getUpdatedBy() != null) entity.setUpdatedBy(request.getUpdatedBy());

            dbLogger.info("DB update – saving entity | id={}", id);
            OrganisationEntity savedEntity = repository.save(entity);
            logger.info("Entity updated successfully | id={}", savedEntity.getId());
            dbLogger.info("DB update – entity saved successfully | id={}", savedEntity.getId());
            return ResponseEntity.ok(OrganisationEntityMapper.toResponse(savedEntity));

        } catch (NotFoundException | BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Error updating entity | id={}", id, ex);
            dbLogger.error("DB update failed | id={} error={}", id, ex.getMessage());
            throw new InternalServerException("Failed to update entity: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> delete(UUID id) {
        logger.info("Deleting entity | id={}", id);
        dbLogger.info("DB select – fetching entity for delete | id={}", id);

        OrganisationEntity entity = repository.findById(id).orElseThrow(() -> {
            logger.error("Entity not found | id={}", id);
            dbLogger.error("DB select – entity not found | id={}", id);
            return new NotFoundException("Entity not found: " + id);
        });

        try {
            dbLogger.info("DB delete – removing entity | id={}", id);
            repository.delete(entity);
            logger.info("Entity deleted successfully | id={}", id);
            dbLogger.info("DB delete – entity removed successfully | id={}", id);
            return ResponseEntity.ok("Entity deleted successfully");

        } catch (Exception ex) {
            logger.error("Error deleting entity | id={}", id, ex);
            dbLogger.error("DB delete failed | id={} error={}", id, ex.getMessage());
            throw new InternalServerException("Failed to delete entity: " + ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getById(UUID id) {
        logger.info("Fetching entity | id={}", id);
        dbLogger.info("DB select – fetching entity by id | id={}", id);

        OrganisationEntity entity = repository.findById(id).orElseThrow(() -> {
            logger.error("Entity not found | id={}", id);
            dbLogger.error("DB select – entity not found | id={}", id);
            return new NotFoundException("Entity not found: " + id);
        });

        logger.info("Entity fetched successfully | id={}", id);
        dbLogger.info("DB select – entity fetched successfully | id={}", id);
        return ResponseEntity.ok(OrganisationEntityMapper.toResponse(entity));
    }

    @Override
    public ResponseEntity<?> getAll(int page, int size, String sortBy, String sortDirection,
            String search, Integer priority, UUID organisationId, Boolean isActive) {
        logger.info("Fetching organisation entities | page={} size={} sortBy={} sortDir={} search={}",
                page, size, sortBy, sortDirection, search);
        dbLogger.info("DB select – querying entities | priority={} orgId={} isActive={}", priority, organisationId, isActive);

        try {
            int safePage  = Math.max(page, 0);
            int safeSize  = Math.min(Math.max(size, 5), 100);
            String sortField  = (sortBy != null && !sortBy.isBlank()) ? sortBy : DEFAULT_SORT_FIELD;
            Sort.Direction dir = (sortDirection != null && sortDirection.equalsIgnoreCase(SORT_ASC))
                    ? Sort.Direction.ASC : Sort.Direction.DESC;

            Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(dir, sortField));

            Page<OrganisationEntity> result = repository.findAll(
                    OrganisationEntitySpecification.buildSpec(search, priority, organisationId, isActive),
                    pageable);

            if (result.getTotalElements() == 0) {
                throw new NotFoundException("No Organisation Entity found.");
            }
            if (safePage >= result.getTotalPages()) {
                throw new NotFoundException(String.format("Page %d not found. Total available pages: %d",
                        safePage + 1, result.getTotalPages()));
            }

            var content = result.getContent().stream()
                    .map(OrganisationEntityMapper::toResponse).toList();

            PaginatedResponse<OrganisationEntityResponse> paginatedData = new PaginatedResponse<>(
                    content, result.getNumber() + 1, result.getSize(),
                    result.getTotalElements(), result.getTotalPages(), result.isLast());

            logger.info("Organisation entities fetched successfully | totalElements={}", result.getTotalElements());
            dbLogger.info("DB select – entities query complete | totalElements={}", result.getTotalElements());
            return ResponseEntity.ok(paginatedData);

        } catch (NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Error fetching organisation entities", ex);
            dbLogger.error("DB select failed | error={}", ex.getMessage());
            throw new InternalServerException("Failed to fetch organisation entities: " + ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getByOrganisation(UUID organisationId) {
        logger.info("Fetching entities by org | orgId={}", organisationId);
        dbLogger.info("DB select – fetching entities by orgId | orgId={}", organisationId);

        List<OrganisationEntityResponse> list = repository.findByOrganisationId(organisationId)
                .stream().map(OrganisationEntityMapper::toResponse).toList();

        if (list.isEmpty()) {
            logger.error("No entities found | orgId={}", organisationId);
            dbLogger.error("DB select – no entities found | orgId={}", organisationId);
            throw new NotFoundException("No entities found for organisation: " + organisationId);
        }

        logger.info("Entities fetched successfully | orgId={} count={}", organisationId, list.size());
        dbLogger.info("DB select – entities fetched | orgId={} count={}", organisationId, list.size());
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<?> searchByAttribute(UUID organisationId, String key, String value) {
        logger.info("Searching entity by attribute | orgId={} key={} value={}", organisationId, key, value);
        dbLogger.info("DB select – attribute search | orgId={} key={} value={}", organisationId, key, value);

        try {
            List<OrganisationEntityResponse> list = repository
                    .findByOrganisationIdAndAttribute(organisationId, key, value)
                    .stream().map(OrganisationEntityMapper::toResponse).toList();

            if (list.isEmpty()) {
                logger.error("No entities found | key={} value={}", key, value);
                dbLogger.error("DB select – no entities found for attribute | key={} value={}", key, value);
                throw new NotFoundException("No entities found for key=" + key + " value=" + value);
            }

            logger.info("Entities found successfully | key={} value={} count={}", key, value, list.size());
            dbLogger.info("DB select – attribute search complete | key={} value={} count={}", key, value, list.size());
            return ResponseEntity.ok(list);

        } catch (NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Error searching entity | key={} value={}", key, value, ex);
            dbLogger.error("DB select failed | key={} value={} error={}", key, value, ex.getMessage());
            throw new InternalServerException("Failed to search entities: " + ex.getMessage());
        }
    }
}