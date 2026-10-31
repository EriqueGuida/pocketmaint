package br.com.pocketmaint.dto;

import br.com.pocketmaint.model.enums.Role;

public record UserMeResponse(
        Long id,
        String username,
        String fullName,
        Role role,
        boolean active
) {}
