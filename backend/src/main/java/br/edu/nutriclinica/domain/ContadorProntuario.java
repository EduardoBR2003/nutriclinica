package br.edu.nutriclinica.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Uma linha por ano, com o último número de prontuário já entregue.
 *
 * <p>Não tem id sintético: a chave primária é o próprio ano, e é sobre essa
 * linha que o lock pessimista de {@code NumeroProntuarioService} serializa as
 * consultas abertas ao mesmo tempo.
 */
@Entity
@Table(name = "contador_prontuario")
@Getter
@Setter
@NoArgsConstructor
public class ContadorProntuario {

    @Id
    @Column(name = "ano")
    private Integer ano;

    @Column(name = "ultimo_numero", nullable = false)
    private long ultimoNumero;
}
