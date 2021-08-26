package com.sisipapa.study.msa.resource2.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/v1/resource2")
@RestController
@RefreshScope
public class Resource2Controller {

    @Value("${spring.message}")
    private String message;

    @Value("${server.port}")
    private String port;

    @GetMapping("/message")
    public String message() {
        return "Resource2Controller message : " + message + " | port : " + port;
    }
}
