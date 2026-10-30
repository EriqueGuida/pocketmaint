package br.com.aberturaordensservico.model;

import br.com.aberturaordensservico.model.Setor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "setor")
@Getter
@Setter
@NoArgsConstructor
public class Setor{

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank (message = "O nome do setor não pode ser vazio")
    private String nome;

    public Setor(String nome) {
        this.nome = nome;
    }
}