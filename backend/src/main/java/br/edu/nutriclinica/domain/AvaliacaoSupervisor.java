package br.edu.nutriclinica.domain;

import br.edu.nutriclinica.domain.enums.ResultadoAvaliacao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "avaliacao_supervisor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvaliacaoSupervisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atendimento_id", nullable = false, unique = true)
    private Atendimento atendimento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private Usuario supervisor;

    @Column(name = "nota_final", precision = 4, scale = 2)
    private BigDecimal notaFinal;

    @Column(name = "parecer_geral", columnDefinition = "TEXT")
    private String parecerGeral;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", length = 30)
    private ResultadoAvaliacao resultado;

    @CreationTimestamp
    @Column(name = "avaliado_em", nullable = false)
    private LocalDateTime avaliadoEm;

    /** A rubrica é sempre enviada em bloco junto com a avaliação. */
    @OneToMany(mappedBy = "avaliacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<ItemRubrica> itens = new ArrayList<>();

    public void adicionarItem(ItemRubrica item) {
        item.setAvaliacao(this);
        this.itens.add(item);
    }
}
