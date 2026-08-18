package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.ExameBioquimico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExameBioquimicoRepository extends JpaRepository<ExameBioquimico, Long> {

    List<ExameBioquimico> findByAtendimentoId(Long atendimentoId);

    void deleteByAtendimentoId(Long atendimentoId);
}
