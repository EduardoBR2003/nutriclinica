package br.edu.nutriclinica.service.secao;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.HistoriaClinica;
import br.edu.nutriclinica.dto.secao.HistoriaClinicaRequest;
import br.edu.nutriclinica.dto.secao.HistoriaClinicaResponse;
import br.edu.nutriclinica.repository.HistoriaClinicaRepository;
import br.edu.nutriclinica.service.AtendimentoEditavelValidator;
import br.edu.nutriclinica.service.AuthService;
import org.springframework.stereotype.Service;

@Service
public class HistoriaClinicaService
        extends SecaoSimplesService<HistoriaClinica, HistoriaClinicaRequest, HistoriaClinicaResponse> {

    public HistoriaClinicaService(HistoriaClinicaRepository repository,
                                  AtendimentoEditavelValidator atendimentoEditavelValidator,
                                  AuthService authService) {
        super(repository, atendimentoEditavelValidator, authService);
    }

    @Override
    protected HistoriaClinica novaSecao(Atendimento atendimento) {
        HistoriaClinica secao = new HistoriaClinica();
        secao.setAtendimento(atendimento);
        return secao;
    }

    @Override
    protected void aplicar(HistoriaClinicaRequest requisicao, HistoriaClinica secao) {
        Merge.aplicar(requisicao.doencasDiagnosticadas(), secao::setDoencasDiagnosticadas);
        Merge.aplicar(requisicao.alergias(), secao::setAlergias);
        Merge.aplicar(requisicao.intolerancias(), secao::setIntolerancias);
        Merge.aplicar(requisicao.historicoFamiliar(), secao::setHistoricoFamiliar);
        Merge.aplicar(requisicao.tabagismo(), secao::setTabagismo);
        Merge.aplicar(requisicao.horasSono(), secao::setHorasSono);
        Merge.aplicar(requisicao.qualidadeSono(), secao::setQualidadeSono);
        Merge.aplicar(requisicao.nivelEstresse(), secao::setNivelEstresse);
        Merge.aplicar(requisicao.habitoIntestinal(), secao::setHabitoIntestinal);
        Merge.aplicar(requisicao.praticaAtividadeFisica(), secao::setPraticaAtividadeFisica);
        Merge.aplicar(requisicao.descricaoAtividade(), secao::setDescricaoAtividade);
        Merge.aplicar(requisicao.observacoes(), secao::setObservacoes);
    }

    @Override
    protected HistoriaClinicaResponse converter(HistoriaClinica secao) {
        return HistoriaClinicaResponse.de(secao);
    }
}
