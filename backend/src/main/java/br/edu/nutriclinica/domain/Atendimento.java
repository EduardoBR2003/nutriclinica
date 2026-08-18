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
import java.util.ArrayList;
import java.util.List;

/**
 * Raiz do prontuário.
 *
 * <p>As seções 1:1 apontam para cá com chave primária compartilhada (@MapsId) e
 * não têm navegação inversa: o contrato as salva uma a uma via PATCH, e cada
 * service busca a sua pelo repository.
 *
 * <p>As seções 1:N são coleções daqui, com {@code orphanRemoval}, porque o
 * contrato as substitui inteiras via PUT: o que sai da lista precisa sair do
 * banco junto, e é o Hibernate que emite esses DELETE. São todas LAZY — quem só
 * lê os campos base do atendimento não carrega nenhuma delas.
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

    // ------------------------------------------------------------------
    // Seções 1:N — substituídas inteiras pelos PUT do contrato
    // ------------------------------------------------------------------

    @OneToMany(mappedBy = "atendimento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Medicamento> medicamentos = new ArrayList<>();

    @OneToMany(mappedBy = "atendimento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExameBioquimico> exames = new ArrayList<>();

    @OneToMany(mappedBy = "atendimento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<RecordatorioRefeicao> refeicoes = new ArrayList<>();

    @OneToMany(mappedBy = "atendimento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Meta> metas = new ArrayList<>();
}
