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

import java.util.Set;

/**
 * Porta de entrada das seções do prontuário: carrega o atendimento aplicando o
 * escopo de LGPD e a máquina de estados.
 *
 * <p>Existe para que cada seção nova (antropometria, exames, recordatório…) não
 * reimplemente essas duas regras — e para que elas vivam no serviço de domínio,
 * nunca só no frontend.
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
     * @throws NaoEncontradoException se o atendimento não existe
     * @throws SemPermissaoException  se está fora do escopo do usuário
     */
    @Transactional(readOnly = true)
    public Atendimento carregarParaLeitura(Long atendimentoId, Usuario usuario) {
        Atendimento atendimento = atendimentoRepository.findById(atendimentoId)
                .orElseThrow(() -> NaoEncontradoException.de("Atendimento", atendimentoId));

        if (!podeVer(atendimento, usuario)) {
            // Mesma mensagem para "não é seu" e para "não existe no seu escopo":
            // confirmar a existência de um prontuário alheio já vazaria informação.
            throw new SemPermissaoException("Este atendimento não pertence ao seu escopo.");
        }
        return atendimento;
    }

    /**
     * Carrega o atendimento para escrita de uma seção do prontuário.
     *
     * <p>Só o estagiário dono escreve, e só enquanto o prontuário está em
     * RASCUNHO ou DEVOLVIDO_PARA_CORRECAO. Em EM_REVISAO e APROVADO a seção é
     * somente leitura — regra inviolável do domínio.
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
     * Escopo de LGPD: estagiário enxerga os próprios atendimentos, supervisor os
     * dos seus orientados. O ADMIN gerencia usuários e vínculos, não prontuários
     * — dado sensível de saúde não entra no seu escopo.
     */
    private boolean podeVer(Atendimento atendimento, Usuario usuario) {
        return switch (usuario.getPerfil()) {
            case ESTAGIARIO -> atendimento.getEstagiario().getId().equals(usuario.getId());
            case SUPERVISOR -> atendimento.getSupervisor().getId().equals(usuario.getId());
            case ADMIN -> false;
        };
    }
}
