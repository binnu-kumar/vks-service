package com.vks.interfaces.signup.repository;

import com.vks.interfaces.signup.entity.SignupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import com.vks.security.UserRole;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SignupRepository extends JpaRepository<SignupEntity, Long> {

    boolean existsByMobileno(String mobileno);

    Optional<SignupEntity> findByEmailidIgnoreCase(String emailid);

    boolean existsByTenantIdAndRole(String tenantId, UserRole role);
}
