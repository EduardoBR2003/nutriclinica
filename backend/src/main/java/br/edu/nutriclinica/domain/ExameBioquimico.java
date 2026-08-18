package br.edu.nutriclinica.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "exame_bioquimico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExameBioquimico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atendimento_id", nullable = false)
    private Atendimento atendimento;

    @Column(name = "nome_exame", nullable = false, length = 80)
    private String nomeExame;

    @Column(name = "valor", precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "unidade", length = 20)
    private String unidade;

    @Column(name = "data_exame")
    private LocalDate dataExame;

    @Column(name = "valor_referencia", length = 60)
    private String valorReferencia;
}
