import type { Perfil, SecaoProntuario, StatusAtendimento } from '@/types/dominio'

export interface FiltroPacientes {
  busca?: string
  page: number
  size: number
}

export interface FiltroAtendimentos {
  status?: StatusAtendimento
  pacienteId?: number
  page: number
  size: number
}

export interface FiltroUsuarios {
  perfil?: Perfil
  page: number
  size: number
}

/**
 * Chaves de cache do TanStack Query.
 *
 * Todas começam pelo nome do recurso para que a invalidação por prefixo
 * (`{ queryKey: ['atendimentos'] }`) alcance lista e detalhe de uma vez.
 */
export const chaves = {
  pacientes: {
    raiz: ['pacientes'] as const,
    lista: (filtro: FiltroPacientes) => ['pacientes', 'lista', filtro] as const,
    detalhe: (id: number) => ['pacientes', 'detalhe', id] as const,
    termo: (id: number) => ['pacientes', 'termo', id] as const,
    evolucao: (id: number) => ['pacientes', 'evolucao', id] as const,
  },
  atendimentos: {
    raiz: ['atendimentos'] as const,
    lista: (filtro: FiltroAtendimentos) => ['atendimentos', 'lista', filtro] as const,
    detalhe: (id: number) => ['atendimentos', 'detalhe', id] as const,
  },
  revisoes: {
    raiz: ['revisoes'] as const,
    pendentes: (page: number, size: number) => ['revisoes', 'pendentes', page, size] as const,
    avaliacao: (atendimentoId: number) => ['revisoes', 'avaliacao', atendimentoId] as const,
    comentarios: (atendimentoId: number) => ['revisoes', 'comentarios', atendimentoId] as const,
  },
  usuarios: {
    raiz: ['usuarios'] as const,
    lista: (filtro: FiltroUsuarios) => ['usuarios', 'lista', filtro] as const,
    detalhe: (id: number) => ['usuarios', 'detalhe', id] as const,
    supervisores: ['usuarios', 'supervisores'] as const,
    vinculos: (supervisorId: number) => ['usuarios', 'vinculos', supervisorId] as const,
  },
} as const

/** Rota do `PATCH`/`PUT` de cada seção, na grafia kebab-case do contrato. */
export const ROTA_SECAO: Record<SecaoProntuario, string> = {
  QUEIXA_PRINCIPAL: 'queixa-principal',
  HISTORIA_CLINICA: 'historia-clinica',
  MEDICAMENTOS: 'medicamentos',
  ANTROPOMETRIA: 'antropometria',
  EXAMES: 'exames',
  RECORDATORIO: 'recordatorio',
  FREQUENCIA_ALIMENTAR: 'frequencia-alimentar',
  COMPORTAMENTO_ALIMENTAR: 'comportamento-alimentar',
  DIAGNOSTICO: 'diagnostico',
  PLANO: 'plano',
  METAS: 'metas',
}
