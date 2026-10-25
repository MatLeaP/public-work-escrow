package com.sistema.obra.public_work_escrow_backend.controller;


import com.sistema.obra.public_work_escrow_backend.dto.MilestoneDto;
import com.sistema.obra.public_work_escrow_backend.model.Bid;
import com.sistema.obra.public_work_escrow_backend.model.Contractor;
import com.sistema.obra.public_work_escrow_backend.model.PublicWorkContract;
import com.sistema.obra.public_work_escrow_backend.model.WorkMilestone;
import com.sistema.obra.public_work_escrow_backend.repositories.BidRepository;
import com.sistema.obra.public_work_escrow_backend.repositories.ContractorRepository;
import com.sistema.obra.public_work_escrow_backend.repositories.PublicWorkContractRepository;
import com.sistema.obra.public_work_escrow_backend.repositories.WorkMilestoneRepository;
import com.sistema.obra.public_work_escrow_backend.service.IpfsService;
import com.sistema.obra.public_work_escrow_backend.service.LicitacionBlockchainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
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

    @Autowired
    private ContractorRepository contractorRepository;

    @Autowired
    private PublicWorkContractRepository contractRepository;

    @Autowired
    private IpfsService ipfsService;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private LicitacionBlockchainService licitacionBlockchainService;

    @Autowired
    private Web3j web3j;

    @Autowired
    private WorkMilestoneRepository workMilestoneRepository;

    @Value("${blockchain.private-key.estado:0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80}")
    private String privateKeyEstado;

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

    @PostMapping(value = "/{contractAddress}/ofertas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> presentarOferta(
            @PathVariable String contractAddress,
            @RequestParam("file") MultipartFile proposalFile,
            @RequestParam("montoTotal") BigDecimal montoTotal,
            @RequestParam("contractorId") Long contractorId,
            @RequestParam(name = "privateKeyEmpresa", required = false) String privateKeyEmpresa) {
        try {
            
            PublicWorkContract contract = contractRepository.findByContractAddress(contractAddress)
                    .orElseThrow(
                            () -> new RuntimeException("Contrato no encontrado para la dirección: " + contractAddress));

            
            Contractor contractor = contractorRepository.findById(contractorId)
                    .orElseThrow(() -> new RuntimeException("Empresa contratista no encontrada."));

            
            String ipfsHash = ipfsService.uploadPropuestaToIpfs(proposalFile);

            
            System.out.println(">>> PrivateKey recibida en Controller: [" + privateKeyEmpresa + "]");

            
            String txHash = licitacionBlockchainService.registrarOfertaEnContrato(
                    contractAddress, ipfsHash, montoTotal, privateKeyEmpresa);

            
            Bid bid = new Bid();
            bid.setPublicWorkContract(contract);
            bid.setContractor(contractor);
            bid.setProposedAmount(montoTotal);
            bid.setProposalIpfsHash(ipfsHash);
            bid.setTransactionHash(txHash);
            bid.setStatus("PENDING");

            bidRepository.save(bid);

            return ResponseEntity.ok(bid);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar la oferta: " + e.getMessage());
        }
    }

    @GetMapping("/{contractAddress}/ofertas")
    public ResponseEntity<List<Bid>> obtenerOfertasPorContrato(@PathVariable String contractAddress) {
        PublicWorkContract contract = contractRepository.findByContractAddress(contractAddress)
                .orElseThrow(
                        () -> new RuntimeException("Contrato no encontrado para la dirección: " + contractAddress));

        List<Bid> ofertas = bidRepository.findByPublicWorkContractId(contract.getId());
        return ResponseEntity.ok(ofertas);
    }

    @PostMapping("/{contractAddress}/adjudicar")
    public ResponseEntity<?> adjudicarYDepositarObra(
            @PathVariable String contractAddress,
            @RequestParam("bidId") Long bidId) {
        try {
            // 1. Buscar la oferta ganadora y validar
            Bid ofertaGanadora = bidRepository.findById(bidId)
                    .orElseThrow(() -> new RuntimeException("Oferta no encontrada con ID: " + bidId));

            PublicWorkContract contrato = ofertaGanadora.getPublicWorkContract();
            Contractor contratista = ofertaGanadora.getContractor();

            if (contratista.getWalletAddress() == null || contratista.getWalletAddress().isEmpty()) {
                throw new RuntimeException("El contratista seleccionado no tiene una billetera asociada.");
            }

            BigInteger montoInteger = ofertaGanadora.getProposedAmount()
                    .multiply(new java.math.BigDecimal("1000000"))
                    .toBigInteger();

            Function function = new Function(
                    "adjudicarGanador",
                    Arrays.asList(
                            new Address(contratista.getWalletAddress()),
                            new Utf8String(ofertaGanadora.getProposalIpfsHash()),
                            new Uint256(montoInteger)), // <-- Aquí viaja 120,000,000,000
                    Collections.emptyList());

            String encodedAdjudicar = FunctionEncoder.encode(function);
            Credentials credentials = Credentials.create(privateKeyEstado);

            EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                    credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
            BigInteger nonce = ethGetTransactionCount.getTransactionCount();

            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            BigInteger gasLimit = BigInteger.valueOf(3_000_000);

            RawTransaction txAdjudicar = RawTransaction.createTransaction(
                    nonce, gasPrice, gasLimit, contractAddress, BigInteger.ZERO, encodedAdjudicar);

            byte[] signedAdjudicar = TransactionEncoder.signMessage(txAdjudicar, credentials);
            EthSendTransaction responseAdjudicar = web3j.ethSendRawTransaction(Numeric.toHexString(signedAdjudicar))
                    .send();

            if (responseAdjudicar.hasError()) {
                throw new RuntimeException(
                        "Error en Blockchain (Adjudicación): " + responseAdjudicar.getError().getMessage());
            }
            String txHashAdjudicar = responseAdjudicar.getTransactionHash();

            // 3. Calcular el monto con 6 decimales exactos de USDT para el depósito
            BigInteger montoUSDT = ofertaGanadora.getProposedAmount()
                    .multiply(new java.math.BigDecimal("1000000"))
                    .toBigInteger();

            // 4. Aprobar el gasto de USDT hacia el contrato Escrow
            blockchainService.aprobarUSDT(contractAddress, montoUSDT);

            // 5. Ejecutar depositarFondosYComenzar() SIN parámetros (el contrato ya tiene
            // el monto guardado)
            String txHashDeposito = blockchainService.depositarFondosYComenzarOnChain(contractAddress);

            // 6. Actualizar estado local en MySQL (Contrato ACTIVO)
            contrato.setContractor(contratista);
            contrato.setStatus(PublicWorkContract.ContractStatus.ACTIVO);
            contractRepository.save(contrato);

            // 7. Actualizar la oferta ganadora y rechazar el resto
            ofertaGanadora.setStatus("WINNER");
            bidRepository.save(ofertaGanadora);

            List<Bid> todasLasOfertas = bidRepository.findByPublicWorkContractId(contrato.getId());
            for (Bid b : todasLasOfertas) {
                if (!b.getId().equals(bidId)) {
                    b.setStatus("REJECTED");
                    bidRepository.save(b);
                }
            }

            System.out
                    .println(">>> Obra " + contrato.getContractCode() + " adjudicada y fondos depositados con éxito.");

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Obra adjudicada y fondos de USDT depositados en custodia con éxito.");
            response.put("transactionHashAdjudicacion", txHashAdjudicar);
            response.put("transactionHashDeposito", txHashDeposito);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error en el proceso unificado: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> subirArchivoIpfs(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "El archivo está vacío."));
            }

            // Llamada a tu servicio existente
            String ipfsHash = ipfsService.uploadPropuestaToIpfs(file);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Archivo subido a IPFS con éxito");
            response.put("ipfsHash", ipfsHash);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error al subir archivo a IPFS: " + e.getMessage()));
        }
    }

    @PostMapping("/{hitoIndex}/subir-evidencia")
    public ResponseEntity<?> subirEvidenciaContratista(
            @PathVariable int hitoIndex,
            @RequestParam("contractAddress") String contractAddress, 
            @RequestParam("file") MultipartFile file,
            @RequestParam("descripcionAvance") String descripcion) {
        try {
            WorkMilestone hito = workMilestoneRepository
                    .findByContract_ContractAddressAndHitoIndex(contractAddress, hitoIndex)
                    .orElseThrow(() -> new RuntimeException("Hito no encontrado para el índice " + hitoIndex));

            
            String ipfsHash = ipfsService.uploadPropuestaToIpfs(file);

            
            hito.setDocumentIpfsHash(ipfsHash);
            hito.setEstadoRevision("EN_REVISION");
            workMilestoneRepository.save(hito);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Evidencia subida a IPFS y enviada a revisión estatal.");
            response.put("ipfsHash", ipfsHash);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error al procesar la evidencia: " + e.getMessage()));
        }
    }

    @PostMapping("/{hitoId}/aprobar-y-liberar")
    public ResponseEntity<?> aprobarHitoYRegistrarEnBlockchain(@PathVariable Long hitoId) {
        try {
            WorkMilestone hito = workMilestoneRepository.findById(hitoId)
                    .orElseThrow(() -> new RuntimeException("Hito no encontrado"));

            if (hito.getDocumentIpfsHash() == null || hito.getDocumentIpfsHash().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error",
                        "El hito no tiene una evidencia IPFS asociada para aprobar."));
            }

            String contractAddress = hito.getContract().getContractAddress();
            int hitoIndex = hito.getHitoIndex();
            String ipfsHash = hito.getDocumentIpfsHash();

            
            String txHash = blockchainService.aprobarYCompletarHitoOnChain(contractAddress,
                    BigInteger.valueOf(hitoIndex), ipfsHash);

            
            hito.setEstadoRevision("APROBADO");
            hito.setCompleted(true);
            workMilestoneRepository.save(hito);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Hito aprobado por el Estado y fondos liberados en la blockchain.");
            response.put("transactionHash", txHash);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error",
                            "Error al aprobar el hito en la blockchain: " + e.getMessage()));
        }
    }

    @GetMapping("/contrato/{contractAddress}")
    public ResponseEntity<?> obtenerHitosParaAuditoria(@PathVariable String contractAddress) {
        try {
            List<WorkMilestone> hitos = workMilestoneRepository.findByContract_ContractAddress(contractAddress);
            return ResponseEntity.ok(hitos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error al obtener hitos: " + e.getMessage()));
        }
    }

    @GetMapping("/{contractAddress}/resumen-final")
    public ResponseEntity<?> obtenerResumenFinal(@PathVariable String contractAddress) {
        try {
            Map<String, Object> resumen = blockchainService.generarResumenObra(contractAddress);
            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error al generar el resumen: " + e.getMessage()));
        }
    }
}