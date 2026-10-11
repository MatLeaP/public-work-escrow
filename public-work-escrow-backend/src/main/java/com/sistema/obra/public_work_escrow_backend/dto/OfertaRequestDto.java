package com.sistema.obra.public_work_escrow_backend.dto;

import java.math.BigInteger;

public class OfertaRequestDto {

    private String privateKeyEmpresa; // Clave privada con la que la empresa firma la transacción
    private BigInteger montoTotal;    // Monto total
    private String hashPropuesta;     // Hash del documento o IPFS CID

    // Constructor por defecto
    public OfertaRequestDto() {}

    public OfertaRequestDto(String privateKeyEmpresa, BigInteger montoTotal, String hashPropuesta) {
        this.privateKeyEmpresa = privateKeyEmpresa;
        this.montoTotal = montoTotal;
        this.hashPropuesta = hashPropuesta;
    }

    public String getPrivateKeyEmpresa() {
        return privateKeyEmpresa;
    }

    public void setPrivateKeyEmpresa(String privateKeyEmpresa) {
        this.privateKeyEmpresa = privateKeyEmpresa;
    }

    public BigInteger getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigInteger montoTotal) {
        this.montoTotal = montoTotal;
    }

    public String getHashPropuesta() {
        return hashPropuesta;
    }

    public void setHashPropuesta(String hashPropuesta) {
        this.hashPropuesta = hashPropuesta;
    }
}