package br.edu.nutriclinica.service;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.dto.AtendimentoResponse;
import br.edu.nutriclinica.dto.PaginaResponse;
import br.edu.nutriclinica.repository.AtendimentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A fila de trabalho do supervisor.
 *
 * <p>Traz os atendimentos que já foram submetidos e ainda aguardam parecer,
 * do mais antigo para o mais novo — a ordem vem no {@code Pageable} de quem
 * chama. O escopo é predicado da query: um supervisor nunca recebe a fila de
 * outro.
 */
@Service
public class RevisaoService {

    private final AtendimentoRepository atendimentoRepository;
    private final AtendimentoResponseAssembler assembler;
    private final AuthService authService;

    public RevisaoService(AtendimentoRepository atendimentoRepository,
                          AtendimentoResponseAssembler assembler,
                          AuthService authService) {
        this.atendimentoRepository = atendimentoRepository;
        this.assembler = assembler;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AtendimentoResponse> pendentes(Pageable pageable) {
        Usuario supervisor = authService.usuarioLogado();

        Page<Atendimento> pagina =
                atendimentoRepository.listarPendentesDeRevisao(supervisor.getId(), pageable);

        return PaginaResponse.de(pagina, assembler.emLote(pagina.getContent(), supervisor));
    }
}
