package com.sistema.obra.public_work_escrow_backend.controller;

import com.sistema.obra.public_work_escrow_backend.dto.LoginRequest;
import com.sistema.obra.public_work_escrow_backend.dto.LoginResponse;
import com.sistema.obra.public_work_escrow_backend.model.Contractor;
import com.sistema.obra.public_work_escrow_backend.model.ProcuringEntity;
import com.sistema.obra.public_work_escrow_backend.repositories.ContractorRepository;
import com.sistema.obra.public_work_escrow_backend.repositories.ProcuringEntityRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class LoginController {

    @Autowired
    private ProcuringEntityRepository procuringRepo;

    @Autowired
    private ContractorRepository contractorRepo;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        // Validar si seleccionó Gobierno / Entidad Compradora
        if ("PROCURING_ENTITY".equalsIgnoreCase(request.getRole())) {
            Optional<ProcuringEntity> entityOpt = procuringRepo.findByEmail(request.getEmail());
            if (entityOpt.isPresent() && entityOpt.get().getPassword().equals(request.getPassword())) {
                ProcuringEntity entity = entityOpt.get();
                LoginResponse response = new LoginResponse(
                    entity.getId(),
                    entity.getCompanyName(),
                    entity.getEmail(),
                    "PROCURING_ENTITY",
                    entity.getWalletAddress(),
                    "Login exitoso como Gobierno"
                );
                return ResponseEntity.ok(response);
            }
        } 
        // Validar si seleccionó Empresa Contratista
        else if ("CONTRACTOR".equalsIgnoreCase(request.getRole())) {
            Optional<Contractor> contractorOpt = contractorRepo.findByEmail(request.getEmail());
            if (contractorOpt.isPresent() && contractorOpt.get().getPassword().equals(request.getPassword())) {
                Contractor contractor = contractorOpt.get();
                LoginResponse response = new LoginResponse(
                    contractor.getId(),
                    contractor.getCompanyName(),
                    contractor.getEmail(),
                    "CONTRACTOR",
                    contractor.getWalletAddress(),
                    "Login exitoso como Contratista"
                );
                return ResponseEntity.ok(response);
            }
        }

        Map<String, String> error = new HashMap<>();
        error.put("error", "Credenciales inválidas o tipo de usuario incorrecto.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}