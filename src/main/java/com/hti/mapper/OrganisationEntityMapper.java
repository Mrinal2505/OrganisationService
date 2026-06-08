package com.hti.mapper;

import com.hti.entity.OrganisationEntity;
import com.hti.response.OrganisationEntityResponse;

public class OrganisationEntityMapper {

    private OrganisationEntityMapper() {}

    public static OrganisationEntityResponse toResponse(OrganisationEntity entity) {
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