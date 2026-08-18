package br.edu.nutriclinica.service.secao;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.RecordatorioItem;
import br.edu.nutriclinica.domain.RecordatorioRefeicao;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.dto.secao.RefeicaoRequest;
import br.edu.nutriclinica.dto.secao.RefeicaoResponse;
import br.edu.nutriclinica.repository.AtendimentoRepository;
import br.edu.nutriclinica.repository.RecordatorioRefeicaoRepository;
import br.edu.nutriclinica.service.AtendimentoEditavelValidator;
import br.edu.nutriclinica.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Recordatório alimentar de 24 horas: refeições com itens aninhados.
 *
 * <p>É a única seção com dois níveis, e os dois são gravados na mesma
 * transação — um recordatório com as refeições novas mas sem os itens delas não
 * é um estado válido do prontuário.
 *
 * <p>A reconciliação é aplicada nos dois níveis: apagar uma refeição apaga os
 * itens dela por {@code orphanRemoval} da própria refeição, sem delete manual, e
 * uma refeição que continuou mantém o id mesmo que os itens tenham mudado.
 */
@Service
public class RecordatorioService {

    private final RecordatorioRefeicaoRepository recordatorioRefeicaoRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final AtendimentoEditavelValidator atendimentoEditavelValidator;
    private final AuthService authService;

    public RecordatorioService(RecordatorioRefeicaoRepository recordatorioRefeicaoRepository,
                               AtendimentoRepository atendimentoRepository,
                               AtendimentoEditavelValidator atendimentoEditavelValidator,
                               AuthService authService) {
        this.recordatorioRefeicaoRepository = recordatorioRefeicaoRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.atendimentoEditavelValidator = atendimentoEditavelValidator;
        this.authService = authService;
    }

    @Transactional
    public List<RefeicaoResponse> substituir(Long atendimentoId, List<RefeicaoRequest> enviadas) {
        Usuario usuario = authService.usuarioLogado();
        Atendimento atendimento = atendimentoEditavelValidator.exigirEditavel(atendimentoId, usuario);

        List<RecordatorioRefeicao> resultado = Merge.reconciliar(
                atendimento.getRefeicoes(),
                enviadas,
                RecordatorioRefeicao::getId,
                RefeicaoRequest::id,
                () -> novaRefeicao(atendimento),
                this::aplicarRefeicao);

        // Um flush só para os dois níveis — e flush, não save: o atendimento já
        // está gerenciado, e um save aqui viraria merge, que persistiria cópias
        // dos filhos novos e devolveria a resposta com id nulo.
        atendimentoRepository.flush();

        return resultado.stream().map(RefeicaoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<RefeicaoResponse> doAtendimento(Long atendimentoId) {
        return recordatorioRefeicaoRepository.comItens(atendimentoId).stream()
                .map(RefeicaoResponse::de)
                .toList();
    }

    private void aplicarRefeicao(RefeicaoRequest enviada, RecordatorioRefeicao refeicao, int posicao) {
        refeicao.setTipoRefeicao(enviada.tipoRefeicao());
        refeicao.setHorario(enviada.horario());
        refeicao.setLocalRefeicao(enviada.localRefeicao());
        refeicao.setOrdem(Merge.ordem(enviada.ordem(), posicao));

        // Itens ausentes no corpo significam refeição sem itens, não itens
        // preservados: o PUT substitui a coleção inteira, nos dois níveis.
        List<RefeicaoRequest.ItemRefeicaoRequest> itens =
                enviada.itens() == null ? List.of() : enviada.itens();

        Merge.reconciliar(
                refeicao.getItens(),
                itens,
                RecordatorioItem::getId,
                RefeicaoRequest.ItemRefeicaoRequest::id,
                () -> novoItem(refeicao),
                (enviado, item, posicaoItem) -> {
                    item.setAlimento(enviado.alimento());
                    item.setQuantidade(enviado.quantidade());
                    item.setMedidaCaseira(enviado.medidaCaseira());
                    item.setOrdem(Merge.ordem(enviado.ordem(), posicaoItem));
                });
    }

    private RecordatorioRefeicao novaRefeicao(Atendimento atendimento) {
        RecordatorioRefeicao refeicao = new RecordatorioRefeicao();
        refeicao.setAtendimento(atendimento);
        return refeicao;
    }

    private RecordatorioItem novoItem(RecordatorioRefeicao refeicao) {
        RecordatorioItem item = new RecordatorioItem();
        item.setRefeicao(refeicao);
        return item;
    }
}
