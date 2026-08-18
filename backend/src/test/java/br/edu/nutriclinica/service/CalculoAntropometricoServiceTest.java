package br.edu.nutriclinica.service;

import br.edu.nutriclinica.domain.enums.ClassificacaoImc;
import br.edu.nutriclinica.domain.enums.RiscoCardiovascular;
import br.edu.nutriclinica.domain.enums.Sexo;
import br.edu.nutriclinica.dto.ResultadoAntropometrico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os pontos de corte verificados aqui vêm de {@code docs/regras-calculo.md}.
 * Cada faixa é testada nos dois lados do limite, que é onde a implementação
 * com {@code double} erraria.
 */
class CalculoAntropometricoServiceTest {

    private final CalculoAntropometricoService service = new CalculoAntropometricoService();

    private static BigDecimal dec(String valor) {
        return valor == null ? null : new BigDecimal(valor);
    }

    @Nested
    @DisplayName("IMC")
    class Imc {

        @ParameterizedTest(name = "{0} kg e {1} cm resultam em IMC {2}")
        @CsvSource({
                "70.00,  175.0, 22.86",
                "60.00,  160.0, 23.44",
                "100.00, 180.0, 30.86",
                "45.50,  152.5, 19.56",
                // altura em metro redondo isola o arredondamento HALF_UP da divisão
                "22.855, 100.0, 22.86",
                "22.854, 100.0, 22.85",
        })
        void calculaImcComDuasCasasHalfUp(String pesoKg, String alturaCm, String esperado) {
            assertThat(service.calcularImc(dec(pesoKg), dec(alturaCm)))
                    .isEqualByComparingTo(dec(esperado));
        }

        @Test
        @DisplayName("peso sem altura não produz IMC")
        void pesoSemAlturaRetornaNull() {
            assertThat(service.calcularImc(dec("70.00"), null)).isNull();
        }

        @Test
        @DisplayName("altura sem peso não produz IMC")
        void alturaSemPesoRetornaNull() {
            assertThat(service.calcularImc(null, dec("175.0"))).isNull();
        }

        @Test
        @DisplayName("altura zero é medida faltante, não divisão por zero")
        void alturaZeroRetornaNull() {
            assertThat(service.calcularImc(dec("70.00"), BigDecimal.ZERO)).isNull();
        }
    }

    @Nested
    @DisplayName("Classificação do IMC — adulto (OMS)")
    class ClassificacaoAdulto {

        @ParameterizedTest(name = "IMC {0} aos 30 anos é {1}")
        @CsvSource({
                "18.49, BAIXO_PESO",
                "18.50, EUTROFIA",
                "24.99, EUTROFIA",
                "25.00, SOBREPESO",
                "29.99, SOBREPESO",
                "30.00, OBESIDADE_GRAU_I",
                "34.99, OBESIDADE_GRAU_I",
                "35.00, OBESIDADE_GRAU_II",
                "39.99, OBESIDADE_GRAU_II",
                "40.00, OBESIDADE_GRAU_III",
        })
        void classificaNosLimitesDaTabelaDaOms(String imc, ClassificacaoImc esperado) {
            assertThat(service.classificarImc(dec(imc), 30)).isEqualTo(esperado);
        }
    }

    @Nested
    @DisplayName("Classificação do IMC — idoso (Lipschitz)")
    class ClassificacaoIdoso {

        @ParameterizedTest(name = "IMC {0} aos 70 anos é {1}")
        @CsvSource({
                "21.99, BAIXO_PESO",
                "22.00, EUTROFIA",
                "27.00, EUTROFIA",
                "27.01, SOBREPESO",
        })
        void classificaNosLimitesDeLipschitz(String imc, ClassificacaoImc esperado) {
            assertThat(service.classificarImc(dec(imc), 70)).isEqualTo(esperado);
        }
    }

    @Nested
    @DisplayName("Fronteira etária")
    class FronteiraEtaria {

