package com.sisipapa.study.msa.auth;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoding {
    public static void main(String[] args){
//        PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
//        System.out.printf("testSecret : %s\n", passwordEncoder.encode("testSecret"));
        PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        System.out.printf("secondappsecret : %s\n", passwordEncoder.encode("secondappsecret"));
    }
}
