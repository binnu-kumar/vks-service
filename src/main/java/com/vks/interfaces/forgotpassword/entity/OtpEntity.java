package com.vks.interfaces.forgotpassword.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "otp")
public class OtpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, unique = true)
    private String id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "otp", nullable = false)
    private String otp;

    @Column(name = "expiry", nullable = false)
    private LocalDateTime expiry;

    @Column(name = "used", nullable = false)
    private boolean used = false;
}
