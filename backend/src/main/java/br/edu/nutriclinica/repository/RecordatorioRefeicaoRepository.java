package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.RecordatorioRefeicao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordatorioRefeicaoRepository extends JpaRepository<RecordatorioRefeicao, Long> {

    List<RecordatorioRefeicao> findByAtendimentoIdOrderByOrdemAsc(Long atendimentoId);

    void deleteByAtendimentoId(Long atendimentoId);
}
