package br.edu.nutriclinica.exception;

/**
 * Tentativa de abrir atendimento para paciente sem termo LGPD com aceite.
 * Vira 409 / SEM_TERMO_CONSENTIMENTO.
 *
 * <p>Regra inviolável do domínio: sem consentimento registrado não se coleta
 * dado de saúde. Tem código próprio porque o frontend reage a ela de forma
 * específica — leva o usuário à tela do termo, não a uma mensagem de erro.
 */
public class SemTermoConsentimentoException extends ConflitoException {

    public static final String CODIGO = "SEM_TERMO_CONSENTIMENTO";

    public SemTermoConsentimentoException(Long pacienteId) {
        super(CODIGO, "O paciente " + pacienteId + " não tem termo de consentimento LGPD registrado. "
                + "Registre o termo antes de abrir o atendimento.");
    }
}
