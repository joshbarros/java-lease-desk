package com.leasedesk.unit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UnitApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnitApplication.class, args);
    }
}
