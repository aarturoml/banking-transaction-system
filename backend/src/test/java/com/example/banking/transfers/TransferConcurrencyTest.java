package com.example.banking.transfers;

import com.example.banking.accounts.domain.Account;
import com.example.banking.accounts.domain.Currency;
import com.example.banking.accounts.infrastructure.AccountRepository;
import com.example.banking.auth.domain.Role;
import com.example.banking.auth.domain.User;
import com.example.banking.auth.infrastructure.UserRepository;
import com.example.banking.transfers.api.DepositRequest;
import com.example.banking.transfers.api.TransferRequest;
import com.example.banking.transfers.application.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TransferConcurrencyTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferService transferService;

    @Test
    void concurrentTransfersDoNotOverdrawSourceAccount() throws Exception {
        User user = userRepository.save(new User(
                "concurrency-" + System.nanoTime() + "@example.com",
                "hashed-password",
                Role.CUSTOMER
        ));
        Account source = accountRepository.save(new Account(user, Currency.USD));
        Account destination = accountRepository.save(new Account(user, Currency.USD));
        Jwt jwt = jwt(user.getEmail());

        transferService.deposit(new DepositRequest(source.getId(), new BigDecimal("100")), jwt);

        int attempts = 20;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            int requestNumber = i;
            results.add(executor.submit(() -> {
                start.await();
                try {
                    transferService.transfer(
                            new TransferRequest(
                                    source.getId(),
                                    destination.getId(),
                                    new BigDecimal("10"),
                                    "USD"
                            ),
                            "concurrent-key-" + requestNumber,
                            jwt
                    );
                    return true;
                } catch (RuntimeException exception) {
                    return false;
                }
            }));
        }

        start.countDown();
        long successfulTransfers = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                successfulTransfers++;
            }
        }
        executor.shutdown();

        Account refreshedSource = accountRepository.findById(source.getId()).orElseThrow();
        Account refreshedDestination = accountRepository.findById(destination.getId()).orElseThrow();

        assertThat(successfulTransfers).isEqualTo(10);
        assertThat(refreshedSource.getBalance()).isEqualByComparingTo("0.0000");
        assertThat(refreshedDestination.getBalance()).isEqualByComparingTo("100.0000");
    }

    private Jwt jwt(String email) {
        return Jwt.withTokenValue("test-token")
                .subject(email)
                .claim("role", "CUSTOMER")
                .header("alg", "HS256")
                .build();
    }
}
