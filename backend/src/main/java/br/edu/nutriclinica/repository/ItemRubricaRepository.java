package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.ItemRubrica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRubricaRepository extends JpaRepository<ItemRubrica, Long> {

    List<ItemRubrica> findByAvaliacaoIdOrderByOrdemAsc(Long avaliacaoId);
}
