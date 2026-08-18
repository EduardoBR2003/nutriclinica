package br.edu.nutriclinica.service;

import br.edu.nutriclinica.domain.Antropometria;
import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Paciente;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.ClassificacaoImc;
import br.edu.nutriclinica.dto.AntropometriaRequest;
import br.edu.nutriclinica.dto.AntropometriaResponse;
import br.edu.nutriclinica.dto.ResultadoAntropometrico;
import br.edu.nutriclinica.exception.NaoEncontradoException;
import br.edu.nutriclinica.repository.AntropometriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

/**
 * Seção de antropometria do prontuário.
 *
 * <p>Orquestra o que {@link CalculoAntropometricoService} calcula: carrega o
 * atendimento com as regras de acesso, grava os valores medidos, recalcula os
 * indicadores derivados do zero e monta a resposta.
 *
 * <p>Nenhum indicador derivado vem do cliente. {@link AntropometriaRequest} não
 * tem campo para eles, então o que chegar no corpo é descartado na
 * desserialização — o IMC gravado é sempre o que o servidor acabou de calcular.
 */
@Service
public class AntropometriaService {

    private final AntropometriaRepository antropometriaRepository;
    private final AtendimentoAcessoService atendimentoAcessoService;
    private final CalculoAntropometricoService calculoAntropometricoService;
    private final AuthService authService;

    public AntropometriaService(AntropometriaRepository antropometriaRepository,
                                AtendimentoAcessoService atendimentoAcessoService,
                                CalculoAntropometricoService calculoAntropometricoService,
                                AuthService authService) {
        this.antropometriaRepository = antropometriaRepository;
        this.atendimentoAcessoService = atendimentoAcessoService;
        this.calculoAntropometricoService = calculoAntropometricoService;
        this.authService = authService;
    }

    /**
     * Salva a seção e devolve os indicadores recalculados.
     *
     * <p>O corpo representa a seção inteira: o que não vier é apagado. O PATCH é
     * parcial em relação ao prontuário — é uma seção entre várias —, não em
     * relação aos campos da própria seção.
     */
    @Transactional
    public AntropometriaResponse salvar(Long atendimentoId, AntropometriaRequest requisicao) {
        Usuario usuario = authService.usuarioLogado();
        Atendimento atendimento =
                atendimentoAcessoService.carregarParaEscritaDoEstagiario(atendimentoId, usuario);

        Antropometria antropometria = antropometriaRepository.findById(atendimentoId)
                .orElseGet(() -> novaSecao(atendimento));

        antropometria.setPesoKg(requisicao.pesoKg());
        antropometria.setAlturaCm(requisicao.alturaCm());
        antropometria.setCircCinturaCm(requisicao.circCinturaCm());
        antropometria.setCircQuadrilCm(requisicao.circQuadrilCm());
        antropometria.setPercentualGordura(requisicao.percentualGordura());
        antropometria.setMassaMagraKg(requisicao.massaMagraKg());
        antropometria.setAferidoEm(requisicao.aferidoEm());

        ResultadoAntropometrico resultado = calcular(atendimento, antropometria);

        antropometria.setImc(resultado.imc());
        antropometria.setClassificacaoImc(nomeDe(resultado.classificacaoImc()));
        antropometria.setRelacaoCinturaQuadril(resultado.relacaoCinturaQuadril());
        // Risco cardiovascular é derivado na leitura, não persistido. Zerar a
        // coluna evita que um valor de gravação anterior sobreviva a uma
        // medida que mudou.
        antropometria.setRiscoCardiovascular(null);

        antropometriaRepository.save(antropometria);

        return AntropometriaResponse.de(antropometria, resultado.rcqElevada(), resultado.riscoCardiovascular());
    }

    /** Leitura da seção, com os derivados recalculados a partir do que está gravado. */
    @Transactional(readOnly = true)
    public AntropometriaResponse buscar(Long atendimentoId) {
        Usuario usuario = authService.usuarioLogado();
        Atendimento atendimento = atendimentoAcessoService.carregarParaLeitura(atendimentoId, usuario);

        Antropometria antropometria = antropometriaRepository.findById(atendimentoId)
                .orElseThrow(() -> NaoEncontradoException.de("Antropometria do atendimento", atendimentoId));

        ResultadoAntropometrico resultado = calcular(atendimento, antropometria);
        return AntropometriaResponse.de(antropometria, resultado.rcqElevada(), resultado.riscoCardiovascular());
    }

    private ResultadoAntropometrico calcular(Atendimento atendimento, Antropometria antropometria) {
        Paciente paciente = atendimento.getPaciente();

        return calculoAntropometricoService.calcular(
                antropometria.getPesoKg(),
                antropometria.getAlturaCm(),
                antropometria.getCircCinturaCm(),
                antropometria.getCircQuadrilCm(),
                paciente.getSexo(),
                idadeNaConsulta(paciente.getDataNascimento(), atendimento.getDataConsulta()));
    }

    /**
     * Idade do paciente <b>na data da consulta</b>, não a idade atual: um
     * atendimento antigo reaberto hoje precisa continuar classificado pela faixa
     * etária de quando foi feito.
     */
    private Integer idadeNaConsulta(LocalDate dataNascimento, LocalDate dataConsulta) {
        if (dataNascimento == null || dataConsulta == null) {
            return null;
        }
        return Period.between(dataNascimento, dataConsulta).getYears();
    }

    private Antropometria novaSecao(Atendimento atendimento) {
        Antropometria antropometria = new Antropometria();
        // @MapsId: o id vem do atendimento associado, não é atribuído à mão.
        antropometria.setAtendimento(atendimento);
        return antropometria;
    }

    private String nomeDe(ClassificacaoImc classificacao) {
        return classificacao == null ? null : classificacao.name();
    }
}
