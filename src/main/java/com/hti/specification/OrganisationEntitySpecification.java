package com.hti.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.hti.entity.OrganisationEntity;

import jakarta.persistence.criteria.Predicate;

public class OrganisationEntitySpecification {

    private OrganisationEntitySpecification() {}

    public static Specification<OrganisationEntity> buildSpec(String search, Integer priority,
            UUID organisationId, Boolean isActive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("createdBy")), like),
                        cb.like(cb.lower(root.get("updatedBy")), like)
                ));
            }
            if (priority != null)
                predicates.add(cb.equal(root.get("priority"), priority));
            if (organisationId != null)
                predicates.add(cb.equal(root.get("organisationId"), organisationId));
            if (isActive != null)
                predicates.add(cb.equal(root.get("isActive"), isActive));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}