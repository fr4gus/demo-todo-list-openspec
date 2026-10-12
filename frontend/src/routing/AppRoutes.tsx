import { Route, Routes } from 'react-router'
import { AdminUsersPage } from '../app/AdminUsersPage'
import { HomePage } from '../app/HomePage'
import { ForgotPasswordPage } from '../features/auth/ForgotPasswordPage'
import { ResetPasswordPage } from '../features/auth/ResetPasswordPage'
import { SignInPage } from '../features/auth/SignInPage'
import { SignUpPage } from '../features/auth/SignUpPage'
import { AuthenticatedLayout } from './AuthenticatedLayout'
import { ProtectedRoute } from './ProtectedRoute'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<SignInPage />} />
      <Route path="/signup" element={<SignUpPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route element={<ProtectedRoute role="USER" />}>
        <Route element={<AuthenticatedLayout />}>
          <Route path="/" element={<HomePage />} />
        </Route>
      </Route>
      <Route element={<ProtectedRoute role="ADMIN" />}>
        <Route element={<AuthenticatedLayout />}>
          <Route path="/admin/users" element={<AdminUsersPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
