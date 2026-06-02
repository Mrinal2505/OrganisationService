package com.hti.request;

import jakarta.validation.constraints.NotBlank;
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
public class OrganisationUpdateRequest {

   
    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 100, message = "Organization name must be between 2 and 100 characters")
    private String organizationName;

  
    private String organizationType;

    private String websiteUrl;

    private String logoUrl;


    private String industryType;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must contain only digits (10-15)")
    private String phone;

    private String registeredAddress;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String timezone;

  
}