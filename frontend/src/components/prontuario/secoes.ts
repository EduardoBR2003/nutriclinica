import type { CampoDef, OpcaoCampo } from '@/components/campos/tipos'
import {
  FREQUENCIA,
  HABITO_INTESTINAL,
  NIVEL_ESTRESSE,
  PRAZO_META,
  QUALIDADE_SONO,
  SECAO_PRONTUARIO,
  TABAGISMO,
  TIPO_MEDICAMENTO,
  TIPO_REFEICAO,
  opcoesDe,
} from '@/lib/rotulos'
import type { SecaoProntuario } from '@/types/dominio'

/**
 * Metadados e campos das onze seções.
 *
 * Os nomes seguem `docs/api.yaml`, não o protótipo: o design foi desenhado
 * antes de o contrato fechar e diverge em várias seções (história clínica,
 * frequência alimentar, comportamento e plano, entre outras).
 */

export type GrupoSecao = 'Avaliação' | 'Consumo alimentar' | 'Conduta'

export interface MetaSecao {
  grupo: GrupoSecao
  titulo: string
  descricao: string
  /** Coleções vão por `PUT` (substituem a lista); as 1:1 vão por `PATCH`. */
  colecao: boolean
}

export const META_SECAO: Record<SecaoProntuario, MetaSecao> = {
  QUEIXA_PRINCIPAL: {
    grupo: 'Avaliação',
    titulo: SECAO_PRONTUARIO.QUEIXA_PRINCIPAL,
    descricao: 'O motivo relatado pelo paciente e o objetivo da consulta.',
    colecao: false,
  },
  HISTORIA_CLINICA: {
    grupo: 'Avaliação',
    titulo: SECAO_PRONTUARIO.HISTORIA_CLINICA,
    descricao: 'Diagnósticos, histórico familiar e hábitos de vida.',
    colecao: false,
  },
  MEDICAMENTOS: {
    grupo: 'Avaliação',
    titulo: SECAO_PRONTUARIO.MEDICAMENTOS,
    descricao: 'Medicamentos e suplementos em uso.',
    colecao: true,
  },
  ANTROPOMETRIA: {
    grupo: 'Avaliação',
    titulo: SECAO_PRONTUARIO.ANTROPOMETRIA,
    descricao: 'Medidas aferidas na consulta — base dos indicadores calculados.',
    colecao: false,
  },
  EXAMES: {
    grupo: 'Avaliação',
    titulo: SECAO_PRONTUARIO.EXAMES,
    descricao: 'Resultados laboratoriais com valor de referência e data de coleta.',
    colecao: true,
  },
  RECORDATORIO: {
    grupo: 'Consumo alimentar',
    titulo: SECAO_PRONTUARIO.RECORDATORIO,
    descricao: 'Refeições das últimas 24 h e os alimentos de cada uma.',
    colecao: true,
  },
  FREQUENCIA_ALIMENTAR: {
    grupo: 'Consumo alimentar',
    titulo: SECAO_PRONTUARIO.FREQUENCIA_ALIMENTAR,
    descricao: 'Frequência habitual por grupo de alimentos e ingestão de água.',
    colecao: false,
  },
  COMPORTAMENTO_ALIMENTAR: {
    grupo: 'Consumo alimentar',
    titulo: SECAO_PRONTUARIO.COMPORTAMENTO_ALIMENTAR,
    descricao: 'Contexto das refeições e sinais de risco comportamental.',
    colecao: false,
  },
  DIAGNOSTICO: {
    grupo: 'Conduta',
    titulo: SECAO_PRONTUARIO.DIAGNOSTICO,
    descricao: 'Problema · Etiologia · Sinais e sintomas.',
    colecao: false,
  },
  PLANO: {
    grupo: 'Conduta',
    titulo: SECAO_PRONTUARIO.PLANO,
    descricao: 'Prescrição energética, macronutrientes e orientações.',
    colecao: false,
  },
  METAS: {
    grupo: 'Conduta',
    titulo: SECAO_PRONTUARIO.METAS,
    descricao: 'Metas pactuadas com o paciente, por prazo e indicador.',
    colecao: true,
  },
}

