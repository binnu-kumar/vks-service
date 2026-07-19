package com.vks.interfaces.signup.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "signup")
public class SignupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, unique = true)
    private String id;

    @Column(name = "first_name", nullable = false)
    private String firstname;

    @Column(name = "last_name", nullable = false)
    private String lastname;

    @Column(name = "email_id", unique = true)
    private String emailid;

    @Column(name = "mobile_no", nullable = false, unique = true)
    private String mobileno;

    @Column(name = "password", nullable = false)
    private String password;
}
