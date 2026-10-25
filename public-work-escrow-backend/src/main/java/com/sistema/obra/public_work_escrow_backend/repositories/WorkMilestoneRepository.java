package com.sistema.obra.public_work_escrow_backend.repositories;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.obra.public_work_escrow_backend.model.WorkMilestone;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkMilestoneRepository extends JpaRepository<WorkMilestone, Long> {


    List<WorkMilestone> findByContractId(Long contractId);

 
    List<WorkMilestone> findByCompleted(boolean completed);

    Optional<WorkMilestone> findByContract_ContractAddressAndHitoIndex(String contractAddress, int hitoIndex);

    List<WorkMilestone> findByContract_ContractAddress(String contractAddress);
}
