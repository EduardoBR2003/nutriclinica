package br.edu.nutriclinica.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recordatorio_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecordatorioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refeicao_id", nullable = false)
    private RecordatorioRefeicao refeicao;

    @Column(name = "alimento", nullable = false, length = 150)
    private String alimento;

    @Column(name = "quantidade", length = 60)
    private String quantidade;

    @Column(name = "medida_caseira", length = 60)
    private String medidaCaseira;

    @Column(name = "ordem", nullable = false)
    private int ordem = 0;
}
