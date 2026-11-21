package com.example.banking.accounts.application;

import com.example.banking.accounts.api.AccountResponse;
import com.example.banking.accounts.api.CreateAccountRequest;
import com.example.banking.accounts.domain.Account;
import com.example.banking.accounts.domain.Currency;
import com.example.banking.accounts.infrastructure.AccountRepository;
import com.example.banking.auth.domain.User;
import com.example.banking.auth.infrastructure.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request, Jwt jwt) {
        User owner = findUser(jwt.getSubject());
        Currency currency = parseCurrency(request.currency());
        return AccountResponse.from(accountRepository.save(new Account(owner, currency)));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findAll(Jwt jwt) {
        List<Account> accounts = isAdmin(jwt)
                ? accountRepository.findAll()
                : accountRepository.findAllByOwnerEmail(jwt.getSubject());

        return accounts.stream().map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(Long id, Jwt jwt) {
        Account account = isAdmin(jwt)
                ? accountRepository.findById(id).orElseThrow(() -> accountNotFound(id))
                : accountRepository.findByIdAndOwnerEmail(id, jwt.getSubject())
                        .orElseThrow(() -> accountNotFound(id));

        return AccountResponse.from(account);
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

    private boolean isAdmin(Jwt jwt) {
        return "ADMIN".equals(jwt.getClaimAsString("role"));
    }

    private ResponseStatusException accountNotFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + id);
    }
}
