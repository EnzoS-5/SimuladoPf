package com.ProjSoft.Simulado.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AvaliacaoRequest(
    @NotBlank @Email @Size(max = 254) String emailAvaliado,
    @NotBlank @Size(max = 4000) String comentarios,
    @NotNull @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer = 2, fraction = 2) BigDecimal nota
) {}
