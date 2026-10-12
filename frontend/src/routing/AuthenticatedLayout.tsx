import { Outlet } from 'react-router'
import { SignOutButton } from '../features/auth/SignOutButton'
import { selectUser } from '../store/authSelectors'
import { useAppSelector } from '../store/hooks'

/** Shell for every signed-in screen: who is signed in, and the way out. */
export function AuthenticatedLayout() {
  const user = useAppSelector(selectUser)
  return (
    <>
      <header className="flex items-center justify-between gap-4 border-b border-gray-200 px-6 py-3">
        <span className="text-sm text-gray-700">{user?.fullName}</span>
        <SignOutButton />
      </header>
      <Outlet />
    </>
  )
}
