import type {
  Frequencia,
  HabitoIntestinal,
  NivelEstresse,
  Perfil,
  PrazoMeta,
  QualidadeSono,
  RacaCor,
  ResultadoAvaliacao,
  RiscoCardiovascular,
  SecaoProntuario,
  Sexo,
  StatusAtendimento,
  Tabagismo,
  TipoMedicamento,
  TipoRefeicao,
} from '@/types/dominio'

/**
 * Tradução de enum do contrato para o texto que aparece na tela.
 *
 * Cada mapa é um `Record` fechado sobre o enum: se o contrato ganhar um valor
 * novo, o TypeScript acusa aqui em vez de a UI mostrar a constante crua.
 */

export const PERFIL: Record<Perfil, string> = {
  ESTAGIARIO: 'Estagiário',
  SUPERVISOR: 'Supervisor',
  ADMIN: 'Administrador',
}

export const STATUS_ATENDIMENTO: Record<StatusAtendimento, string> = {
  RASCUNHO: 'Rascunho',
  EM_REVISAO: 'Em revisão',
  APROVADO: 'Aprovado',
  DEVOLVIDO_PARA_CORRECAO: 'Devolvido',
}

export const SECAO_PRONTUARIO: Record<SecaoProntuario, string> = {
  QUEIXA_PRINCIPAL: 'Queixa principal',
  HISTORIA_CLINICA: 'História clínica',
  MEDICAMENTOS: 'Medicamentos',
  ANTROPOMETRIA: 'Antropometria',
  EXAMES: 'Exames',
  RECORDATORIO: 'Recordatório 24 h',
  FREQUENCIA_ALIMENTAR: 'Frequência alimentar',
  COMPORTAMENTO_ALIMENTAR: 'Comportamento alimentar',
  DIAGNOSTICO: 'Diagnóstico PES',
  PLANO: 'Plano alimentar',
  METAS: 'Metas',
}

export const SEXO: Record<Sexo, string> = {
  FEMININO: 'Feminino',
  MASCULINO: 'Masculino',
  OUTRO: 'Outro',
  NAO_INFORMADO: 'Não informado',
}

export const RACA_COR: Record<RacaCor, string> = {
  BRANCA: 'Branca',
  PRETA: 'Preta',
  PARDA: 'Parda',
  AMARELA: 'Amarela',
  INDIGENA: 'Indígena',
  NAO_INFORMADO: 'Não informado',
}

export const FREQUENCIA: Record<Frequencia, string> = {
  NUNCA: 'Nunca',
  RARAMENTE: 'Raramente',
  SEMANAL: 'Semanal',
  QUASE_DIARIO: 'Quase diário',
  DIARIO: 'Diário',
}

export const TIPO_MEDICAMENTO: Record<TipoMedicamento, string> = {
  MEDICAMENTO: 'Medicamento',
  SUPLEMENTO: 'Suplemento',
}

export const TIPO_REFEICAO: Record<TipoRefeicao, string> = {
  DESJEJUM: 'Desjejum',
  LANCHE_MANHA: 'Lanche da manhã',
  ALMOCO: 'Almoço',
  LANCHE_TARDE: 'Lanche da tarde',
  JANTAR: 'Jantar',
  CEIA: 'Ceia',
  OUTRO: 'Outro',
}

export const PRAZO_META: Record<PrazoMeta, string> = {
  CURTO: 'Curto',
  MEDIO: 'Médio',
}

export const TABAGISMO: Record<Tabagismo, string> = {
  NUNCA_FUMOU: 'Nunca fumou',
  EX_FUMANTE: 'Ex-fumante',
  FUMANTE: 'Fumante',
}

export const QUALIDADE_SONO: Record<QualidadeSono, string> = {
  RUIM: 'Ruim',
  REGULAR: 'Regular',
  BOA: 'Boa',
}

export const NIVEL_ESTRESSE: Record<NivelEstresse, string> = {
  BAIXO: 'Baixo',
  MODERADO: 'Moderado',
  ALTO: 'Alto',
}

export const HABITO_INTESTINAL: Record<HabitoIntestinal, string> = {
  DIARIO: 'Diário',
  ALTERNADO: 'Alternado',
  CONSTIPADO: 'Constipado',
  DIARREICO: 'Diarreico',
  IRREGULAR: 'Irregular',
}

export const RISCO_CARDIOVASCULAR: Record<RiscoCardiovascular, string> = {
  BAIXO: 'Baixo',
  AUMENTADO: 'Aumentado',
  MUITO_AUMENTADO: 'Muito aumentado',
}

/**
 * `classificacaoImc` é `string` no contrato — o backend manda o nome do enum
 * (`OBESIDADE_GRAU_I`). O `?? valor` garante que um valor novo apareça cru em
 * vez de sumir da tela.
 */
export const CLASSIFICACAO_IMC: Record<string, string> = {
  BAIXO_PESO: 'Baixo peso',
  EUTROFIA: 'Eutrofia',
  SOBREPESO: 'Sobrepeso',
  OBESIDADE_GRAU_I: 'Obesidade grau I',
  OBESIDADE_GRAU_II: 'Obesidade grau II',
  OBESIDADE_GRAU_III: 'Obesidade grau III',
}

export const RESULTADO_AVALIACAO: Record<ResultadoAvaliacao, string> = {
  APROVADO: 'Aprovado',
  DEVOLVIDO: 'Devolvido',
}

/** Um mapa vira lista de opções de `<select>` preservando a ordem de declaração. */
export function opcoesDe<T extends string>(
  mapa: Record<T, string>,
): readonly { valor: T; texto: string }[] {
  return (Object.keys(mapa) as T[]).map((valor) => ({ valor, texto: mapa[valor] }))
}
