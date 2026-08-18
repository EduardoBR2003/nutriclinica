package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    boolean existsByNumeroProntuario(String numeroProntuario);

    Page<Atendimento> findByEstagiarioIdAndStatus(Long estagiarioId, StatusAtendimento status, Pageable pageable);

    Page<Atendimento> findByEstagiarioId(Long estagiarioId, Pageable pageable);

    Page<Atendimento> findBySupervisorIdAndStatus(Long supervisorId, StatusAtendimento status, Pageable pageable);

    Page<Atendimento> findBySupervisorId(Long supervisorId, Pageable pageable);

    List<Atendimento> findByPacienteIdOrderByDataConsultaDesc(Long pacienteId);
}
