package br.edu.nutriclinica.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "queixa_principal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QueixaPrincipal {

    @Id
    @Column(name = "atendimento_id")
    private Long atendimentoId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "atendimento_id")
    private Atendimento atendimento;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "tempo_queixa", length = 100)
    private String tempoQueixa;

    @Column(name = "tratamento_anterior", columnDefinition = "TEXT")
    private String tratamentoAnterior;

    @Column(name = "objetivo_consulta", columnDefinition = "TEXT")
    private String objetivoConsulta;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;
}
