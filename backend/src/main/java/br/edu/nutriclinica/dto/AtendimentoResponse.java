package br.edu.nutriclinica.dto;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.enums.SecaoProntuario;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Schema `Atendimento` do contrato.
 *
 * <p>{@code editavel} e {@code secoesPreenchidas} não são colunas. O primeiro
 * depende de <b>quem</b> está pedindo — o mesmo atendimento é editável para o
 * estagiário dono e somente leitura para o supervisor —, então é calculado por
 * requisição, nunca guardado. O segundo é levantado em lote pela query de seções.
 *
 * <p>{@code editavel} é conveniência para o frontend saber se habilita o
 * formulário; a regra que de fato barra a escrita continua no serviço de
 * domínio, onde vale para qualquer chamador.
 */
public record AtendimentoResponse(
        Long id,
        String numeroProntuario,
        PacienteResponse paciente,
        UsuarioResponse estagiario,
        UsuarioResponse supervisor,
        LocalDate dataConsulta,
        StatusAtendimento status,
        boolean editavel,
        List<SecaoProntuario> secoesPreenchidas,
        LocalDateTime submetidoEm,
        LocalDateTime avaliadoEm) {

    public static AtendimentoResponse de(Atendimento atendimento,
                                         boolean pacientePossuiTermo,
                                         boolean editavel,
                                         Set<SecaoProntuario> secoesPreenchidas) {
        return new AtendimentoResponse(
                atendimento.getId(),
                atendimento.getNumeroProntuario(),
                PacienteResponse.de(atendimento.getPaciente(), pacientePossuiTermo),
                UsuarioResponse.de(atendimento.getEstagiario()),
                UsuarioResponse.de(atendimento.getSupervisor()),
                atendimento.getDataConsulta(),
                atendimento.getStatus(),
                editavel,
                // Ordenadas pela ordem do enum, que é a ordem do prontuário na
                // tela: a lista serve de checklist de preenchimento.
                secoesPreenchidas.stream().sorted().toList(),
                atendimento.getSubmetidoEm(),
                atendimento.getAvaliadoEm());
    }
}
