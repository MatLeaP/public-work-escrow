package com.sistema.obra.public_work_escrow_backend.dto;

import lombok.Data;

@Data 
public class ProcuringEntityResponseDto {

    private Long id;
    private String name;
    private String email;
    private String walletAddress;    
    
}
