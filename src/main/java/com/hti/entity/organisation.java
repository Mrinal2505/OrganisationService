package com.hti.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "organisation",
uniqueConstraints = {
    @UniqueConstraint(name = "uk_org_email",  columnNames = "email"),
    @UniqueConstraint(name = "uk_org_domain", columnNames = "domain"),
    @UniqueConstraint(name = "uk_org_reg_no", columnNames = "company_registration_number")
}
)
public class organisation {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @Column(name = "domain")
    private String domain;

    @Column(name = "organization_type")
    private String organizationType;

    @Column(name = "company_registration_number")
    private String companyRegistrationNumber;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "industry_type")
    private String industryType;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "registered_address", columnDefinition = "TEXT")
    private String registeredAddress;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "country")
    private String country;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "timezone")
    private String timezone;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}