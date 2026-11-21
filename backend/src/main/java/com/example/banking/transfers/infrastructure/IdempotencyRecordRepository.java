package com.example.banking.transfers.infrastructure;

import com.example.banking.transfers.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByActorIdAndOperationAndKey(
            Long actorId,
            String operation,
            String key
    );
}
