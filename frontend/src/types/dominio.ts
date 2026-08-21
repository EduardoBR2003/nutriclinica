import type { components } from '@/types/api'

/**
 * Aliases de domínio sobre os tipos gerados de `docs/api.yaml`.
 *
 * O contrato não declara `required` nos schemas de resposta, então tudo em
 * `api.d.ts` sai opcional (`usuario.perfil?: Perfil`) — inviável para lógica de
 * perfil sob `strict`. Aqui os tipos que a app trata como sempre presentes são
 * estreitados num lugar só. A correção de raiz é acrescentar `required:` ao
 * contrato, o que fica para um bloco futuro por tocar a fonte da verdade.
 */

export type Perfil = components['schemas']['Perfil']
export type StatusAtendimento = components['schemas']['StatusAtendimento']
export type SecaoProntuario = components['schemas']['SecaoProntuario']

export type Usuario = Required<components['schemas']['Usuario']>
export type TokenResponse = Required<
  Omit<components['schemas']['TokenResponse'], 'usuario'>
> & { usuario: Usuario }

export type Erro = components['schemas']['Erro']
export type CampoErro = NonNullable<Erro['campos']>[number]

/** Códigos estáveis emitidos pelo backend — é por eles que o cliente distingue os erros. */
export const CODIGOS_ERRO = [
  'VALIDACAO',
  'NAO_AUTORIZADO',
  'SEM_PERMISSAO',
  'NAO_ENCONTRADO',
  'TRANSICAO_INVALIDA',
  'SEM_TERMO_CONSENTIMENTO',
  'SECOES_INCOMPLETAS',
  'ERRO_INTERNO',
] as const

export type CodigoErro = (typeof CODIGOS_ERRO)[number]

/* ─── Enums do contrato ──────────────────────────────────────────────────── */

export type Frequencia = components['schemas']['Frequencia']
export type Sexo = NonNullable<components['schemas']['Paciente']['sexo']>
export type RacaCor = NonNullable<components['schemas']['Paciente']['racaCor']>
export type TipoMedicamento = components['schemas']['Medicamento']['tipo']
export type TipoRefeicao = components['schemas']['Refeicao']['tipoRefeicao']
export type PrazoMeta = components['schemas']['Meta']['prazo']
export type ResultadoAvaliacao = NonNullable<components['schemas']['Avaliacao']['resultado']>
export type Tabagismo = NonNullable<components['schemas']['HistoriaClinica']['tabagismo']>
export type QualidadeSono = NonNullable<components['schemas']['HistoriaClinica']['qualidadeSono']>
export type NivelEstresse = NonNullable<components['schemas']['HistoriaClinica']['nivelEstresse']>
export type HabitoIntestinal = NonNullable<components['schemas']['HistoriaClinica']['habitoIntestinal']>
export type RiscoCardiovascular = NonNullable<
  components['schemas']['Antropometria']['riscoCardiovascular']
>

/* ─── Paginação ──────────────────────────────────────────────────────────── */

/**
 * O contrato declara `Pagina` sem genérico e cada `PaginaX` como `allOf` com
 * `content`. Um único genérico aqui evita repetir isso em toda lista.
 */
export type Pagina<T> = Required<components['schemas']['Pagina']> & { content: T[] }

/* ─── Usuários ───────────────────────────────────────────────────────────── */

export type UsuarioRequest = components['schemas']['UsuarioRequest']
export type CadastroRequest = components['schemas']['CadastroRequest']

/** O auto-cadastro não oferece ADMIN: conta de administrador só nasce pela mão de outra. */
export type PerfilCadastro = CadastroRequest['perfil']

/* ─── Pacientes ──────────────────────────────────────────────────────────── */

/** `telefone`, `email` e `racaCor` são de fato opcionais no cadastro. */
export type Paciente = Required<
  Omit<components['schemas']['Paciente'], 'telefone' | 'email' | 'racaCor'>
> &
  Pick<components['schemas']['Paciente'], 'telefone' | 'email' | 'racaCor'>

export type PacienteRequest = components['schemas']['PacienteRequest']
export type TermoConsentimento = components['schemas']['TermoConsentimento']
export type TermoConsentimentoRequest = components['schemas']['TermoConsentimentoRequest']
export type PontoEvolucao = components['schemas']['PontoEvolucao']

/* ─── Atendimentos ───────────────────────────────────────────────────────── */

/**
 * `submetidoEm` e `avaliadoEm` só existem depois das transições; o resto o
 * servidor sempre devolve — inclusive `editavel` e `secoesPreenchidas`, que a
 * UI usa em vez de reinferir a regra de edição do status.
 */
export type Atendimento = Required<
  Omit<
    components['schemas']['Atendimento'],
    'submetidoEm' | 'avaliadoEm' | 'paciente' | 'estagiario' | 'supervisor'
  >
> &
  Pick<components['schemas']['Atendimento'], 'submetidoEm' | 'avaliadoEm'> & {
    // Reaproveita os aliases já estreitados em vez das versões todo-opcional.
    paciente: Paciente
    estagiario: Usuario
    supervisor: Usuario
  }

export type AtendimentoCompleto = Atendimento &
  Omit<components['schemas']['AtendimentoCompleto'], keyof components['schemas']['Atendimento']>

export interface NovoAtendimento {
  pacienteId: number
  supervisorId: number
  dataConsulta: string
}

/* ─── Seções do prontuário ───────────────────────────────────────────────── */

export type QueixaPrincipal = components['schemas']['QueixaPrincipal']
export type HistoriaClinica = components['schemas']['HistoriaClinica']
export type Medicamento = components['schemas']['Medicamento']
export type AntropometriaRequest = components['schemas']['AntropometriaRequest']
export type Antropometria = components['schemas']['Antropometria']
export type ExameBioquimico = components['schemas']['ExameBioquimico']
export type Refeicao = components['schemas']['Refeicao']
export type ItemRefeicao = NonNullable<Refeicao['itens']>[number]
export type FrequenciaAlimentar = components['schemas']['FrequenciaAlimentar']
export type ComportamentoAlimentar = components['schemas']['ComportamentoAlimentar']
export type DiagnosticoPes = components['schemas']['DiagnosticoPes']
export type PlanoIntervencao = components['schemas']['PlanoIntervencao']
export type Meta = components['schemas']['Meta']

/* ─── Revisão ────────────────────────────────────────────────────────────── */

export type Avaliacao = components['schemas']['Avaliacao']
export type ItemAvaliacao = NonNullable<Avaliacao['itens']>[number]
export type AvaliacaoRequest = components['schemas']['AvaliacaoRequest']
export type ItemAvaliacaoRequest = AvaliacaoRequest['itens'][number]
export type ComentarioSecao = components['schemas']['ComentarioSecao']

/** Ordem canônica das seções — a mesma do formulário e do array `campos` do 409. */
export const SECOES: readonly SecaoProntuario[] = [
  'QUEIXA_PRINCIPAL',
  'HISTORIA_CLINICA',
  'MEDICAMENTOS',
  'ANTROPOMETRIA',
  'EXAMES',
  'RECORDATORIO',
  'FREQUENCIA_ALIMENTAR',
  'COMPORTAMENTO_ALIMENTAR',
  'DIAGNOSTICO',
  'PLANO',
  'METAS',
] as const
