package br.edu.nutriclinica.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Diagnóstico no modelo PES: Problema, Etiologia, Sinais/Sintomas. */
@Entity
@Table(name = "diagnostico_pes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticoPes {

    @Id
    @Column(name = "atendimento_id")
    private Long atendimentoId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "atendimento_id")
    private Atendimento atendimento;

    @Column(name = "problema", columnDefinition = "TEXT")
    private String problema;

    @Column(name = "etiologia", columnDefinition = "TEXT")
    private String etiologia;

    @Column(name = "sinais_sintomas", columnDefinition = "TEXT")
    private String sinaisSintomas;
}
