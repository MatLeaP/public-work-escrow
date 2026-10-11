package com.sistema.obra.public_work_escrow_backend.dto;

import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;

public class MilestoneInputStruct extends DynamicStruct {

    public String descripcion;
    public BigInteger porcentaje;

    public MilestoneInputStruct(String descripcion, BigInteger porcentaje) {
        super(
            new Utf8String(descripcion), 
            new Uint256(porcentaje)
        );
        this.descripcion = descripcion;
        this.porcentaje = porcentaje;
    }

    public MilestoneInputStruct(Utf8String descripcion, Uint256 porcentaje) {
        super(descripcion, porcentaje);
        this.descripcion = descripcion.getValue();
        this.porcentaje = porcentaje.getValue();
    }
}