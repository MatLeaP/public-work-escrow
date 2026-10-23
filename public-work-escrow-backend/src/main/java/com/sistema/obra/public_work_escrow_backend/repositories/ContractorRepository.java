package com.sistema.obra.public_work_escrow_backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.obra.public_work_escrow_backend.model.Contractor;
import java.util.List;


@Repository 
public interface ContractorRepository extends JpaRepository<Contractor, Long> {


    Optional<Contractor> findByEmail(String email);
    Optional<Contractor> findByWalletAddress(String walletAddress);

    

}
