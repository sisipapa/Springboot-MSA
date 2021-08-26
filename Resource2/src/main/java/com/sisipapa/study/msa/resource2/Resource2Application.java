package com.sisipapa.study.msa.resource2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class Resource2Application {
    public static void main(String[] args) {
        SpringApplication.run(Resource2Application.class, args);
    }
}
