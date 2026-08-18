package br.edu.nutriclinica.service.secao;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.ComportamentoAlimentar;
import br.edu.nutriclinica.dto.secao.ComportamentoAlimentarRequest;
import br.edu.nutriclinica.dto.secao.ComportamentoAlimentarResponse;
import br.edu.nutriclinica.repository.ComportamentoAlimentarRepository;
import br.edu.nutriclinica.service.AtendimentoEditavelValidator;
import br.edu.nutriclinica.service.AuthService;
import org.springframework.stereotype.Service;

@Service
public class ComportamentoAlimentarService extends SecaoSimplesService<
        ComportamentoAlimentar, ComportamentoAlimentarRequest, ComportamentoAlimentarResponse> {

    public ComportamentoAlimentarService(ComportamentoAlimentarRepository repository,
                                         AtendimentoEditavelValidator atendimentoEditavelValidator,
                                         AuthService authService) {
        super(repository, atendimentoEditavelValidator, authService);
    }

    @Override
    protected ComportamentoAlimentar novaSecao(Atendimento atendimento) {
        ComportamentoAlimentar secao = new ComportamentoAlimentar();
        secao.setAtendimento(atendimento);
        return secao;
    }

    @Override
    protected void aplicar(ComportamentoAlimentarRequest requisicao, ComportamentoAlimentar secao) {
        Merge.aplicar(requisicao.comeAssistindoTela(), secao::setComeAssistindoTela);
        Merge.aplicar(requisicao.comeRapido(), secao::setComeRapido);
        Merge.aplicar(requisicao.pulaRefeicoes(), secao::setPulaRefeicoes);
        Merge.aplicar(requisicao.compulsaoAlimentar(), secao::setCompulsaoAlimentar);
        Merge.aplicar(requisicao.alimentacaoEmocional(), secao::setAlimentacaoEmocional);
        Merge.aplicar(requisicao.restricaoAlimentar(), secao::setRestricaoAlimentar);
        Merge.aplicar(requisicao.observacoes(), secao::setObservacoes);
    }

    @Override
    protected ComportamentoAlimentarResponse converter(ComportamentoAlimentar secao) {
        return ComportamentoAlimentarResponse.de(secao);
    }
}
