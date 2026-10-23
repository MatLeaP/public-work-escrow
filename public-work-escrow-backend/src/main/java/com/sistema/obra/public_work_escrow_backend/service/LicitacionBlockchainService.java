package com.sistema.obra.public_work_escrow_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.sistema.obra.public_work_escrow_backend.dto.MilestoneDto;
import com.sistema.obra.public_work_escrow_backend.dto.MilestoneInputStruct;
import com.sistema.obra.public_work_escrow_backend.dto.MilestoneOutputStruct;

import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;

import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Function;

import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class LicitacionBlockchainService {

        private final Web3j web3j;

        private final Credentials credentials;

        @Value("${blockchain.usdt-token-address}")
        private String usdtTokenAddress;

        @Value("${blockchain.private-key.estado:0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80}")
        private String privateKeyEstado;

        LicitacionBlockchainService(Web3j web3j, Credentials credentials) {
                this.web3j = web3j;
                this.credentials = credentials;
        }

        public String desplegarContratoLicitacion(String ipfsPliegoHash, List<MilestoneDto> hitos) throws Exception {

                // 1. Cargar Bytecode desde el JSON de Hardhat
                InputStream inputStream = new ClassPathResource("contracts/PublicWorkEscrow.json").getInputStream();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(inputStream);

                String bytecode = jsonNode.has("bytecode") ? jsonNode.get("bytecode").asText()
                                : jsonNode.get("bytecode").get("object").asText();
                if (bytecode.startsWith("0x")) {
                        bytecode = bytecode.substring(2);
                }

                // 2. Mapear DTOs a MilestoneInputStruct con porcentajes
                List<MilestoneInputStruct> hitosEVM = new ArrayList<>();
                if (hitos != null && !hitos.isEmpty()) {
                        for (MilestoneDto hito : hitos) {
                                hitosEVM.add(new MilestoneInputStruct(
                                                hito.getDescripcion(),
                                                BigInteger.valueOf(hito.getPorcentaje())));
                        }
                }

                // 3. Crear el DynamicArray codificable por Web3j
                DynamicArray<MilestoneInputStruct> dynamicArrayHitos = new DynamicArray<>(
                                MilestoneInputStruct.class,
                                hitosEVM);

                // 4. Construir la lista con el tipo explícito List<Type> para evitar
                // incompatibilidad de tipos
                List<Type> constructorArgs = new ArrayList<>();
                constructorArgs.add(new Address(usdtTokenAddress)); // 1° Dirección de USDT
                constructorArgs.add(new Utf8String(ipfsPliegoHash)); // 2° IPFS Hash
                constructorArgs.add(dynamicArrayHitos); // 3° Hitos con porcentajes

                String constructorEncoded = FunctionEncoder.encodeConstructor(constructorArgs);

                // Si el bytecode no traía '0x', se lo agregamos
                String data = "0x" + bytecode + constructorEncoded;

                // 5. Configurar Transacción
                TransactionManager txManager = new RawTransactionManager(web3j, credentials);
                TransactionReceiptProcessor receiptProcessor = new PollingTransactionReceiptProcessor(web3j, 1000, 15);
                ContractGasProvider gasProvider = new DefaultGasProvider();

                // 6. Enviar Transacción a Hardhat/EVM
                EthSendTransaction response = txManager.sendTransaction(
                                gasProvider.getGasPrice(),
                                BigInteger.valueOf(8_000_000L),
                                null,
                                data,
                                BigInteger.ZERO);

                if (response.hasError()) {
                        throw new RuntimeException("Error RPC de Web3j: " + response.getError().getMessage());
                }

                // 7. Esperar Confirmación de la Transacción
                TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(response.getTransactionHash());

                if (!receipt.isStatusOK()) {
                        throw new RuntimeException("La transacción de despliegue fue revertida (Reverted).");
                }

                String contractAddress = receipt.getContractAddress();
                System.out.println("Contrato desplegado exitosamente con " + hitosEVM.size() + " hitos en: "
                                + contractAddress);

                return contractAddress;
        }

        public String obtenerIpfsHash(String contractAddress) throws Exception {
                Function function = new Function(
                                "ipfsPliegoHash", // Nombre del getter público o función en Solidity
                                Collections.emptyList(), // Parámetros de entrada (ninguno)
                                Collections.singletonList(new org.web3j.abi.TypeReference<Utf8String>() {
                                }) // Tipo de retorno
                );

                String encodedFunction = FunctionEncoder.encode(function);

                EthCall response = web3j.ethCall(
                                Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                                DefaultBlockParameterName.LATEST).send();

                List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

                if (!results.isEmpty()) {
                        return (String) results.get(0).getValue();
                }
                throw new RuntimeException("No se pudo obtener el hash de IPFS desde el contrato.");
        }

        public MilestoneDto obtenerHitoPorIndice(String contractAddress, int index) throws Exception {
                Function function = new Function(
                                "hitos",
                                Collections.singletonList(new Uint256(index)),
                                Arrays.asList(
                                                new org.web3j.abi.TypeReference<Utf8String>() {
                                                }, // descripcion
                                                new org.web3j.abi.TypeReference<Uint256>() {
                                                }, // porcentaje
                                                new org.web3j.abi.TypeReference<Bool>() {
                                                }, // completado
                                                new org.web3j.abi.TypeReference<Bool>() {
                                                }, // aprobado
                                                new org.web3j.abi.TypeReference<Utf8String>() {
                                                } // ipfsHash (NUEVO)
                                ));

                String encodedFunction = FunctionEncoder.encode(function);

                EthCall response = web3j.ethCall(
                                Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                                DefaultBlockParameterName.LATEST).send();

                List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

                if (results.size() >= 5) { // Validamos que traiga los 5 campos
                        String descripcion = (String) results.get(0).getValue();

                        BigInteger porcentajeBigInt = (BigInteger) results.get(1).getValue();
                        Long porcentaje = porcentajeBigInt != null ? porcentajeBigInt.longValue() : 0L;

                        Boolean completado = (Boolean) results.get(2).getValue();
                        Boolean aprobado = (Boolean) results.get(3).getValue();
                        String ipfsHash = (String) results.get(4).getValue(); // Capturamos el hash de IPFS

                        return new MilestoneDto(descripcion, porcentaje, completado, aprobado, ipfsHash);
                }

                throw new RuntimeException("No se encontro el hito en el indice " + index);
        }

        public BigInteger obtenerCantidadHitos(String contractAddress) throws Exception {
                Function function = new Function(
                                "obtenerCantidadHitos",
                                Collections.emptyList(),
                                Collections.singletonList(new org.web3j.abi.TypeReference<Uint256>() {
                                }));

                String encodedFunction = FunctionEncoder.encode(function);

                EthCall response = web3j.ethCall(
                                Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                                DefaultBlockParameterName.LATEST).send();

                List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

                if (!results.isEmpty()) {
                        return (BigInteger) results.get(0).getValue();
                }
                return BigInteger.ZERO;
        }

        public List<MilestoneDto> obtenerTodosLosHitosDirecto(String contractAddress) throws Exception {
                Function function = new Function(
                                "obtenerTodosLosHitos",
                                Collections.emptyList(),
                                Arrays.asList(
                                                new TypeReference<DynamicArray<MilestoneOutputStruct>>() {
                                                }));

                String encodedFunction = FunctionEncoder.encode(function);
                EthCall response = web3j.ethCall(
                                Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                                DefaultBlockParameterName.LATEST).send();

                List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
                List<MilestoneDto> listaHitos = new ArrayList<>();

                if (results != null && !results.isEmpty()) {
                        DynamicArray<?> arrayStructs = (DynamicArray<?>) results.get(0);

                        for (Object obj : arrayStructs.getValue()) {
                                if (obj instanceof DynamicStruct) {
                                        DynamicStruct struct = (DynamicStruct) obj;
                                        List<Type> componentes = struct.getValue();

                                        // Validación defensiva según la cantidad de campos que devuelva la blockchain
                                        String descripcion = "";
                                        BigInteger porcentaje = BigInteger.ZERO;
                                        Boolean completado = false;
                                        Boolean aprobado = false;
                                        String ipfsHash = "";

                                        if (componentes.size() > 0 && componentes.get(0) != null) {
                                                descripcion = (String) componentes.get(0).getValue();
                                        }
                                        if (componentes.size() > 1 && componentes.get(1) != null) {
                                                porcentaje = (BigInteger) componentes.get(1).getValue();
                                        }
                                        if (componentes.size() > 2 && componentes.get(2) != null) {
                                                completado = (Boolean) componentes.get(2).getValue();
                                        }
                                        if (componentes.size() > 3 && componentes.get(3) != null) {
                                                aprobado = (Boolean) componentes.get(3).getValue();
                                        }
                                        // Si el struct ya incluye el 5to campo (ipfsHash), lo leemos de forma segura
                                        if (componentes.size() > 4 && componentes.get(4) != null) {
                                                ipfsHash = (String) componentes.get(4).getValue();
                                        }

                                        listaHitos.add(new MilestoneDto(
                                                        descripcion,
                                                        porcentaje != null ? porcentaje.longValue() : 0L,
                                                        completado,
                                                        aprobado,
                                                        ipfsHash));
                                }
                        }
                }

                return listaHitos;
        }

        // Método para registrar la oferta firmada en el Smart Contract de la obra
        public String registrarOfertaEnContrato(String contractAddress, String ipfsHash, BigDecimal montoTotal,
                        String privateKeyEmpresa) throws Exception {

                // 1. Asignar credenciales de la empresa
                Credentials credsEmpresa = (privateKeyEmpresa != null && !privateKeyEmpresa.trim().isEmpty())
                                ? Credentials.create(privateKeyEmpresa.trim())
                                : this.credentials;

                String addressEmpresa = credsEmpresa.getAddress();
                System.out.println(">>> Billetera firmante: " + addressEmpresa);

                // 2. 🟢 FAUCET AUTOMÁTICO: Verificar si la billetera tiene saldo para gas en
                // Hardhat
                BigInteger balance = web3j.ethGetBalance(addressEmpresa, DefaultBlockParameterName.LATEST).send()
                                .getBalance();
                if (balance.compareTo(BigInteger.valueOf(100_000_000_000_000_000L)) < 0) { // Menos de 0.1 ETH
                        System.out.println(">>> [Faucet] Fondeando automáticamente la cuenta " + addressEmpresa
                                        + " con 1 ETH...");
                        TransactionManager mainTxManager = new RawTransactionManager(web3j, this.credentials);
                        mainTxManager.sendTransaction(
                                        DefaultGasProvider.GAS_PRICE,
                                        DefaultGasProvider.GAS_LIMIT,
                                        addressEmpresa,
                                        "",
                                        BigInteger.valueOf(1_000_000_000_000_000_000L) // 1 ETH
                        );
                }

                // 3. Garantizar que el monto sea un BigInteger positivo mayor a cero
                BigInteger montoInteger = montoTotal.setScale(0, RoundingMode.DOWN).toBigInteger();
                if (montoInteger.compareTo(BigInteger.ZERO) <= 0) {
                        montoInteger = BigInteger.valueOf(montoTotal.longValue());
                }

                System.out.println(">>> Registrando Oferta en Blockchain - Contrato: " + contractAddress);
                System.out.println(">>> Monto enviado a Solidity (uint256): " + montoInteger);
                System.out.println(">>> Hash IPFS enviado: " + ipfsHash);

                // 4. Crear la llamada coincidiendo con los tipos de presentarOferta(uint256,
                // string)
                Function function = new Function(
                                "presentarOferta",
                                Arrays.asList(
                                                new Uint256(montoInteger),
                                                new Utf8String(ipfsHash)),
                                Collections.emptyList());

                String encodedFunction = FunctionEncoder.encode(function);

                TransactionManager txManager = new RawTransactionManager(web3j, credsEmpresa);
                ContractGasProvider gasProvider = new DefaultGasProvider();

                EthSendTransaction response = txManager.sendTransaction(
                                gasProvider.getGasPrice(),
                                BigInteger.valueOf(3_000_000L),
                                contractAddress,
                                encodedFunction,
                                BigInteger.ZERO);

                if (response.hasError()) {
                        System.err.println("❌ ERROR DE HARDHAT AL ENVIAR TX: " + response.getError().getMessage());
                        System.err.println("Código de error: " + response.getError().getCode());
                        throw new RuntimeException(
                                        "Error en txManager.sendTransaction: " + response.getError().getMessage());
                }

                TransactionReceiptProcessor receiptProcessor = new PollingTransactionReceiptProcessor(web3j, 1000, 15);
                TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(response.getTransactionHash());

                if (!receipt.isStatusOK()) {
                        throw new RuntimeException("La transacción de oferta fue revertida por el Smart Contract.");
                }

                return receipt.getTransactionHash();
        }

        public String adjudicarGanadorDinamico(
                        String contractAddress,
                        String walletContratista,
                        String ipfsPropuestaHash,
                        BigDecimal montoTotal) throws Exception {

                // 1. Convertir el monto a BigInteger (unidades/wei según tu lógica)
                BigInteger montoInteger = montoTotal.toBigInteger();

                // 2. Definir la función 'adjudicarGanador(address,string,uint256)'
                Function function = new Function(
                                "adjudicarGanador",
                                Arrays.asList(
                                                new Address(walletContratista),
                                                new Utf8String(ipfsPropuestaHash),
                                                new Uint256(montoInteger)),
                                Collections.emptyList());

                // 3. Codificar la función a HEX
                String encodedFunction = FunctionEncoder.encode(function);

                // 4. Obtener las credenciales del Estado (licitante)
                Credentials credentials = Credentials.create(privateKeyEstado);

                // 5. Obtener el Nonce de la cuenta
                EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                                credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
                BigInteger nonce = ethGetTransactionCount.getTransactionCount();

                // 6. Configurar Gas y construir la RawTransaction
                BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
                BigInteger gasLimit = BigInteger.valueOf(3_000_000); // Límite de gas estándar para Hardhat

                RawTransaction rawTransaction = RawTransaction.createTransaction(
                                nonce,
                                gasPrice,
                                gasLimit,
                                contractAddress,
                                BigInteger.ZERO,
                                encodedFunction);

                // 7. Firmar y enviar la transacción
                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                String hexValue = Numeric.toHexString(signedMessage);

                EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

                if (ethSendTransaction.hasError()) {
                        throw new RuntimeException("Error al enviar transacción a Hardhat: "
                                        + ethSendTransaction.getError().getMessage());
                }

                String txHash = ethSendTransaction.getTransactionHash();
                System.out.println(">>> Adjudicación registrada dinámicamente en Blockchain. TxHash: " + txHash);

                return txHash;
        }

        private String enviarTransaccion(String contractAddress, Function function, Credentials credentials)
                        throws Exception {
                EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                                credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
                BigInteger nonce = ethGetTransactionCount.getTransactionCount();

                BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
                BigInteger gasLimit = BigInteger.valueOf(3_000_000);

                String encodedFunction = FunctionEncoder.encode(function);

                RawTransaction rawTransaction = RawTransaction.createTransaction(
                                nonce,
                                gasPrice,
                                gasLimit,
                                contractAddress,
                                BigInteger.ZERO,
                                encodedFunction);

                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                String hexValue = Numeric.toHexString(signedMessage);

                EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

                if (ethSendTransaction.hasError()) {
                        throw new RuntimeException(
                                        "Error en Blockchain: " + ethSendTransaction.getError().getMessage());
                }

                return ethSendTransaction.getTransactionHash();
        }

        public void aprobarUSDT(String escrowContractAddress, BigInteger montoUSDT) throws Exception {
                Credentials credentials = Credentials.create(privateKeyEstado);

                Function approveFn = new Function(
                                "approve",
                                Arrays.asList(new Address(escrowContractAddress), new Uint256(montoUSDT)),
                                Collections.emptyList());
                String txApprove = enviarTransaccion(usdtTokenAddress, approveFn, credentials);
                System.out.println(">>> Approve realizado hacia el Escrow. TxHash: " + txApprove);
        }

        public String depositarFondosYComenzarOnChain(String escrowContractAddress) throws Exception {
                Credentials credentials = Credentials.create(privateKeyEstado);

                EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                                credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
                BigInteger nonce = ethGetTransactionCount.getTransactionCount();

                BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
                BigInteger gasLimit = BigInteger.valueOf(3_000_000);

                org.web3j.abi.datatypes.Function depositarFn = new org.web3j.abi.datatypes.Function(
                                "depositarFondosYComenzar",
                                Collections.emptyList(),
                                Collections.emptyList());

                String encodedFunction = org.web3j.abi.FunctionEncoder.encode(depositarFn);

                RawTransaction rawTransaction = RawTransaction.createTransaction(
                                nonce, gasPrice, gasLimit, escrowContractAddress, BigInteger.ZERO, encodedFunction);

                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                String hexValue = Numeric.toHexString(signedMessage);

                EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

                if (ethSendTransaction.hasError()) {
                        throw new RuntimeException(
                                        "Error en Blockchain: " + ethSendTransaction.getError().getMessage());
                }

                return ethSendTransaction.getTransactionHash();
        }

        public String aprobarYCompletarHitoOnChain(String contractAddress, BigInteger hitoIndex, String ipfsHash)
                        throws Exception {
                Credentials credentials = Credentials.create(privateKeyEstado); // Clave privada del Comitente/Estado

                EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                                credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
                BigInteger nonce = ethGetTransactionCount.getTransactionCount();

                BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
                BigInteger gasLimit = BigInteger.valueOf(3_000_000);

                Function function = new Function(
                                "aprobarYLiberarHito", // Nombre exacto en tu Smart Contract
                                Arrays.asList(
                                                new Uint256(hitoIndex),
                                                new Utf8String(ipfsHash)),
                                Collections.emptyList());

                String encodedFunction = FunctionEncoder.encode(function);

                RawTransaction rawTransaction = RawTransaction.createTransaction(
                                nonce, gasPrice, gasLimit, contractAddress, BigInteger.ZERO, encodedFunction);

                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(Numeric.toHexString(signedMessage))
                                .send();

                if (ethSendTransaction.hasError()) {
                        throw new RuntimeException(
                                        "Error en Blockchain: " + ethSendTransaction.getError().getMessage());
                }

                return ethSendTransaction.getTransactionHash();
        }
}