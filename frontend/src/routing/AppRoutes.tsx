import { Route, Routes } from 'react-router'
import { HomePage } from '../app/HomePage'
import { LoginPage } from '../app/LoginPage'
import { ProtectedRoute } from './ProtectedRoute'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<HomePage />} />
      </Route>
    </Routes>
  )
}
