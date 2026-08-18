package br.edu.nutriclinica.service.secao;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Meta;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.dto.secao.MetaRequest;
import br.edu.nutriclinica.dto.secao.MetaResponse;
import br.edu.nutriclinica.repository.AtendimentoRepository;
import br.edu.nutriclinica.repository.MetaRepository;
import br.edu.nutriclinica.service.AtendimentoEditavelValidator;
import br.edu.nutriclinica.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Seção de metas: coleção substituída inteira pelo PUT. */
@Service
public class MetaService {

    private final MetaRepository metaRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final AtendimentoEditavelValidator atendimentoEditavelValidator;
    private final AuthService authService;

    public MetaService(MetaRepository metaRepository,
                       AtendimentoRepository atendimentoRepository,
                       AtendimentoEditavelValidator atendimentoEditavelValidator,
                       AuthService authService) {
        this.metaRepository = metaRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.atendimentoEditavelValidator = atendimentoEditavelValidator;
        this.authService = authService;
    }

    @Transactional
    public List<MetaResponse> substituir(Long atendimentoId, List<MetaRequest> enviadas) {
        Usuario usuario = authService.usuarioLogado();
        Atendimento atendimento = atendimentoEditavelValidator.exigirEditavel(atendimentoId, usuario);

        List<Meta> resultado = Merge.reconciliar(
                atendimento.getMetas(),
                enviadas,
                Meta::getId,
                MetaRequest::id,
                () -> nova(atendimento),
                (enviada, meta, posicao) -> {
                    meta.setDescricao(enviada.descricao());
                    meta.setPrazo(enviada.prazo());
                    meta.setIndicador(enviada.indicador());
                    meta.setDataRetorno(enviada.dataRetorno());
                });

        atendimentoRepository.flush();

        return converter(resultado);
    }

    @Transactional(readOnly = true)
    public List<MetaResponse> doAtendimento(Long atendimentoId) {
        return converter(metaRepository.findByAtendimentoIdOrderByIdAsc(atendimentoId));
    }

    private Meta nova(Atendimento atendimento) {
        Meta meta = new Meta();
        meta.setAtendimento(atendimento);
        return meta;
    }

    private List<MetaResponse> converter(List<Meta> metas) {
        return metas.stream().map(MetaResponse::de).toList();
    }
}
