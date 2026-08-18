package br.edu.nutriclinica.service.secao;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.QueixaPrincipal;
import br.edu.nutriclinica.dto.secao.QueixaPrincipalRequest;
import br.edu.nutriclinica.dto.secao.QueixaPrincipalResponse;
import br.edu.nutriclinica.repository.QueixaPrincipalRepository;
import br.edu.nutriclinica.service.AtendimentoEditavelValidator;
import br.edu.nutriclinica.service.AuthService;
import org.springframework.stereotype.Service;

@Service
public class QueixaPrincipalService
        extends SecaoSimplesService<QueixaPrincipal, QueixaPrincipalRequest, QueixaPrincipalResponse> {

    public QueixaPrincipalService(QueixaPrincipalRepository repository,
                                  AtendimentoEditavelValidator atendimentoEditavelValidator,
                                  AuthService authService) {
        super(repository, atendimentoEditavelValidator, authService);
    }

    @Override
    protected QueixaPrincipal novaSecao(Atendimento atendimento) {
        QueixaPrincipal secao = new QueixaPrincipal();
        secao.setAtendimento(atendimento);
        return secao;
    }

    @Override
    protected void aplicar(QueixaPrincipalRequest requisicao, QueixaPrincipal secao) {
        Merge.aplicar(requisicao.motivo(), secao::setMotivo);
        Merge.aplicar(requisicao.tempoQueixa(), secao::setTempoQueixa);
        Merge.aplicar(requisicao.tratamentoAnterior(), secao::setTratamentoAnterior);
        Merge.aplicar(requisicao.objetivoConsulta(), secao::setObjetivoConsulta);
        Merge.aplicar(requisicao.observacoes(), secao::setObservacoes);
    }

    @Override
    protected QueixaPrincipalResponse converter(QueixaPrincipal secao) {
        return QueixaPrincipalResponse.de(secao);
    }
}
