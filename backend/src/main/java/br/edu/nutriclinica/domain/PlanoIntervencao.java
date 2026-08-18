package br.edu.nutriclinica.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "plano_intervencao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanoIntervencao {

    @Id
    @Column(name = "atendimento_id")
    private Long atendimentoId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "atendimento_id")
    private Atendimento atendimento;

    @Column(name = "objetivos", columnDefinition = "TEXT")
    private String objetivos;

    @Column(name = "prescricao_energetica_kcal")
    private Integer prescricaoEnergeticaKcal;

    @Column(name = "perc_carboidrato", precision = 4, scale = 1)
    private BigDecimal percCarboidrato;

    @Column(name = "perc_proteina", precision = 4, scale = 1)
    private BigDecimal percProteina;

    @Column(name = "perc_lipideo", precision = 4, scale = 1)
    private BigDecimal percLipideo;

    @Column(name = "estrategias_comportamentais", columnDefinition = "TEXT")
    private String estrategiasComportamentais;

    @Column(name = "educacao_alimentar", columnDefinition = "TEXT")
    private String educacaoAlimentar;
}
