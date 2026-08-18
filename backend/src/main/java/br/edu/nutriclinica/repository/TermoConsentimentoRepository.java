package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.TermoConsentimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TermoConsentimentoRepository extends JpaRepository<TermoConsentimento, Long> {

    Optional<TermoConsentimento> findByPacienteId(Long pacienteId);

    /** Pré-condição para abrir um atendimento. */
    boolean existsByPacienteIdAndAceiteLgpdTrue(Long pacienteId);
}
