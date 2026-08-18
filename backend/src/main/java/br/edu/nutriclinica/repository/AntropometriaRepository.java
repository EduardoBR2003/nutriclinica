package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.Antropometria;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.dto.PontoEvolucaoResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Seção 1:1 do prontuário: o id é o próprio atendimento_id (chave compartilhada). */
public interface AntropometriaRepository extends JpaRepository<Antropometria, Long> {

    /**
     * Série de evolução do paciente para o gráfico.
     *
     * <p>Só entram atendimentos <b>aprovados</b>: um rascunho ainda pode ter
     * peso digitado errado, e um prontuário devolvido para correção está
     * justamente sob contestação — nenhum dos dois deveria virar ponto de uma
     * curva que o supervisor vai ler como histórico.
     *
     * <p>O escopo de LGPD é conferido antes, no paciente: quem não enxerga o
     * paciente não chega aqui.
     */
    @Query("""
            select new br.edu.nutriclinica.dto.PontoEvolucaoResponse(
                       a.dataConsulta, ant.pesoKg, ant.imc, ant.circCinturaCm, ant.circQuadrilCm)
            from Antropometria ant
            join ant.atendimento a
            where a.paciente.id = :pacienteId and a.status = :status
            order by a.dataConsulta
            """)
    List<PontoEvolucaoResponse> serieDoPaciente(@Param("pacienteId") Long pacienteId,
                                                @Param("status") StatusAtendimento status);
}
