package com.sistema.obra.public_work_escrow_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.sistema.obra.public_work_escrow_backend.dto.MilestoneDto;
import com.sistema.obra.public_work_escrow_backend.dto.PliegoUploadResponse;
import com.sistema.obra.public_work_escrow_backend.service.IpfsService;
import com.sistema.obra.public_work_escrow_backend.service.LicitacionBlockchainService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pliegos")
@CrossOrigin(origins = "*") 
public class PliegoController {

    @Autowired
    private IpfsService ipfsService;

    @Autowired
    private LicitacionBlockchainService blockchainService;

    @Value("${ipfs.gateway.base-url:http://localhost:8080/ipfs/}")
    private String gatewayBaseUrl;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPliego(
            @RequestPart("file") MultipartFile file,
            @RequestPart("hitos") List<MilestoneDto> hitos
    ) {

        System.out.println(">>> Hitos recibidos en el Backend: " + (hitos != null ? hitos.size() : 0));
    if (hitos != null) {
        for (int i = 0; i < hitos.size(); i++) {
            System.out.println("  Hito [" + i + "]: " + hitos.get(i).getDescripcion() + " - Monto: " + hitos.get(i).getPorcentaje());
        }
    }

        if (file == null || file.isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Por favor seleccione un archivo PDF válido.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            // 1. Subir el archivo del pliego a IPFS
            String ipfsHash = ipfsService.uploadPliegoToIpfs(file);
            String gatewayUrl = gatewayBaseUrl + ipfsHash;

            // 2. Desplegar el contrato en Hardhat/EVM con el hash de IPFS y la lista de hitos
            String contractAddress = blockchainService.desplegarContratoLicitacion(ipfsHash, hitos);

            int cantidadHitos = (hitos != null) ? hitos.size() : 0;
            PliegoUploadResponse response = new PliegoUploadResponse(
                    file.getOriginalFilename(),
                    ipfsHash,
                    gatewayUrl,
                    contractAddress,
                    "Documento cargado en IPFS y Smart Contract desplegado exitosamente con " + cantidadHitos + " hitos."
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace(); 
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error en el proceso de publicación: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}