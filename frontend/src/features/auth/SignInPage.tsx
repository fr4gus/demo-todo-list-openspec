import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { AuthLayout } from '../../components/AuthLayout'
import { SubmitButton } from '../../components/SubmitButton'
import { TextField } from '../../components/TextField'
import { homePath } from '../../routing/homePath'
import { useAppDispatch } from '../../store/hooks'
import { useLoginMutation } from './authApi'
import { loggedIn } from './authSlice'
import { getProblem } from './problem'

const MESSAGES: Record<string, string> = {
  EMAIL_NOT_FOUND: "Email doesn't exist",
  INVALID_PASSWORD: 'invalid password',
}

export function SignInPage() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const notice = (location.state as { message?: string } | null)?.message
  const [login, { isLoading }] = useLoginMutation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const result = await login({ email, password })
    if ('error' in result) {
      const code = getProblem(result.error)?.code
      setError((code && MESSAGES[code]) ?? 'Something went wrong. Please try again.')
      return
    }
    dispatch(loggedIn(result.data))
    navigate(homePath(result.data.user.role), { replace: true })
  }

  return (
    <AuthLayout title="Sign in">
      {notice && (
        <p role="status" className="text-green-700">
          {notice}
        </p>
      )}
      <form onSubmit={onSubmit} className="flex flex-col gap-4">
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <TextField
          label="Password"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {error && (
          <p role="alert" className="text-red-600">
            {error}
          </p>
        )}
        <SubmitButton disabled={isLoading}>Sign in</SubmitButton>
      </form>
      <nav className="flex flex-col gap-1 text-sm">
        <Link to="/forgot-password" className="text-blue-700 underline">
          Forgot your password?
        </Link>
        <Link to="/signup" className="text-blue-700 underline">
          Create an account
        </Link>
      </nav>
    </AuthLayout>
  )
}
