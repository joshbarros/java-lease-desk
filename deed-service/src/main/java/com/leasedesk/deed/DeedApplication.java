package com.leasedesk.deed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeedApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeedApplication.class, args);
    }
}
