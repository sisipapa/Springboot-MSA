package com.sisipapa.study.msa.resource2.controller;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/v1/pay")
@RestController
@RefreshScope
public class PayController {
    @GetMapping("/health")
    public String health() {
        return "PayController running";
    }
}
