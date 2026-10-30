package br.com.aberturaordensservico.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "equipamento")
@Getter
@Setter
@NoArgsConstructor
public class Equipamento { 
    
    public Equipamento(String nome, String numeroPatrimonio, Setor setor) {
        this.nome = nome;
        this.numeroPatrimonio = numeroPatrimonio;
        this.setor = setor;
    }
    
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank (message = "O nome do equipamento não pode ser vazio")
    private String nome;

    @NotBlank (message = "O número de patrimônio do equipamento não pode ser vazio")
    private String numeroPatrimonio;

    @ManyToOne
    @JoinColumn (name = "setor_id", nullable = false)
    @NotNull (message = "O setor do equipamento não pode ser vazio")
    private Setor setor;
}
