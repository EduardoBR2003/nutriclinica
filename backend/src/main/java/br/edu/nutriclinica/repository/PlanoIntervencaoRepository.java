package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.PlanoIntervencao;
import org.springframework.data.jpa.repository.JpaRepository;

/** Seção 1:1 do prontuário: o id é o próprio atendimento_id (chave compartilhada). */
public interface PlanoIntervencaoRepository extends JpaRepository<PlanoIntervencao, Long> {
}
