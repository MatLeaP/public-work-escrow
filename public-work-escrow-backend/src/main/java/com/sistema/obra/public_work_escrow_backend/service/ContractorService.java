package com.sistema.obra.public_work_escrow_backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sistema.obra.public_work_escrow_backend.model.Contractor;
import com.sistema.obra.public_work_escrow_backend.repositories.ContractorRepository;

@Service 
public class ContractorService {

    @Autowired 
    private ContractorRepository contractorRepository;

    public List<Contractor> getAllContractors(){

        return contractorRepository.findAll();
    }

    public Optional<Contractor> getContractorById(Long id){

        return contractorRepository.findById(id);
    }

    public Contractor registerContractor(Contractor contractor){

        return contractorRepository.save(contractor);
    }

}
