package com.ape.friction.api.mok.repositories;

import com.ape.friction.api.entities.users.AccessLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

//import com.ape.friction.api.interceptors.JaversUpdateRepository;

import java.util.UUID;
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface AccessLevelRepository extends JpaRepository<AccessLevel, UUID> {
}
