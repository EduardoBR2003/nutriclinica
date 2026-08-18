package br.edu.nutriclinica.domain;

import br.edu.nutriclinica.domain.enums.TipoRefeicao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Recordatório 24h: atendimento -> refeições -> itens. */
@Entity
@Table(name = "recordatorio_refeicao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecordatorioRefeicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atendimento_id", nullable = false)
    private Atendimento atendimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_refeicao", nullable = false, length = 30)
    private TipoRefeicao tipoRefeicao;

    @Column(name = "horario")
    private LocalTime horario;

    @Column(name = "local_refeicao", length = 80)
    private String localRefeicao;

    @Column(name = "ordem", nullable = false)
    private int ordem = 0;

    /** Os itens são sempre gravados em bloco junto com a refeição. */
    @OneToMany(mappedBy = "refeicao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<RecordatorioItem> itens = new ArrayList<>();

    public void adicionarItem(RecordatorioItem item) {
        item.setRefeicao(this);
        this.itens.add(item);
    }
}
