package com.hti.serviceimpl;

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

import com.hti.Repository.OrganisationRepository;
import com.hti.entity.organisation;
import com.hti.exception.BadRequestException;
import com.hti.exception.InternalServerException;
import com.hti.exception.NotFoundException;
import com.hti.mapper.OrganisationMapper;
import com.hti.request.OrganisationRequest;
import com.hti.request.OrganisationUpdateRequest;
import com.hti.response.OrganisationResponse;
import com.hti.response.PaginatedResponse;
import com.hti.service.OrganisationService;
import com.hti.specification.OrganisationSpecification;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganisationImpl implements OrganisationService {

    private static final Logger logger   = LoggerFactory.getLogger("tracklogger");
    private static final Logger dbLogger = LoggerFactory.getLogger("dblogger");

    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final String SORT_ASC           = "asc";

    private final OrganisationRepository repository;

    @Override
    @Transactional
    public ResponseEntity<?> create(OrganisationRequest request) {
        logger.info("Create organisation request received | name={}", request.getOrganizationName());
        dbLogger.info("DB check – organisation existence by registrationNumber+domain+email | email={}", request.getEmail());

        if (repository.existsByCompanyRegistrationNumberAndDomainAndEmail(
                request.getCompanyRegistrationNumber(), request.getDomain(), request.getEmail())) {
            throw new BadRequestException(
                    "Organisation with same registration number, domain and email already exists");
        }
        try {
            organisation org = organisation.builder()
                    .organizationName(request.getOrganizationName())
                    .domain(request.getDomain())
                    .organizationType(request.getOrganizationType())
                    .companyRegistrationNumber(request.getCompanyRegistrationNumber())
                    .websiteUrl(request.getWebsiteUrl())
                    .logoUrl(request.getLogoUrl())
                    .industryType(request.getIndustryType())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .registeredAddress(request.getRegisteredAddress())
                    .city(request.getCity())
                    .state(request.getState())
                    .country(request.getCountry())
                    .postalCode(request.getPostalCode())
                    .timezone(request.getTimezone())
                    .build();

            dbLogger.info("DB insert – saving organisation | name={}", request.getOrganizationName());
            org = repository.save(org);
            logger.info("Organisation created successfully | id={} name={}", org.getId(), org.getOrganizationName());
            dbLogger.info("DB insert – organisation saved successfully | id={}", org.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(OrganisationMapper.toResponse(org));

        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Error creating organisation | name={}", request.getOrganizationName(), ex);
            throw new InternalServerException("Failed to create organisation: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> update(UUID id, OrganisationUpdateRequest request) {
        logger.info("Updating organisation | id={}", id);
        dbLogger.info("DB select – fetching organisation for update | id={}", id);

        organisation org = repository.findById(id).orElseThrow(() -> {
            logger.error("Organisation not found | id={}", id);
            dbLogger.error("DB select – organisation not found | id={}", id);
            return new NotFoundException("Organisation not found: " + id);
        });

        try {
            org.setOrganizationName(request.getOrganizationName());
            org.setOrganizationType(request.getOrganizationType());
            org.setWebsiteUrl(request.getWebsiteUrl());
            org.setLogoUrl(request.getLogoUrl());
            org.setIndustryType(request.getIndustryType());
            org.setPhone(request.getPhone());
            org.setRegisteredAddress(request.getRegisteredAddress());
            org.setCity(request.getCity());
            org.setState(request.getState());
            org.setCountry(request.getCountry());
            org.setPostalCode(request.getPostalCode());
            org.setTimezone(request.getTimezone());

            dbLogger.info("DB update – saving organisation | id={}", id);
            org = repository.save(org);
            logger.info("Organisation updated successfully | id={}", org.getId());
            dbLogger.info("DB update – organisation saved successfully | id={}", org.getId());
            return ResponseEntity.ok(OrganisationMapper.toResponse(org));

        } catch (Exception ex) {
            logger.error("Error updating organisation | id={}", id, ex);
            dbLogger.error("DB update failed | id={} error={}", id, ex.getMessage());
            throw new InternalServerException("Failed to update organisation: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> delete(UUID id) {
        logger.info("Deleting organisation | id={}", id);

        // Fix #6 – single DB hit: findById instead of existsById + deleteById
        dbLogger.info("DB select – fetching organisation for delete | id={}", id);
        organisation org = repository.findById(id).orElseThrow(() -> {
            logger.error("Organisation not found | id={}", id);
            dbLogger.error("DB select – organisation not found | id={}", id);
            return new NotFoundException("Organisation not found: " + id);
        });

        try {
            dbLogger.info("DB delete – removing organisation | id={}", id);
            repository.delete(org);
            logger.info("Organisation deleted successfully | id={}", id);
            dbLogger.info("DB delete – organisation removed successfully | id={}", id);
            return ResponseEntity.ok("Organisation deleted successfully");

        } catch (Exception ex) {
            logger.error("Error deleting organisation | id={}", id, ex);
            dbLogger.error("DB delete failed | id={} error={}", id, ex.getMessage());
            throw new InternalServerException("Failed to delete organisation: " + ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getById(UUID id) {
        logger.info("Fetching organisation | id={}", id);
        dbLogger.info("DB select – fetching organisation by id | id={}", id);

        organisation org = repository.findById(id).orElseThrow(() -> {
            logger.error("Organisation not found | id={}", id);
            dbLogger.error("DB select – organisation not found | id={}", id);
            return new NotFoundException("Organisation not found: " + id);
        });

        logger.info("Organisation fetched successfully | id={}", id);
        dbLogger.info("DB select – organisation fetched successfully | id={}", id);
        return ResponseEntity.ok(OrganisationMapper.toResponse(org));
    }

    @Override
    public ResponseEntity<?> getAll(int page, int size, String sortBy, String sortDirection, String search,
            String organizationType, String industryType, String city, String state, String country) {
        logger.info("Fetching organisations | page={} size={} sortBy={} sortDir={} search={}",
                page, size, sortBy, sortDirection, search);
        try {
            // Fix #14 – constants instead of magic strings
            int safePage  = Math.max(page, 0);
            int safeSize  = Math.min(Math.max(size, 5), 100);
            String sortField  = (sortBy != null && !sortBy.isBlank()) ? sortBy : DEFAULT_SORT_FIELD;
            Sort.Direction dir = (sortDirection != null && sortDirection.equalsIgnoreCase(SORT_ASC))
                    ? Sort.Direction.ASC : Sort.Direction.DESC;

            Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(dir, sortField));

            // Fix #10 – spec builder extracted to OrganisationSpecification
            Page<organisation> result = repository.findAll(
                    OrganisationSpecification.buildSpec(search, organizationType, industryType, city, state, country),
                    pageable);

            // Fix #11 – consistent page validation order: total check first, then page overflow
            if (result.getTotalElements() == 0) {
                throw new NotFoundException("No Organisation found.");
            }
            if (safePage >= result.getTotalPages()) {
                throw new NotFoundException(String.format("Page %d not found. Total available pages: %d",
                        safePage + 1, result.getTotalPages()));
            }

            // Fix #9 – toResponse() extracted to OrganisationMapper
            var content = result.getContent().stream().map(OrganisationMapper::toResponse).toList();

            PaginatedResponse<OrganisationResponse> paginatedData = new PaginatedResponse<>(
                    content, result.getNumber() + 1, result.getSize(),
                    result.getTotalElements(), result.getTotalPages(), result.isLast());

            logger.info("Organisations fetched successfully | totalElements={}", result.getTotalElements());
            return ResponseEntity.ok(paginatedData);

        } catch (NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Error fetching organisations", ex);
            throw new InternalServerException("Failed to fetch organisations: " + ex.getMessage());
        }
    }
}