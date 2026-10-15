package com.sistema.obra.public_work_escrow_backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sistema.obra.public_work_escrow_backend.model.ProcuringEntity;
import com.sistema.obra.public_work_escrow_backend.repositories.ProcuringEntityRepository;

@Service
public class ProcuringEntityService {

    @Autowired 
    private ProcuringEntityRepository procuringEntityRepository;

    public List<ProcuringEntity> getAllProcuringEntities(){

        return procuringEntityRepository.findAll();
    }

    public Optional<ProcuringEntity> getProcuringEntityById(Long id){

        return procuringEntityRepository.findById(id);
    }

    public ProcuringEntity registerProcuringEntity(ProcuringEntity procuringEntity){

        return procuringEntityRepository.save(procuringEntity);
    }
}
