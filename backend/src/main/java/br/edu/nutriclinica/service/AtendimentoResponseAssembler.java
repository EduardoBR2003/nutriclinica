package br.edu.nutriclinica.service;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.SecaoProntuario;
import br.edu.nutriclinica.dto.AtendimentoResponse;
import br.edu.nutriclinica.repository.TermoConsentimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Monta o schema {@code Atendimento} do contrato a partir da entidade.
 *
 * <p>Existe como colaborador, e não como método privado de um service, porque
 * quatro caminhos diferentes devolvem um atendimento — a listagem, o prontuário
 * completo, a submissão e a fila de revisão — e todos precisam dos mesmos dois
 * campos derivados, que não são colunas: {@code possuiTermo} e
 * {@code secoesPreenchidas}. Sem um lugar comum, o levantamento em lote seria
 * copiado quatro vezes, e bastaria uma cópia esquecer o lote para o N+1 voltar
 * pela porta dos fundos.
 *
 * <p>{@code editavel} também é derivado, e depende de <b>quem</b> pergunta: o
 * mesmo atendimento é editável para o estagiário dono e somente leitura para o
 * supervisor. Por isso o usuário entra como parâmetro em todo método daqui.
 */
@Service
public class AtendimentoResponseAssembler {

    private final TermoConsentimentoRepository termoConsentimentoRepository;
    private final SecoesPreenchidasService secoesPreenchidasService;
    private final AtendimentoEditavelValidator atendimentoEditavelValidator;

    public AtendimentoResponseAssembler(TermoConsentimentoRepository termoConsentimentoRepository,
                                        SecoesPreenchidasService secoesPreenchidasService,
                                        AtendimentoEditavelValidator atendimentoEditavelValidator) {
        this.termoConsentimentoRepository = termoConsentimentoRepository;
        this.secoesPreenchidasService = secoesPreenchidasService;
        this.atendimentoEditavelValidator = atendimentoEditavelValidator;
    }

    /**
     * Converte a página inteira com duas consultas de apoio — termos e seções —
     * em vez de duas por atendimento.
     */
    @Transactional(readOnly = true)
    public List<AtendimentoResponse> emLote(List<Atendimento> atendimentos, Usuario usuario) {
        if (atendimentos.isEmpty()) {
            return List.of();
        }

        List<Long> atendimentoIds = atendimentos.stream().map(Atendimento::getId).toList();
        List<Long> pacienteIds = atendimentos.stream().map(a -> a.getPaciente().getId()).distinct().toList();

        Set<Long> comTermo = Set.copyOf(termoConsentimentoRepository.idsComAceite(pacienteIds));
        Map<Long, Set<SecaoProntuario>> secoes = secoesPreenchidasService.porAtendimento(atendimentoIds);

        return atendimentos.stream()
                .map(atendimento -> AtendimentoResponse.de(
                        atendimento,
                        comTermo.contains(atendimento.getPaciente().getId()),
                        atendimentoEditavelValidator.editavelPor(atendimento, usuario),
                        secoes.getOrDefault(atendimento.getId(), Set.of())))
                .toList();
    }

    /** O mesmo, para um atendimento só. */
    @Transactional(readOnly = true)
    public AtendimentoResponse unico(Atendimento atendimento, Usuario usuario) {
        return AtendimentoResponse.de(
                atendimento,
                termoConsentimentoRepository.existsByPacienteIdAndAceiteLgpdTrue(atendimento.getPaciente().getId()),
                atendimentoEditavelValidator.editavelPor(atendimento, usuario),
                secoesPreenchidasService.doAtendimento(atendimento.getId()));
    }
}
