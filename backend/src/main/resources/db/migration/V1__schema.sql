-- =============================================================
-- NutriClinica — Schema inicial
-- Convenções: snake_case, tabelas no singular, enums via CHECK
-- =============================================================

-- ---------- ACESSO ----------

CREATE TABLE usuario (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    senha_hash      VARCHAR(100) NOT NULL,
    perfil          VARCHAR(20)  NOT NULL,
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_usuario_perfil
        CHECK (perfil IN ('ESTAGIARIO', 'SUPERVISOR', 'ADMIN'))
);

-- Define quais estagiários cada supervisor orienta.
CREATE TABLE vinculo_supervisao (
    id              BIGSERIAL PRIMARY KEY,
    supervisor_id   BIGINT NOT NULL REFERENCES usuario(id),
    estagiario_id   BIGINT NOT NULL REFERENCES usuario(id),
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_vinculo UNIQUE (supervisor_id, estagiario_id)
);

CREATE TABLE refresh_token (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    token           VARCHAR(255) NOT NULL UNIQUE,
    expira_em       TIMESTAMP NOT NULL,
    revogado        BOOLEAN NOT NULL DEFAULT FALSE
);

-- ---------- PACIENTE ----------

CREATE TABLE paciente (
    id                  BIGSERIAL PRIMARY KEY,
    nome                VARCHAR(150) NOT NULL,
    data_nascimento     DATE NOT NULL,
    sexo                VARCHAR(20) NOT NULL,
    raca_cor            VARCHAR(20),
    telefone            VARCHAR(20),
    email               VARCHAR(150),
    criado_por_id       BIGINT NOT NULL REFERENCES usuario(id),
    criado_em           TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_paciente_sexo
        CHECK (sexo IN ('FEMININO', 'MASCULINO', 'OUTRO', 'NAO_INFORMADO')),
    CONSTRAINT ck_paciente_raca
        CHECK (raca_cor IS NULL OR raca_cor IN
              ('BRANCA','PRETA','PARDA','AMARELA','INDIGENA','NAO_INFORMADO'))
);

CREATE INDEX idx_paciente_nome ON paciente (LOWER(nome));

