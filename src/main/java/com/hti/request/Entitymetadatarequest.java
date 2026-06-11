package com.hti.request;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
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
public class Entitymetadatarequest {

    @NotNull(message = "Organisation ID is required")
    private UUID organisationId;

    @NotBlank(message = "Entity type is required")
    private String entityType;

    private Map<String, Object> metadata;

    private String createdBy;
}