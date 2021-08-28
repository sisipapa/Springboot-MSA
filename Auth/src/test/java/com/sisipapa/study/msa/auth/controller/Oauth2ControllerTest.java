package com.sisipapa.study.msa.auth.controller;

import com.google.gson.Gson;
import com.sisipapa.study.msa.auth.model.OAuthToken;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class Oauth2ControllerTest {

    Logger log = (Logger) LoggerFactory.getLogger(Oauth2ControllerTest.class);

    @Autowired
    private Gson gson;
    @Autowired
    private RestTemplate restTemplate;

    @Test
    void callbackSocial(){
        String credentials = "testClientId:testSecret";
        String encodedCredentials = new String(Base64.encodeBase64(credentials.getBytes()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Authorization", "Basic " + encodedCredentials);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("refresh_token", "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX25hbWUiOiJzaXNpcGFwYTIzOUBnbWFpbC5jb20iLCJzY29wZSI6WyJyZWFkIl0sImF0aSI6ImU0ODM4MmEzLTk2OWMtNGFlYy05MThkLWQyNzc4ZTEzNzg0NiIsImV4cCI6MTYzMDE4NDkzMSwiYXV0aG9yaXRpZXMiOlsiUk9MRV9VU0VSIl0sImp0aSI6ImIyMjg4ZTg0LTEwNTctNDI5Yy1hNWY3LWQwMGQ4ZDU0ZjhkOSIsImNsaWVudF9pZCI6InRlc3RDbGllbnRJZCJ9.VQAyQ-y-IV6s-a-vHZ_h-wRbU_R03mwkb_XLKaPtQCll4KTOOuuLyHYvsiDcR7RblITbBBj4gnRzUdSNG0M87dAP3NYAarLq_DF-MHVrxalj7o_PbIoXmgrI7geO-51bvoj0bApd9Rgo9dF-yIKTUp7BkCJeJaIpzKg_TXO0KmaVCpltwenMnDUsbbcME9dLoMOquQ28w_Rcm46mdceT-vfq4gpFmy_fBhDVPc4702YyuKbfb0BxGDCZ_SXP4G7ykWqt7IuMdjuBrtPBqKZCrolQiVI3Dcsk3n9UADx6wfwwx-UBRtNtVSKpJMl3LbWAjxSnFqjVZskdFgCrvK68BA");
        params.add("grant_type", "refresh_token");
//        params.add("code", code);
//        params.add("grant_type", "authorization_code");
//        params.add("redirect_uri", "http://localhost:8081/oauth2/callback");
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:8081/oauth/token", request, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            OAuthToken token = gson.fromJson(response.getBody(), OAuthToken.class);
            System.out.println("###############################################################################################################################");
            log.info("### access_token : " + token.getAccess_token());
            log.info("### refresh_token : " + token.getRefresh_token());
            log.info("### token_type : " + token.getToken_type());
            log.info("### scope : " + token.getScope());
            log.info("### expires_in : " + token.getExpires_in());
            System.out.println("###############################################################################################################################");
        }
    }
}