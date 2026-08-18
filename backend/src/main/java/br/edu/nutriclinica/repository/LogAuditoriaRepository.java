package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.LogAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    Page<LogAuditoria> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId, Pageable pageable);

    Page<LogAuditoria> findByEntidadeAndEntidadeIdOrderByCriadoEmDesc(String entidade, Long entidadeId, Pageable pageable);
}
