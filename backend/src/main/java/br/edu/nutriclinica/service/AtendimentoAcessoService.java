package br.edu.nutriclinica.service;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.Perfil;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.exception.NaoEncontradoException;
import br.edu.nutriclinica.exception.SemPermissaoException;
import br.edu.nutriclinica.exception.TransicaoInvalidaException;
import br.edu.nutriclinica.repository.AtendimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

/**
 * Porta de entrada das seções do prontuário: carrega o atendimento aplicando o
 * escopo de LGPD e a máquina de estados.
 *
 * <p>Existe para que cada seção nova (antropometria, exames, recordatório…) não
 * reimplemente essas duas regras — e para que elas vivam no serviço de domínio,
 * nunca só no frontend.
 *
 * <p>O escopo é predicado da query, com o id do usuário como parâmetro: o banco
 * devolve o atendimento apenas se ele for visível, e nunca chega à aplicação um
 * prontuário que depois seria descartado em Java.
 */
@Service
public class AtendimentoAcessoService {

    /** Estados em que o estagiário ainda pode escrever no prontuário. */
    private static final Set<StatusAtendimento> STATUS_EDITAVEIS =
            Set.of(StatusAtendimento.RASCUNHO, StatusAtendimento.DEVOLVIDO_PARA_CORRECAO);

    private final AtendimentoRepository atendimentoRepository;

    public AtendimentoAcessoService(AtendimentoRepository atendimentoRepository) {
        this.atendimentoRepository = atendimentoRepository;
    }

    /**
     * Carrega o atendimento para leitura, respeitando o escopo do usuário.
     *
     * <p>Estagiário enxerga os próprios atendimentos; supervisor os que
     * supervisiona e os dos estagiários que orienta; admin, todos.
     *
     * @throws NaoEncontradoException se o atendimento não existe <b>ou</b> está
     *         fora do escopo do usuário. Os dois casos respondem igual de
     *         propósito: um 403 aqui confirmaria que aquele id existe, e a
     *         existência de um prontuário alheio já é informação sensível.
     */
    @Transactional(readOnly = true)
    public Atendimento carregarParaLeitura(Long atendimentoId, Usuario usuario) {
        Optional<Atendimento> visivel = switch (usuario.getPerfil()) {
            case ESTAGIARIO -> atendimentoRepository.buscarVisivelPeloEstagiario(atendimentoId, usuario.getId());
            case SUPERVISOR -> atendimentoRepository.buscarVisivelPeloSupervisor(atendimentoId, usuario.getId());
            case ADMIN -> atendimentoRepository.findById(atendimentoId);
        };

        return visivel.orElseThrow(() -> NaoEncontradoException.de("Atendimento", atendimentoId));
    }

    /**
     * Carrega o atendimento para escrita de uma seção do prontuário.
     *
     * <p>Só o estagiário dono escreve, e só enquanto o prontuário está em
     * RASCUNHO ou DEVOLVIDO_PARA_CORRECAO. Em EM_REVISAO e APROVADO a seção é
     * somente leitura — regra inviolável do domínio.
     *
     * <p>Aqui o 403 é legítimo: quem chegou até este ponto já enxerga o
     * atendimento, então recusar a escrita não revela nada de novo.
     *
     * @throws TransicaoInvalidaException se o status atual não admite escrita
     */
    @Transactional(readOnly = true)
    public Atendimento carregarParaEscritaDoEstagiario(Long atendimentoId, Usuario usuario) {
        Atendimento atendimento = carregarParaLeitura(atendimentoId, usuario);

        if (usuario.getPerfil() != Perfil.ESTAGIARIO
                || !atendimento.getEstagiario().getId().equals(usuario.getId())) {
            throw new SemPermissaoException(
                    "Somente o estagiário responsável pode editar este prontuário.");
        }

        if (!STATUS_EDITAVEIS.contains(atendimento.getStatus())) {
            throw new TransicaoInvalidaException(
                    "O prontuário está em " + atendimento.getStatus()
                            + " e não pode ser editado. Só é editável em RASCUNHO ou DEVOLVIDO_PARA_CORRECAO.");
        }
        return atendimento;
    }

    /**
     * O {@code editavel} do contrato: se <b>este</b> usuário pode escrever no
     * prontuário agora.
     *
     * <p>Depende de quem pergunta — o mesmo atendimento é editável para o
     * estagiário dono e somente leitura para o supervisor —, por isso é
     * calculado por requisição e nunca persistido. Espelha exatamente o que
     * {@link #carregarParaEscritaDoEstagiario} aceita, para que o formulário
     * habilitado no frontend nunca discorde da regra do backend.
     */
    public boolean editavelPor(Atendimento atendimento, Usuario usuario) {
        return usuario.getPerfil() == Perfil.ESTAGIARIO
                && atendimento.getEstagiario().getId().equals(usuario.getId())
                && STATUS_EDITAVEIS.contains(atendimento.getStatus());
    }
}
