package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.ComentarioSecao;
import br.edu.nutriclinica.domain.enums.SecaoProntuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioSecaoRepository extends JpaRepository<ComentarioSecao, Long> {

    /**
     * O autor vem junto por {@code @EntityGraph}: ele é LAZY na entidade e entra
     * inteiro em toda resposta de comentário — sem isto, uma lista de dez
     * comentários custaria onze consultas.
     */
    @EntityGraph(attributePaths = "autor")
    List<ComentarioSecao> findByAtendimentoIdOrderByCriadoEmAsc(Long atendimentoId);

    List<ComentarioSecao> findByAtendimentoIdAndSecaoOrderByCriadoEmAsc(Long atendimentoId, SecaoProntuario secao);

    long countByAtendimentoIdAndResolvidoFalse(Long atendimentoId);
}
