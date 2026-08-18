package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.RecordatorioItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordatorioItemRepository extends JpaRepository<RecordatorioItem, Long> {

    List<RecordatorioItem> findByRefeicaoIdOrderByOrdemAsc(Long refeicaoId);
}
