package com.hti.request;

import java.util.Map;
import java.util.UUID;

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

    @NotNull(message = "Entity ID is required")
    private UUID entityId;

    private Map<String, Object> metadata;
}