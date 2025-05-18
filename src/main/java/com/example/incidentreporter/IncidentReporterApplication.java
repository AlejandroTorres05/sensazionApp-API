package com.example.incidentreporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IncidentReporterApplication {
    public static void main(String[] args) {
        SpringApplication.run(IncidentReporterApplication.class, args);
    }
}