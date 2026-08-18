package br.edu.nutriclinica.service;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.AvaliacaoSupervisor;
import br.edu.nutriclinica.domain.ItemRubrica;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.ResultadoAvaliacao;
import br.edu.nutriclinica.dto.AvaliacaoRequest;
import br.edu.nutriclinica.dto.AvaliacaoResponse;
import br.edu.nutriclinica.exception.NaoEncontradoException;
import br.edu.nutriclinica.exception.ValidacaoException;
import br.edu.nutriclinica.repository.AvaliacaoSupervisorRepository;
import br.edu.nutriclinica.service.auditoria.Auditavel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * A avaliação do supervisor: rubrica, nota e veredito.
 *
 * <p>Registrar a avaliação é o que fecha a revisão, então esta é a operação que
 * dispara as duas transições finais da máquina de estados — mas quem as aplica
 * continua sendo o {@link AtendimentoWorkflowService}. Aqui não se escreve
 * {@code status}.
 */
@Service
public class AvaliacaoService {

    /** Casas decimais da nota final. Espelha {@code nota_final NUMERIC(4,2)}. */
    private static final int ESCALA_NOTA = 2;

    private final AvaliacaoSupervisorRepository avaliacaoSupervisorRepository;
    private final AtendimentoEditavelValidator atendimentoEditavelValidator;
    private final AtendimentoWorkflowService atendimentoWorkflowService;
    private final AuthService authService;

    public AvaliacaoService(AvaliacaoSupervisorRepository avaliacaoSupervisorRepository,
                            AtendimentoEditavelValidator atendimentoEditavelValidator,
                            AtendimentoWorkflowService atendimentoWorkflowService,
                            AuthService authService) {
        this.avaliacaoSupervisorRepository = avaliacaoSupervisorRepository;
        this.atendimentoEditavelValidator = atendimentoEditavelValidator;
        this.atendimentoWorkflowService = atendimentoWorkflowService;
        this.authService = authService;
    }

    /**
     * Registra a avaliação e conclui a revisão.
     *
     * <p>Ordem das recusas: <b>404</b> fora do escopo, <b>403</b> para quem
     * enxerga mas não é o supervisor designado, <b>409</b> se o atendimento não
     * está EM_REVISAO. A transição vem antes de qualquer escrita: assim avaliar
     * um RASCUNHO responde {@code TRANSICAO_INVALIDA}, e não uma violação de
     * constraint disfarçada de erro interno.
     *
     * <p>Reavaliar é normal — um prontuário devolvido volta corrigido e recebe
     * parecer novo. A tabela tem {@code UNIQUE (atendimento_id)}, então a linha é
     * <b>reaproveitada</b>: o id da avaliação não muda, e só os itens da rubrica
     * são substituídos.
     */
    @Auditavel(acao = "AVALIACAO", entidade = "Atendimento")
    @Transactional
    public AvaliacaoResponse registrar(Long atendimentoId, AvaliacaoRequest requisicao) {
        Usuario supervisor = authService.usuarioLogado();
        Atendimento atendimento = atendimentoEditavelValidator.carregarParaLeitura(atendimentoId, supervisor);

        atendimentoEditavelValidator.exigirSupervisorDesignado(atendimento, supervisor);
        atendimentoWorkflowService.transitar(atendimento, acaoDe(requisicao.resultado()));

        AvaliacaoSupervisor avaliacao = avaliacaoSupervisorRepository.findByAtendimentoId(atendimentoId)
                .orElseGet(() -> nova(atendimento));

        avaliacao.setSupervisor(supervisor);
        avaliacao.setResultado(requisicao.resultado());
        avaliacao.setParecerGeral(requisicao.parecerGeral());
        avaliacao.setNotaFinal(calcularNotaFinal(requisicao.itens()));
        // Um relógio só: o mesmo instante que a transição carimbou no
        // atendimento. Os dois campos são iguais por construção.
        avaliacao.setAvaliadoEm(atendimento.getAvaliadoEm());

        substituirItens(avaliacao, requisicao.itens());

        return AvaliacaoResponse.de(avaliacaoSupervisorRepository.save(avaliacao));
    }

