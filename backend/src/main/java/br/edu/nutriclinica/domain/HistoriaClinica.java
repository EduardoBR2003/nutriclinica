package br.edu.nutriclinica.domain;

import br.edu.nutriclinica.domain.enums.HabitoIntestinal;
import br.edu.nutriclinica.domain.enums.NivelEstresse;
import br.edu.nutriclinica.domain.enums.QualidadeSono;
import br.edu.nutriclinica.domain.enums.Tabagismo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "historia_clinica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoriaClinica {

    @Id
    @Column(name = "atendimento_id")
    private Long atendimentoId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "atendimento_id")
    private Atendimento atendimento;

    @Column(name = "doencas_diagnosticadas", columnDefinition = "TEXT")
    private String doencasDiagnosticadas;

    @Column(name = "alergias", columnDefinition = "TEXT")
    private String alergias;

    @Column(name = "intolerancias", columnDefinition = "TEXT")
    private String intolerancias;

    @Column(name = "historico_familiar", columnDefinition = "TEXT")
    private String historicoFamiliar;

    @Enumerated(EnumType.STRING)
    @Column(name = "tabagismo", length = 20)
    private Tabagismo tabagismo;

    @Column(name = "horas_sono", precision = 3, scale = 1)
    private BigDecimal horasSono;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualidade_sono", length = 20)
    private QualidadeSono qualidadeSono;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_estresse", length = 20)
    private NivelEstresse nivelEstresse;

    @Enumerated(EnumType.STRING)
    @Column(name = "habito_intestinal", length = 30)
    private HabitoIntestinal habitoIntestinal;

    @Column(name = "pratica_atividade_fisica")
    private Boolean praticaAtividadeFisica;

    @Column(name = "descricao_atividade", columnDefinition = "TEXT")
    private String descricaoAtividade;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;
}
