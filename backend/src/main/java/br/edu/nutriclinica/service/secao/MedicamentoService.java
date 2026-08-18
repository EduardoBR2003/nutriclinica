package br.edu.nutriclinica.service.secao;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Medicamento;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.dto.secao.MedicamentoRequest;
import br.edu.nutriclinica.dto.secao.MedicamentoResponse;
import br.edu.nutriclinica.repository.AtendimentoRepository;
import br.edu.nutriclinica.repository.MedicamentoRepository;
import br.edu.nutriclinica.service.AtendimentoEditavelValidator;
import br.edu.nutriclinica.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seção de medicamentos e suplementos: uma coleção que o PUT substitui inteira.
 *
 * <p>A substituição não é apagar tudo e reinserir. Os itens que continuaram
 * mantêm o id, para que um comentário do supervisor preso a um medicamento
 * continue apontando para o mesmo registro depois de o estagiário corrigir a
 * dose de outro.
 */
@Service
public class MedicamentoService {

    private final MedicamentoRepository medicamentoRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final AtendimentoEditavelValidator atendimentoEditavelValidator;
    private final AuthService authService;

    public MedicamentoService(MedicamentoRepository medicamentoRepository,
                              AtendimentoRepository atendimentoRepository,
                              AtendimentoEditavelValidator atendimentoEditavelValidator,
                              AuthService authService) {
        this.medicamentoRepository = medicamentoRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.atendimentoEditavelValidator = atendimentoEditavelValidator;
        this.authService = authService;
    }

    @Transactional
    public List<MedicamentoResponse> substituir(Long atendimentoId, List<MedicamentoRequest> enviados) {
        Usuario usuario = authService.usuarioLogado();
        Atendimento atendimento = atendimentoEditavelValidator.exigirEditavel(atendimentoId, usuario);

        List<Medicamento> resultado = Merge.reconciliar(
                atendimento.getMedicamentos(),
                enviados,
                Medicamento::getId,
                MedicamentoRequest::id,
                () -> novo(atendimento),
                (enviado, medicamento, posicao) -> {
                    medicamento.setTipo(enviado.tipo());
                    medicamento.setNome(enviado.nome());
                    medicamento.setDose(enviado.dose());
                    medicamento.setFrequencia(enviado.frequencia());
                });

        // Flush, não save: o atendimento já está gerenciado, e um save aqui
        // viraria merge — que persiste *cópias* dos filhos novos e devolveria a
        // resposta com id nulo. O flush cascateia sobre as instâncias que a
        // resposta usa, e é o que faz os ids existirem antes de montá-la.
        atendimentoRepository.flush();

        return converter(resultado);
    }

    @Transactional(readOnly = true)
    public List<MedicamentoResponse> doAtendimento(Long atendimentoId) {
        return converter(medicamentoRepository.findByAtendimentoIdOrderByIdAsc(atendimentoId));
    }

    private Medicamento novo(Atendimento atendimento) {
        Medicamento medicamento = new Medicamento();
        medicamento.setAtendimento(atendimento);
        return medicamento;
    }

    private List<MedicamentoResponse> converter(List<Medicamento> medicamentos) {
        return medicamentos.stream().map(MedicamentoResponse::de).toList();
    }
}
