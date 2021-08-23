package com.sisipapa.study.msa.resource.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RefreshScope
public class ResourceController {

    @Value("${server.port}")
    private int port;

    @Value("${spring.message}")
    private String message;

    @GetMapping("/resource/detail")
    public String resource() {
        return "resource Detail - Port " + port + " - " + message;
    }
}
