package com.sisipapa.study.msa.resource.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/v1")
@RestController
@RefreshScope
public class MemberController {

    @GetMapping("/member/health")
    public String memberHealth() {
        log.debug("MemberController running");
        return "MemberController running";
    }

    @GetMapping("/member2/health")
    public String member2Health() {
        log.info("MemberController member2Health");
        return "MemberController running";
    }

}
