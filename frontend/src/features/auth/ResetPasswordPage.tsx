import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { AuthLayout } from '../../components/AuthLayout'
import { SubmitButton } from '../../components/SubmitButton'
import { TextField } from '../../components/TextField'
import { useConfirmPasswordResetMutation } from './authApi'
import { getProblem } from './problem'

export function ResetPasswordPage() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const navigate = useNavigate()
  const [confirmReset, { isLoading }] = useConfirmPasswordResetMutation()
  const [newPassword, setNewPassword] = useState('')
  const [linkInvalid, setLinkInvalid] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!token || linkInvalid) {
    return (
      <AuthLayout title="Link no longer works">
        <p role="alert">
          This password reset link is invalid or has expired. Request a new one to
          continue.
        </p>
        <Link to="/forgot-password" className="text-blue-700 underline">
          Request a new link
        </Link>
      </AuthLayout>
    )
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const result = await confirmReset({ token: token!, newPassword })
    if (!('error' in result)) {
      navigate('/login', {
        replace: true,
        state: { message: 'Password updated. Sign in with your new password.' },
      })
      return
    }
    const problem = getProblem(result.error)
    if (problem?.code === 'RESET_TOKEN_INVALID') {
      setLinkInvalid(true)
    } else if (problem?.code === 'VALIDATION_FAILED') {
      setError('Password must be at least 8 characters.')
    } else {
      setError('Something went wrong. Please try again.')
    }
  }

  return (
    <AuthLayout title="Set a new password">
      <form onSubmit={onSubmit} className="flex flex-col gap-4">
        <TextField
          label="New password"
          type="password"
          autoComplete="new-password"
          required
          minLength={8}
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
        />
        {error && (
          <p role="alert" className="text-red-600">
            {error}
          </p>
        )}
        <SubmitButton disabled={isLoading}>Update password</SubmitButton>
      </form>
    </AuthLayout>
  )
}
