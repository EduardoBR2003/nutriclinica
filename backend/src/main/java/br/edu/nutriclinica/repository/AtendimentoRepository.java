package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Como em {@link PacienteRepository}, o escopo de LGPD é predicado da query e
 * não filtro em Java. Não existe aqui nenhum método que liste atendimentos sem
 * o id do usuário autenticado: se existisse, seria só questão de tempo até
 * alguém chamá-lo por engano e vazar prontuário alheio.
 *
 * <p>Os filtros opcionais do contrato (status e pacienteId) usam
 * {@code (:x is null or ...)}. O tipo do parâmetro é inferido da comparação ao
 * lado, então o Postgres não reclama de parâmetro indeterminado.
 */
public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    String ESCOPO_ESTAGIARIO = "a.estagiario.id = :usuarioId";

    /** É o supervisor designado, ou orienta o estagiário que conduziu o atendimento. */
    String ESCOPO_SUPERVISOR = """
            (a.supervisor.id = :usuarioId
             or a.estagiario.id in (select v.estagiario.id from VinculoSupervisao v
                                    where v.supervisor.id = :usuarioId and v.ativo = true))
            """;

    String FILTROS = """
            (:status is null or a.status = :status)
            and (:pacienteId is null or a.paciente.id = :pacienteId)
            """;

    @Query("select a from Atendimento a where " + FILTROS + " and " + ESCOPO_ESTAGIARIO)
    Page<Atendimento> listarParaEstagiario(@Param("usuarioId") Long usuarioId,
                                           @Param("status") StatusAtendimento status,
                                           @Param("pacienteId") Long pacienteId,
                                           Pageable pageable);

    @Query("select a from Atendimento a where " + FILTROS + " and " + ESCOPO_SUPERVISOR)
    Page<Atendimento> listarParaSupervisor(@Param("usuarioId") Long usuarioId,
                                           @Param("status") StatusAtendimento status,
                                           @Param("pacienteId") Long pacienteId,
                                           Pageable pageable);

    @Query("select a from Atendimento a where " + FILTROS)
    Page<Atendimento> listarParaAdmin(@Param("status") StatusAtendimento status,
                                      @Param("pacienteId") Long pacienteId,
                                      Pageable pageable);

    @Query("select a from Atendimento a where a.id = :id and " + ESCOPO_ESTAGIARIO)
    Optional<Atendimento> buscarVisivelPeloEstagiario(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Query("select a from Atendimento a where a.id = :id and " + ESCOPO_SUPERVISOR)
    Optional<Atendimento> buscarVisivelPeloSupervisor(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    /**
     * Quais seções já têm registro persistido, para um conjunto de atendimentos.
     *
     * <p>Uma consulta para a página inteira em vez de onze por atendimento — numa
     * listagem de 20, a diferença é entre 1 e 220 idas ao banco. Cada seção nova
     * do prontuário entra aqui como mais uma linha de UNION ALL, não como mais
     * uma consulta por atendimento.
     *
     * <p>É nativa porque JPQL não faz UNION. As seções 1:N usam DISTINCT: basta
     * um medicamento para a seção contar como preenchida.
     *
     * @return pares (atendimento_id, nome da seção)
     */
    @Query(value = """
            SELECT atendimento_id, 'QUEIXA_PRINCIPAL'        AS secao FROM queixa_principal        WHERE atendimento_id IN (:ids)
            UNION ALL
            SELECT atendimento_id, 'HISTORIA_CLINICA'        AS secao FROM historia_clinica        WHERE atendimento_id IN (:ids)
            UNION ALL
            SELECT DISTINCT atendimento_id, 'MEDICAMENTOS'   AS secao FROM medicamento             WHERE atendimento_id IN (:ids)
            UNION ALL
            SELECT atendimento_id, 'ANTROPOMETRIA'           AS secao FROM antropometria           WHERE atendimento_id IN (:ids)
            UNION ALL
            SELECT DISTINCT atendimento_id, 'EXAMES'         AS secao FROM exame_bioquimico        WHERE atendimento_id IN (:ids)
            UNION ALL
            SELECT DISTINCT atendimento_id, 'RECORDATORIO'   AS secao FROM recordatorio_refeicao   WHERE atendimento_id IN (:ids)
            UNION ALL
            SELECT atendimento_id, 'FREQUENCIA_ALIMENTAR'    AS secao FROM frequencia_alimentar    WHERE atendimento_id IN (:ids)
            UNION ALL
            SELECT atendimento_id, 'COMPORTAMENTO_ALIMENTAR' AS secao FROM comportamento_alimentar WHERE atendimento_id IN (:ids)
            UNION ALL
            SELECT atendimento_id, 'DIAGNOSTICO'             AS secao FROM diagnostico_pes         WHERE atendimento_id IN (:ids)
            UNION ALL
            SELECT atendimento_id, 'PLANO'                   AS secao FROM plano_intervencao       WHERE atendimento_id IN (:ids)
            UNION ALL
            SELECT DISTINCT atendimento_id, 'METAS'          AS secao FROM meta                    WHERE atendimento_id IN (:ids)
            """, nativeQuery = true)
    List<Object[]> secoesPreenchidas(@Param("ids") Collection<Long> ids);
}
