package com.example.banking.ledger.infrastructure;

import com.example.banking.ledger.domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    Page<LedgerEntry> findAllByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);
}