export const GRUPOS: readonly GrupoSecao[] = ['Avaliação', 'Consumo alimentar', 'Conduta']

/** Campo opcional de enum precisa de uma opção vazia para poder ser limpo. */
function comVazio(opcoes: readonly OpcaoCampo[]): OpcaoCampo[] {
  return [{ valor: '', texto: '—' }, ...opcoes]
}

const FREQ = comVazio(opcoesDe(FREQUENCIA))

/* ─── Seções 1:1 (PATCH) ─────────────────────────────────────────────────── */

export interface FormQueixa {
  motivo: string
  tempoQueixa: string
  tratamentoAnterior: string
  objetivoConsulta: string
  observacoes: string
}

export const PADRAO_QUEIXA: FormQueixa = {
  motivo: '',
  tempoQueixa: '',
  tratamentoAnterior: '',
  objetivoConsulta: '',
  observacoes: '',
}

export const CAMPOS_QUEIXA: readonly CampoDef<FormQueixa>[] = [
  {
    nome: 'motivo',
    label: 'Motivo da consulta',
    tipo: 'area',
    obrigatorio: true,
    largo: true,
    dica: 'Nas palavras do paciente',
  },
  { nome: 'tempoQueixa', label: 'Tempo da queixa', tipo: 'texto', dica: 'Ex.: 14 meses' },
  { nome: 'tratamentoAnterior', label: 'Tratamento anterior', tipo: 'area' },
  { nome: 'objetivoConsulta', label: 'Objetivo da consulta', tipo: 'area' },
  { nome: 'observacoes', label: 'Observações', tipo: 'area', largo: true },
]

export interface FormHistoria {
  doencasDiagnosticadas: string
  alergias: string
  intolerancias: string
  historicoFamiliar: string
  tabagismo: string
  horasSono: number | null
  qualidadeSono: string
  nivelEstresse: string
  habitoIntestinal: string
  praticaAtividadeFisica: boolean
  descricaoAtividade: string
  observacoes: string
}

export const PADRAO_HISTORIA: FormHistoria = {
  doencasDiagnosticadas: '',
  alergias: '',
  intolerancias: '',
  historicoFamiliar: '',
  tabagismo: '',
  horasSono: null,
  qualidadeSono: '',
  nivelEstresse: '',
  habitoIntestinal: '',
  praticaAtividadeFisica: false,
  descricaoAtividade: '',
  observacoes: '',
}

export const CAMPOS_HISTORIA: readonly CampoDef<FormHistoria>[] = [
  { nome: 'doencasDiagnosticadas', label: 'Doenças diagnosticadas', tipo: 'area', largo: true },
  { nome: 'alergias', label: 'Alergias', tipo: 'area' },
  { nome: 'intolerancias', label: 'Intolerâncias', tipo: 'area' },
  { nome: 'historicoFamiliar', label: 'Histórico familiar', tipo: 'area', largo: true },
  { nome: 'tabagismo', label: 'Tabagismo', tipo: 'select', opcoes: comVazio(opcoesDe(TABAGISMO)) },
  { nome: 'horasSono', label: 'Horas de sono', tipo: 'numero', unidade: 'h' },
  {
    nome: 'qualidadeSono',
    label: 'Qualidade do sono',
    tipo: 'select',
    opcoes: comVazio(opcoesDe(QUALIDADE_SONO)),
  },
  {
    nome: 'nivelEstresse',
    label: 'Nível de estresse',
    tipo: 'select',
    opcoes: comVazio(opcoesDe(NIVEL_ESTRESSE)),
  },
  {
    nome: 'habitoIntestinal',
    label: 'Hábito intestinal',
    tipo: 'select',
    opcoes: comVazio(opcoesDe(HABITO_INTESTINAL)),
  },
  { nome: 'praticaAtividadeFisica', label: 'Pratica atividade física', tipo: 'bool' },
  {
    nome: 'descricaoAtividade',
    label: 'Descrição da atividade',
    tipo: 'texto',
    dica: 'Ex.: caminhada 3x/semana, 40 min',
  },
  { nome: 'observacoes', label: 'Observações', tipo: 'area', largo: true },
]

