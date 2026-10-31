package br.com.pocketmaint.dto;

import br.com.pocketmaint.model.enums.Role;

public record UserSummaryDto(
        Long id,
        String username,
        String fullName,
        Role role
) {}
