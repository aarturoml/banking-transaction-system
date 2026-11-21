package com.example.banking.transfers.api;

import com.example.banking.transfers.domain.Transfer;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        Long id,
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt
) {

    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceAccount() == null ? null : transfer.getSourceAccount().getId(),
                transfer.getDestinationAccount() == null ? null : transfer.getDestinationAccount().getId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus().name(),
                transfer.getCreatedAt()
        );
    }
}
