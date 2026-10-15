package com.sistema.obra.public_work_escrow_backend.dto;

import lombok.Data;

@Data 
public class ContractorResponseDto {


    private Long id;
    private String companyName;
    private String email;
    private String walletAddress;

}
