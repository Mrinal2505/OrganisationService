package com.hti.serviceimpl;

import java.util.List;
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

import com.hti.Repository.OrganisationEntityRepository;
import com.hti.Repository.OrganisationRepository;
import com.hti.entity.OrganisationEntity;
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

    @Override
    @Transactional
    public ResponseEntity<?> create(OrganisationEntityRequest request) {
        logger.info("Creating entity | type={} orgId={}", request.getEntityType(), request.getOrganisationId());
        dbLogger.info("DB check – organisation exists | orgId={}", request.getOrganisationId());

        if (!organisationRepository.existsById(request.getOrganisationId())) {
            logger.error("Organisation not found | orgId={}", request.getOrganisationId());
            dbLogger.error("DB check – organisation not found | orgId={}", request.getOrganisationId());
            throw new NotFoundException("Organisation not found: " + request.getOrganisationId());
        }
        try {
            OrganisationEntity entity = OrganisationEntity.builder()
                    .organisationId(request.getOrganisationId())
                    .entityType(request.getEntityType())
                    .priority(request.getPriority())
                    .attributes(request.getAttributes())
                    .build();

            dbLogger.info("DB insert – saving entity | type={} orgId={}", request.getEntityType(), request.getOrganisationId());
            entity = repository.save(entity);
            logger.info("Entity created successfully | id={} type={}", entity.getId(), entity.getEntityType());
            dbLogger.info("DB insert – entity saved successfully | id={}", entity.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(OrganisationEntityMapper.toResponse(entity));

        } catch (Exception ex) {
            logger.error("Error creating entity | type={}", request.getEntityType(), ex);
            dbLogger.error("DB insert failed | type={} error={}", request.getEntityType(), ex.getMessage());
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
            if (request.getEntityType() != null)  entity.setEntityType(request.getEntityType());
            if (request.getPriority()   != null)  entity.setPriority(request.getPriority());
            if (request.getAttributes() != null)  entity.setAttributes(request.getAttributes());

            dbLogger.info("DB update – saving entity | id={}", id);
            entity = repository.save(entity);
            logger.info("Entity updated successfully | id={}", entity.getId());
            dbLogger.info("DB update – entity saved successfully | id={}", entity.getId());
            return ResponseEntity.ok(OrganisationEntityMapper.toResponse(entity));

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

        // Fix #6 – single DB hit: findById instead of existsById + deleteById
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
            String search, String entityType, Integer priority, UUID organisationId) {
        logger.info("Fetching organisation entities | page={} size={} sortBy={} sortDir={} search={}",
                page, size, sortBy, sortDirection, search);
        dbLogger.info("DB select – querying entities with filters | entityType={} priority={} orgId={}",
                entityType, priority, organisationId);
        try {
            // Fix #14 – constants instead of magic strings
            int safePage = Math.max(page, 0);
            int safeSize = Math.min(Math.max(size, 5), 100);
            String sortField  = (sortBy != null && !sortBy.isBlank()) ? sortBy : DEFAULT_SORT_FIELD;
            Sort.Direction dir = (sortDirection != null && sortDirection.equalsIgnoreCase(SORT_ASC))
                    ? Sort.Direction.ASC : Sort.Direction.DESC;

            Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(dir, sortField));

            // Fix #10 – spec builder extracted to OrganisationEntitySpecification
            Page<OrganisationEntity> result = repository.findAll(
                    OrganisationEntitySpecification.buildSpec(search, entityType, priority, organisationId),
                    pageable);

            if (result.getTotalElements() == 0) {
                throw new NotFoundException("No Organisation Entity found.");
            }

            // Fix #9 – toResponse() extracted to OrganisationEntityMapper
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
                .stream().map(OrganisationEntityMapper::toResponse).collect(Collectors.toList());

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
    public ResponseEntity<?> getByEntityType(String entityType) {
        logger.info("Fetching entities by type | type={}", entityType);
        dbLogger.info("DB select – fetching entities by type | type={}", entityType);

        List<OrganisationEntityResponse> list = repository.findByEntityType(entityType)
                .stream().map(OrganisationEntityMapper::toResponse).collect(Collectors.toList());

        if (list.isEmpty()) {
            logger.error("No entities found | type={}", entityType);
            dbLogger.error("DB select – no entities found | type={}", entityType);
            throw new NotFoundException("No entities found for type: " + entityType);
        }

        logger.info("Entities fetched successfully | type={} count={}", entityType, list.size());
        dbLogger.info("DB select – entities fetched | type={} count={}", entityType, list.size());
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<?> searchByAttribute(UUID organisationId, String key, String value) {
        logger.info("Searching entity by attribute | orgId={} key={} value={}", organisationId, key, value);
        dbLogger.info("DB select – attribute search | orgId={} key={} value={}", organisationId, key, value);

        try {
            List<OrganisationEntityResponse> list = repository
                    .findByOrganisationIdAndAttribute(organisationId, key, value)
                    .stream().map(OrganisationEntityMapper::toResponse).collect(Collectors.toList());

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