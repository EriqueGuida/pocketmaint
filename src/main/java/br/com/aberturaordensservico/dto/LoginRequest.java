package br.com.aberturaordensservico.dto;

import br.com.aberturaordensservico.model.enums.OriginChannel;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "O nome de usuário não pode ser vazio")
    private String username;

    @NotBlank(message = "A senha não pode ser vazia")
    private String password;

    private OriginChannel clientChannel;
}
