package br.edu.nutriclinica.service;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.ComentarioSecao;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.dto.ComentarioSecaoRequest;
import br.edu.nutriclinica.dto.ComentarioSecaoResponse;
import br.edu.nutriclinica.repository.ComentarioSecaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Comentários do supervisor nas seções do prontuário.
 *
 * <p>São o feedback que acompanha uma devolução: sem eles, o estagiário recebe
 * o prontuário de volta sem saber o que corrigir.
 *
 * <p>Leem todos os que enxergam o atendimento — o estagiário dono
 * <b>precisa</b> lê-los. Escreve só o supervisor designado: a orientação é de
 * quem foi designado para revisar, como a nota.
 *
 * <p>Não há porteiro de status, e isso é decisão. O supervisor comenta durante a
 * revisão e continua podendo comentar depois de devolver ou aprovar — um
 * comentário num prontuário fechado é anotação, não edição. Comentar não escreve
 * no prontuário: não entra em {@code secoesPreenchidas}, não aparece em
 * {@code AtendimentoCompleto} e não muda status.
 */
@Service
public class ComentarioSecaoService {

    private final ComentarioSecaoRepository comentarioSecaoRepository;
    private final AtendimentoEditavelValidator atendimentoEditavelValidator;
    private final AuthService authService;

    public ComentarioSecaoService(ComentarioSecaoRepository comentarioSecaoRepository,
                                  AtendimentoEditavelValidator atendimentoEditavelValidator,
                                  AuthService authService) {
        this.comentarioSecaoRepository = comentarioSecaoRepository;
        this.atendimentoEditavelValidator = atendimentoEditavelValidator;
        this.authService = authService;
    }

    /** Em ordem cronológica: a conversa se lê de cima para baixo. */
    @Transactional(readOnly = true)
    public List<ComentarioSecaoResponse> listar(Long atendimentoId) {
        Usuario usuario = authService.usuarioLogado();
        Atendimento atendimento = atendimentoEditavelValidator.carregarParaLeitura(atendimentoId, usuario);

        return comentarioSecaoRepository.findByAtendimentoIdOrderByCriadoEmAsc(atendimento.getId())
                .stream()
                .map(ComentarioSecaoResponse::de)
                .toList();
    }

    @Transactional
    public ComentarioSecaoResponse adicionar(Long atendimentoId, ComentarioSecaoRequest requisicao) {
        Usuario usuario = authService.usuarioLogado();
        Atendimento atendimento = atendimentoEditavelValidator.carregarParaLeitura(atendimentoId, usuario);

        atendimentoEditavelValidator.exigirSupervisorDesignado(atendimento, usuario);

        ComentarioSecao comentario = new ComentarioSecao();
        comentario.setAtendimento(atendimento);
        comentario.setAutor(usuario);
        comentario.setSecao(requisicao.secao());
        comentario.setTexto(requisicao.texto());

        return ComentarioSecaoResponse.de(comentarioSecaoRepository.save(comentario));
    }
}
