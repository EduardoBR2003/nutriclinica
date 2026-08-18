package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Os itens desta seção não têm coluna de ordem — o contrato não dá uma —, então
 * a leitura ordena por id: ordem de inserção, estável entre requisições.
 *
 * <p>Não existe aqui um {@code deleteByAtendimentoId}: apagar a coleção inteira
 * é atalho para contornar a reconciliação, que preserva os ids do que continuou.
 */
public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

    List<Medicamento> findByAtendimentoIdOrderByIdAsc(Long atendimentoId);
}
