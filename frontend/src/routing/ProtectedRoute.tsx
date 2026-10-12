import { Navigate, Outlet, useLocation } from 'react-router'
import { selectIsAuthenticated } from '../store/authSelectors'
import { useAppSelector } from '../store/hooks'

export function ProtectedRoute() {
  const isAuthenticated = useAppSelector(selectIsAuthenticated)
  const location = useLocation()
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  return <Outlet />
}
