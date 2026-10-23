package com.sistema.obra.public_work_escrow_backend.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sistema.obra.public_work_escrow_backend.dto.MilestoneDto;
import com.sistema.obra.public_work_escrow_backend.model.ProcuringEntity;
import com.sistema.obra.public_work_escrow_backend.model.PublicWorkContract;
import com.sistema.obra.public_work_escrow_backend.model.WorkMilestone;
import com.sistema.obra.public_work_escrow_backend.repositories.ProcuringEntityRepository;
import com.sistema.obra.public_work_escrow_backend.repositories.PublicWorkContractRepository;

import jakarta.transaction.Transactional;

@Service
public class PublicWorkContractService {

    @Autowired
    private PublicWorkContractRepository contractRepository;

    @Autowired
    private ProcuringEntityRepository procuringEntityRepository;

    @Autowired
    private IpfsService ipfsService;

    // AQUÍ INYECTAS TU SERVICIO BLOCKCHAIN EXISTENTE
    @Autowired
    private LicitacionBlockchainService blockchainService;

    @Transactional
    public PublicWorkContract crearObraYDesplegarContrato(
            String contractCode,
            String title,
            String description,
            BigDecimal totalAmount,
            Long procuringEntityId,
            MultipartFile file,
            List<MilestoneDto> hitosDto) throws Exception {

        // 1. Validar entidad compradora
        ProcuringEntity procuringEntity = procuringEntityRepository.findById(procuringEntityId)
                .orElseThrow(() -> new RuntimeException("Entidad compradora no encontrada."));

        // 2. Subir PDF a IPFS
        String ipfsHash = ipfsService.uploadPliegoToIpfs(file);

        // 3. LLAMAS A TU MÉTODO EXISTENTE DE BLOCKCHAIN PARA DESPLEGAR EL CONTRATO
        String contractAddress = blockchainService.desplegarContratoLicitacion(ipfsHash, hitosDto);

        // 4. Guardar en MySQL con la entidad y los hitos
        PublicWorkContract contract = new PublicWorkContract();
        contract.setContractCode(contractCode);
        contract.setTitle(title);
        contract.setDescription(description);
        contract.setTotalAmount(totalAmount);
        contract.setStatus(PublicWorkContract.ContractStatus.ACTIVO);
        contract.setIpfsMetadataHash(ipfsHash);
        contract.setBlockchainTxHash(contractAddress); // Aquí guardas la address devuelta por tu servicio blockchain
        contract.setProcuringEntity(procuringEntity);
        contract.setContractAddress(contractAddress);

        if (hitosDto != null) {
            for (int i = 0; i < hitosDto.size(); i++) {
                MilestoneDto dto = hitosDto.get(i);
                WorkMilestone milestone = new WorkMilestone();
                milestone.setHitoIndex(i); // 👈 ¡Clave! Asignamos la posición exacta que coincide con la blockchain
                milestone.setDescription(dto.getDescripcion());
                milestone.setAmountOrPercentage(BigDecimal.valueOf(dto.getPorcentaje()));
                contract.addMilestone(milestone);
            }
        }

        return contractRepository.save(contract);
    }
}