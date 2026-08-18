import { Route, Routes } from 'react-router'

import { LayoutApp } from '@/components/LayoutApp'
import { RotaProtegida } from '@/components/RotaProtegida'
import EmConstrucao from '@/pages/EmConstrucao'
import Inicio from '@/pages/Inicio'
import Login from '@/pages/Login'
import NaoEncontrada from '@/pages/NaoEncontrada'
import SemPermissao from '@/pages/SemPermissao'

const TODOS = ['ESTAGIARIO', 'SUPERVISOR', 'ADMIN'] as const

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/sem-permissao" element={<SemPermissao />} />

      {/* Exige apenas sessão; o recorte por perfil vem nos grupos internos. */}
      <Route element={<RotaProtegida />}>
        <Route element={<LayoutApp />}>
          <Route index element={<Inicio />} />

          <Route element={<RotaProtegida perfis={TODOS} />}>
            <Route path="/pacientes" element={<EmConstrucao titulo="Pacientes" />} />
            <Route path="/pacientes/novo" element={<EmConstrucao titulo="Novo paciente" />} />
            <Route path="/pacientes/:id" element={<EmConstrucao titulo="Paciente" />} />
          </Route>

          <Route element={<RotaProtegida perfis={['ESTAGIARIO', 'SUPERVISOR']} />}>
            <Route path="/atendimentos" element={<EmConstrucao titulo="Atendimentos" />} />
            <Route path="/atendimentos/:id" element={<EmConstrucao titulo="Atendimento" />} />
          </Route>

          <Route element={<RotaProtegida perfis={['SUPERVISOR']} />}>
            <Route path="/revisoes" element={<EmConstrucao titulo="Revisões pendentes" />} />
          </Route>

          <Route element={<RotaProtegida perfis={['ADMIN']} />}>
            <Route path="/admin/usuarios" element={<EmConstrucao titulo="Usuários" />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NaoEncontrada />} />
    </Routes>
  )
}
