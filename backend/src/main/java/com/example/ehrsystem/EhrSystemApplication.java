package com.example.ehrsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EhrSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(EhrSystemApplication.class, args);
    }

}
