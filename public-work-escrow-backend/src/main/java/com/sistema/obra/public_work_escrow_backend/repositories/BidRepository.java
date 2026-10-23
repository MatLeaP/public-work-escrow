package com.sistema.obra.public_work_escrow_backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.obra.public_work_escrow_backend.model.Bid;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    // Obtener todas las ofertas presentadas para una obra específica
    List<Bid> findByPublicWorkContractId(Long publicWorkContractId);

    // Obtener las ofertas de una empresa contratista
    List<Bid> findByContractorId(Long contractorId);
}
