import { useAppDispatch } from '../../store/hooks'
import { loggedOut } from './authSlice'

/** Signing out discards the token; ProtectedRoute then shows the sign-in screen. */
export function SignOutButton() {
  const dispatch = useAppDispatch()
  return (
    <button
      type="button"
      onClick={() => dispatch(loggedOut())}
      className="rounded border border-gray-300 px-3 py-1 text-sm"
    >
      Sign out
    </button>
  )
}
