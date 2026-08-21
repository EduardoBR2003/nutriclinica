import type { FieldValues, Path } from 'react-hook-form'

export type TipoCampo =
  | 'texto'
  | 'senha'
  | 'numero'
  | 'data'
  | 'select'
  | 'bool'
  | 'area'

export interface OpcaoCampo {
  valor: string
  texto: string
}

/**
 * Descrição declarativa de um campo, no espírito do `GradeCampos` do design
 * (`['pesoKg','Peso','n',{ req:true, unidade:'kg' }]`), só que tipada contra o
 * formulário a que pertence.
 */
export interface CampoDef<T extends FieldValues> {
  nome: Path<T>
  label: string
  tipo: TipoCampo
  /** Marca com `*`. Quem valida de fato é o servidor. */
  obrigatorio?: boolean
  /** Ocupa a linha inteira da grade. */
  largo?: boolean
  /** Placeholder. */
  dica?: string
  /** Texto auxiliar abaixo do campo. */
  ajuda?: string
  /** Sufixo dentro do input numérico (kg, cm, %…). */
  unidade?: string
  opcoes?: readonly OpcaoCampo[]
  /** `'calculado'` rotula o valor como vindo do servidor. */
  somenteLeitura?: 'calculado' | true
  linhas?: number
}
