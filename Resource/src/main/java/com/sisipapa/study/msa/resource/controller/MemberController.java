package com.sisipapa.study.msa.resource.controller;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/v1/member")
@RestController
@RefreshScope
public class MemberController {

    @GetMapping("/health")
    public String health() {
        return "MemberController running";
    }

}
