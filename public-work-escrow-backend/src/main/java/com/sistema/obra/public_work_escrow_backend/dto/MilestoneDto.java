package com.sistema.obra.public_work_escrow_backend.dto;

import java.math.BigInteger;

public class MilestoneDto {

    private String descripcion;
    private Long porcentaje; 
    private Boolean completado;
    private Boolean aprobado;
    private String ipfsHash;

    
    public MilestoneDto() {
    }

    
    public MilestoneDto(String descripcion, Long porcentaje, Boolean completado, Boolean aprobado, String ipfsHash) {
        this.descripcion = descripcion;
        this.porcentaje = porcentaje;
        this.completado = completado;
        this.aprobado = aprobado;
        this.ipfsHash = ipfsHash;
    }

    
    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public long getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(Long porcentaje) {
        this.porcentaje = porcentaje;
    }

    public Boolean getCompletado() {
        return completado;
    }

    public void setCompletado(Boolean completado) {
        this.completado = completado;
    }

    public Boolean getAprobado() {
        return aprobado;
    }

    public void setAprobado(Boolean aprobado) {
        this.aprobado = aprobado;
    }
}