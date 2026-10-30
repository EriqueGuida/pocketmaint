package br.com.aberturaordensservico.dto;

import br.com.aberturaordensservico.model.enums.Role;

public record UserMeResponse(
        Long id,
        String username,
        String fullName,
        Role role,
        boolean active
) {}
