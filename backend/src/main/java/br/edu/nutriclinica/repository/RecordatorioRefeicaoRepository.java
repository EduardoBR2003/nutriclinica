package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.RecordatorioRefeicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecordatorioRefeicaoRepository extends JpaRepository<RecordatorioRefeicao, Long> {

    /**
     * Refeições e itens numa consulta só.
     *
     * <p>Sem o {@code join fetch}, montar o prontuário custaria uma consulta por
     * refeição para buscar os itens dela — o N+1 clássico, e num recordatório de
     * 24 horas são seis refeições por atendimento.
     *
     * <p>É a única coleção com {@code join fetch} aqui: buscar duas ou mais
     * listas na mesma consulta produziria produto cartesiano, e o Hibernate
     * recusa com {@code MultipleBagFetchException}.
     */
    @Query("""
            select distinct r from RecordatorioRefeicao r
            left join fetch r.itens
            where r.atendimento.id = :atendimentoId
            order by r.ordem
            """)
    List<RecordatorioRefeicao> comItens(@Param("atendimentoId") Long atendimentoId);
}
