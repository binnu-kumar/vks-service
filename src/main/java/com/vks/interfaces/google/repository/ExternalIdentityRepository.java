package com.vks.interfaces.google.repository;

import com.vks.interfaces.google.entity.ExternalIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentityEntity, UUID> {

    Optional<ExternalIdentityEntity> findByProviderAndSubject(String provider, String subject);
}
