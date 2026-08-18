package br.edu.nutriclinica.service.secao;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.service.AtendimentoEditavelValidator;
import br.edu.nutriclinica.service.AuthService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * O esqueleto comum das seções 1:1 do prontuário.
 *
 * <p>Existe por causa de uma regra do bloco: a validação de escrita é a primeira
 * linha de todo endpoint de seção, sem exceção. Concentrá-la aqui torna
 * impossível uma seção nova esquecer de chamá-la — a subclasse não tem onde
 * escrever o {@code salvar} sem passar por {@link #salvar}.
 *
 * <p>Cada subclasse só declara as três coisas que a distinguem: como nasce a
 * seção, como o corpo do PATCH é aplicado nela e como ela vira resposta.
 *
 * @param <E>   entidade da seção, com chave compartilhada com o atendimento
 * @param <REQ> corpo do PATCH, com campos {@code JsonNullable}
 * @param <RES> schema de resposta do contrato
 */
public abstract class SecaoSimplesService<E, REQ, RES> {

    private final JpaRepository<E, Long> repository;
    private final AtendimentoEditavelValidator atendimentoEditavelValidator;
    private final AuthService authService;

    protected SecaoSimplesService(JpaRepository<E, Long> repository,
                                  AtendimentoEditavelValidator atendimentoEditavelValidator,
                                  AuthService authService) {
        this.repository = repository;
        this.atendimentoEditavelValidator = atendimentoEditavelValidator;
        this.authService = authService;
    }

    /**
     * Aplica o PATCH à seção e devolve o estado resultante.
     *
     * <p>Campo ausente no corpo mantém o valor gravado; campo presente com
     * {@code null} limpa. É {@link Merge#aplicar} quem faz essa distinção, campo
     * a campo, dentro de {@link #aplicar}.
     */
    @Transactional
    public RES salvar(Long atendimentoId, REQ requisicao) {
        Usuario usuario = authService.usuarioLogado();
        Atendimento atendimento = atendimentoEditavelValidator.exigirEditavel(atendimentoId, usuario);

        E secao = repository.findById(atendimentoId).orElseGet(() -> novaSecao(atendimento));
        aplicar(requisicao, secao);

        return converter(repository.save(secao));
    }

    /**
     * Leitura a partir de um atendimento que o chamador já carregou com o escopo
     * aplicado — é assim que o prontuário completo monta a seção sem repetir a
     * consulta de acesso.
     *
     * @return vazio se a seção ainda não foi preenchida
     */
    @Transactional(readOnly = true)
    public Optional<RES> buscarDoAtendimento(Long atendimentoId) {
        return repository.findById(atendimentoId).map(this::converter);
    }

    /** A seção recém-criada; o id vem do atendimento por {@code @MapsId}. */
    protected abstract E novaSecao(Atendimento atendimento);

    /** Copia para a seção só os campos que vieram no corpo. */
    protected abstract void aplicar(REQ requisicao, E secao);

    protected abstract RES converter(E secao);
}
