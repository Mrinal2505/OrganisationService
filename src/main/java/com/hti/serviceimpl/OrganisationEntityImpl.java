package com.hti.serviceimpl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.hti.Repository.OrganisationEntityRepository;
import com.hti.Repository.OrganisationRepository;
import com.hti.entity.OrganisationEntity;
import com.hti.exception.InternalServerException;
import com.hti.exception.NotFoundException;
import com.hti.request.OrganisationEntityRequest;
import com.hti.request.OrganisationEntityUpdateRequest;
import com.hti.response.OrganisationEntityResponse;
import com.hti.response.PaginatedResponse;
import com.hti.service.OrganisationEntityService;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganisationEntityImpl implements OrganisationEntityService {

    private static final Logger logger = LoggerFactory.getLogger("tracklogger");

    private final OrganisationEntityRepository repository;
    private final OrganisationRepository organisationRepository; 
    @Override
    public ResponseEntity<?> create(OrganisationEntityRequest request) {
        logger.info("Creating entity | type={} orgId={}", request.getEntityType(), request.getOrganisationId());
     
        if (!organisationRepository.existsById(request.getOrganisationId())) {
            logger.error("Organisation not found | orgId={}", request.getOrganisationId());
            throw new NotFoundException("Organisation not found: " + request.getOrganisationId());
        }
        try {
            OrganisationEntity entity = OrganisationEntity.builder()
                    .organisationId(request.getOrganisationId())
                    .entityType(request.getEntityType())
                    .priority(request.getPriority())
                    .attributes(request.getAttributes())
                    .build();
            entity = repository.save(entity);
            logger.info("Entity created successfully | id={} type={}", entity.getId(), entity.getEntityType());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(entity));
        } catch (Exception ex) {
            logger.error("Error creating entity | type={}", request.getEntityType(), ex);
            throw new InternalServerException("Failed to create entity: " + ex.getMessage());
        }
    }

   @Override
