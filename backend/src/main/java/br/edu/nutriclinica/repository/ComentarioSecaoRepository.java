package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.ComentarioSecao;
import br.edu.nutriclinica.domain.enums.SecaoProntuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioSecaoRepository extends JpaRepository<ComentarioSecao, Long> {

    List<ComentarioSecao> findByAtendimentoIdOrderByCriadoEmAsc(Long atendimentoId);

    List<ComentarioSecao> findByAtendimentoIdAndSecaoOrderByCriadoEmAsc(Long atendimentoId, SecaoProntuario secao);

    long countByAtendimentoIdAndResolvidoFalse(Long atendimentoId);
}
