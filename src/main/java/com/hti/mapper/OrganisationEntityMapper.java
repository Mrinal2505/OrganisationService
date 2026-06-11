package com.hti.mapper;

import com.hti.entity.OrganisationEntity;
import com.hti.response.OrganisationEntityResponse;

public class OrganisationEntityMapper {

    private OrganisationEntityMapper() {}

    public static OrganisationEntityResponse toResponse(OrganisationEntity entity) {
        return OrganisationEntityResponse.builder()
                .id(entity.getId())
                .organisationId(entity.getOrganisationId())
                .metaId(entity.getMetaId())
                .priority(entity.getPriority())
                .attributes(entity.getAttributes())
                .isActive(entity.isActive())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}