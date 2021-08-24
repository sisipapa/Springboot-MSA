package com.sisipapa.study.msa.resource.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
public class ResourceController {

    @Value("${spring.message}")
    private String message;

    @Value("${user.password}")
    private String password;

    @GetMapping("/message")
    public String message() {
        return "ResourceController message : " + message;
    }

    @GetMapping("/password")
    public String password() {return "ResourceController password : " + password;}
}
