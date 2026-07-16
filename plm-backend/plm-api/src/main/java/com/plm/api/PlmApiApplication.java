package com.plm.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootable Spring Boot application for the PLM GraphQL API. Component-scans the whole
 * {@code com.plm} package tree so it picks up the internal service layer and repositories from
 * plm-service in addition to its own GraphQL/security beans.
 */
@SpringBootApplication(scanBasePackages = "com.plm")
public class PlmApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlmApiApplication.class, args);
    }
}
