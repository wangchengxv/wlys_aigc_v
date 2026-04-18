import { Navigate } from 'react-router-dom'

export function LoginPage() {
  return <Navigate to="/?login=1" replace />
}
