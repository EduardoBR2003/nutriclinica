package br.edu.nutriclinica.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "comportamento_alimentar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComportamentoAlimentar {

    @Id
    @Column(name = "atendimento_id")
    private Long atendimentoId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "atendimento_id")
    private Atendimento atendimento;

    @Column(name = "come_assistindo_tela")
    private Boolean comeAssistindoTela;

    @Column(name = "come_rapido")
    private Boolean comeRapido;

    @Column(name = "pula_refeicoes")
    private Boolean pulaRefeicoes;

    @Column(name = "compulsao_alimentar")
    private Boolean compulsaoAlimentar;

    @Column(name = "alimentacao_emocional")
    private Boolean alimentacaoEmocional;

    @Column(name = "restricao_alimentar")
    private Boolean restricaoAlimentar;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;
}
