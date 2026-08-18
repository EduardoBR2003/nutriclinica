package br.edu.nutriclinica.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** LGPD: sem termo registrado, nenhum atendimento pode ser aberto. */
@Entity
@Table(name = "termo_consentimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TermoConsentimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false, unique = true)
    private Paciente paciente;

    @Column(name = "aceite_lgpd", nullable = false)
    private boolean aceiteLgpd;

    @Column(name = "autoriza_uso_pesquisa", nullable = false)
    private boolean autorizaUsoPesquisa = false;

    @Column(name = "data_aceite", nullable = false)
    private LocalDate dataAceite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    private Usuario registradoPor;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
