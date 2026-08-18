package br.edu.nutriclinica.repository;

import br.edu.nutriclinica.domain.ContadorProntuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContadorProntuarioRepository extends JpaRepository<ContadorProntuario, Integer> {

    /**
     * Garante a linha do ano antes do lock.
     *
     * <p>É um passo separado porque {@code SELECT ... FOR UPDATE} não tem o que
     * travar quando a linha ainda não existe — no primeiro atendimento do ano,
     * duas transações passariam juntas e ambas tentariam inserir. Com
     * {@code ON CONFLICT DO NOTHING} a segunda espera no lock especulativo da
     * primeira e segue sem erro.
     */
    @Modifying
    @Query(value = """
            INSERT INTO contador_prontuario (ano, ultimo_numero)
            VALUES (:ano, 0)
            ON CONFLICT (ano) DO NOTHING
            """, nativeQuery = true)
    void criarSeAusente(@Param("ano") int ano);

    /**
     * Carrega o contador do ano com {@code SELECT ... FOR UPDATE}: quem chegar
     * depois espera o commit de quem chegou antes, e não há como duas
     * transações lerem o mesmo {@code ultimoNumero}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ContadorProntuario c where c.ano = :ano")
    Optional<ContadorProntuario> buscarParaAtualizacao(@Param("ano") int ano);
}
