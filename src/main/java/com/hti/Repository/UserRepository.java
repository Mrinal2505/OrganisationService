package com.hti.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.hti.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>,
JpaSpecificationExecutor<User>{
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByOrganisationId(UUID organisationId);
    List<User> findByEntityId(UUID entityId);
    boolean existsByUsername(String username);       
    Optional<User> findByUsername(String username); 
}