package com.vks.interfaces.customer.model;

import com.vks.security.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerProfileResponse {

    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNo;
    private String tenantId;
    private UserRole role;
}
