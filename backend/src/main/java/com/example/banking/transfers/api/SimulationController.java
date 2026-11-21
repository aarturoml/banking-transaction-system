package com.example.banking.transfers.api;

import com.example.banking.transfers.application.TransferService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/simulations")
@SecurityRequirement(name = "BearerAuth")
public class SimulationController {

    private final TransferService transferService;

    public SimulationController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/deposits")
    public TransferResponse deposit(
            @Valid @RequestBody DepositRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return transferService.deposit(request, jwt);
    }
}
