import { api } from '@/lib/api'
import type { FiltroPacientes } from '@/lib/chaves'
import type {
  Pagina,
  Paciente,
  PacienteRequest,
  PontoEvolucao,
  TermoConsentimento,
  TermoConsentimentoRequest,
} from '@/types/dominio'

/** Chamadas de `/api/pacientes`. Só montam a URL e devolvem o corpo. */

export async function listarPacientes(filtro: FiltroPacientes) {
  const { data } = await api.get<Pagina<Paciente>>('/api/pacientes', {
    params: { busca: filtro.busca || undefined, page: filtro.page, size: filtro.size },
  })
  return data
}

export async function obterPaciente(id: number) {
  const { data } = await api.get<Paciente>(`/api/pacientes/${id}`)
  return data
}

export async function criarPaciente(corpo: PacienteRequest) {
  const { data } = await api.post<Paciente>('/api/pacientes', corpo)
  return data
}

export async function atualizarPaciente(id: number, corpo: PacienteRequest) {
  const { data } = await api.put<Paciente>(`/api/pacientes/${id}`, corpo)
  return data
}

export async function obterTermo(id: number) {
  const { data } = await api.get<TermoConsentimento>(`/api/pacientes/${id}/termo`)
  return data
}

export async function salvarTermo(id: number, corpo: TermoConsentimentoRequest) {
  const { data } = await api.put<TermoConsentimento>(`/api/pacientes/${id}/termo`, corpo)
  return data
}

export async function obterEvolucao(id: number) {
  const { data } = await api.get<PontoEvolucao[]>(`/api/pacientes/${id}/evolucao`)
  return data
}
