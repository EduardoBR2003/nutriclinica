package br.edu.nutriclinica.domain;

import br.edu.nutriclinica.domain.enums.RiscoCardiovascular;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * imc, classificacaoImc, relacaoCinturaQuadril e riscoCardiovascular são
 * gravados pelo backend a partir dos valores medidos. NUNCA aceitar do cliente.
 */
@Entity
@Table(name = "antropometria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Antropometria {

    @Id
    @Column(name = "atendimento_id")
    private Long atendimentoId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "atendimento_id")
    private Atendimento atendimento;

    @Column(name = "peso_kg", precision = 6, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "altura_cm", precision = 5, scale = 1)
    private BigDecimal alturaCm;

    @Column(name = "circ_cintura_cm", precision = 5, scale = 1)
    private BigDecimal circCinturaCm;

    @Column(name = "circ_quadril_cm", precision = 5, scale = 1)
    private BigDecimal circQuadrilCm;

    @Column(name = "percentual_gordura", precision = 4, scale = 1)
    private BigDecimal percentualGordura;

    @Column(name = "massa_magra_kg", precision = 6, scale = 2)
    private BigDecimal massaMagraKg;

    @Column(name = "imc", precision = 5, scale = 2)
    private BigDecimal imc;

    @Column(name = "classificacao_imc", length = 40)
    private String classificacaoImc;

    @Column(name = "relacao_cintura_quadril", precision = 4, scale = 2)
    private BigDecimal relacaoCinturaQuadril;

    @Enumerated(EnumType.STRING)
    @Column(name = "risco_cardiovascular", length = 20)
    private RiscoCardiovascular riscoCardiovascular;

    @Column(name = "aferido_em")
    private LocalDate aferidoEm;
}