    /**
     * A avaliação do atendimento, para o {@code GET} do contrato.
     *
     * <p>Sem porteiro de perfil: quem enxerga o atendimento lê a avaliação, e é
     * assim que o estagiário vê a própria nota. Escrever é que exige ser o
     * supervisor designado — a assimetria entre ler e escrever fica num lugar só.
     *
     * <p>Sem porteiro de status, também: é justamente quando o atendimento está
     * APROVADO ou DEVOLVIDO_PARA_CORRECAO que o estagiário precisa ler o parecer.
     */
    @Transactional(readOnly = true)
    public AvaliacaoResponse buscar(Long atendimentoId) {
        Usuario usuario = authService.usuarioLogado();
        Atendimento atendimento = atendimentoEditavelValidator.carregarParaLeitura(atendimentoId, usuario);

        return doAtendimento(atendimento)
                .orElseThrow(() -> NaoEncontradoException.de("Avaliação do atendimento", atendimento.getId()));
    }

    /**
     * A mesma leitura, a partir de um atendimento que o chamador já carregou com
     * o escopo aplicado — é assim que o prontuário completo traz a avaliação sem
     * repetir a consulta de acesso.
     *
     * @return vazio se o atendimento ainda não foi avaliado
     */
    @Transactional(readOnly = true)
    public Optional<AvaliacaoResponse> doAtendimento(Atendimento atendimento) {
        return avaliacaoSupervisorRepository.findByAtendimentoId(atendimento.getId())
                .map(AvaliacaoResponse::de);
    }

    /**
     * Média ponderada da rubrica: {@code Σ(nota × peso) / Σ(pesos)}.
     *
     * <p>Peso ausente vale 1. A guarda de soma zero permanece mesmo com a
     * validação de borda no request: uma protege o contrato, a outra impede que
     * uma divisão por zero vire 500 se a anotação sumir num refactor.
     */
    private BigDecimal calcularNotaFinal(List<AvaliacaoRequest.Item> itens) {
        BigDecimal somaPesos = BigDecimal.ZERO;
        BigDecimal somaPonderada = BigDecimal.ZERO;

        for (AvaliacaoRequest.Item item : itens) {
            BigDecimal peso = pesoDe(item);
            somaPesos = somaPesos.add(peso);
            somaPonderada = somaPonderada.add(peso.multiply(item.nota()));
        }

        if (somaPesos.signum() <= 0) {
            throw new ValidacaoException("itens",
                    "A soma dos pesos da rubrica precisa ser maior que zero.");
        }

        return somaPonderada.divide(somaPesos, ESCALA_NOTA, RoundingMode.HALF_UP);
    }

    /**
     * Troca os itens da rubrica pelos enviados.
     *
     * <p>Não usa {@code Merge.reconciliar} como as seções 1:N do prontuário: o
     * contrato de {@code AvaliacaoRequest} não expõe id de item, então não há id
     * a preservar — a rubrica vem sempre inteira.
     *
     * <p>O {@code flush} entre a limpeza e a recriação torna determinística a
     * ordem DELETE-antes-de-INSERT. Com {@code orphanRemoval} e chave
     * {@code IDENTITY}, o Hibernate emitiria os INSERT primeiro; hoje isso é
     * inofensivo, e é exatamente por isso que {@code item_rubrica} não deve
     * ganhar um único índice em {@code (avaliacao_id, ordem)}.
     */
    private void substituirItens(AvaliacaoSupervisor avaliacao, List<AvaliacaoRequest.Item> itens) {
        avaliacao.getItens().clear();
        avaliacaoSupervisorRepository.saveAndFlush(avaliacao);

        for (int posicao = 0; posicao < itens.size(); posicao++) {
            AvaliacaoRequest.Item enviado = itens.get(posicao);

            ItemRubrica item = new ItemRubrica();
            item.setCriterio(enviado.criterio());
            item.setPeso(pesoDe(enviado));
            item.setNota(enviado.nota());
            item.setComentario(enviado.comentario());
            // A ordem não está no contrato: é a posição em que o supervisor
            // montou a rubrica, e @OrderBy a devolve igual.
            item.setOrdem(posicao);

            avaliacao.adicionarItem(item);
        }
    }

    private BigDecimal pesoDe(AvaliacaoRequest.Item item) {
        return item.peso() == null ? BigDecimal.ONE : item.peso();
    }

    private AtendimentoWorkflowService.Acao acaoDe(ResultadoAvaliacao resultado) {
        return resultado == ResultadoAvaliacao.APROVADO
                ? AtendimentoWorkflowService.Acao.APROVAR
                : AtendimentoWorkflowService.Acao.DEVOLVER;
    }

    private AvaliacaoSupervisor nova(Atendimento atendimento) {
        AvaliacaoSupervisor avaliacao = new AvaliacaoSupervisor();
        avaliacao.setAtendimento(atendimento);
        return avaliacao;
    }
}
