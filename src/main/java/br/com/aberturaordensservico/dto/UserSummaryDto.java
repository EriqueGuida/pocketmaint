package br.com.aberturaordensservico.dto;

import br.com.aberturaordensservico.model.enums.Role;

public record UserSummaryDto(
        Long id,
        String username,
        String fullName,
        Role role
) {}
