package com.example.cloud_file_storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@SpringBootApplication
@ComponentScan(basePackages = {"com.example.cloud_file_storage"})
public class CloudFileStorageApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudFileStorageApplication.class, args);
    }
}