import { BrowserRouter } from 'react-router'
import { AppRoutes } from '../routing/AppRoutes'
import { AppProviders } from './AppProviders'

export default function App() {
  return (
    <AppProviders>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AppProviders>
  )
}
