package com.hti.request;

import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganisationEntityUpdateRequest {

    // ✅ MUTABLE — entity classification can be corrected or changed
    @NotBlank(message = "Entity type is required")
    private String entityType;

    // ✅ MUTABLE — priority ordering can be adjusted anytime
    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 100, message = "Priority must be at most 100")
    private Integer priority;

    // ✅ MUTABLE — dynamic key-value data, always updatable
    private Map<String, Object> attributes;

    // ❌ REMOVED — organisationId: parent FK reference, changing it would
    //              re-parent the entity to a different org — use delete + recreate instead
}
