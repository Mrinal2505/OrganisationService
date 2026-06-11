package com.hti.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganisationResponse {
    private UUID id;
    private String organisationName;
    private String domain;
    private String organisationType;
    private String companyRegistrationNumber;
    private String websiteUrl;
    private String logoUrl;
    private String industryType;
    private String email;
    private String phone;
    private String registeredAddress;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String timezone;
    private Integer numberOfEmployees;
    private boolean isActive;
    private LocalDateTime deletedAt;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}