package com.vks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Port: 8087
 */
@SpringBootApplication(scanBasePackages = "com.vks")
@EnableScheduling
public class VksServiceMainApplication {
    public static void main(String[] args) {
        SpringApplication.run(VksServiceMainApplication.class, args);
    }
}
