package com.sistema.obra.public_work_escrow_backend.dto;

import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;


public class HitoStruct extends DynamicStruct {

    public HitoStruct(String descripcion, BigInteger montoInWei) {
        super(
            new Utf8String(descripcion != null ? descripcion : ""),
            new Uint256(montoInWei != null ? montoInWei : BigInteger.ZERO)
        );
    }

    
    public HitoStruct(Utf8String descripcion, Uint256 monto) {
        super(descripcion, monto);
    }
}