package br.edu.nutriclinica.service.secao;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.FrequenciaAlimentar;
import br.edu.nutriclinica.dto.secao.FrequenciaAlimentarRequest;
import br.edu.nutriclinica.dto.secao.FrequenciaAlimentarResponse;
import br.edu.nutriclinica.repository.FrequenciaAlimentarRepository;
import br.edu.nutriclinica.service.AtendimentoEditavelValidator;
import br.edu.nutriclinica.service.AuthService;
import org.springframework.stereotype.Service;

@Service
public class FrequenciaAlimentarService
        extends SecaoSimplesService<FrequenciaAlimentar, FrequenciaAlimentarRequest, FrequenciaAlimentarResponse> {

    public FrequenciaAlimentarService(FrequenciaAlimentarRepository repository,
                                      AtendimentoEditavelValidator atendimentoEditavelValidator,
                                      AuthService authService) {
        super(repository, atendimentoEditavelValidator, authService);
    }

    @Override
    protected FrequenciaAlimentar novaSecao(Atendimento atendimento) {
        FrequenciaAlimentar secao = new FrequenciaAlimentar();
        secao.setAtendimento(atendimento);
        return secao;
    }

    @Override
    protected void aplicar(FrequenciaAlimentarRequest requisicao, FrequenciaAlimentar secao) {
        Merge.aplicar(requisicao.frutas(), secao::setFrutas);
        Merge.aplicar(requisicao.verdurasLegumes(), secao::setVerdurasLegumes);
        Merge.aplicar(requisicao.ultraprocessados(), secao::setUltraprocessados);
        Merge.aplicar(requisicao.refrigerante(), secao::setRefrigerante);
        Merge.aplicar(requisicao.bebidaAlcoolica(), secao::setBebidaAlcoolica);
        Merge.aplicar(requisicao.cafe(), secao::setCafe);
        Merge.aplicar(requisicao.aguaMlDia(), secao::setAguaMlDia);
        Merge.aplicar(requisicao.observacoes(), secao::setObservacoes);
    }

    @Override
    protected FrequenciaAlimentarResponse converter(FrequenciaAlimentar secao) {
        return FrequenciaAlimentarResponse.de(secao);
    }
}
