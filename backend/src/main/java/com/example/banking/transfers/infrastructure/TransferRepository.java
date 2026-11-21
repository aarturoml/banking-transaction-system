package com.example.banking.transfers.infrastructure;

import com.example.banking.transfers.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
}
