package com.sistema.obra.public_work_escrow_backend.config;

import io.ipfs.api.IPFS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IpfsConfig {

    @Value("${ipfs.host}")
    private String host;

    @Value("${ipfs.port}")
    private int port;

    @Bean
    public IPFS ipfsClient() {
        // Inicializa el cliente IPFS conectándose al nodo local/servidor en el puerto 5001
        return new IPFS("/ip4/" + host + "/tcp/" + port);
    }
}