import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { AuthLayout } from '../../components/AuthLayout'
import { SubmitButton } from '../../components/SubmitButton'
import { TextField } from '../../components/TextField'
import { useRegisterMutation } from './authApi'
import { getProblem } from './problem'

export function SignUpPage() {
  const navigate = useNavigate()
  const [register, { isLoading }] = useRegisterMutation()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<string[]>([])

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setErrors([])
    const result = await register({ fullName, email, password })
    if (!('error' in result)) {
      navigate('/login', {
        replace: true,
        state: { message: 'Account created. Sign in to continue.' },
      })
      return
    }
    const problem = getProblem(result.error)
    if (problem?.code === 'EMAIL_IN_USE') {
      setErrors(['That email is already in use.'])
    } else if (problem?.errors?.length) {
      setErrors(problem.errors.map((e) => `${e.field}: ${e.message}`))
    } else {
      setErrors(['Something went wrong. Please try again.'])
    }
  }

  return (
    <AuthLayout title="Create your account">
      <form onSubmit={onSubmit} className="flex flex-col gap-4">
        <TextField
          label="Full name"
          autoComplete="name"
          required
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
        />
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
          autoComplete="new-password"
          required
          minLength={8}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {errors.length > 0 && (
          <ul role="alert" className="text-red-600">
            {errors.map((message) => (
              <li key={message}>{message}</li>
            ))}
          </ul>
        )}
        <SubmitButton disabled={isLoading}>Sign up</SubmitButton>
      </form>
      <Link to="/login" className="text-sm text-blue-700 underline">
        Already have an account? Sign in
      </Link>
    </AuthLayout>
  )
}
