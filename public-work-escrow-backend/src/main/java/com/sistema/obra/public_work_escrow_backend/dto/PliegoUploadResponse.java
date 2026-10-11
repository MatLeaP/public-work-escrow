package com.sistema.obra.public_work_escrow_backend.dto;

public class PliegoUploadResponse {

    private String fileName;
    private String ipfsHash;
    private String gatewayUrl;
    private String contractAddress;
    private String mensaje;

    public PliegoUploadResponse(String fileName, String ipfsHash, String gatewayUrl, String contractAddress, String mensaje) {
        this.fileName = fileName;
        this.ipfsHash = ipfsHash;
        this.gatewayUrl = gatewayUrl;
        this.contractAddress = contractAddress;
        this.mensaje = mensaje;
    }

    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getIpfsHash() { return ipfsHash; }
    public void setIpfsHash(String ipfsHash) { this.ipfsHash = ipfsHash; }

    public String getGatewayUrl() { return gatewayUrl; }
    public void setGatewayUrl(String gatewayUrl) { this.gatewayUrl = gatewayUrl; }

    public String getContractAddress() { return contractAddress; }
    public void setContractAddress(String contractAddress) { this.contractAddress = contractAddress; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}