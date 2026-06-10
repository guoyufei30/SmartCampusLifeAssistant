package com.smartcampuslifeserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartCampusLifeServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartCampusLifeServerApplication.class, args);
    }

}
