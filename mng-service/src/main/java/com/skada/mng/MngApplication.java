package com.skada.mng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.skada.common", "com.skada.mng"})
public class MngApplication {

    public static void main(String[] args) {
        SpringApplication.run(MngApplication.class, args);
    }
}
