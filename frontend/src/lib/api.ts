import axios from 'axios'

/**
 * Cliente HTTP único da aplicação. Todo acesso à API passa por aqui,
 * sempre embrulhado em um hook do TanStack Query — nunca chamado
 * diretamente de dentro de um componente.
 */
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})
