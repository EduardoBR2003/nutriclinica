package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.VinculoSupervisao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VinculoSupervisaoRepository extends JpaRepository<VinculoSupervisao, Long> {

    List<VinculoSupervisao> findBySupervisorIdAndAtivoTrue(Long supervisorId);

    List<VinculoSupervisao> findByEstagiarioIdAndAtivoTrue(Long estagiarioId);

    Optional<VinculoSupervisao> findBySupervisorIdAndEstagiarioId(Long supervisorId, Long estagiarioId);

    boolean existsBySupervisorIdAndEstagiarioIdAndAtivoTrue(Long supervisorId, Long estagiarioId);
}
