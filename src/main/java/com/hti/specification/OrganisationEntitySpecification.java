package com.hti.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.hti.entity.OrganisationEntity;

import jakarta.persistence.criteria.Predicate;

public class OrganisationEntitySpecification {

    private OrganisationEntitySpecification() {}

    public static Specification<OrganisationEntity> buildSpec(String search, String entityType,
            Integer priority, UUID organisationId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("entityType")), like)));
            }
            if (entityType != null && !entityType.isBlank())
                predicates.add(cb.equal(cb.lower(root.get("entityType")), entityType.toLowerCase()));
            if (priority != null)
                predicates.add(cb.equal(root.get("priority"), priority));
            if (organisationId != null)
                predicates.add(cb.equal(root.get("organisationId"), organisationId));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}