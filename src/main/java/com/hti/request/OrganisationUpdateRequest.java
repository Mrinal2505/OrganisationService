package com.hti.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganisationUpdateRequest {

    // ✅ MUTABLE — display name can change (rebranding, typo fix, etc.)
    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 100, message = "Organization name must be between 2 and 100 characters")
    private String organizationName;

    // ✅ MUTABLE — business category can evolve
    private String organizationType;

    // ✅ MUTABLE — website can change
    private String websiteUrl;

    // ✅ MUTABLE — logo can be updated
    private String logoUrl;

    // ✅ MUTABLE — industry focus can shift
    private String industryType;

    // ✅ MUTABLE — contact number can change
    @NotBlank(message = "Phone is required")
    @Size(min = 10, max = 15, message = "Phone must be between 10 and 15 digits")
    private String phone;

    // ✅ MUTABLE — address details can change
    private String registeredAddress;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String timezone;

    // ❌ REMOVED — email: used as unique identifier (existsByEmail check in create),
    //              changing it would break identity/auth lookups

    // ❌ REMOVED — domain: acts as a unique org identifier, immutable post-creation

    // ❌ REMOVED — companyRegistrationNumber: legal/government-issued ID, must never change
}