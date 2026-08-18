-- =============================================================
-- NutriClinica — Seed inicial de acesso
--
-- Um usuário de cada perfil, todos com a senha "senha123", e o vínculo
-- de supervisão entre o supervisor e o estagiário.
--
-- O hash BCrypt abaixo foi gerado com BCryptPasswordEncoder (cost 10) e
-- confere com "senha123". Troque estas senhas antes de qualquer uso real.
-- =============================================================

INSERT INTO usuario (nome, email, senha_hash, perfil, ativo) VALUES
    ('Administrador do Sistema',
     'admin@nutriclinica.edu.br',
     '$2a$10$ppghIu4XhjdMoMfpR.n5rOaOVhcgTyADMIjM3Yiymp9utFNtx8fM6',
     'ADMIN', TRUE),

    ('Profa. Helena Duarte',
     'supervisor@nutriclinica.edu.br',
     '$2a$10$ppghIu4XhjdMoMfpR.n5rOaOVhcgTyADMIjM3Yiymp9utFNtx8fM6',
     'SUPERVISOR', TRUE),

    ('Marina Rocha',
     'estagiario@nutriclinica.edu.br',
     '$2a$10$ppghIu4XhjdMoMfpR.n5rOaOVhcgTyADMIjM3Yiymp9utFNtx8fM6',
     'ESTAGIARIO', TRUE);

-- Ids resolvidos pelo e-mail para não depender da ordem da sequência.
INSERT INTO vinculo_supervisao (supervisor_id, estagiario_id, ativo)
SELECT s.id, e.id, TRUE
FROM usuario s, usuario e
WHERE s.email = 'supervisor@nutriclinica.edu.br'
  AND e.email = 'estagiario@nutriclinica.edu.br';
