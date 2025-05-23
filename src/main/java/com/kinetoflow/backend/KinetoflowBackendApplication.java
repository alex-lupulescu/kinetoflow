package com.kinetoflow.backend;

import com.kinetoflow.backend.entity.User; // Import User entity
import com.kinetoflow.backend.enums.UserRole; // Import UserRole enum
import com.kinetoflow.backend.repository.UserRepository; // Import UserRepository
import lombok.extern.slf4j.Slf4j; // Import Slf4j for logging
import org.springframework.boot.CommandLineRunner; // Import CommandLineRunner
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean; // Import Bean
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling; // Import EnableScheduling
import org.springframework.security.crypto.password.PasswordEncoder; // Import PasswordEncoder

/**
 * Main application class for the KinetoFlow Backend.
 * This class bootstraps the Spring Boot application.
 */
@SpringBootApplication
@EnableAsync // Enable asynchronous method execution
@EnableScheduling // Enable scheduled task execution
@Slf4j // Add Lombok logger
public class KinetoflowBackendApplication {

    /**
     * The main method which serves as the entry point for the Spring Boot application.
     *
     * @param args Command line arguments passed to the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(KinetoflowBackendApplication.class, args);
    }

    /**
     * Creates the initial APP_ADMIN user on application startup if it doesn't exist.
     * This uses a CommandLineRunner, which executes after the context is loaded.
     *
     * @param userRepository  Repository for user data access.
     * @param passwordEncoder Encoder for hashing the password.
     * @return A CommandLineRunner instance.
     */
    @Bean
    CommandLineRunner initAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@kinetoflow.app"; // Define the admin email
            // IMPORTANT: Use a strong, unique password! Consider externalizing this configuration (e.g., env variables).
            String adminPassword = "asd";

            // Check if the admin user already exists
            if (!userRepository.existsByEmail(adminEmail)) {
                log.info("Initial APP_ADMIN user not found. Creating...");

                User appAdmin = new User();
                appAdmin.setName("Platform Administrator");
                appAdmin.setEmail(adminEmail);
                appAdmin.setPassword(passwordEncoder.encode(adminPassword)); // Hash the password
                appAdmin.setRole(UserRole.APP_ADMIN);
                appAdmin.setActive(true); // Activate the admin user immediately
                appAdmin.setCompany(null); // APP_ADMIN does not belong to a specific company

                // Note: createdAt and updatedAt will be set automatically by @CreationTimestamp/@UpdateTimestamp

                userRepository.save(appAdmin);
                log.info("Initial APP_ADMIN user created successfully with email: {}", adminEmail);
            } else {
                log.info("Initial APP_ADMIN user ({}) already exists. Skipping creation.", adminEmail);
            }
        };
    }

    // Other beans or configuration can go here
}