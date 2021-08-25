package com.sisipapa.study.msa.gateway.config;

import com.sisipapa.study.msa.gateway.filter.GatewayErrorFilter;
import com.sisipapa.study.msa.gateway.filter.GatewayPostFilter;
import com.sisipapa.study.msa.gateway.filter.GatewayPreFilter;
import com.sisipapa.study.msa.gateway.filter.GatewayRouteFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZuulFilterConfig {
    @Bean
    public GatewayPreFilter preFilter() {
        return new GatewayPreFilter();
    }

    @Bean
    public GatewayPostFilter postFilter() {
        return new GatewayPostFilter();
    }

    @Bean
    public GatewayRouteFilter routeFilter() {
        return new GatewayRouteFilter();
    }

    @Bean
    public GatewayErrorFilter errorFilter() {
        return new GatewayErrorFilter();
    }
}
