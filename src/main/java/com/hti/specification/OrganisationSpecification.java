package com.hti.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.hti.entity.organisation;

import jakarta.persistence.criteria.Predicate;

public class OrganisationSpecification {

    private OrganisationSpecification() {}

    public static Specification<organisation> buildSpec(String search, String organisationType,
            String industryType, String city, String state, String country, Boolean isActive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude soft-deleted records
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("companyRegistrationNumber")), like),
                        cb.like(cb.lower(root.get("organisationName")), like),
                        cb.like(cb.lower(root.get("domain")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("phone")), like),
                        cb.like(cb.lower(root.get("city")), like),
                        cb.like(cb.lower(root.get("state")), like),
                        cb.like(cb.lower(root.get("country")), like)));
            }
            if (organisationType != null && !organisationType.isBlank())
                predicates.add(cb.equal(cb.lower(root.get("organisationType")), organisationType.toLowerCase()));
            if (industryType != null && !industryType.isBlank())
                predicates.add(cb.equal(cb.lower(root.get("industryType")), industryType.toLowerCase()));
            if (city != null && !city.isBlank())
                predicates.add(cb.equal(cb.lower(root.get("city")), city.toLowerCase()));
            if (state != null && !state.isBlank())
                predicates.add(cb.equal(cb.lower(root.get("state")), state.toLowerCase()));
            if (country != null && !country.isBlank())
                predicates.add(cb.equal(cb.lower(root.get("country")), country.toLowerCase()));
            if (isActive != null)
                predicates.add(cb.equal(root.get("isActive"), isActive));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}