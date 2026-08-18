-- =============================================================
-- NutriClinica — Contador do número de prontuário
--
-- O número tem o formato AAAA-NNNNNN e é sequencial dentro do ano,
-- reiniciando em 1 a cada ano novo.
--
-- Uma linha por ano, incrementada sob lock pessimista (SELECT ... FOR
-- UPDATE). Duas consultas abertas ao mesmo tempo serializam no lock da
-- linha do ano, e não há como duas receberem o mesmo número.
--
-- Deliberadamente NÃO se usa SELECT MAX(numero_prontuario) + 1 sobre
-- atendimento: entre o SELECT e o INSERT cabe outra transação, e o
-- resultado seria número duplicado (a UNIQUE de numero_prontuario
-- derrubaria a segunda gravação em produção).
-- =============================================================

CREATE TABLE contador_prontuario (
    ano             INTEGER PRIMARY KEY,
    ultimo_numero   BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_contador_ano    CHECK (ano BETWEEN 2000 AND 2999),
    CONSTRAINT ck_contador_numero CHECK (ultimo_numero >= 0)
);
