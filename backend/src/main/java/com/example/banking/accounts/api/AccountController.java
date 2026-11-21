package com.example.banking.accounts.api;

import com.example.banking.accounts.application.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@SecurityRequirement(name = "BearerAuth")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return accountService.create(request, jwt);
    }

    @GetMapping
    public List<AccountResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
        return accountService.findAll(jwt);
    }

    @GetMapping("/{id}")
    public AccountResponse findById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return accountService.findById(id, jwt);
    }
}
