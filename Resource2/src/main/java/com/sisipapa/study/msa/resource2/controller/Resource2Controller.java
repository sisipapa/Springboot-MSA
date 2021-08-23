package com.sisipapa.study.msa.resource2.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RefreshScope
public class Resource2Controller {

    @Value("${server.port}")
    private int port;

    @Value("${spring.message}")
    private String message;

    @GetMapping("/resource2/detail")
    public String resource2() {
        return "resource Detail - Port " + port + " - " + message;
    }
}
