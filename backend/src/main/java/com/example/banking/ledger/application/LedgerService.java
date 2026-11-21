package com.example.banking.ledger.application;

import com.example.banking.accounts.infrastructure.AccountRepository;
import com.example.banking.ledger.api.LedgerEntryResponse;
import com.example.banking.ledger.infrastructure.LedgerEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LedgerService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(
            AccountRepository accountRepository,
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> findByAccount(
            Long accountId,
            Pageable pageable,
            Jwt jwt
    ) {
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found: " + accountId));

        boolean isAdmin = "ADMIN".equals(jwt.getClaimAsString("role"));
        boolean isOwner = account.getOwner().getEmail().equals(jwt.getSubject());
        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account access denied");
        }

        return ledgerEntryRepository
                .findAllByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(LedgerEntryResponse::from);
    }
}
