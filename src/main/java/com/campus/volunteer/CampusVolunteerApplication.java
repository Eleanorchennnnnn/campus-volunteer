package com.campus.volunteer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.campus.volunteer.mapper")
@SpringBootApplication
public class CampusVolunteerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusVolunteerApplication.class, args);
    }

}