export interface FormAntropometria {
  aferidoEm: string
  pesoKg: number | null
  alturaCm: number | null
  circCinturaCm: number | null
  circQuadrilCm: number | null
  percentualGordura: number | null
  massaMagraKg: number | null
}

export const PADRAO_ANTROPOMETRIA: FormAntropometria = {
  aferidoEm: '',
  pesoKg: null,
  alturaCm: null,
  circCinturaCm: null,
  circQuadrilCm: null,
  percentualGordura: null,
  massaMagraKg: null,
}

export const CAMPOS_ANTROPOMETRIA: readonly CampoDef<FormAntropometria>[] = [
  { nome: 'aferidoEm', label: 'Aferido em', tipo: 'data', obrigatorio: true },
  {
    nome: 'pesoKg',
    label: 'Peso',
    tipo: 'numero',
    obrigatorio: true,
    unidade: 'kg',
    ajuda: '1 a 400',
  },
  {
    nome: 'alturaCm',
    label: 'Altura',
    tipo: 'numero',
    obrigatorio: true,
    unidade: 'cm',
    ajuda: '30 a 250',
  },
  { nome: 'circCinturaCm', label: 'Circ. cintura', tipo: 'numero', unidade: 'cm' },
  { nome: 'circQuadrilCm', label: 'Circ. quadril', tipo: 'numero', unidade: 'cm' },
  { nome: 'percentualGordura', label: 'Gordura corporal', tipo: 'numero', unidade: '%' },
  { nome: 'massaMagraKg', label: 'Massa magra', tipo: 'numero', unidade: 'kg' },
]

export interface FormFrequencia {
  frutas: string
  verdurasLegumes: string
  ultraprocessados: string
  refrigerante: string
  bebidaAlcoolica: string
  cafe: string
  aguaMlDia: number | null
  observacoes: string
}

export const PADRAO_FREQUENCIA: FormFrequencia = {
  frutas: '',
  verdurasLegumes: '',
  ultraprocessados: '',
  refrigerante: '',
  bebidaAlcoolica: '',
  cafe: '',
  aguaMlDia: null,
  observacoes: '',
}

export const CAMPOS_FREQUENCIA: readonly CampoDef<FormFrequencia>[] = [
  { nome: 'frutas', label: 'Frutas', tipo: 'select', opcoes: FREQ },
  { nome: 'verdurasLegumes', label: 'Verduras e legumes', tipo: 'select', opcoes: FREQ },
  { nome: 'ultraprocessados', label: 'Ultraprocessados', tipo: 'select', opcoes: FREQ },
  { nome: 'refrigerante', label: 'Refrigerante', tipo: 'select', opcoes: FREQ },
  { nome: 'bebidaAlcoolica', label: 'Bebida alcoólica', tipo: 'select', opcoes: FREQ },
  { nome: 'cafe', label: 'Café', tipo: 'select', opcoes: FREQ },
  { nome: 'aguaMlDia', label: 'Água por dia', tipo: 'numero', unidade: 'ml', ajuda: '0 a 10000' },
  { nome: 'observacoes', label: 'Observações', tipo: 'area', largo: true },
]

export interface FormComportamento {
  comeAssistindoTela: boolean
  comeRapido: boolean
  pulaRefeicoes: boolean
  compulsaoAlimentar: boolean
  alimentacaoEmocional: boolean
  restricaoAlimentar: boolean
  observacoes: string
}

export const PADRAO_COMPORTAMENTO: FormComportamento = {
  comeAssistindoTela: false,
  comeRapido: false,
  pulaRefeicoes: false,
  compulsaoAlimentar: false,
  alimentacaoEmocional: false,
  restricaoAlimentar: false,
  observacoes: '',
}

