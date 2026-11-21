package com.example.banking.transfers.domain;

import com.example.banking.accounts.domain.Account;
import com.example.banking.ledger.domain.LedgerEntry;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.persistence.ManyToOne
    @jakarta.persistence.JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @jakarta.persistence.ManyToOne
    @jakarta.persistence.JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LedgerEntry> ledgerEntries = new ArrayList<>();

    protected Transfer() {
    }

    private Transfer(
            Account sourceAccount,
            Account destinationAccount,
            BigDecimal amount,
            String currency,
            TransferStatus status
    ) {
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public static Transfer completed(
            Account sourceAccount,
            Account destinationAccount,
            BigDecimal amount,
            String currency
    ) {
        return new Transfer(
                sourceAccount, destinationAccount, amount, currency, TransferStatus.COMPLETED);
    }

    public static Transfer simulationDeposit(
            Account destinationAccount,
            BigDecimal amount,
            String currency
    ) {
        return new Transfer(
                null, destinationAccount, amount, currency, TransferStatus.SIMULATION_DEPOSIT);
    }

    public Long getId() {
        return id;
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }

    public Account getDestinationAccount() {
        return destinationAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<LedgerEntry> getLedgerEntries() {
        return ledgerEntries;
    }

    public void addLedgerEntry(LedgerEntry entry) {
        ledgerEntries.add(entry);
    }
}
