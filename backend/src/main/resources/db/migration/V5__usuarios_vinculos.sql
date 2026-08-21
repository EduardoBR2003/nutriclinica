-- Bloco 7 — usuários e vínculos.
--
-- Nenhuma tabela nova: usuario e vinculo_supervisao vieram da V1. O que muda é
-- que agora existem endpoints escrevendo nelas, e duas garantias que só faziam
-- falta quando o cadastro deixou de ser exclusivo do seed.

-- O UNIQUE da V1 é sensível à caixa: 'Ana@x.br' e 'ana@x.br' passariam como dois
-- usuários distintos e virariam dois logins para a mesma pessoa. O serviço já
-- normaliza para minúsculas antes de gravar; este índice é a rede embaixo disso,
-- para o caso de alguma escrita futura esquecer de normalizar.
CREATE UNIQUE INDEX uk_usuario_email_lower ON usuario (LOWER(email));

-- "Quais supervisores orientam este estagiário" é a consulta que alimenta o
-- select de abertura de atendimento. A V1 indexou o lado do supervisor
-- (uk_vinculo começa por supervisor_id), mas não o do estagiário — sem isto, a
-- busca é varredura da tabela inteira.
CREATE INDEX idx_vinculo_estagiario ON vinculo_supervisao (estagiario_id, ativo);
