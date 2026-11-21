package com.example.banking.accounts.api;

import jakarta.validation.constraints.NotBlank;

public record CreateAccountRequest(
        @NotBlank String currency
) {
}
