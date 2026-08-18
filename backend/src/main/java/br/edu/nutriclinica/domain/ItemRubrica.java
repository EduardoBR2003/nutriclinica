package br.edu.nutriclinica.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "item_rubrica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemRubrica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "avaliacao_id", nullable = false)
    private AvaliacaoSupervisor avaliacao;

    @Column(name = "criterio", nullable = false, length = 120)
    private String criterio;

    @Column(name = "peso", nullable = false, precision = 4, scale = 2)
    private BigDecimal peso = BigDecimal.ONE;

    @Column(name = "nota", precision = 4, scale = 2)
    private BigDecimal nota;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "ordem", nullable = false)
    private int ordem = 0;
}
