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
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;


import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class LicitacionBlockchainService {

        private final Web3j web3j;

        private final Credentials credentials;

        @Value("${blockchain.usdt-contract-address}")
        private String usdtContractAddress;

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
                constructorArgs.add(new Address(usdtContractAddress)); // 1° Dirección de USDT
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
                                                }, // monto
                                                new org.web3j.abi.TypeReference<Bool>() {
                                                }, // completado
                                                new org.web3j.abi.TypeReference<Bool>() {
                                                } // aprobado
                                ));

                String encodedFunction = FunctionEncoder.encode(function);

                EthCall response = web3j.ethCall(
                                Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                                DefaultBlockParameterName.LATEST).send();

                List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

                if (results.size() >= 4) {
                        String descripcion = (String) results.get(0).getValue();

                        // Convertir BigInteger a Long de forma segura
                        BigInteger porcentajeBigInt = (BigInteger) results.get(1).getValue();
                        Long porcentaje = porcentajeBigInt != null ? porcentajeBigInt.longValue() : 0L;

                        Boolean completado = (Boolean) results.get(2).getValue();
                        Boolean aprobado = (Boolean) results.get(3).getValue();

                        return new MilestoneDto(descripcion, porcentaje, completado, aprobado);
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
                        // Extraemos el DynamicArray devuelto
                        DynamicArray<?> arrayStructs = (DynamicArray<?>) results.get(0);

                        for (Object obj : arrayStructs.getValue()) {
                                if (obj instanceof DynamicStruct) {
                                        DynamicStruct struct = (DynamicStruct) obj;
                                        List<Type> componentes = struct.getValue();

                                        // Extraemos los componentes por su posición en el struct
                                        String descripcion = (String) componentes.get(0).getValue();
                                        BigInteger porcentaje = (BigInteger) componentes.get(1).getValue();
                                        Boolean completado = (Boolean) componentes.get(2).getValue();
                                        Boolean aprobado = (Boolean) componentes.get(3).getValue();

                                        listaHitos.add(new MilestoneDto(descripcion, porcentaje.longValue(), completado,
                                                        aprobado));
                                }
                        }
                }

                return listaHitos;
        }

        
}