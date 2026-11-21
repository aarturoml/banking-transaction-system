package com.example.banking.ledger.api;

import com.example.banking.ledger.domain.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryResponse(
        Long id,
        Long accountId,
        Long transferId,
        String direction,
        BigDecimal amount,
        Instant createdAt
) {

    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getAccount().getId(),
                entry.getTransfer() == null ? null : entry.getTransfer().getId(),
                entry.getDirection().name(),
                entry.getAmount(),
                entry.getCreatedAt()
        );
    }
}
