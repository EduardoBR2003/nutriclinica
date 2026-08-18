package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.Antropometria;
import org.springframework.data.jpa.repository.JpaRepository;

/** Seção 1:1 do prontuário: o id é o próprio atendimento_id (chave compartilhada). */
public interface AntropometriaRepository extends JpaRepository<Antropometria, Long> {
}
