package com.vks.interfaces.customer.controller;

import com.vks.common.ApiEndpoints;
import com.vks.interfaces.customer.model.CustomerProfileResponse;
import com.vks.interfaces.customer.service.CustomerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiEndpoints.BASE_CUSTOMER)
@RequiredArgsConstructor
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping(ApiEndpoints.CUSTOMER_PROFILE)
    public ResponseEntity<CustomerProfileResponse> getProfile() {
        return ResponseEntity.ok(customerProfileService.getProfile());
    }
}
