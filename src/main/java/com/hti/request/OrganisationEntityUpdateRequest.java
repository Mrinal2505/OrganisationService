package com.hti.request;

import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class OrganisationEntityUpdateRequest {

    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 100, message = "Priority must be at most 100")
    private Integer priority;

    private Map<String, Object> attributes;

    private Boolean isActive;

    private String updatedBy;
}