-- LGPD: sem termo registrado, nenhum atendimento pode ser aberto.
CREATE TABLE termo_consentimento (
    id                      BIGSERIAL PRIMARY KEY,
    paciente_id             BIGINT NOT NULL UNIQUE REFERENCES paciente(id) ON DELETE CASCADE,
    aceite_lgpd             BOOLEAN NOT NULL,
    autoriza_uso_pesquisa   BOOLEAN NOT NULL DEFAULT FALSE,
    data_aceite             DATE NOT NULL,
    registrado_por_id       BIGINT NOT NULL REFERENCES usuario(id),
    observacoes             TEXT,
    criado_em               TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------- ATENDIMENTO ----------

CREATE TABLE atendimento (
    id                  BIGSERIAL PRIMARY KEY,
    numero_prontuario   VARCHAR(30) NOT NULL UNIQUE,
    paciente_id         BIGINT NOT NULL REFERENCES paciente(id),
    estagiario_id       BIGINT NOT NULL REFERENCES usuario(id),
    supervisor_id       BIGINT NOT NULL REFERENCES usuario(id),
    data_consulta       DATE NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'RASCUNHO',
    submetido_em        TIMESTAMP,
    avaliado_em         TIMESTAMP,
    criado_em           TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_atendimento_status CHECK (status IN
        ('RASCUNHO', 'EM_REVISAO', 'APROVADO', 'DEVOLVIDO_PARA_CORRECAO'))
);

CREATE INDEX idx_atendimento_estagiario ON atendimento (estagiario_id, status);
CREATE INDEX idx_atendimento_supervisor  ON atendimento (supervisor_id, status);
CREATE INDEX idx_atendimento_paciente    ON atendimento (paciente_id, data_consulta DESC);

-- ---------- SEÇÕES DO PRONTUÁRIO (1:1 com atendimento) ----------

CREATE TABLE queixa_principal (
    atendimento_id      BIGINT PRIMARY KEY REFERENCES atendimento(id) ON DELETE CASCADE,
    motivo              TEXT,
    tempo_queixa        VARCHAR(100),
    tratamento_anterior TEXT,
    objetivo_consulta   TEXT,
    observacoes         TEXT
);

CREATE TABLE historia_clinica (
    atendimento_id          BIGINT PRIMARY KEY REFERENCES atendimento(id) ON DELETE CASCADE,
    doencas_diagnosticadas  TEXT,
    alergias                TEXT,
    intolerancias           TEXT,
    historico_familiar      TEXT,
    tabagismo               VARCHAR(20),
    horas_sono              NUMERIC(3,1),
    qualidade_sono          VARCHAR(20),
    nivel_estresse          VARCHAR(20),
    habito_intestinal       VARCHAR(30),
    pratica_atividade_fisica BOOLEAN,
    descricao_atividade     TEXT,
    observacoes             TEXT,
    CONSTRAINT ck_hc_tabagismo CHECK (tabagismo IS NULL OR tabagismo IN
        ('NUNCA_FUMOU', 'EX_FUMANTE', 'FUMANTE')),
    CONSTRAINT ck_hc_escala CHECK (
        (qualidade_sono IS NULL OR qualidade_sono IN ('RUIM','REGULAR','BOA')) AND
        (nivel_estresse IS NULL OR nivel_estresse IN ('BAIXO','MODERADO','ALTO'))
    ),
    CONSTRAINT ck_hc_intestinal CHECK (habito_intestinal IS NULL OR habito_intestinal IN
        ('DIARIO', 'ALTERNADO', 'CONSTIPADO', 'DIARREICO', 'IRREGULAR'))
);

CREATE TABLE medicamento (
    id              BIGSERIAL PRIMARY KEY,
    atendimento_id  BIGINT NOT NULL REFERENCES atendimento(id) ON DELETE CASCADE,
    tipo            VARCHAR(20) NOT NULL,
    nome            VARCHAR(150) NOT NULL,
    dose            VARCHAR(60),
    frequencia      VARCHAR(60),
    CONSTRAINT ck_medicamento_tipo CHECK (tipo IN ('MEDICAMENTO', 'SUPLEMENTO'))
);

-- imc, classificacao_imc e relacao_cintura_quadril são gravados pelo
-- backend a partir dos valores medidos. NUNCA aceitar do cliente.
CREATE TABLE antropometria (
    atendimento_id          BIGINT PRIMARY KEY REFERENCES atendimento(id) ON DELETE CASCADE,
    peso_kg                 NUMERIC(6,2),
    altura_cm               NUMERIC(5,1),
    circ_cintura_cm         NUMERIC(5,1),
    circ_quadril_cm         NUMERIC(5,1),
    percentual_gordura      NUMERIC(4,1),
    massa_magra_kg          NUMERIC(6,2),
    imc                     NUMERIC(5,2),
    classificacao_imc       VARCHAR(40),
    relacao_cintura_quadril NUMERIC(4,2),
    risco_cardiovascular    VARCHAR(20),
    aferido_em              DATE,
    CONSTRAINT ck_antro_peso   CHECK (peso_kg   IS NULL OR peso_kg   BETWEEN 1 AND 400),
    CONSTRAINT ck_antro_altura CHECK (altura_cm IS NULL OR altura_cm BETWEEN 30 AND 250)
);

CREATE TABLE dados_gestante_infantil (
    atendimento_id              BIGINT PRIMARY KEY REFERENCES atendimento(id) ON DELETE CASCADE,
    peso_pre_gestacional_kg     NUMERIC(6,2),
    idade_gestacional_semanas   INTEGER,
    ganho_peso_gestacional_kg   NUMERIC(5,2),
    escore_peso_estatura        NUMERIC(5,2),
    escore_peso_idade           NUMERIC(5,2),
    escore_estatura_idade       NUMERIC(5,2),
    escore_imc_idade            NUMERIC(5,2)
);

CREATE TABLE exame_bioquimico (
    id              BIGSERIAL PRIMARY KEY,
    atendimento_id  BIGINT NOT NULL REFERENCES atendimento(id) ON DELETE CASCADE,
    nome_exame      VARCHAR(80) NOT NULL,
    valor           NUMERIC(10,2),
    unidade         VARCHAR(20),
    data_exame      DATE,
    valor_referencia VARCHAR(60)
);

-- Recordatório 24h: atendimento → refeições → itens
CREATE TABLE recordatorio_refeicao (
    id              BIGSERIAL PRIMARY KEY,
    atendimento_id  BIGINT NOT NULL REFERENCES atendimento(id) ON DELETE CASCADE,
    tipo_refeicao   VARCHAR(30) NOT NULL,
    horario         TIME,
    local_refeicao  VARCHAR(80),
    ordem           INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_refeicao_tipo CHECK (tipo_refeicao IN
        ('DESJEJUM','LANCHE_MANHA','ALMOCO','LANCHE_TARDE','JANTAR','CEIA','OUTRO'))
);

CREATE TABLE recordatorio_item (
    id              BIGSERIAL PRIMARY KEY,
    refeicao_id     BIGINT NOT NULL REFERENCES recordatorio_refeicao(id) ON DELETE CASCADE,
    alimento        VARCHAR(150) NOT NULL,
    quantidade      VARCHAR(60),
    medida_caseira  VARCHAR(60),
    ordem           INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE frequencia_alimentar (
    atendimento_id      BIGINT PRIMARY KEY REFERENCES atendimento(id) ON DELETE CASCADE,
    frutas              VARCHAR(20),
    verduras_legumes    VARCHAR(20),
    ultraprocessados    VARCHAR(20),
    refrigerante        VARCHAR(20),
    bebida_alcoolica    VARCHAR(20),
    cafe                VARCHAR(20),
    agua_ml_dia         INTEGER,
    observacoes         TEXT,
    CONSTRAINT ck_freq_valores CHECK (
        COALESCE(frutas,'NUNCA') IN ('NUNCA','RARAMENTE','SEMANAL','QUASE_DIARIO','DIARIO') AND
        COALESCE(verduras_legumes,'NUNCA') IN ('NUNCA','RARAMENTE','SEMANAL','QUASE_DIARIO','DIARIO') AND
        COALESCE(ultraprocessados,'NUNCA') IN ('NUNCA','RARAMENTE','SEMANAL','QUASE_DIARIO','DIARIO') AND
        COALESCE(refrigerante,'NUNCA') IN ('NUNCA','RARAMENTE','SEMANAL','QUASE_DIARIO','DIARIO') AND
        COALESCE(bebida_alcoolica,'NUNCA') IN ('NUNCA','RARAMENTE','SEMANAL','QUASE_DIARIO','DIARIO') AND
        COALESCE(cafe,'NUNCA') IN ('NUNCA','RARAMENTE','SEMANAL','QUASE_DIARIO','DIARIO')
    )
);

CREATE TABLE comportamento_alimentar (
    atendimento_id          BIGINT PRIMARY KEY REFERENCES atendimento(id) ON DELETE CASCADE,
    come_assistindo_tela    BOOLEAN,
    come_rapido             BOOLEAN,
    pula_refeicoes          BOOLEAN,
    compulsao_alimentar     BOOLEAN,
    alimentacao_emocional   BOOLEAN,
    restricao_alimentar     BOOLEAN,
    observacoes             TEXT
);

-- Diagnóstico no modelo PES: Problema, Etiologia, Sinais/Sintomas
CREATE TABLE diagnostico_pes (
    atendimento_id  BIGINT PRIMARY KEY REFERENCES atendimento(id) ON DELETE CASCADE,
    problema        TEXT,
    etiologia       TEXT,
    sinais_sintomas TEXT
);

CREATE TABLE plano_intervencao (
    atendimento_id              BIGINT PRIMARY KEY REFERENCES atendimento(id) ON DELETE CASCADE,
    objetivos                   TEXT,
    prescricao_energetica_kcal  INTEGER,
    perc_carboidrato            NUMERIC(4,1),
    perc_proteina               NUMERIC(4,1),
    perc_lipideo                NUMERIC(4,1),
    estrategias_comportamentais TEXT,
    educacao_alimentar          TEXT
);

CREATE TABLE meta (
    id              BIGSERIAL PRIMARY KEY,
    atendimento_id  BIGINT NOT NULL REFERENCES atendimento(id) ON DELETE CASCADE,
    descricao       TEXT NOT NULL,
    prazo           VARCHAR(20) NOT NULL,
    indicador       VARCHAR(150),
    data_retorno    DATE,
    CONSTRAINT ck_meta_prazo CHECK (prazo IN ('CURTO', 'MEDIO'))
);

-- ---------- REVISÃO E AVALIAÇÃO ----------

CREATE TABLE avaliacao_supervisor (
    id              BIGSERIAL PRIMARY KEY,
    atendimento_id  BIGINT NOT NULL UNIQUE REFERENCES atendimento(id) ON DELETE CASCADE,
    supervisor_id   BIGINT NOT NULL REFERENCES usuario(id),
    nota_final      NUMERIC(4,2),
    parecer_geral   TEXT,
    resultado       VARCHAR(30),
    avaliado_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_avaliacao_resultado CHECK (resultado IN ('APROVADO','DEVOLVIDO')),
    CONSTRAINT ck_avaliacao_nota CHECK (nota_final IS NULL OR nota_final BETWEEN 0 AND 10)
);

CREATE TABLE item_rubrica (
    id              BIGSERIAL PRIMARY KEY,
    avaliacao_id    BIGINT NOT NULL REFERENCES avaliacao_supervisor(id) ON DELETE CASCADE,
    criterio        VARCHAR(120) NOT NULL,
    peso            NUMERIC(4,2) NOT NULL DEFAULT 1,
    nota            NUMERIC(4,2),
    comentario      TEXT,
    ordem           INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_rubrica_nota CHECK (nota IS NULL OR nota BETWEEN 0 AND 10)
);

CREATE TABLE comentario_secao (
    id              BIGSERIAL PRIMARY KEY,
    atendimento_id  BIGINT NOT NULL REFERENCES atendimento(id) ON DELETE CASCADE,
    autor_id        BIGINT NOT NULL REFERENCES usuario(id),
    secao           VARCHAR(40) NOT NULL,
    texto           TEXT NOT NULL,
    resolvido       BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comentario_atendimento ON comentario_secao (atendimento_id, secao);

-- ---------- AUDITORIA (LGPD) ----------

CREATE TABLE log_auditoria (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT REFERENCES usuario(id),
    acao            VARCHAR(30) NOT NULL,
    entidade        VARCHAR(60) NOT NULL,
    entidade_id     BIGINT,
    detalhe         TEXT,
    endereco_ip     VARCHAR(45),
    criado_em       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auditoria_criado_em ON log_auditoria (criado_em DESC);
