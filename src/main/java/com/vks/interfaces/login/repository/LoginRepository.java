package com.vks.interfaces.login.repository;

import com.vks.interfaces.signup.entity.SignupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginRepository extends JpaRepository<SignupEntity, String> {

    @Query("SELECT u FROM SignupEntity u WHERE u.mobileno = :username OR u.emailid = :username")
    Optional<SignupEntity> findByMobilenoOrEmailid(@Param("username") String username);
}
