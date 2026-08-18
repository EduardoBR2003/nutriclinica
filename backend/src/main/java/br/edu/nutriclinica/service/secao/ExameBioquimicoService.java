package br.edu.nutriclinica.service.secao;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.ExameBioquimico;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.dto.secao.ExameBioquimicoRequest;
import br.edu.nutriclinica.dto.secao.ExameBioquimicoResponse;
import br.edu.nutriclinica.repository.AtendimentoRepository;
import br.edu.nutriclinica.repository.ExameBioquimicoRepository;
import br.edu.nutriclinica.service.AtendimentoEditavelValidator;
import br.edu.nutriclinica.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Seção de exames bioquímicos: coleção substituída inteira pelo PUT. */
@Service
public class ExameBioquimicoService {

    private final ExameBioquimicoRepository exameBioquimicoRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final AtendimentoEditavelValidator atendimentoEditavelValidator;
    private final AuthService authService;

    public ExameBioquimicoService(ExameBioquimicoRepository exameBioquimicoRepository,
                                  AtendimentoRepository atendimentoRepository,
                                  AtendimentoEditavelValidator atendimentoEditavelValidator,
                                  AuthService authService) {
        this.exameBioquimicoRepository = exameBioquimicoRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.atendimentoEditavelValidator = atendimentoEditavelValidator;
        this.authService = authService;
    }

    @Transactional
    public List<ExameBioquimicoResponse> substituir(Long atendimentoId, List<ExameBioquimicoRequest> enviados) {
        Usuario usuario = authService.usuarioLogado();
        Atendimento atendimento = atendimentoEditavelValidator.exigirEditavel(atendimentoId, usuario);

        List<ExameBioquimico> resultado = Merge.reconciliar(
                atendimento.getExames(),
                enviados,
                ExameBioquimico::getId,
                ExameBioquimicoRequest::id,
                () -> novo(atendimento),
                (enviado, exame, posicao) -> {
                    exame.setNomeExame(enviado.nomeExame());
                    exame.setValor(enviado.valor());
                    exame.setUnidade(enviado.unidade());
                    exame.setDataExame(enviado.dataExame());
                    exame.setValorReferencia(enviado.valorReferencia());
                });

        atendimentoRepository.flush();

        return converter(resultado);
    }

    @Transactional(readOnly = true)
    public List<ExameBioquimicoResponse> doAtendimento(Long atendimentoId) {
        return converter(exameBioquimicoRepository.findByAtendimentoIdOrderByIdAsc(atendimentoId));
    }

    private ExameBioquimico novo(Atendimento atendimento) {
        ExameBioquimico exame = new ExameBioquimico();
        exame.setAtendimento(atendimento);
        return exame;
    }

    private List<ExameBioquimicoResponse> converter(List<ExameBioquimico> exames) {
        return exames.stream().map(ExameBioquimicoResponse::de).toList();
    }
}
