package br.edu.nutriclinica.service;

import br.edu.nutriclinica.domain.enums.SecaoProntuario;
import br.edu.nutriclinica.repository.AtendimentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Levanta quais seções de cada prontuário já têm registro persistido.
 *
 * <p>É o {@code secoesPreenchidas} do contrato — o checklist que o estagiário vê
 * antes de submeter. Sempre em lote: uma consulta para a página inteira, em vez
 * de onze por atendimento.
 */
@Service
public class SecoesPreenchidasService {

    private static final Logger log = LoggerFactory.getLogger(SecoesPreenchidasService.class);

    private final AtendimentoRepository atendimentoRepository;

    public SecoesPreenchidasService(AtendimentoRepository atendimentoRepository) {
        this.atendimentoRepository = atendimentoRepository;
    }

    /**
     * @param atendimentoIds pode vir vazio (página sem resultados)
     * @return mapa id → seções preenchidas; ids sem nenhuma seção não aparecem
     */
    @Transactional(readOnly = true)
    public Map<Long, Set<SecaoProntuario>> porAtendimento(Collection<Long> atendimentoIds) {
        // IN () é sintaxe inválida no Postgres: a página vazia sai antes da query.
        if (atendimentoIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Set<SecaoProntuario>> porId = new HashMap<>();
        for (Object[] linha : atendimentoRepository.secoesPreenchidas(atendimentoIds)) {
            Long atendimentoId = ((Number) linha[0]).longValue();
            SecaoProntuario secao = converter((String) linha[1]);
            if (secao != null) {
                porId.computeIfAbsent(atendimentoId, id -> EnumSet.noneOf(SecaoProntuario.class)).add(secao);
            }
        }
        return porId;
    }

    /** Atalho para o atendimento único. */
    @Transactional(readOnly = true)
    public Set<SecaoProntuario> doAtendimento(Long atendimentoId) {
        return porAtendimento(List.of(atendimentoId))
                .getOrDefault(atendimentoId, Set.of());
    }

    /**
     * Os nomes das seções são literais da query nativa. Um literal que não case
     * com o enum é erro de programação, mas não deve derrubar a leitura do
     * prontuário inteiro — some da lista e fica registrado no log.
     */
    private SecaoProntuario converter(String nome) {
        try {
            return SecaoProntuario.valueOf(nome);
        } catch (IllegalArgumentException e) {
            log.warn("Seção desconhecida na query de seções preenchidas: {}", nome);
            return null;
        }
    }
}
