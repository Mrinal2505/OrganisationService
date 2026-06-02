package com.hti.request;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class OrganisationEntityRequest {

    private UUID organisationId;

    @NotBlank(message = "Entity type is required")
    private String entityType;

    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 100, message = "Priority must be at most 100")
    private Integer priority;

    private Map<String, Object> attributes;
}