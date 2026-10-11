package com.sistema.obra.public_work_escrow_backend.dto;

public class LicitacionResponseDTO {

    private String ipfsHash;
    private String contractAddress;
    private String estado;
    private String mensaje;

    public LicitacionResponseDTO(String ipfsHash, String contractAddress, String estado, String mensaje) {
        this.ipfsHash = ipfsHash;
        this.contractAddress = contractAddress;
        this.estado = estado;
        this.mensaje = mensaje;
    }

    
    public String getIpfsHash() { return ipfsHash; }
    public void setIpfsHash(String ipfsHash) { this.ipfsHash = ipfsHash; }

    public String getContractAddress() { return contractAddress; }
    public void setContractAddress(String contractAddress) { this.contractAddress = contractAddress; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
