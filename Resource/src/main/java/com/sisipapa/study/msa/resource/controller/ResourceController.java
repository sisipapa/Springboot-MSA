package com.sisipapa.study.msa.resource.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/v1/resource")
@RestController
@RefreshScope
public class ResourceController {

    @Value("${spring.message}")
    private String message;

    @Value("${db.ip}")
    private String ip;

    @Value("${db.port}")
    private String port;

    @Value("${db.id}")
    private String id;

    @Value("${db.password}")
    private String password;

    @GetMapping("/message")
    public String message() {
        return "ResourceController message : " + message;
    }

    @GetMapping("/db")
    public String db() {
        log.info("ResourceController db : " + ip + ":" + port + " [" + id +" : " + password + "]");
        return "ResourceController db : " + ip + ":" + port + " [" + id +" : " + password + "]";
    }
}
