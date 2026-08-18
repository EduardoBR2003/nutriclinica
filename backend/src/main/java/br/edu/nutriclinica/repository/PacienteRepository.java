package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * O filtro LGPD (estagiário só enxerga os próprios pacientes, supervisor os dos
 * seus orientados) entra aqui em bloco posterior, sempre no repositório.
 */
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Page<Paciente> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}
