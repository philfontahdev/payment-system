package com.auth_payment_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentRequest(
        @NotNull @Min(50) Long amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        String description
) {}
