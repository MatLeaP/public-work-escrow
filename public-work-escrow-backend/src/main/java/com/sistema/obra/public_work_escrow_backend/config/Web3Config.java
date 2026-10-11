package com.sistema.obra.public_work_escrow_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class Web3Config {

    @Value("${ethereum.node.url:http://127.0.0.1:8545}")
    private String nodeUrl;

    @Value("${ethereum.private.key}")
    private String privateKey;

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(nodeUrl));
    }

    @Bean
    public Credentials credentials() {
        return Credentials.create(privateKey);
    }
}
