package com.hti.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.hti.entity.organisation;

@Repository
public interface OrganisationRepository extends JpaRepository<organisation, UUID>,
JpaSpecificationExecutor<organisation> {  
    Optional<organisation> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByCompanyRegistrationNumber(String companyRegistrationNumber);
    
    boolean existsByDomain(String domain);    	
}