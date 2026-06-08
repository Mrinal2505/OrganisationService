package com.hti.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.hti.entity.EntityMetadata;

import jakarta.persistence.criteria.Predicate;

public class Entitymetadataspecification {

    private Entitymetadataspecification() {}

    public static Specification<EntityMetadata> buildSpec(String search, UUID organisationId, UUID entityId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("organisationId").as(String.class)), like),
                        cb.like(cb.lower(root.get("entityId").as(String.class)), like)
                ));
            }
            if (organisationId != null)
                predicates.add(cb.equal(root.get("organisationId"), organisationId));
            if (entityId != null)
                predicates.add(cb.equal(root.get("entityId"), entityId));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}