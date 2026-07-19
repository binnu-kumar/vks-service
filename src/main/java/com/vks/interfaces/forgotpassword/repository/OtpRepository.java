package com.vks.interfaces.forgotpassword.repository;

import com.vks.interfaces.forgotpassword.entity.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpEntity, String> {

    Optional<OtpEntity> findTopByUsernameAndUsedFalseOrderByExpiryDesc(String username);

    void deleteByUsername(String username);
}
