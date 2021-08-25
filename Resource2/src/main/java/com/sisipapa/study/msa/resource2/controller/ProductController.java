package com.sisipapa.study.msa.resource2.controller;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/v1")
@RestController
@RefreshScope
public class ProductController {
    @GetMapping("/product/health")
    public String productHealth() {
        return "PayController running";
    }
    @GetMapping("/product2/health")
    public String product2Health() {
        return "PayController running";
    }
}
