package com.sistema.obra.public_work_escrow_backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "public_works_contracts")
public class PublicWorkContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)    
    private String contractCode;

    @Column(nullable = false, unique = true)
    private String contractAddress;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status;

    @Column(name = "blockchain_tx_hash")
    private String blockchainTxHash;

    @Column(name = ("ipfs_metadata_hash"))
    private String ipfsMetadataHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procuring_entity_id", nullable = false)
    private ProcuringEntity procuringEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_id", nullable = true)
    private Contractor contractor;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WorkMilestone> milestones = new ArrayList<>();

    @OneToMany(mappedBy = "publicWorkContract", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Bid> bids = new ArrayList<>();



    public enum ContractStatus {
        
        EN_CREACION,
        ACTIVO,
        EN_LIBERACION,
        ADJUDICADA,
        COMPLETADO,
        CANCELADO

    } 

    
    public PublicWorkContract(){

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = ContractStatus.EN_CREACION;

    }


    public PublicWorkContract(String contractCode, String title, BigDecimal totalAmount, ProcuringEntity procutingEntity){

        this();
        this.contractCode = contractCode;
        this.title = title;
        this.totalAmount = totalAmount;
        this.procuringEntity = procutingEntity;

    }

    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus status) { 
        this.status = status; 
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public List<WorkMilestone> getMilestones() {
        return milestones;
    }

    public void setMilestones(List<WorkMilestone> milestones) {
        this.milestones = milestones;
        if (milestones != null) {
            for (WorkMilestone milestone : milestones) {
                milestone.setContract(this);
            }
        }
    }

    // Método helper para agregar un hito cómodamente
    public void addMilestone(WorkMilestone milestone) {
        milestones.add(milestone);
        milestone.setContract(this);
    }

    public List<Bid> getBids() {
        return bids;
    }

    public void setBids(List<Bid> bids) {
        this.bids = bids;
        if (bids != null) {
            for (Bid bid : bids) {
                bid.setPublicWorkContract(this);
            }
        }
    }

    public void addBid(Bid bid) {
        bids.add(bid);
        bid.setPublicWorkContract(this);
    }
}
