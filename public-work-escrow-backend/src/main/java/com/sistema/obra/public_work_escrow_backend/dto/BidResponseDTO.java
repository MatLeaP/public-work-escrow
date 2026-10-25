package com.sistema.obra.public_work_escrow_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BidResponseDTO {
    private Long id;
    private Long contractId;
    private String companyName;
    private BigDecimal proposedAmount;
    private String proposalIpfsHash;
    private String transactionHash;
    private String status;
    private LocalDateTime submissionDate;

    public BidResponseDTO(Long id, Long contractId, String companyName, BigDecimal proposedAmount, 
                          String proposalIpfsHash, String transactionHash, String status, LocalDateTime submissionDate) {
        this.id = id;
        this.contractId = contractId;
        this.companyName = companyName;
        this.proposedAmount = proposedAmount;
        this.proposalIpfsHash = proposalIpfsHash;
        this.transactionHash = transactionHash;
        this.status = status;
        this.submissionDate = submissionDate;
    }
}
