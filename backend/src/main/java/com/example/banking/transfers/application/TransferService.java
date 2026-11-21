package com.example.banking.transfers.application;

import com.example.banking.accounts.domain.Account;
import com.example.banking.accounts.domain.Currency;
import com.example.banking.accounts.infrastructure.AccountRepository;
import com.example.banking.auth.domain.User;
import com.example.banking.auth.infrastructure.UserRepository;
import com.example.banking.ledger.domain.LedgerDirection;
import com.example.banking.ledger.domain.LedgerEntry;
import com.example.banking.transfers.api.DepositRequest;
import com.example.banking.transfers.api.TransferRequest;
import com.example.banking.transfers.api.TransferResponse;
import com.example.banking.transfers.domain.IdempotencyRecord;
import com.example.banking.transfers.domain.Transfer;
import com.example.banking.transfers.infrastructure.IdempotencyRecordRepository;
import com.example.banking.transfers.infrastructure.TransferRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

@Service
public class TransferService {

    private static final String TRANSFER_OPERATION = "TRANSFER";

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransferRepository transferRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public TransferService(
            AccountRepository accountRepository,
            UserRepository userRepository,
            TransferRepository transferRepository,
            IdempotencyRecordRepository idempotencyRecordRepository
    ) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transferRepository = transferRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request, String idempotencyKey, Jwt jwt) {
        validateIdempotencyKey(idempotencyKey);
        User actor = findUser(jwt.getSubject());
        String requestHash = hash(request);

        var existing = idempotencyRecordRepository
                .findByActorIdAndOperationAndKey(actor.getId(), TRANSFER_OPERATION, idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Idempotency key was used with a different payload");
            }
            return transferRepository.findById(existing.get().getResponseReference())
                    .map(TransferResponse::from)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Idempotency record is incomplete"));
        }

        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Source and destination accounts must differ");
        }

        Currency currency = parseCurrency(request.currency());
        Long firstId = Math.min(request.sourceAccountId(), request.destinationAccountId());
        Long secondId = Math.max(request.sourceAccountId(), request.destinationAccountId());
        Account firstLocked = lockAccount(firstId);
        Account secondLocked = lockAccount(secondId);
        Account source = request.sourceAccountId().equals(firstId) ? firstLocked : secondLocked;
        Account destination = request.destinationAccountId().equals(firstId) ? firstLocked : secondLocked;

        if (source.getOwner().getId() == null
                || !source.getOwner().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Source account is not owned by actor");
        }
        if (source.getCurrency() != currency || destination.getCurrency() != currency) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency mismatch");
        }

        try {
            source.debit(request.amount());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds");
        }
        destination.credit(request.amount());

        Transfer transfer = Transfer.completed(
                source, destination, request.amount(), currency.name());
        transfer.addLedgerEntry(new LedgerEntry(
                transfer, source, LedgerDirection.DEBIT, request.amount()));
        transfer.addLedgerEntry(new LedgerEntry(
                transfer, destination, LedgerDirection.CREDIT, request.amount()));
        transferRepository.saveAndFlush(transfer);

        idempotencyRecordRepository.save(new IdempotencyRecord(
                actor, TRANSFER_OPERATION, idempotencyKey, requestHash, transfer.getId()));

        return TransferResponse.from(transfer);
    }

    @Transactional
    public TransferResponse deposit(DepositRequest request, Jwt jwt) {
        Account account = lockAccount(request.accountId());
        Currency currency = account.getCurrency();
        account.credit(request.amount());

        Transfer deposit = Transfer.simulationDeposit(
                account, request.amount(), currency.name());
        deposit.addLedgerEntry(new LedgerEntry(
                deposit, account, LedgerDirection.CREDIT, request.amount()));
        transferRepository.saveAndFlush(deposit);

        return TransferResponse.from(deposit);
    }

    private Account lockAccount(Long id) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found: " + id));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));
    }

    private Currency parseCurrency(String value) {
        try {
            return Currency.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported currency: " + value);
        }
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Idempotency-Key must contain 1 to 255 characters");
        }
    }

    private String hash(TransferRequest request) {
        String payload = request.sourceAccountId()
                + "|" + request.destinationAccountId()
                + "|" + request.amount().stripTrailingZeros().toPlainString()
                + "|" + request.currency().trim().toUpperCase(Locale.ROOT);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
