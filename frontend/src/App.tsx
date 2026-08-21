import { Route, Routes } from 'react-router'

import { LayoutApp } from '@/components/LayoutApp'
import { RotaProtegida } from '@/components/RotaProtegida'
import AtendimentoNovo from '@/pages/AtendimentoNovo'
import Atendimentos from '@/pages/Atendimentos'
import Cadastro from '@/pages/Cadastro'
import Evolucao from '@/pages/Evolucao'
import Inicio from '@/pages/Inicio'
import Login from '@/pages/Login'
import NaoEncontrada from '@/pages/NaoEncontrada'
import PacienteForm from '@/pages/PacienteForm'
import Pacientes from '@/pages/Pacientes'
import Prontuario from '@/pages/Prontuario'
import Revisoes from '@/pages/Revisoes'
import SemPermissao from '@/pages/SemPermissao'
import Termo from '@/pages/Termo'
import UsuarioForm from '@/pages/UsuarioForm'
import Usuarios from '@/pages/Usuarios'
import Vinculos from '@/pages/Vinculos'

const TODOS = ['ESTAGIARIO', 'SUPERVISOR', 'ADMIN'] as const
const CLINICA = ['ESTAGIARIO', 'SUPERVISOR'] as const

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/cadastro" element={<Cadastro />} />
      <Route path="/sem-permissao" element={<SemPermissao />} />

      {/* Exige apenas sessão; o recorte por perfil vem nos grupos internos. */}
      <Route element={<RotaProtegida />}>
        <Route element={<LayoutApp />}>
          <Route index element={<Inicio />} />

          <Route element={<RotaProtegida perfis={TODOS} />}>
            <Route path="/pacientes" element={<Pacientes />} />
            <Route path="/pacientes/:id/evolucao" element={<Evolucao />} />
          </Route>

          {/* Supervisor lê os pacientes dos orientados, mas não os cadastra. */}
          <Route element={<RotaProtegida perfis={['ESTAGIARIO', 'ADMIN']} />}>
            <Route path="/pacientes/novo" element={<PacienteForm />} />
            <Route path="/pacientes/:id/editar" element={<PacienteForm />} />
            <Route path="/pacientes/:id/termo" element={<Termo />} />
          </Route>

          <Route element={<RotaProtegida perfis={CLINICA} />}>
            <Route path="/atendimentos" element={<Atendimentos />} />
            <Route path="/atendimentos/:id" element={<Prontuario />} />
          </Route>

          <Route element={<RotaProtegida perfis={['ESTAGIARIO']} />}>
            <Route path="/atendimentos/novo" element={<AtendimentoNovo />} />
          </Route>

          <Route element={<RotaProtegida perfis={['SUPERVISOR']} />}>
            <Route path="/revisoes" element={<Revisoes />} />
          </Route>

          <Route element={<RotaProtegida perfis={['ADMIN']} />}>
            <Route path="/admin/usuarios" element={<Usuarios />} />
            <Route path="/admin/usuarios/novo" element={<UsuarioForm />} />
            <Route path="/admin/usuarios/:id" element={<UsuarioForm />} />
            <Route path="/admin/vinculos" element={<Vinculos />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NaoEncontrada />} />
    </Routes>
  )
}