export const CAMPOS_COMPORTAMENTO: readonly CampoDef<FormComportamento>[] = [
  { nome: 'comeAssistindoTela', label: 'Come assistindo tela', tipo: 'bool' },
  { nome: 'comeRapido', label: 'Come rápido', tipo: 'bool' },
  { nome: 'pulaRefeicoes', label: 'Pula refeições', tipo: 'bool' },
  { nome: 'compulsaoAlimentar', label: 'Compulsão alimentar', tipo: 'bool' },
  { nome: 'alimentacaoEmocional', label: 'Alimentação emocional', tipo: 'bool' },
  { nome: 'restricaoAlimentar', label: 'Restrição alimentar', tipo: 'bool' },
  { nome: 'observacoes', label: 'Observações', tipo: 'area', largo: true },
]

export interface FormDiagnostico {
  problema: string
  etiologia: string
  sinaisSintomas: string
}

export const PADRAO_DIAGNOSTICO: FormDiagnostico = {
  problema: '',
  etiologia: '',
  sinaisSintomas: '',
}

export const CAMPOS_DIAGNOSTICO: readonly CampoDef<FormDiagnostico>[] = [
  {
    nome: 'problema',
    label: 'Problema',
    tipo: 'area',
    obrigatorio: true,
    largo: true,
    dica: 'Ex.: Ingestão energética excessiva',
  },
  {
    nome: 'etiologia',
    label: 'Etiologia (relacionado a…)',
    tipo: 'area',
    obrigatorio: true,
    largo: true,
    dica: 'Ex.: relacionada ao trabalho em turno noturno',
  },
  {
    nome: 'sinaisSintomas',
    label: 'Sinais e sintomas (evidenciado por…)',
    tipo: 'area',
    obrigatorio: true,
    largo: true,
    dica: 'Ex.: evidenciado por IMC de 27,5 kg/m²',
  },
]

export interface FormPlano {
  objetivos: string
  prescricaoEnergeticaKcal: number | null
  percCarboidrato: number | null
  percProteina: number | null
  percLipideo: number | null
  estrategiasComportamentais: string
  educacaoAlimentar: string
}

export const PADRAO_PLANO: FormPlano = {
  objetivos: '',
  prescricaoEnergeticaKcal: null,
  percCarboidrato: null,
  percProteina: null,
  percLipideo: null,
  estrategiasComportamentais: '',
  educacaoAlimentar: '',
}

export const CAMPOS_PLANO: readonly CampoDef<FormPlano>[] = [
  { nome: 'objetivos', label: 'Objetivos do plano', tipo: 'area', obrigatorio: true, largo: true },
  {
    nome: 'prescricaoEnergeticaKcal',
    label: 'Prescrição energética',
    tipo: 'numero',
    unidade: 'kcal',
  },
  { nome: 'percCarboidrato', label: 'Carboidrato', tipo: 'numero', unidade: '%' },
  { nome: 'percProteina', label: 'Proteína', tipo: 'numero', unidade: '%' },
  { nome: 'percLipideo', label: 'Lipídio', tipo: 'numero', unidade: '%' },
  {
    nome: 'estrategiasComportamentais',
    label: 'Estratégias comportamentais',
    tipo: 'area',
    largo: true,
  },
  { nome: 'educacaoAlimentar', label: 'Educação alimentar', tipo: 'area', largo: true },
]

/* ─── Coleções (PUT) ─────────────────────────────────────────────────────── */

export interface FormMedicamento {
  id?: number
  tipo: string
  nome: string
  dose: string
  frequencia: string
}

export const PADRAO_MEDICAMENTO: FormMedicamento = {
  tipo: 'MEDICAMENTO',
  nome: '',
  dose: '',
  frequencia: '',
}

export const CAMPOS_MEDICAMENTO: readonly CampoDef<FormMedicamento>[] = [
  { nome: 'tipo', label: 'Tipo', tipo: 'select', opcoes: opcoesDe(TIPO_MEDICAMENTO) },
  { nome: 'nome', label: 'Nome', tipo: 'texto', obrigatorio: true },
  { nome: 'dose', label: 'Dose', tipo: 'texto', dica: 'Ex.: 500 mg' },
  { nome: 'frequencia', label: 'Frequência', tipo: 'texto', dica: 'Ex.: 1x à noite' },
]

