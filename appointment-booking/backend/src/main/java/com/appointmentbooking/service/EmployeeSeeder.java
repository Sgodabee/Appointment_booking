package com.appointmentbooking.service;

import com.appointmentbooking.domain.model.Employee;
import com.appointmentbooking.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class EmployeeSeeder implements ApplicationRunner {

    private static final String[][] SEED_EMPLOYEES = {
        { "admin",        "Admin@1234",  "System Administrator", "ADMIN"    },
        { "branch.staff", "Staff@1234",  "Branch Staff Member",  "EMPLOYEE" },
    };

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder    passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        for (String[] seed : SEED_EMPLOYEES) {
            String username = seed[0].toLowerCase();
            String password = seed[1];
            String fullName = seed[2];
            String role     = seed[3];

            employeeRepository.findByUsernameAndActiveTrue(username)
                .ifPresentOrElse(
                    existing -> log.debug("EmployeeSeeder: '{}' already exists, skipping.", username),
                    () -> {
                        Employee emp = Employee.builder()
                            .username(username)
                            .passwordHash(passwordEncoder.encode(password))
                            .fullName(fullName)
                            .role(role)
                            .active(true)
                            .build();
                        employeeRepository.save(emp);
                        log.info("EmployeeSeeder: created employee '{}'", username);
                    }
                );
        }
    }
}
