package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.QueixaPrincipal;
import org.springframework.data.jpa.repository.JpaRepository;

/** Seção 1:1 do prontuário: o id é o próprio atendimento_id (chave compartilhada). */
public interface QueixaPrincipalRepository extends JpaRepository<QueixaPrincipal, Long> {
}
