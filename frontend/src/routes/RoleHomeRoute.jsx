import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { roleHome } from '../lib/roles'

export function RoleHomeRoute() {
  const { user } = useAuth()
  return <Navigate to={roleHome(user?.role)} replace />
}
