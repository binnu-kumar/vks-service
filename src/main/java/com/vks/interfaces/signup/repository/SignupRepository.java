package com.vks.interfaces.signup.repository;

import com.vks.interfaces.signup.entity.SignupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SignupRepository extends JpaRepository<SignupEntity, String> {

    boolean existsByMobileno(String mobileno);
}
