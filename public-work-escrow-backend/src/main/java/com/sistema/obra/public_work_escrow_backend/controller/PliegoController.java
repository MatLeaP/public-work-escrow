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
import com.sistema.obra.public_work_escrow_backend.model.PublicWorkContract;
import com.sistema.obra.public_work_escrow_backend.repositories.PublicWorkContractRepository;
import com.sistema.obra.public_work_escrow_backend.service.PublicWorkContractService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pliegos")
@CrossOrigin(origins = "*")
public class PliegoController {

    @Autowired
    private PublicWorkContractService contractService;

    @Autowired
    private PublicWorkContractRepository contractRepository;

    @Value("${ipfs.gateway.base-url:http://localhost:8080/ipfs/}")
    private String gatewayBaseUrl;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPliego(
            @RequestPart("file") MultipartFile file,
            @RequestParam("contractCode") String contractCode,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("totalAmount") BigDecimal totalAmount,
            @RequestParam("procuringEntityId") Long procuringEntityId,
            @RequestPart("hitos") List<MilestoneDto> hitos) {
        System.out.println(">>> Procesando contrato: " + contractCode + " - Título: " + title);
        System.out.println(">>> Hitos recibidos en el Backend: " + (hitos != null ? hitos.size() : 0));

        if (hitos != null) {
            for (int i = 0; i < hitos.size(); i++) {
                System.out.println("  Hito [" + i + "]: " + hitos.get(i).getDescripcion() + " - Porcentaje/Monto: "
                        + hitos.get(i).getPorcentaje());
            }
        }

        if (file == null || file.isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Por favor seleccione un archivo PDF válido.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            // Delegamos todo el flujo transaccional al servicio unificado
            PublicWorkContract savedContract = contractService.crearObraYDesplegarContrato(
                    contractCode,
                    title,
                    description,
                    totalAmount,
                    procuringEntityId,
                    file,
                    hitos);

            String gatewayUrl = gatewayBaseUrl + savedContract.getIpfsMetadataHash();
            int cantidadHitos = (hitos != null) ? hitos.size() : 0;

            PliegoUploadResponse response = new PliegoUploadResponse(
                    file.getOriginalFilename(),
                    savedContract.getIpfsMetadataHash(),
                    gatewayUrl,
                    savedContract.getBlockchainTxHash(),
                    "Contrato registrado en MySQL, pliego en IPFS y Smart Contract desplegado con " + cantidadHitos
                            + " hitos.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error en el proceso de publicación: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<PublicWorkContract>> listarTodasLasObras() {
        return ResponseEntity.ok(contractRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerObraPorId(@PathVariable Long id) {
        return contractRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body((PublicWorkContract) null));
    }

    @GetMapping("/contratista/{contractorId}/asignadas")
    public ResponseEntity<?> obtenerObrasAsignadasPorContratista(@PathVariable Long contractorId) {
        try {
            List<PublicWorkContract> obras = contractRepository.findByContractorId(contractorId);
            return ResponseEntity.ok(obras);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error al obtener obras asignadas: " + e.getMessage()));
        }
    }
}