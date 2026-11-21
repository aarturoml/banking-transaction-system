package com.example.banking.accounts;

import com.example.banking.accounts.domain.Account;
import com.example.banking.accounts.domain.Currency;
import com.example.banking.accounts.infrastructure.AccountRepository;
import com.example.banking.auth.domain.Role;
import com.example.banking.auth.domain.User;
import com.example.banking.auth.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void savesAccountWithZeroBalance() {
        User user = userRepository.save(
                new User("customer@example.com", "hashed-password", Role.CUSTOMER)
        );

        Account account = accountRepository.save(new Account(user, Currency.USD));

        assertThat(account.getId()).isNotNull();
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getCurrency()).isEqualTo(Currency.USD);
        assertThat(accountRepository.findAllByOwnerId(user.getId()))
                .singleElement()
                .extracting(Account::getId)
                .isEqualTo(account.getId());
    }
}
