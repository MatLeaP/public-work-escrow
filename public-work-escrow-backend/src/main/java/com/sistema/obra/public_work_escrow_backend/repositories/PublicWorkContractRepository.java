package com.sistema.obra.public_work_escrow_backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.obra.public_work_escrow_backend.model.PublicWorkContract;

@Repository 
public interface PublicWorkContractRepository extends JpaRepository<PublicWorkContract, Long>{

    Optional<PublicWorkContract> findByContractCode(String contractCode);

    List<PublicWorkContract> findByStatus(PublicWorkContract.ContractStatus status);

    List<PublicWorkContract> findByProcuringEntityId(Long procuringEntityId);

    List<PublicWorkContract> findByContractorId(Long contractorId);

    List<PublicWorkContract> findAll();

    Optional<PublicWorkContract> findByContractAddress(String contractAddress);


}
