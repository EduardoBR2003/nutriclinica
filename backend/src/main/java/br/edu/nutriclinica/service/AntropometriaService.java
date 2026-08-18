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
import br.edu.nutriclinica.service.secao.Merge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

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
 *
 * <p>É a única seção que não estende {@code SecaoSimplesService}: o salvamento
 * dela não termina no merge, precisa do recálculo em cima do resultado dele.
 */
@Service
public class AntropometriaService {

    private final AntropometriaRepository antropometriaRepository;
    private final AtendimentoEditavelValidator atendimentoEditavelValidator;
    private final CalculoAntropometricoService calculoAntropometricoService;
    private final AuthService authService;

    public AntropometriaService(AntropometriaRepository antropometriaRepository,
                                AtendimentoEditavelValidator atendimentoEditavelValidator,
                                CalculoAntropometricoService calculoAntropometricoService,
                                AuthService authService) {
        this.antropometriaRepository = antropometriaRepository;
        this.atendimentoEditavelValidator = atendimentoEditavelValidator;
        this.calculoAntropometricoService = calculoAntropometricoService;
        this.authService = authService;
    }

    /**
     * Salva a seção e devolve os indicadores recalculados.
     *
     * <p>O PATCH é parcial campo a campo: a medida ausente no corpo é mantida, a
     * enviada como {@code null} é apagada.
     *
     * <p>Os derivados, porém, nunca são parciais — são recalculados do zero
     * sobre o estado da seção <b>depois</b> do merge. É a única forma de o IMC
     * continuar coerente quando um PATCH que só mandou o peso muda a conta que
     * dependia da altura gravada antes.
     */
    @Transactional
    public AntropometriaResponse salvar(Long atendimentoId, AntropometriaRequest requisicao) {
        Usuario usuario = authService.usuarioLogado();
        Atendimento atendimento =
                atendimentoEditavelValidator.exigirEditavel(atendimentoId, usuario);

        Antropometria antropometria = antropometriaRepository.findById(atendimentoId)
                .orElseGet(() -> novaSecao(atendimento));

        Merge.aplicar(requisicao.pesoKg(), antropometria::setPesoKg);
        Merge.aplicar(requisicao.alturaCm(), antropometria::setAlturaCm);
        Merge.aplicar(requisicao.circCinturaCm(), antropometria::setCircCinturaCm);
        Merge.aplicar(requisicao.circQuadrilCm(), antropometria::setCircQuadrilCm);
        Merge.aplicar(requisicao.percentualGordura(), antropometria::setPercentualGordura);
        Merge.aplicar(requisicao.massaMagraKg(), antropometria::setMassaMagraKg);
        Merge.aplicar(requisicao.aferidoEm(), antropometria::setAferidoEm);

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
        Atendimento atendimento = atendimentoEditavelValidator.carregarParaLeitura(atendimentoId, usuario);

        return buscarDoAtendimento(atendimento)
                .orElseThrow(() -> NaoEncontradoException.de("Antropometria do atendimento", atendimentoId));
    }

    /**
     * Mesma leitura, a partir de um atendimento que o chamador já carregou com o
     * escopo aplicado — é assim que o prontuário completo monta a seção sem
     * repetir a consulta de acesso nem a regra de recálculo dos derivados.
     *
     * @return vazio se a seção ainda não foi preenchida
     */
    @Transactional(readOnly = true)
    public Optional<AntropometriaResponse> buscarDoAtendimento(Atendimento atendimento) {
        return antropometriaRepository.findById(atendimento.getId())
                .map(antropometria -> {
                    ResultadoAntropometrico resultado = calcular(atendimento, antropometria);
                    return AntropometriaResponse.de(
                            antropometria, resultado.rcqElevada(), resultado.riscoCardiovascular());
                });
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
