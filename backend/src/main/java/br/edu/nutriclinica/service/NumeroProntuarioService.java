package br.edu.nutriclinica.service;

import br.edu.nutriclinica.domain.ContadorProntuario;
import br.edu.nutriclinica.repository.ContadorProntuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Gera o número de prontuário no formato {@code AAAA-NNNNNN}, sequencial dentro
 * do ano e reiniciando em 1 a cada ano novo.
 *
 * <p>O ano é o da <b>data da consulta</b>, não o da criação do registro: um
 * atendimento de dezembro lançado em janeiro pertence à série do ano em que foi
 * realizado.
 *
 * <p>A reserva do número roda na transação de quem chamou — {@code MANDATORY} é
 * explícito quanto a isso. Duas razões: sem transação não haveria lock nenhum, e
 * se a criação do atendimento falhar depois o rollback devolve o número em vez
 * de abrir um buraco na sequência.
 */
@Service
public class NumeroProntuarioService {

    private final ContadorProntuarioRepository contadorProntuarioRepository;

    public NumeroProntuarioService(ContadorProntuarioRepository contadorProntuarioRepository) {
        this.contadorProntuarioRepository = contadorProntuarioRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String gerar(LocalDate dataConsulta) {
        int ano = dataConsulta.getYear();

        contadorProntuarioRepository.criarSeAusente(ano);
        ContadorProntuario contador = contadorProntuarioRepository.buscarParaAtualizacao(ano)
                .orElseThrow(() -> new IllegalStateException(
                        "Contador de prontuário do ano " + ano + " não pôde ser criado."));

        contador.setUltimoNumero(contador.getUltimoNumero() + 1);
        contadorProntuarioRepository.save(contador);

        return "%d-%06d".formatted(ano, contador.getUltimoNumero());
    }
}
