-- Bloco 6 — revisão e avaliação.
--
-- Nenhuma coluna nova: avaliacao_supervisor, item_rubrica, comentario_secao e
-- log_auditoria já nasceram na V1 e as entidades JPA batem com elas. O que
-- faltava eram os índices das consultas que só agora passam a existir.

-- Fila de revisão: os EM_REVISAO de um supervisor, do mais antigo para o mais
-- novo. O idx_atendimento_supervisor (supervisor_id, status) resolve o filtro
-- mas não o ORDER BY submetido_em. O índice parcial serve aos dois e ainda fica
-- minúsculo, porque indexa só a fatia que de fato é fila — um prontuário
-- aprovado sai dele.
CREATE INDEX idx_atendimento_fila_revisao
    ON atendimento (supervisor_id, submetido_em)
    WHERE status = 'EM_REVISAO';

-- Chave estrangeira sem índice: toda leitura da rubrica filtra por avaliacao_id,
-- e o ON DELETE CASCADE de avaliacao_supervisor varre a tabela inteira sem ele.
CREATE INDEX idx_rubrica_avaliacao ON item_rubrica (avaliacao_id);

-- "Quem acessou o prontuário X, e quando" é a pergunta que a LGPD exige
-- responder, e é exatamente o que log_auditoria passa a registrar a cada
-- leitura. Só havia índice por criado_em, então essa consulta era varredura.
CREATE INDEX idx_auditoria_entidade
    ON log_auditoria (entidade, entidade_id, criado_em DESC);

-- comentario_secao.secao era o único enum da V1 sem CHECK, ao lado dos que já
-- têm (ck_meta_prazo, ck_avaliacao_resultado). Fecha a lacuna agora que a coluna
-- passa a receber escrita.
ALTER TABLE comentario_secao ADD CONSTRAINT ck_comentario_secao CHECK (secao IN
    ('QUEIXA_PRINCIPAL', 'HISTORIA_CLINICA', 'MEDICAMENTOS', 'ANTROPOMETRIA',
     'EXAMES', 'RECORDATORIO', 'FREQUENCIA_ALIMENTAR', 'COMPORTAMENTO_ALIMENTAR',
     'DIAGNOSTICO', 'PLANO', 'METAS'));

-- Deliberadamente fora:
--   * CHECK em log_auditoria.acao — o vocabulário de ações é aberto por
--     natureza, e cada ação nova exigiria uma migration.
--   * UNIQUE (avaliacao_id, ordem) em item_rubrica — colidiria com a
--     substituição da rubrica numa reavaliação, em que o Hibernate emite os
--     INSERT antes dos DELETE dos itens antigos.