public ResponseEntity<?> update(UUID id, OrganisationEntityUpdateRequest request) {
    logger.info("Updating entity | id={}", id);

    OrganisationEntity entity = repository.findById(id).orElseThrow(() -> {
        logger.error("Entity not found | id={}", id);
        return new NotFoundException("Entity not found: " + id);
    });

    try {
        if (request.getEntityType() != null)
            entity.setEntityType(request.getEntityType());

        if (request.getPriority() != null)
            entity.setPriority(request.getPriority());

        if (request.getAttributes() != null)
            entity.setAttributes(request.getAttributes());

        entity = repository.save(entity);
        logger.info("Entity updated successfully | id={}", entity.getId());
        return ResponseEntity.ok(toResponse(entity));

    } catch (Exception ex) {
        logger.error("Error updating entity | id={}", id, ex);
        throw new InternalServerException("Failed to update entity: " + ex.getMessage());
    }
}

    @Override
    public ResponseEntity<?> delete(UUID id) {
        logger.info("Deleting entity | id={}", id);
        if (!repository.existsById(id)) {
            logger.error("Entity not found | id={}", id);
            throw new NotFoundException("Entity not found: " + id);
        }
        try {
            repository.deleteById(id);
            logger.info("Entity deleted successfully | id={}", id);
            return ResponseEntity.ok("Entity deleted successfully");
        } catch (Exception ex) {
            logger.error("Error deleting entity | id={}", id, ex);
            throw new InternalServerException("Failed to delete entity: " + ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getById(UUID id) {
        logger.info("Fetching entity | id={}", id);

        OrganisationEntity entity = repository.findById(id).orElseThrow(() -> {
            logger.error("Entity not found | id={}", id);
            return new NotFoundException("Entity not found: " + id);
        });

        logger.info("Entity fetched successfully | id={}", id);
        return ResponseEntity.ok(toResponse(entity));
    }

    @Override
    public ResponseEntity<?> getAll(int page, int size, String sortBy, String sortDirection,
                                    String search, String entityType, Integer priority, UUID organisationId) {
        logger.info("Fetching organisation entities | page={}, size={}, sortBy={}, sortDir={}, search={}",
                page, size, sortBy, sortDirection, search);
        try {
            int safePage = Math.max(page, 0);
            int safeSize = Math.min(Math.max(size, 5), 100);

            Sort.Direction direction = (sortDirection != null && sortDirection.equalsIgnoreCase("asc"))
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";

            Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, sortField));

            Specification<OrganisationEntity> spec = buildOrganisationEntitySpec(
                    search, entityType, priority, organisationId
            );

            Page<OrganisationEntity> result = repository.findAll(spec, pageable);

            if (result.getTotalElements() == 0) {
                throw new NotFoundException("No Organisation Entity found.");
            }

            var content = result.getContent().stream().map(this::toResponse).toList();

            PaginatedResponse<OrganisationEntityResponse> paginatedData = new PaginatedResponse<>(
                    content,
                    result.getNumber() + 1,
                    result.getSize(),
                    result.getTotalElements(),
                    result.getTotalPages(),
                    result.isLast()
            );

            logger.info("Organisation entities fetched successfully | totalElements={}", result.getTotalElements());
            return ResponseEntity.ok(paginatedData);

        } catch (NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Error fetching organisation entities", ex);
            throw new InternalServerException("Failed to fetch organisation entities: " + ex.getMessage());
        }
    }

    private Specification<OrganisationEntity> buildOrganisationEntitySpec(String search, String entityType,
                                                                           Integer priority, UUID organisationId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("entityType")),     like)       
                ));
            }

            if (entityType != null && !entityType.isBlank())
                predicates.add(cb.equal(cb.lower(root.get("entityType")), entityType.toLowerCase()));

            if (priority != null)
                predicates.add(cb.equal(root.get("priority"), priority));

            if (organisationId != null)
                predicates.add(cb.equal(root.get("organisationId"), organisationId));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public ResponseEntity<?> getByOrganisation(UUID organisationId) {
        logger.info("Fetching entities by org | orgId={}", organisationId);

        List<OrganisationEntityResponse> list = repository.findByOrganisationId(organisationId)
                .stream().map(this::toResponse).collect(Collectors.toList());

        if (list.isEmpty()) {
            logger.error("No entities found | orgId={}", organisationId);
            throw new NotFoundException("No entities found for organisation: " + organisationId);
        }

        logger.info("Entities fetched successfully | orgId={} count={}", organisationId, list.size());
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<?> getByEntityType(String entityType) {
        logger.info("Fetching entities by type | type={}", entityType);

        List<OrganisationEntityResponse> list = repository.findByEntityType(entityType)
                .stream().map(this::toResponse).collect(Collectors.toList());

        if (list.isEmpty()) {
            logger.error("No entities found | type={}", entityType);
            throw new NotFoundException("No entities found for type: " + entityType);
        }

        logger.info("Entities fetched successfully | type={} count={}", entityType, list.size());
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<?> searchByAttribute(UUID organisationId, String key, String value) {
        logger.info("Searching entity by attribute | orgId={} key={} value={}", organisationId, key, value);

        try {
            List<OrganisationEntityResponse> list = repository
                    .findByOrganisationIdAndAttribute(organisationId, key, value)
                    .stream().map(this::toResponse).collect(Collectors.toList());

            if (list.isEmpty()) {
                logger.error("No entities found | key={} value={}", key, value);
                throw new NotFoundException("No entities found for key=" + key + " value=" + value);
            }

            logger.info("Entities found successfully | key={} value={} count={}", key, value, list.size());
            return ResponseEntity.ok(list);

        } catch (NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Error searching entity | key={} value={}", key, value, ex);
            throw new InternalServerException("Failed to search entities: " + ex.getMessage());
        }
    }

    private OrganisationEntityResponse toResponse(OrganisationEntity entity) {
        return OrganisationEntityResponse.builder()
                .id(entity.getId())
                .organisationId(entity.getOrganisationId())
                .entityType(entity.getEntityType())
                .priority(entity.getPriority())
                .attributes(entity.getAttributes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}