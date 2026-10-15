package com.sistema.obra.public_work_escrow_backend.repositories;

import com.sistema.obra.public_work_escrow_backend.model.ProcuringEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;


@Repository 
public interface ProcuringEntityRepository extends JpaRepository<ProcuringEntity, Long>{

    Optional<ProcuringEntity> findByEmail(String email);
    Optional<ProcuringEntity> findByWalletAddress(String walletAddress);
}