        @ParameterizedTest(name = "IMC {1} aos {0} anos é {2}")
        @CsvSource({
                // 21.00 é eutrofia pela OMS, mas baixo peso por Lipschitz
                "59, 21.00, EUTROFIA",
                "60, 21.00, BAIXO_PESO",
                // 26.00 é sobrepeso pela OMS, mas eutrofia por Lipschitz
                "59, 26.00, SOBREPESO",
                "60, 26.00, EUTROFIA",
        })
        void aosCinquentaENoveUsaOmsEAosSessentaUsaLipschitz(int idadeAnos,
                                                            String imc,
                                                            ClassificacaoImc esperado) {
            assertThat(service.classificarImc(dec(imc), idadeAnos)).isEqualTo(esperado);
        }

        @ParameterizedTest(name = "aos {0} anos não há classificação por IMC absoluto")
        @ValueSource(ints = {0, 5, 12, 19})
        void menorDeVinteAnosNaoEhClassificado(int idadeAnos) {
            assertThat(service.classificarImc(dec("22.00"), idadeAnos)).isNull();
        }

        @Test
        @DisplayName("aos 20 anos já se aplica a tabela do adulto")
        void aosVinteAnosClassifica() {
            assertThat(service.classificarImc(dec("22.00"), 20)).isEqualTo(ClassificacaoImc.EUTROFIA);
        }

        @Test
        @DisplayName("idade desconhecida não permite escolher a tabela")
        void idadeNulaRetornaNull() {
            assertThat(service.classificarImc(dec("22.00"), null)).isNull();
        }

        @Test
        @DisplayName("sem IMC não há o que classificar")
        void imcNuloRetornaNull() {
            assertThat(service.classificarImc(null, 30)).isNull();
        }
    }

    @Nested
    @DisplayName("Relação cintura/quadril")
    class RelacaoCinturaQuadril {

        @ParameterizedTest(name = "cintura {0} e quadril {1} resultam em RCQ {2}")
        @CsvSource({
                "80.0,  100.0, 0.80",
                "90.0,  100.0, 0.90",
                "85.5,  95.0,  0.90",
                "102.0, 108.0, 0.94",
        })
        void calculaRcqComDuasCasasHalfUp(String cintura, String quadril, String esperado) {
            assertThat(service.calcularRelacaoCinturaQuadril(dec(cintura), dec(quadril)))
                    .isEqualByComparingTo(dec(esperado));
        }

        @Test
        @DisplayName("cintura sem quadril não produz RCQ")
        void cinturaSemQuadrilRetornaNull() {
            assertThat(service.calcularRelacaoCinturaQuadril(dec("90.0"), null)).isNull();
        }

        @Test
        @DisplayName("quadril sem cintura não produz RCQ")
        void quadrilSemCinturaRetornaNull() {
            assertThat(service.calcularRelacaoCinturaQuadril(null, dec("100.0"))).isNull();
        }

        @ParameterizedTest(name = "RCQ {1} no sexo {0} é elevada? {2}")
        @CsvSource({
                "MASCULINO, 0.89, false",
                "MASCULINO, 0.90, true",
                "FEMININO,  0.84, false",
                "FEMININO,  0.85, true",
        })
        void avaliaRcqElevadaNosLimites(Sexo sexo, String rcq, boolean esperado) {
            assertThat(service.avaliarRcqElevada(dec(rcq), sexo)).isEqualTo(esperado);
        }

        @ParameterizedTest(name = "sexo {0} não tem ponto de corte de RCQ")
        @EnumSource(value = Sexo.class, names = {"OUTRO", "NAO_INFORMADO"})
        void sexoSemPontoDeCorteRetornaNull(Sexo sexo) {
            assertThat(service.avaliarRcqElevada(dec("0.95"), sexo)).isNull();
        }

        @Test
        @DisplayName("sem RCQ não há como avaliar o corte")
        void rcqNulaRetornaNull() {
            assertThat(service.avaliarRcqElevada(null, Sexo.MASCULINO)).isNull();
        }
    }

    @Nested
    @DisplayName("Risco cardiovascular")
    class Risco {