export interface FormExame {
  id?: number
  nomeExame: string
  valor: number | null
  unidade: string
  valorReferencia: string
  dataExame: string
}

export const PADRAO_EXAME: FormExame = {
  nomeExame: '',
  valor: null,
  unidade: '',
  valorReferencia: '',
  dataExame: '',
}

export const CAMPOS_EXAME: readonly CampoDef<FormExame>[] = [
  { nome: 'nomeExame', label: 'Exame', tipo: 'texto', obrigatorio: true },
  { nome: 'valor', label: 'Valor', tipo: 'numero' },
  { nome: 'unidade', label: 'Unidade', tipo: 'texto', dica: 'mg/dL' },
  { nome: 'valorReferencia', label: 'Referência', tipo: 'texto', dica: '70–99' },
  { nome: 'dataExame', label: 'Data da coleta', tipo: 'data' },
]

export interface FormMeta {
  id?: number
  descricao: string
  prazo: string
  indicador: string
  dataRetorno: string
}

export const PADRAO_META: FormMeta = {
  descricao: '',
  prazo: 'CURTO',
  indicador: '',
  dataRetorno: '',
}

export const CAMPOS_META: readonly CampoDef<FormMeta>[] = [
  { nome: 'descricao', label: 'Descrição', tipo: 'texto', obrigatorio: true, largo: true },
  { nome: 'prazo', label: 'Prazo', tipo: 'select', obrigatorio: true, opcoes: opcoesDe(PRAZO_META) },
  { nome: 'indicador', label: 'Indicador de acompanhamento', tipo: 'texto' },
  { nome: 'dataRetorno', label: 'Data de retorno', tipo: 'data', obrigatorio: true },
]

export interface FormItemRefeicao {
  id?: number
  alimento: string
  quantidade: string
  medidaCaseira: string
}

export const PADRAO_ITEM_REFEICAO: FormItemRefeicao = {
  alimento: '',
  quantidade: '',
  medidaCaseira: '',
}

export const CAMPOS_ITEM_REFEICAO: readonly CampoDef<FormItemRefeicao>[] = [
  { nome: 'alimento', label: 'Alimento', tipo: 'texto', obrigatorio: true },
  { nome: 'quantidade', label: 'Quantidade', tipo: 'texto', dica: '1' },
  { nome: 'medidaCaseira', label: 'Medida caseira', tipo: 'texto', dica: 'xícara, colher de sopa…' },
]

export interface FormRefeicao {
  id?: number
  tipoRefeicao: string
  horario: string
  localRefeicao: string
  itens: FormItemRefeicao[]
}

export const PADRAO_REFEICAO: FormRefeicao = {
  tipoRefeicao: 'DESJEJUM',
  horario: '',
  localRefeicao: '',
  itens: [],
}

export const CAMPOS_REFEICAO: readonly CampoDef<FormRefeicao>[] = [
  { nome: 'tipoRefeicao', label: 'Refeição', tipo: 'select', opcoes: opcoesDe(TIPO_REFEICAO) },
  { nome: 'horario', label: 'Horário', tipo: 'texto', dica: '07:30' },
  { nome: 'localRefeicao', label: 'Local', tipo: 'texto', dica: 'Casa, trabalho…' },
]

export const ROTULO_ITEM: Record<string, { rotulo: string; vazio: string }> = {
  MEDICAMENTOS: {
    rotulo: 'Medicamento',
    vazio: 'Nenhum medicamento ou suplemento registrado.',
  },
  EXAMES: { rotulo: 'Exame', vazio: 'Nenhum resultado laboratorial registrado.' },
  METAS: { rotulo: 'Meta', vazio: 'Nenhuma meta pactuada.' },
  RECORDATORIO: {
    rotulo: 'Refeição',
    vazio: 'Nenhuma refeição registrada — obrigatório ao menos uma.',
  },
}
