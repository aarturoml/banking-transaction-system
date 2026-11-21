package com.example.banking.transfers;

import com.example.banking.accounts.domain.Account;
import com.example.banking.accounts.domain.Currency;
import com.example.banking.accounts.infrastructure.AccountRepository;
import com.example.banking.auth.domain.Role;
import com.example.banking.auth.domain.User;
import com.example.banking.auth.infrastructure.UserRepository;
import com.example.banking.transfers.api.DepositRequest;
import com.example.banking.transfers.api.TransferRequest;
import com.example.banking.transfers.api.TransferResponse;
import com.example.banking.transfers.application.TransferService;
import com.example.banking.transfers.infrastructure.IdempotencyRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TransferServiceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferService transferService;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Test
    void transferIsAtomicAndIdempotent() {
        User user = userRepository.save(new User(
                "transfer-" + System.nanoTime() + "@example.com",
                "hashed-password",
                Role.CUSTOMER
        ));
        Account source = accountRepository.save(new Account(user, Currency.USD));
        Account destination = accountRepository.save(new Account(user, Currency.USD));
        Jwt adminJwt = jwt(user.getEmail(), "ADMIN");
        Jwt customerJwt = jwt(user.getEmail(), "CUSTOMER");

        transferService.deposit(new DepositRequest(source.getId(), new BigDecimal("100.0000")), adminJwt);

        TransferRequest request = new TransferRequest(
                source.getId(),
                destination.getId(),
                new BigDecimal("30.0000"),
                "USD"
        );

        TransferResponse first = transferService.transfer(request, "same-key", customerJwt);
        TransferResponse repeated = transferService.transfer(request, "same-key", customerJwt);

        Account refreshedSource = accountRepository.findById(source.getId()).orElseThrow();
        Account refreshedDestination = accountRepository.findById(destination.getId()).orElseThrow();

        assertThat(first.id()).isEqualTo(repeated.id());
        assertThat(refreshedSource.getBalance()).isEqualByComparingTo("70.0000");
        assertThat(refreshedDestination.getBalance()).isEqualByComparingTo("30.0000");
        assertThat(idempotencyRecordRepository.findByActorIdAndOperationAndKey(
                user.getId(), "TRANSFER", "same-key"
        )).isPresent();
    }

    @Test
    void rejectsSameIdempotencyKeyWithDifferentPayload() {
        User user = userRepository.save(new User(
                "conflict-" + System.nanoTime() + "@example.com",
                "hashed-password",
                Role.CUSTOMER
        ));
        Account source = accountRepository.save(new Account(user, Currency.USD));
        Account destination = accountRepository.save(new Account(user, Currency.USD));
        Jwt jwt = jwt(user.getEmail(), "CUSTOMER");

        transferService.deposit(new DepositRequest(source.getId(), new BigDecimal("100")), jwt);

        TransferRequest first = new TransferRequest(
                source.getId(), destination.getId(), new BigDecimal("10"), "USD");
        TransferRequest different = new TransferRequest(
                source.getId(), destination.getId(), new BigDecimal("20"), "USD");

        transferService.transfer(first, "conflicting-key", jwt);

        assertThatThrownBy(() -> transferService.transfer(different, "conflicting-key", jwt))
                .hasMessageContaining("different payload");
    }

    private Jwt jwt(String email, String role) {
        return Jwt.withTokenValue("test-token")
                .subject(email)
                .claim("role", role)
                .header("alg", "HS256")
                .build();
    }
}
