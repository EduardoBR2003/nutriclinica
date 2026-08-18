# NutriClinica

Sistema de prontuário eletrônico para a **clínica escola de nutrição**, desenvolvido como
projeto de Iniciação Tecnológica.

Estagiários de Nutrição realizam avaliações nutricionais de pacientes; cada atendimento vira
um prontuário estruturado. Ao final, o estagiário submete o atendimento para revisão e o
supervisor (professor) avalia o trabalho com uma rubrica de critérios e nota, aprovando ou
devolvendo para correção.

### Perfis de usuário

| Perfil        | O que faz                                                       |
|---------------|-----------------------------------------------------------------|
| `ESTAGIARIO`  | Cadastra pacientes, preenche atendimentos, submete para revisão  |
| `SUPERVISOR`  | Revisa, comenta, atribui nota, aprova ou devolve                 |
| `ADMIN`       | Gerencia usuários e o vínculo estagiário ↔ supervisor            |

### Ciclo de vida do atendimento

```
RASCUNHO ──submeter──> EM_REVISAO ──aprovar──> APROVADO
                            │
                        devolver
                            ↓
                 DEVOLVIDO_PARA_CORRECAO ──editar──> RASCUNHO
```

## Stack

**Backend** (`/backend`) — Java 21, Spring Boot 3.5, Maven, Spring Web, Spring Data JPA,
Spring Security (JWT), Bean Validation, Flyway, PostgreSQL 17, springdoc-openapi,
JUnit 5 + Testcontainers.

**Frontend** (`/frontend`) — React 19, Vite, TypeScript (strict), Tailwind CSS, shadcn/ui,
React Router, TanStack Query, React Hook Form + Zod, Axios, Recharts.

**Banco** — PostgreSQL 17 (local via Docker Compose; produção no Neon).

## Estrutura

```
/
├── backend/           # API Spring Boot (Maven)
├── frontend/          # SPA React + Vite
├── docs/
│   ├── api.yaml       # Contrato OpenAPI — fonte da verdade
│   ├── modelo-er.md   # Modelo de dados
│   └── requisitos.md  # Backlog de requisitos
├── docker-compose.yml # PostgreSQL local
└── .github/workflows/ # CI (testes do backend + build do frontend)
```

## Pré-requisitos

- Java 21 (JDK)
- Node.js 20+ e npm
- Docker e Docker Compose
- Git

O Maven não precisa estar instalado — o projeto usa o Maven Wrapper (`./mvnw`).

## Como rodar localmente

**1. Variáveis de ambiente**

```bash
cp .env.example .env            # ajuste usuário e senha do banco
cp frontend/.env.example frontend/.env
```

**2. Banco de dados**

```bash
docker compose up -d            # PostgreSQL 17 em localhost:5433
```

> **Por que 5433 e não 5432?** A porta padrão do PostgreSQL costuma já estar ocupada por
> uma instalação nativa do Postgres na máquina de desenvolvimento. O container publica em
> `5433` no host para conviver com ela — dentro do container o Postgres segue na 5432.
> Se a sua 5432 estiver livre, basta trocar `POSTGRES_PORT` e `DB_URL` no seu `.env`.

| Parâmetro | Valor local |
|-----------|-------------|
| Host      | `localhost` |
| Porta     | `5433`      |
| Database  | `nutriclinica` |
| Usuário   | o que estiver em `POSTGRES_USER` no seu `.env` |

**3. Backend**

```bash
cd backend
./mvnw spring-boot:run          # http://localhost:8080
```

Swagger UI: <http://localhost:8080/swagger-ui.html>

O backend lê `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` do ambiente. Para carregar o `.env`
da raiz na sessão atual:

```bash
set -a && source .env && set +a
```

**4. Frontend**

```bash
cd frontend
npm install
npm run dev                     # http://localhost:5173
```

## Testes e build

```bash
cd backend && ./mvnw test       # testes do backend
cd frontend && npm run build    # build de produção do frontend
```

## Perfis do backend

| Perfil | Uso                | Comportamento                                                  |
|--------|--------------------|----------------------------------------------------------------|
| `dev`  | Desenvolvimento    | Defaults apontando para o Postgres local, SQL logado, Swagger on |
| `prod` | Produção           | Exige as variáveis de ambiente, sem SQL logado, Swagger off      |

O schema é gerenciado **exclusivamente pelo Flyway** (`backend/src/main/resources/db/migration`).
`ddl-auto` fica em `validate` — nunca `update`.

## Etapas do projeto

- [x] **0 — Fundação**: estrutura do monorepo, contrato da API e modelo de dados
- [ ] 1 — Design das telas
- [ ] 2 — Frontend com dados mockados (MSW)
- [ ] 3 — Backend
- [ ] 4 — Integração
- [ ] 5 — Deploy (Vercel + Render + Neon)
- [ ] 6 — Requisitos complementares, testes e documentação

## Privacidade (LGPD)

O sistema trata **dados sensíveis de saúde**. Segredos nunca são versionados (sempre variável
de ambiente com `.env.example` como referência) e o acesso aos pacientes é filtrado no
backend: estagiário enxerga apenas os pacientes dos próprios atendimentos e supervisor apenas
os dos seus orientados.
