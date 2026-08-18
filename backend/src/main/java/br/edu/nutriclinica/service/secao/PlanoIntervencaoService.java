package br.edu.nutriclinica.service.secao;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.PlanoIntervencao;
import br.edu.nutriclinica.dto.secao.PlanoIntervencaoRequest;
import br.edu.nutriclinica.dto.secao.PlanoIntervencaoResponse;
import br.edu.nutriclinica.repository.PlanoIntervencaoRepository;
import br.edu.nutriclinica.service.AtendimentoEditavelValidator;
import br.edu.nutriclinica.service.AuthService;
import org.springframework.stereotype.Service;

@Service
public class PlanoIntervencaoService
        extends SecaoSimplesService<PlanoIntervencao, PlanoIntervencaoRequest, PlanoIntervencaoResponse> {

    public PlanoIntervencaoService(PlanoIntervencaoRepository repository,
                                   AtendimentoEditavelValidator atendimentoEditavelValidator,
                                   AuthService authService) {
        super(repository, atendimentoEditavelValidator, authService);
    }

    @Override
    protected PlanoIntervencao novaSecao(Atendimento atendimento) {
        PlanoIntervencao secao = new PlanoIntervencao();
        secao.setAtendimento(atendimento);
        return secao;
    }

    @Override
    protected void aplicar(PlanoIntervencaoRequest requisicao, PlanoIntervencao secao) {
        Merge.aplicar(requisicao.objetivos(), secao::setObjetivos);
        Merge.aplicar(requisicao.prescricaoEnergeticaKcal(), secao::setPrescricaoEnergeticaKcal);
        Merge.aplicar(requisicao.percCarboidrato(), secao::setPercCarboidrato);
        Merge.aplicar(requisicao.percProteina(), secao::setPercProteina);
        Merge.aplicar(requisicao.percLipideo(), secao::setPercLipideo);
        Merge.aplicar(requisicao.estrategiasComportamentais(), secao::setEstrategiasComportamentais);
        Merge.aplicar(requisicao.educacaoAlimentar(), secao::setEducacaoAlimentar);
    }

    @Override
    protected PlanoIntervencaoResponse converter(PlanoIntervencao secao) {
        return PlanoIntervencaoResponse.de(secao);
    }
}
