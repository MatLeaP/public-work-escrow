package com.sistema.obra.public_work_escrow_backend.service;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class IpfsService {

    @Autowired
    private IPFS ipfsClient;

    /**
     * Sube el documento PDF del pliego a IPFS, lo ancla y retorna su Hash (CID).
     */
    public String uploadPliegoToIpfs(MultipartFile file) throws IOException {
        return uploadFileToIpfs(file);
    }

    /**
     * Sube el documento PDF de la propuesta técnica/oferta a IPFS, lo ancla y retorna su Hash (CID).
     */
    public String uploadPropuestaToIpfs(MultipartFile file) throws IOException {
        return uploadFileToIpfs(file);
    }

    
    private String uploadFileToIpfs(MultipartFile file) throws IOException {
        NamedStreamable.InputStreamWrapper ipfsFile = new NamedStreamable.InputStreamWrapper(
                file.getOriginalFilename(),
                file.getInputStream()
        );

        MerkleNode response = ipfsClient.add(ipfsFile).get(0);
        String ipfsHash = response.hash.toString();

        
        ipfsClient.pin.add(response.hash);

        return ipfsHash;
    }
}