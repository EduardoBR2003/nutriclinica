package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.AvaliacaoSupervisor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AvaliacaoSupervisorRepository extends JpaRepository<AvaliacaoSupervisor, Long> {

    Optional<AvaliacaoSupervisor> findByAtendimentoId(Long atendimentoId);

    boolean existsByAtendimentoId(Long atendimentoId);
}
