package com.sistema.obra.public_work_escrow_backend.dto;

import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;



public class MilestoneOutputStruct extends DynamicStruct {

    public Utf8String descripcion;
    public Uint256 porcentaje;
    public Bool completado;
    public Bool aprobado;

    
    public MilestoneOutputStruct(Utf8String descripcion, Uint256 porcentaje, Bool completado, Bool aprobado) {
        super(descripcion, porcentaje, completado, aprobado);
        this.descripcion = descripcion;
        this.porcentaje = porcentaje;
        this.completado = completado;
        this.aprobado = aprobado;
    }
}