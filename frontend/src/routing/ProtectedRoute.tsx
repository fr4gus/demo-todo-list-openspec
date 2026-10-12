import { Navigate, Outlet, useLocation } from 'react-router'
import type { Role } from '../features/auth/authTypes'
import { selectIsAuthenticated, selectRole } from '../store/authSelectors'
import { useAppSelector } from '../store/hooks'
import { homePath } from './homePath'

interface Props {
  /** Only this role may enter; the other role is sent to its own home. Omit to allow any signed-in user. */
  role?: Role
}

export function ProtectedRoute({ role }: Props) {
  const isAuthenticated = useAppSelector(selectIsAuthenticated)
  const currentRole = useAppSelector(selectRole)
  const location = useLocation()

  if (!isAuthenticated || currentRole === null) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  if (role && currentRole !== role) {
    return <Navigate to={homePath(currentRole)} replace />
  }
  return <Outlet />
}
