package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.VinculoSupervisao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VinculoSupervisaoRepository extends JpaRepository<VinculoSupervisao, Long> {

    List<VinculoSupervisao> findBySupervisorIdAndAtivoTrue(Long supervisorId);

    List<VinculoSupervisao> findByEstagiarioIdAndAtivoTrue(Long estagiarioId);

    /** Inclui os desativados: é a base da reconciliação do PUT de vínculos. */
    List<VinculoSupervisao> findBySupervisorId(Long supervisorId);

    Optional<VinculoSupervisao> findBySupervisorIdAndEstagiarioId(Long supervisorId, Long estagiarioId);

    boolean existsBySupervisorIdAndEstagiarioIdAndAtivoTrue(Long supervisorId, Long estagiarioId);

    /**
     * Os supervisores que o estagiário pode escolher ao abrir um atendimento —
     * exatamente o conjunto que {@code AtendimentoService.validarSupervisor}
     * aceita, mais o filtro de conta ativa: oferecer um supervisor desligado da
     * casa só produziria um atendimento que ninguém revisa.
     */
    @Query("""
            select v.supervisor from VinculoSupervisao v
            where v.estagiario.id = :estagiarioId
              and v.ativo = true
              and v.supervisor.ativo = true
            order by v.supervisor.nome
            """)
    List<Usuario> supervisoresDoEstagiario(@Param("estagiarioId") Long estagiarioId);

    /** Os orientados de um supervisor, para a tela de vínculos abrir já marcada. */
    @Query("""
            select v.estagiario from VinculoSupervisao v
            where v.supervisor.id = :supervisorId
              and v.ativo = true
            order by v.estagiario.nome
            """)
    List<Usuario> estagiariosDoSupervisor(@Param("supervisorId") Long supervisorId);
}
