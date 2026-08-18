package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * O filtro de LGPD é parte da query, não um passo depois dela: o id do usuário
 * autenticado entra como parâmetro e o banco devolve apenas o que ele pode ver.
 * Carregar tudo e filtrar em Java daria o mesmo JSON, mas com os dados de saúde
 * de toda a clínica trafegando para a aplicação a cada listagem.
 *
 * <p>Um método por perfil em vez de uma query única com CASE: o escopo de cada
 * perfil fica legível e auditável isoladamente, que é o que uma regra de LGPD
 * precisa ser.
 *
 * <p>{@code busca} nunca chega nula — o serviço normaliza para string vazia, que
 * casa com todos os nomes. Evita o {@code (:busca is null or ...)} e, com ele,
 * um parâmetro sem tipo definido no Postgres.
 */
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    /** Cadastrou o paciente, ou o atendeu em algum momento. */
    String ESCOPO_ESTAGIARIO = """
            (p.criadoPor.id = :usuarioId
             or exists (select 1 from Atendimento a
                        where a.paciente = p and a.estagiario.id = :usuarioId))
            """;

    /**
     * Supervisiona o atendimento, ou o paciente pertence a um estagiário que ele
     * orienta — seja porque o estagiário o cadastrou, seja porque o atendeu.
     */
    String ESCOPO_SUPERVISOR = """
            (exists (select 1 from Atendimento a
                     where a.paciente = p and a.supervisor.id = :usuarioId)
             or exists (select 1 from Atendimento a2
                        where a2.paciente = p
                          and a2.estagiario.id in (select v.estagiario.id from VinculoSupervisao v
                                                   where v.supervisor.id = :usuarioId and v.ativo = true))
             or p.criadoPor.id in (select v2.estagiario.id from VinculoSupervisao v2
                                          where v2.supervisor.id = :usuarioId and v2.ativo = true))
            """;

    String FILTRO_BUSCA = "lower(p.nome) like lower(concat('%', :busca, '%'))";

    @Query("select p from Paciente p where " + FILTRO_BUSCA + " and " + ESCOPO_ESTAGIARIO)
    Page<Paciente> listarParaEstagiario(@Param("usuarioId") Long usuarioId,
                                        @Param("busca") String busca,
                                        Pageable pageable);

    @Query("select p from Paciente p where " + FILTRO_BUSCA + " and " + ESCOPO_SUPERVISOR)
    Page<Paciente> listarParaSupervisor(@Param("usuarioId") Long usuarioId,
                                        @Param("busca") String busca,
                                        Pageable pageable);

    /** O ADMIN enxerga a clínica inteira; sobra apenas o filtro de busca. */
    @Query("select p from Paciente p where " + FILTRO_BUSCA)
    Page<Paciente> listarParaAdmin(@Param("busca") String busca, Pageable pageable);

    @Query("select p from Paciente p where p.id = :id and " + ESCOPO_ESTAGIARIO)
    Optional<Paciente> buscarVisivelPeloEstagiario(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Query("select p from Paciente p where p.id = :id and " + ESCOPO_SUPERVISOR)
    Optional<Paciente> buscarVisivelPeloSupervisor(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}
