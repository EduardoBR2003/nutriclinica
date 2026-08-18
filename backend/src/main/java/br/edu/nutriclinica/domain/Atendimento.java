package br.edu.nutriclinica.domain;

import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import jakarta.persistence.*;
import lombok.AccessLevel;
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

    /**
     * Os três campos da máquina de estados não têm setter: escrevem-se juntos,
     * por {@link #aplicarTransicao}, e nunca um sem o outro.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Setter(AccessLevel.NONE)
    private StatusAtendimento status = StatusAtendimento.RASCUNHO;

    @Column(name = "submetido_em")
    @Setter(AccessLevel.NONE)
    private LocalDateTime submetidoEm;

    @Column(name = "avaliado_em")
    @Setter(AccessLevel.NONE)
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

    // ------------------------------------------------------------------
    // Máquina de estados
    // ------------------------------------------------------------------

    /**
     * Registra uma transição de estado já decidida.
     *
     * <p>Único caminho de escrita do status, e é por ele que os carimbos de tempo
     * ficam coerentes: {@code submetidoEm} é, por definição, o instante em que o
     * prontuário entrou em EM_REVISAO, e {@code avaliadoEm} o instante em que o
     * supervisor o fechou. Deixá-los a cargo de quem chama abriria a
     * possibilidade de um atendimento EM_REVISAO sem data de submissão — estado
     * que a fila de revisão não saberia ordenar.
     *
     * <p>Quem decide se a transição é <b>permitida</b> é o
     * {@code AtendimentoWorkflowService}, e só ele deveria chamar este método.
     * A entidade é o registro do que foi decidido, não a regra: ela não conhece
     * a tabela de transições nem quem está autenticado.
     *
     * <p>Os setters dos três campos foram removidos justamente para que essa
     * regra não dependa de disciplina — um {@code setStatus} esquecido em algum
     * serviço novo não compila. O Hibernate não se importa: o mapeamento é por
     * campo (o {@code @Id} está no campo), então ele nunca chamou esses setters.
     */
    public void aplicarTransicao(StatusAtendimento novo, LocalDateTime momento) {
        this.status = novo;
        switch (novo) {
            case EM_REVISAO -> this.submetidoEm = momento;
            case APROVADO, DEVOLVIDO_PARA_CORRECAO -> this.avaliadoEm = momento;
            case RASCUNHO -> { }
        }
    }
}
