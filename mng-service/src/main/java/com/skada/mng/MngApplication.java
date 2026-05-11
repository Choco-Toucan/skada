package com.skada.mng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.skada.common", "com.skada.mng"})
public class MngApplication {

    public static void main(String[] args) {
        SpringApplication.run(MngApplication.class, args);
    }
}
