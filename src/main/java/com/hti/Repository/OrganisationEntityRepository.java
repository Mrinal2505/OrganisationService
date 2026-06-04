package com.hti.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hti.entity.OrganisationEntity;

@Repository
public interface OrganisationEntityRepository extends JpaRepository<OrganisationEntity, UUID>,
        JpaSpecificationExecutor<OrganisationEntity> {

    List<OrganisationEntity> findByOrganisationId(UUID organisationId);

    List<OrganisationEntity> findByEntityType(String entityType);   

    boolean existsByOrganisationIdAndEntityType(UUID organisationId, String entityType); 

    @Query(value = """
        SELECT * FROM organisation_entity
        WHERE organisation_id = :orgId
        AND attributes ->> :key = :value
        """, nativeQuery = true)
    List<OrganisationEntity> findByOrganisationIdAndAttribute(
        @Param("orgId") UUID orgId,
        @Param("key") String key,
        @Param("value") String value
    );
}