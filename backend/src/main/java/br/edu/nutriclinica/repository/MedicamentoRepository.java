package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

    List<Medicamento> findByAtendimentoId(Long atendimentoId);

    void deleteByAtendimentoId(Long atendimentoId);
}
