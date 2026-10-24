package com.sirius.sirius;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SiriusApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiriusApplication.class, args);
    }

}
