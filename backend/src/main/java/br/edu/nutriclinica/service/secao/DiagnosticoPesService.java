package br.edu.nutriclinica.service.secao;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.DiagnosticoPes;
import br.edu.nutriclinica.dto.secao.DiagnosticoPesRequest;
import br.edu.nutriclinica.dto.secao.DiagnosticoPesResponse;
import br.edu.nutriclinica.repository.DiagnosticoPesRepository;
import br.edu.nutriclinica.service.AtendimentoEditavelValidator;
import br.edu.nutriclinica.service.AuthService;
import org.springframework.stereotype.Service;

@Service
public class DiagnosticoPesService
        extends SecaoSimplesService<DiagnosticoPes, DiagnosticoPesRequest, DiagnosticoPesResponse> {

    public DiagnosticoPesService(DiagnosticoPesRepository repository,
                                 AtendimentoEditavelValidator atendimentoEditavelValidator,
                                 AuthService authService) {
        super(repository, atendimentoEditavelValidator, authService);
    }

    @Override
    protected DiagnosticoPes novaSecao(Atendimento atendimento) {
        DiagnosticoPes secao = new DiagnosticoPes();
        secao.setAtendimento(atendimento);
        return secao;
    }

    @Override
    protected void aplicar(DiagnosticoPesRequest requisicao, DiagnosticoPes secao) {
        Merge.aplicar(requisicao.problema(), secao::setProblema);
        Merge.aplicar(requisicao.etiologia(), secao::setEtiologia);
        Merge.aplicar(requisicao.sinaisSintomas(), secao::setSinaisSintomas);
    }

    @Override
    protected DiagnosticoPesResponse converter(DiagnosticoPes secao) {
        return DiagnosticoPesResponse.de(secao);
    }
}
