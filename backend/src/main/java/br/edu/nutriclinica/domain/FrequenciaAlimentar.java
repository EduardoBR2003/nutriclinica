package br.edu.nutriclinica.domain;

import br.edu.nutriclinica.domain.enums.Frequencia;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "frequencia_alimentar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FrequenciaAlimentar {

    @Id
    @Column(name = "atendimento_id")
    private Long atendimentoId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "atendimento_id")
    private Atendimento atendimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "frutas", length = 20)
    private Frequencia frutas;

    @Enumerated(EnumType.STRING)
    @Column(name = "verduras_legumes", length = 20)
    private Frequencia verdurasLegumes;

    @Enumerated(EnumType.STRING)
    @Column(name = "ultraprocessados", length = 20)
    private Frequencia ultraprocessados;

    @Enumerated(EnumType.STRING)
    @Column(name = "refrigerante", length = 20)
    private Frequencia refrigerante;

    @Enumerated(EnumType.STRING)
    @Column(name = "bebida_alcoolica", length = 20)
    private Frequencia bebidaAlcoolica;

    @Enumerated(EnumType.STRING)
    @Column(name = "cafe", length = 20)
    private Frequencia cafe;

    @Column(name = "agua_ml_dia")
    private Integer aguaMlDia;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;
}