        @ParameterizedTest(name = "cintura {1} cm no sexo {0} indica risco {2}")
        @CsvSource({
                "MASCULINO, 93.9,  BAIXO",
                "MASCULINO, 94.0,  AUMENTADO",
                "MASCULINO, 101.9, AUMENTADO",
                "MASCULINO, 102.0, MUITO_AUMENTADO",
                "FEMININO,  79.9,  BAIXO",
                "FEMININO,  80.0,  AUMENTADO",
                "FEMININO,  87.9,  AUMENTADO",
                "FEMININO,  88.0,  MUITO_AUMENTADO",
        })
        void estratificaNosLimitesDaCintura(Sexo sexo, String cintura, RiscoCardiovascular esperado) {
            assertThat(service.avaliarRiscoCardiovascular(dec(cintura), sexo)).isEqualTo(esperado);
        }

        @ParameterizedTest(name = "sexo {0} não tem ponto de corte de cintura")
        @EnumSource(value = Sexo.class, names = {"OUTRO", "NAO_INFORMADO"})
        void sexoSemPontoDeCorteRetornaNull(Sexo sexo) {
            assertThat(service.avaliarRiscoCardiovascular(dec("110.0"), sexo)).isNull();
        }

        @Test
        @DisplayName("sem cintura não há estratificação")
        void cinturaNulaRetornaNull() {
            assertThat(service.avaliarRiscoCardiovascular(null, Sexo.MASCULINO)).isNull();
        }
    }

    @Nested
    @DisplayName("Cálculo completo")
    class Completo {

        @Test
        @DisplayName("aferição completa preenche todos os indicadores")
        void aferacaoCompleta() {
            ResultadoAntropometrico resultado = service.calcular(
                    dec("95.00"), dec("175.0"), dec("104.0"), dec("110.0"), Sexo.MASCULINO, 45);

            assertThat(resultado.imc()).isEqualByComparingTo("31.02");
            assertThat(resultado.classificacaoImc()).isEqualTo(ClassificacaoImc.OBESIDADE_GRAU_I);
            assertThat(resultado.relacaoCinturaQuadril()).isEqualByComparingTo("0.95");
            assertThat(resultado.rcqElevada()).isTrue();
            assertThat(resultado.riscoCardiovascular()).isEqualTo(RiscoCardiovascular.MUITO_AUMENTADO);
        }

        @Test
        @DisplayName("faltar quadril não impede IMC nem risco cardiovascular")
        void indicadoresSaoIndependentes() {
            ResultadoAntropometrico resultado = service.calcular(
                    dec("70.00"), dec("175.0"), dec("95.0"), null, Sexo.MASCULINO, 30);

            assertThat(resultado.imc()).isEqualByComparingTo("22.86");
            assertThat(resultado.classificacaoImc()).isEqualTo(ClassificacaoImc.EUTROFIA);
            assertThat(resultado.relacaoCinturaQuadril()).isNull();
            assertThat(resultado.rcqElevada()).isNull();
            assertThat(resultado.riscoCardiovascular()).isEqualTo(RiscoCardiovascular.AUMENTADO);
        }

        @Test
        @DisplayName("prontuário vazio não lança exceção")
        void tudoAusenteRetornaTudoNull() {
            ResultadoAntropometrico resultado =
                    service.calcular(null, null, null, null, null, null);

            assertThat(resultado.imc()).isNull();
            assertThat(resultado.classificacaoImc()).isNull();
            assertThat(resultado.relacaoCinturaQuadril()).isNull();
            assertThat(resultado.rcqElevada()).isNull();
            assertThat(resultado.riscoCardiovascular()).isNull();
        }

        @Test
        @DisplayName("adolescente tem IMC calculado, mas não classificado")
        void menorDeVinteAnosTemImcSemClassificacao() {
            ResultadoAntropometrico resultado = service.calcular(
                    dec("55.00"), dec("165.0"), null, null, Sexo.FEMININO, 16);

            assertThat(resultado.imc()).isEqualByComparingTo("20.20");
            assertThat(resultado.classificacaoImc()).isNull();
        }
    }
}
