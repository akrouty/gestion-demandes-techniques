package com.akrouty.gestiondemandes.request.presentation;

import jakarta.validation.constraints.NotBlank;

public record AnalyseIaRequest(@NotBlank String titre, @NotBlank String description) {
}
