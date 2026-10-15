package com.sistema.obra.public_work_escrow_backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sistema.obra.public_work_escrow_backend.model.Contractor;
import com.sistema.obra.public_work_escrow_backend.model.ProcuringEntity;
import com.sistema.obra.public_work_escrow_backend.repositories.ContractorRepository;
import com.sistema.obra.public_work_escrow_backend.repositories.ProcuringEntityRepository;

@Configuration 
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(ProcuringEntityRepository procuringRepo, ContractorRepository contractorRepo) {
        return args -> {
            // Precargar Entidad de Gobierno (Cuenta #0 de Hardhat)
            if (procuringRepo.count() == 0) {
                ProcuringEntity government = new ProcuringEntity();
                government.setCompanyName("Gobierno de la Ciudad");
                government.setEmail("gobierno@caba.gov.ar");
                government.setPassword("123456");
                government.setWalletAddress("0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266");
                government.setPrivateKey("0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80");
                procuringRepo.save(government);
            }

            // Precargar Empresa Contratista A (Cuenta #1 de Hardhat)
            if (contractorRepo.count() == 0) {
                Contractor contractorA = new Contractor();
                contractorA.setCompanyName("Constructora Austral S.A.");
                contractorA.setEmail("contacto@constructoraustral.com");
                contractorA.setPassword("123456");
                contractorA.setWalletAddress("0x70997970C51812dc3A010C7d01b50e0d17dc79C8");
                contractorA.setPrivateKey("0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d");
                contractorRepo.save(contractorA);

                // Precargar Empresa Contratista B (Cuenta #2 de Hardhat)
                Contractor contractorB = new Contractor();
                contractorB.setCompanyName("Obras y Pavimentos del Plata");
                contractorB.setEmail("info@obrasdelplata.com");
                contractorB.setPassword("123456");
                contractorB.setWalletAddress("0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC");
                contractorB.setPrivateKey("0x5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a807cdad9054");
                contractorRepo.save(contractorB);
            }
        };
    }

}
