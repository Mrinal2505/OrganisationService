package com.hti.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class OrganisationRequest {

    @NotBlank(message = "Organisation name is required")
    @Size(min = 2, max = 100, message = "Organisation name must be between 2 and 100 characters")
    private String organisationName;

    @NotBlank(message = "Domain is required")
    private String domain;

    private String organisationType;

    private String companyRegistrationNumber;

    private String websiteUrl;

    private String logoUrl;

    private String industryType;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must contain only digits (10-15)")
    private String phone;

    private String registeredAddress;

    private String city;

    private String state;

    private String country;

    private String postalCode;

    private String timezone;

    @NotNull(message = "Number of employees is required")
    @Min(value = 1, message = "Number of employees must be at least 1")
    private Integer numberOfEmployees;

    private String createdBy;
}