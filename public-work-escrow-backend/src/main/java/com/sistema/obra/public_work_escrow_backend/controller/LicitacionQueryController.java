package com.sistema.obra.public_work_escrow_backend.controller;

import com.sistema.obra.public_work_escrow_backend.dto.MilestoneDto;

import com.sistema.obra.public_work_escrow_backend.service.LicitacionBlockchainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/licitaciones")
@CrossOrigin(origins = "*")
public class LicitacionQueryController {

    @Autowired
    private LicitacionBlockchainService blockchainService;

    
    @GetMapping("/{contractAddress}/ipfs")
    public ResponseEntity<?> obtenerIpfsHash(@PathVariable String contractAddress) {
        try {
            String ipfsHash = blockchainService.obtenerIpfsHash(contractAddress);
            Map<String, String> response = new HashMap<>();
            response.put("contractAddress", contractAddress);
            response.put("ipfsHash", ipfsHash);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al consultar IPFS Hash: " + e.getMessage());
        }
    }

    
    @GetMapping("/{contractAddress}/hitos/{index}")
    public ResponseEntity<?> obtenerHito(
            @PathVariable String contractAddress,
            @PathVariable int index) {
        try {
            MilestoneDto hito = blockchainService.obtenerHitoPorIndice(contractAddress, index);
            return ResponseEntity.ok(hito);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al consultar Hito: " + e.getMessage());
        }
    }

    @GetMapping("/{contractAddress}/hitos")
    public ResponseEntity<?> obetenerHitos(
            @PathVariable String contractAddress) throws Exception {
        BigInteger cantidadBigInt = blockchainService.obtenerCantidadHitos(contractAddress);

        Integer cantidadHitos = (cantidadBigInt != null) ? cantidadBigInt.intValue() : 0;

        return ResponseEntity.ok(cantidadHitos);
    }

    @GetMapping("/{contractAddress}/listaHitos")
    public ResponseEntity<?> obtenerListaHitos(
            @PathVariable String contractAddress) throws Exception {

        List<MilestoneDto> listaHitos = blockchainService.obtenerTodosLosHitosDirecto(contractAddress);

        return ResponseEntity.ok(listaHitos);

    }


    

}