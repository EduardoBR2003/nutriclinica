package br.edu.nutriclinica.domain;

import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Raiz do prontuário. As seções 1:1 apontam para cá com chave primária
 * compartilhada (@MapsId); a navegação inversa é feita pelos repositories,
 * já que o contrato salva seção por seção via PATCH.
 */
@Entity
@Table(name = "atendimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "numero_prontuario", nullable = false, length = 30, unique = true)
    private String numeroProntuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estagiario_id", nullable = false)
    private Usuario estagiario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private Usuario supervisor;

    @Column(name = "data_consulta", nullable = false)
    private LocalDate dataConsulta;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusAtendimento status = StatusAtendimento.RASCUNHO;

    @Column(name = "submetido_em")
    private LocalDateTime submetidoEm;

    @Column(name = "avaliado_em")
    private LocalDateTime avaliadoEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
