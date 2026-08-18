package br.edu.nutriclinica.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "dados_gestante_infantil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DadosGestanteInfantil {

    @Id
    @Column(name = "atendimento_id")
    private Long atendimentoId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "atendimento_id")
    private Atendimento atendimento;

    @Column(name = "peso_pre_gestacional_kg", precision = 6, scale = 2)
    private BigDecimal pesoPreGestacionalKg;

    @Column(name = "idade_gestacional_semanas")
    private Integer idadeGestacionalSemanas;

    @Column(name = "ganho_peso_gestacional_kg", precision = 5, scale = 2)
    private BigDecimal ganhoPesoGestacionalKg;

    @Column(name = "escore_peso_estatura", precision = 5, scale = 2)
    private BigDecimal escorePesoEstatura;

    @Column(name = "escore_peso_idade", precision = 5, scale = 2)
    private BigDecimal escorePesoIdade;

    @Column(name = "escore_estatura_idade", precision = 5, scale = 2)
    private BigDecimal escoreEstaturaIdade;

    @Column(name = "escore_imc_idade", precision = 5, scale = 2)
    private BigDecimal escoreImcIdade;
}
