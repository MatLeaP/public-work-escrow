package com.sistema.obra.public_work_escrow_backend.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "work_milestones")
@Data
public class WorkMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int hitoIndex;

    @Column(nullable = false)
    private String description; 

    @Column(nullable = false)
    private BigDecimal amountOrPercentage; 

    @Column(name = "estado_revision")
    private String estadoRevision = "PENDIENTE";

    @Column(nullable = false)
    private boolean completed = false;

  
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_work_contract_id", nullable = false)
    private PublicWorkContract contract;

    @Column(name = "document_ipfs_hash")
    private String documentIpfsHash;

    
    public WorkMilestone() {
    }

    public WorkMilestone(String description, BigDecimal amountOrPercentage, PublicWorkContract contract) {
        this.description = description;
        this.amountOrPercentage = amountOrPercentage;
        this.contract = contract;
        this.completed = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmountOrPercentage() {
        return amountOrPercentage;
    }

    public void setAmountOrPercentage(BigDecimal amountOrPercentage) {
        this.amountOrPercentage = amountOrPercentage;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public PublicWorkContract getContract() {
        return contract;
    }

    public void setContract(PublicWorkContract contract) {
        this.contract = contract;
    }
}
