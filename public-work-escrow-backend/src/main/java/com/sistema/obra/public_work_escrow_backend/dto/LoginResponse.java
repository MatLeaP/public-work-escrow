package com.sistema.obra.public_work_escrow_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginResponse {
    private Long id;
    private String name;          
    private String email;
    private String role;          
    private String walletAddress;
    private String message;

    // Constructor explícito (sin trucos de Lombok, 100% compatible con cualquier versión de Java)
    public LoginResponse(Long id, String name, String email, String role, String walletAddress, String message) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.walletAddress = walletAddress;
        this.message = message;
    }
}