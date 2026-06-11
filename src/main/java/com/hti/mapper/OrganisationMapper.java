package com.hti.mapper;

import com.hti.entity.organisation;
import com.hti.response.OrganisationResponse;

public class OrganisationMapper {

    private OrganisationMapper() {}

    public static OrganisationResponse toResponse(organisation org) {
        return OrganisationResponse.builder()
                .id(org.getId())
                .organisationName(org.getOrganisationName())
                .domain(org.getDomain())
                .organisationType(org.getOrganisationType())
                .companyRegistrationNumber(org.getCompanyRegistrationNumber())
                .websiteUrl(org.getWebsiteUrl())
                .logoUrl(org.getLogoUrl())
                .industryType(org.getIndustryType())
                .email(org.getEmail())
                .phone(org.getPhone())
                .registeredAddress(org.getRegisteredAddress())
                .city(org.getCity())
                .state(org.getState())
                .country(org.getCountry())
                .postalCode(org.getPostalCode())
                .timezone(org.getTimezone())
                .numberOfEmployees(org.getNumberOfEmployees())
                .isActive(org.isActive())
                .deletedAt(org.getDeletedAt())
                .createdBy(org.getCreatedBy())
                .updatedBy(org.getUpdatedBy())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }
}