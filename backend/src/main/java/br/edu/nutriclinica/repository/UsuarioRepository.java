package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.Perfil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca do login, e por isso ignora a caixa: o e-mail é guardado em minúsculas
     * e ninguém digita o próprio e-mail sempre do mesmo jeito.
     */
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Mesma checagem, poupando o próprio usuário na edição. */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Page<Usuario> findByPerfil(Perfil perfil, Pageable pageable);

    List<Usuario> findByIdIn(List<Long> ids);
}
