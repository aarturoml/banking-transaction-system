package com.example.banking.accounts.api;

import com.example.banking.accounts.domain.Account;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        String ownerEmail,
        String currency,
        BigDecimal balance,
        Instant createdAt
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getOwner().getEmail(),
                account.getCurrency().name(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }
}
