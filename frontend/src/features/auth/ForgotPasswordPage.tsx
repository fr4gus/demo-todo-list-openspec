import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { AuthLayout } from '../../components/AuthLayout'
import { SubmitButton } from '../../components/SubmitButton'
import { TextField } from '../../components/TextField'
import { useRequestPasswordResetMutation } from './authApi'

export function ForgotPasswordPage() {
  const [requestReset, { isLoading }] = useRequestPasswordResetMutation()
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const result = await requestReset({ email })
    if (!('error' in result)) {
      setSent(true)
      return
    }
    const status = (result.error as { status?: unknown }).status
    setError(
      status === 429
        ? 'Too many requests. Please try again later.'
        : 'Something went wrong. Please try again.',
    )
  }

  return (
    <AuthLayout title="Forgot your password?">
      {sent ? (
        // Same message whether or not the account exists.
        <p role="status">
          If an account exists for that email, we sent a link to reset your password.
        </p>
      ) : (
        <form onSubmit={onSubmit} className="flex flex-col gap-4">
          <p className="text-sm text-gray-600">
            Enter your email and we will send you a link to choose a new password.
          </p>
          <TextField
            label="Email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          {error && (
            <p role="alert" className="text-red-600">
              {error}
            </p>
          )}
          <SubmitButton disabled={isLoading}>Send reset link</SubmitButton>
        </form>
      )}
      <Link to="/login" className="text-sm text-blue-700 underline">
        Back to sign in
      </Link>
    </AuthLayout>
  )
}
