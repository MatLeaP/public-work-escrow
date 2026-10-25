package com.sistema.obra.public_work_escrow_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "bids")
@Data
@NoArgsConstructor
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal proposedAmount;

    @Column(nullable = false)
    private String proposalIpfsHash;

    @Column(length = 66)
    private String transactionHash;

    @Column(nullable = false)
    private LocalDateTime submissionDate;

    @Column(nullable = false)
    private String status; 

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_work_contract_id", nullable = false)
    @JsonBackReference
    private PublicWorkContract publicWorkContract;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_id", nullable = false)
    private Contractor contractor;

    @PrePersist
    public void prePersist() {
        this.submissionDate = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}