package com.hti.serviceimpl;

import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import com.hti.exception.BadRequestException;
import com.hti.exception.InternalServerException;
import com.hti.exception.NotFoundException;
import com.hti.mapper.Entitymetadatamapper;
import com.hti.request.Entitymetadatarequest;
import com.hti.request.Entitymetadataupdaterequest;
import com.hti.response.Entitymetadataresponse;
import com.hti.response.PaginatedResponse;
import com.hti.service.Entitymetadataservice;
import com.hti.specification.Entitymetadataspecification;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Entitymetadataimpl implements Entitymetadataservice {

    private static final Logger logger   = LoggerFactory.getLogger("tracklogger");
    private static final Logger dbLogger = LoggerFactory.getLogger("dblogger");

    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final String SORT_ASC           = "asc";

    private final EntityMetadataRepository repository;
    private final OrganisationRepository organisationRepository;
    private final OrganisationEntityRepository organisationEntityRepository;

    @Override
    @Transactional
    public ResponseEntity<?> create(Entitymetadatarequest request) {
        logger.info("Creating entity metadata | orgId={} entityType={}", request.getOrganisationId(), request.getEntityType());
        dbLogger.info("DB check – organisation exists | orgId={}", request.getOrganisationId());

        if (!organisationRepository.existsById(request.getOrganisationId())) {
            logger.error("Organisation not found | orgId={}", request.getOrganisationId());
            dbLogger.error("DB check – organisation not found | orgId={}", request.getOrganisationId());
            throw new NotFoundException("Organisation not found: " + request.getOrganisationId());
        }

        dbLogger.info("DB check – entity metadata duplicate | orgId={} entityType={}", request.getOrganisationId(), request.getEntityType());
        if (repository.existsByOrganisationIdAndEntityType(request.getOrganisationId(), request.getEntityType())) {
            logger.error("Entity metadata already exists | orgId={} entityType={}", request.getOrganisationId(), request.getEntityType());
            dbLogger.error("DB check – entity metadata duplicate | orgId={} entityType={}", request.getOrganisationId(), request.getEntityType());
            throw new BadRequestException("Entity metadata already exists for organisation and entity type: " + request.getEntityType());
        }

        try {
            EntityMetadata entityMetadata = EntityMetadata.builder()
                    .organisationId(request.getOrganisationId())
                    .entityType(request.getEntityType())
                    .metadata(request.getMetadata())
                    .createdBy(request.getCreatedBy())
                    .build();

            dbLogger.info("DB insert – saving entity metadata | orgId={} entityType={}", request.getOrganisationId(), request.getEntityType());
            entityMetadata = repository.save(entityMetadata);
            logger.info("Entity metadata created successfully | id={}", entityMetadata.getId());
            dbLogger.info("DB insert – entity metadata saved successfully | id={}", entityMetadata.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(Entitymetadatamapper.toResponse(entityMetadata));

        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Error creating entity metadata | orgId={}", request.getOrganisationId(), ex);
            dbLogger.error("DB insert failed | error={}", ex.getMessage());
            throw new InternalServerException("Failed to create entity metadata: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> update(UUID id, Entitymetadataupdaterequest request) {
        logger.info("Updating entity metadata | id={}", id);
        dbLogger.info("DB select – fetching entity metadata for update | id={}", id);

        EntityMetadata entityMetadata = repository.findById(id).orElseThrow(() -> {
            logger.error("Entity metadata not found | id={}", id);
            dbLogger.error("DB select – entity metadata not found | id={}", id);
            return new NotFoundException("Entity metadata not found: " + id);
        });

        // Guard – if metadata keys are changing, block if entities are already linked
        if (request.getMetadata() != null) {
            dbLogger.info("DB check – checking linked entities before metadata key update | id={}", id);
            boolean hasLinkedEntities = !organisationEntityRepository.findByMetaId(id).isEmpty();
            if (hasLinkedEntities) {
                Set<String> existingKeys = entityMetadata.getMetadata() != null
                        ? entityMetadata.getMetadata().keySet() : Set.of();
                Set<String> newKeys = request.getMetadata().keySet();
                if (!existingKeys.equals(newKeys)) {
                    logger.error("Cannot change metadata keys – entities already linked | id={}", id);
                    dbLogger.error("DB check – metadata key change blocked, linked entities exist | id={}", id);
                    throw new BadRequestException(
                            "Cannot change metadata keys — organisation entities are already linked to this template. " +
                            "Existing keys: " + existingKeys + ". Attempted keys: " + newKeys);
                }
            }
            entityMetadata.setMetadata(request.getMetadata());
        }

        try {
            if (request.getIsActive()  != null) entityMetadata.setActive(request.getIsActive());
            if (request.getUpdatedBy() != null) entityMetadata.setUpdatedBy(request.getUpdatedBy());

            dbLogger.info("DB update – saving entity metadata | id={}", id);
            entityMetadata = repository.save(entityMetadata);
            logger.info("Entity metadata updated successfully | id={}", entityMetadata.getId());
            dbLogger.info("DB update – entity metadata saved successfully | id={}", entityMetadata.getId());
            return ResponseEntity.ok(Entitymetadatamapper.toResponse(entityMetadata));

        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Error updating entity metadata | id={}", id, ex);
            dbLogger.error("DB update failed | id={} error={}", id, ex.getMessage());
            throw new InternalServerException("Failed to update entity metadata: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> delete(UUID id) {
        logger.info("Deleting entity metadata | id={}", id);
        dbLogger.info("DB select – fetching entity metadata for delete | id={}", id);

        EntityMetadata entityMetadata = repository.findById(id).orElseThrow(() -> {
            logger.error("Entity metadata not found | id={}", id);
            dbLogger.error("DB select – entity metadata not found | id={}", id);
            return new NotFoundException("Entity metadata not found: " + id);
        });

        // Guard – block delete if organisation entities are still referencing this metaId
        dbLogger.info("DB check – checking linked entities before delete | id={}", id);
        List<?> linkedEntities = organisationEntityRepository.findByMetaId(id);
        if (!linkedEntities.isEmpty()) {
            logger.error("Cannot delete entity metadata – linked entities exist | id={} count={}", id, linkedEntities.size());
            dbLogger.error("DB check – delete blocked, linked entities exist | id={} count={}", id, linkedEntities.size());
            throw new BadRequestException(
                    "Cannot delete entity metadata — " + linkedEntities.size() +
                    " organisation entity/entities are still referencing this template. " +
                    "Delete or unlink those entities first.");
        }

        try {
            dbLogger.info("DB delete – removing entity metadata | id={}", id);
            repository.delete(entityMetadata);
            logger.info("Entity metadata deleted successfully | id={}", id);
            dbLogger.info("DB delete – entity metadata removed successfully | id={}", id);
            return ResponseEntity.ok("Entity metadata deleted successfully");

        } catch (Exception ex) {
            logger.error("Error deleting entity metadata | id={}", id, ex);
            dbLogger.error("DB delete failed | id={} error={}", id, ex.getMessage());
            throw new InternalServerException("Failed to delete entity metadata: " + ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getById(UUID id) {
        logger.info("Fetching entity metadata | id={}", id);
        dbLogger.info("DB select – fetching entity metadata by id | id={}", id);

        EntityMetadata entityMetadata = repository.findById(id).orElseThrow(() -> {
            logger.error("Entity metadata not found | id={}", id);
            dbLogger.error("DB select – entity metadata not found | id={}", id);
            return new NotFoundException("Entity metadata not found: " + id);
        });

        logger.info("Entity metadata fetched successfully | id={}", id);
        dbLogger.info("DB select – entity metadata fetched successfully | id={}", id);
        return ResponseEntity.ok(Entitymetadatamapper.toResponse(entityMetadata));
    }

    @Override
    public ResponseEntity<?> getAll(int page, int size, String sortBy, String sortDirection,
            String search, UUID organisationId, String entityType, Boolean isActive) {
        logger.info("Fetching entity metadata | page={} size={} sortBy={} sortDir={} search={}",
                page, size, sortBy, sortDirection, search);
        dbLogger.info("DB select – querying entity metadata | orgId={} entityType={} isActive={}", organisationId, entityType, isActive);

        try {
            int safePage   = Math.max(page, 0);
            int safeSize   = Math.min(Math.max(size, 5), 100);
            String sortField   = (sortBy != null && !sortBy.isBlank()) ? sortBy : DEFAULT_SORT_FIELD;
            Sort.Direction dir = (sortDirection != null && sortDirection.equalsIgnoreCase(SORT_ASC))
                    ? Sort.Direction.ASC : Sort.Direction.DESC;

            Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(dir, sortField));

            Page<EntityMetadata> result = repository.findAll(
                    Entitymetadataspecification.buildSpec(search, organisationId, entityType, isActive),
                    pageable);

            if (result.getTotalElements() == 0) {
                throw new NotFoundException("No entity metadata found.");
            }
            if (safePage >= result.getTotalPages()) {
                throw new NotFoundException(String.format("Page %d not found. Total available pages: %d",
                        safePage + 1, result.getTotalPages()));
            }

            var content = result.getContent().stream()
                    .map(Entitymetadatamapper::toResponse).toList();

            PaginatedResponse<Entitymetadataresponse> paginatedData = new PaginatedResponse<>(
                    content, result.getNumber() + 1, result.getSize(),
                    result.getTotalElements(), result.getTotalPages(), result.isLast());

            logger.info("Entity metadata fetched successfully | totalElements={}", result.getTotalElements());
            dbLogger.info("DB select – entity metadata query complete | totalElements={}", result.getTotalElements());
            return ResponseEntity.ok(paginatedData);

        } catch (NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Error fetching entity metadata", ex);
            dbLogger.error("DB select failed | error={}", ex.getMessage());
            throw new InternalServerException("Failed to fetch entity metadata: " + ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getByOrganisationId(UUID organisationId) {
        logger.info("Fetching entity metadata by orgId | orgId={}", organisationId);
        dbLogger.info("DB select – fetching entity metadata by orgId | orgId={}", organisationId);

        List<Entitymetadataresponse> list = repository.findByOrganisationId(organisationId)
                .stream().map(Entitymetadatamapper::toResponse).toList();

        if (list.isEmpty()) {
            logger.error("No entity metadata found | orgId={}", organisationId);
            dbLogger.error("DB select – no entity metadata found | orgId={}", organisationId);
            throw new NotFoundException("No entity metadata found for organisation: " + organisationId);
        }

        logger.info("Entity metadata fetched successfully | orgId={} count={}", organisationId, list.size());
        dbLogger.info("DB select – entity metadata fetched | orgId={} count={}", organisationId, list.size());
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<?> getByEntityType(String entityType) {
        logger.info("Fetching entity metadata by entityType | entityType={}", entityType);
        dbLogger.info("DB select – fetching entity metadata by entityType | entityType={}", entityType);

        List<Entitymetadataresponse> list = repository.findByEntityType(entityType)
                .stream().map(Entitymetadatamapper::toResponse).toList();

        if (list.isEmpty()) {
            logger.error("No entity metadata found | entityType={}", entityType);
            dbLogger.error("DB select – no entity metadata found | entityType={}", entityType);
            throw new NotFoundException("No entity metadata found for entity type: " + entityType);
        }

        logger.info("Entity metadata fetched successfully | entityType={} count={}", entityType, list.size());
        dbLogger.info("DB select – entity metadata fetched | entityType={} count={}", entityType, list.size());
        return ResponseEntity.ok(list);
    }
}