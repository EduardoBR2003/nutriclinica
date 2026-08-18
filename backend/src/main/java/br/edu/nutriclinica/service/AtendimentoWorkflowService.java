package br.edu.nutriclinica.service;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.dto.AtendimentoResponse;
import br.edu.nutriclinica.dto.ErroResponse;
import br.edu.nutriclinica.exception.SecoesIncompletasException;
import br.edu.nutriclinica.exception.TransicaoInvalidaException;
import br.edu.nutriclinica.service.auditoria.Auditavel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A máquina de estados do atendimento, e o <b>único</b> ponto do sistema capaz
 * de alterar {@code atendimento.status}.
 *
 * <p>Isso não é convenção: {@code Atendimento} não expõe setter para
 * {@code status}, {@code submetidoEm} nem {@code avaliadoEm}. O caminho de
 * escrita é {@code Atendimento.aplicarTransicao}, chamado só daqui — um
 * {@code setStatus} esquecido em qualquer serviço novo não compila. As quatro
 * transições legais cabem na constante {@link #ORIGENS}, e uma transição nova é
 * uma linha nova ali, não um {@code if} novo espalhado por algum service.
 *
 * <pre>
 * RASCUNHO ──submeter──> EM_REVISAO ──aprovar──> APROVADO
 *                            │
 *                        devolver
 *                            ↓
 *                 DEVOLVIDO_PARA_CORRECAO ──submeter──> EM_REVISAO
 * </pre>
 *
 * <p>Escrever numa <b>seção</b> não passa por aqui e não muda status: um
 * prontuário devolvido continua devolvido enquanto o estagiário corrige, e só
 * volta à fila quando ele submete de novo. Quem guarda essa regra é o
 * {@link AtendimentoEditavelValidator}.
 */
@Service
public class AtendimentoWorkflowService {

    /**
     * O verbo da máquina, com o estado que ele pretende alcançar.
     *
     * <p>Fica aninhado aqui, e não em {@code domain.enums}, porque não é
     * persistido nem espelha o contrato — todos os enums de lá espelham uma
     * coluna. Ninguém deve conseguir nomear uma transição sem depender do
     * serviço que a executa.
     *
     * <p>O destino mora na ação, e não numa tabela {@code (origem, ação) →
     * destino}, para que a mensagem de erro saiba dizer o que se <i>pretendia</i>
     * mesmo quando a origem é inválida — que é justamente o caso que falha.
     */
    public enum Acao {
        SUBMETER(StatusAtendimento.EM_REVISAO),
        APROVAR(StatusAtendimento.APROVADO),
        DEVOLVER(StatusAtendimento.DEVOLVIDO_PARA_CORRECAO);

        private final StatusAtendimento destino;

        Acao(StatusAtendimento destino) {
            this.destino = destino;
        }

        public StatusAtendimento getDestino() {
            return destino;
        }
    }

    /** De quais estados cada ação pode partir. É a máquina de estados inteira. */
    private static final Map<Acao, Set<StatusAtendimento>> ORIGENS = Map.of(
            Acao.SUBMETER, EnumSet.of(StatusAtendimento.RASCUNHO, StatusAtendimento.DEVOLVIDO_PARA_CORRECAO),
            Acao.APROVAR, EnumSet.of(StatusAtendimento.EM_REVISAO),
            Acao.DEVOLVER, EnumSet.of(StatusAtendimento.EM_REVISAO));

    private final AtendimentoEditavelValidator atendimentoEditavelValidator;
    private final SecoesObrigatoriasValidator secoesObrigatoriasValidator;
    private final AtendimentoResponseAssembler assembler;
    private final AuthService authService;

    public AtendimentoWorkflowService(AtendimentoEditavelValidator atendimentoEditavelValidator,
                                      SecoesObrigatoriasValidator secoesObrigatoriasValidator,
                                      AtendimentoResponseAssembler assembler,
                                      AuthService authService) {
        this.atendimentoEditavelValidator = atendimentoEditavelValidator;
        this.secoesObrigatoriasValidator = secoesObrigatoriasValidator;
        this.assembler = assembler;
        this.authService = authService;
    }

    /**
     * Envia o prontuário para a fila de revisão do supervisor.
     *
     * <p>A ordem das recusas é deliberada:
     * <ol>
     *   <li><b>404</b> — atendimento fora do escopo do usuário;
     *   <li><b>403</b> — quem enxerga, mas não é o estagiário dono;
     *   <li><b>409 TRANSICAO_INVALIDA</b> — o status atual não admite submissão;
     *   <li><b>409 SECOES_INCOMPLETAS</b> — falta conteúdo obrigatório.
     * </ol>
     *
     * <p>O 409 de status vem antes do de seções porque é o erro mais
     * fundamental: reenviar um prontuário que já está EM_REVISAO é problema de
     * estado, e responder a ele com uma lista de seções descreveria a situação
     * errada.
     *
     * <p>E os dois vêm depois do 403 por uma razão de LGPD: o array
     * {@code campos} diz quais seções do prontuário estão vazias, o que é
     * conteúdo clínico. Quem não é o dono não deve recebê-lo.
     */
    @Auditavel(acao = "SUBMISSAO", entidade = "Atendimento")
    @Transactional
    public AtendimentoResponse submeter(Long atendimentoId) {
        Usuario usuario = authService.usuarioLogado();
        Atendimento atendimento = atendimentoEditavelValidator.carregarParaLeitura(atendimentoId, usuario);

        atendimentoEditavelValidator.exigirEstagiarioDono(atendimento, usuario);
        exigirOrigemValida(atendimento, Acao.SUBMETER);
        exigirSecoesCompletas(atendimento);

        transitar(atendimento, Acao.SUBMETER);

        return assembler.unico(atendimento, usuario);
    }

    /**
     * Aplica uma transição já autorizada pelo chamador.
     *
     * <p>Recebe o atendimento <b>já carregado com o escopo aplicado</b>, nunca um
     * id: este serviço não confere quem está autenticado nem se o prontuário é
     * visível — isso é do {@link AtendimentoEditavelValidator}, e duplicá-lo aqui
     * criaria uma segunda definição da mesma regra. O que ele garante é uma coisa
     * só, e completamente: que o estado de destino é alcançável a partir do atual.
     *
     * <p>{@code MANDATORY} pelo mesmo motivo de
     * {@link NumeroProntuarioService#gerar}: a transição roda na transação de
     * quem chamou, e sem transação não haveria escrita nenhuma a garantir.
     *
     * @throws TransicaoInvalidaException se a ação não parte do status atual (409)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Atendimento transitar(Atendimento atendimento, Acao acao) {
        exigirOrigemValida(atendimento, acao);
        atendimento.aplicarTransicao(acao.getDestino(), LocalDateTime.now());
        return atendimento;
    }

    /**
     * A checagem da tabela, sem aplicar nada.
     *
     * <p>Existe separada para que {@code submeter} possa pôr o 409 de status
     * antes da validação de conteúdo, sem já ter mexido no atendimento.
     */
    private void exigirOrigemValida(Atendimento atendimento, Acao acao) {
        if (!ORIGENS.get(acao).contains(atendimento.getStatus())) {
            throw new TransicaoInvalidaException(
                    "O atendimento está em " + atendimento.getStatus()
                            + " e não pode ir para " + acao.getDestino() + ".");
        }
    }

    private void exigirSecoesCompletas(Atendimento atendimento) {
        List<ErroResponse.CampoErro> pendencias =
                secoesObrigatoriasValidator.pendenciasDe(atendimento.getId());

        if (!pendencias.isEmpty()) {
            throw new SecoesIncompletasException(pendencias);
        }
    }
}
