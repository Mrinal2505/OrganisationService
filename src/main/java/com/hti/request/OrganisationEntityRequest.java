package com.hti.request;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Organisation ID is required")
    private UUID organisationId;

    @NotNull(message = "Meta ID is required")
    private UUID metaId;

    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 100, message = "Priority must be at most 100")
    private Integer priority;

    private Map<String, Object> attributes;

    private String createdBy;
}