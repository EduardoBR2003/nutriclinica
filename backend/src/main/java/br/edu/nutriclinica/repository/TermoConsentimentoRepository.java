package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.TermoConsentimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TermoConsentimentoRepository extends JpaRepository<TermoConsentimento, Long> {

    Optional<TermoConsentimento> findByPacienteId(Long pacienteId);

    /** Pré-condição para abrir um atendimento. */
    boolean existsByPacienteIdAndAceiteLgpdTrue(Long pacienteId);

    /**
     * Dentre os pacientes informados, quais têm termo com aceite.
     *
     * <p>É daqui que sai o {@code possuiTermo} do PacienteResponse, derivado e
     * não persistido. Em lote para que uma página de 20 pacientes custe uma
     * consulta, não vinte.
     */
    @Query("""
            select t.paciente.id from TermoConsentimento t
            where t.aceiteLgpd = true and t.paciente.id in :pacienteIds
            """)
    List<Long> idsComAceite(@Param("pacienteIds") Collection<Long> pacienteIds);
}
