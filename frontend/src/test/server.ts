import { setupServer } from 'msw/node'

// Handlers are added per test with server.use(); the defaults are empty on purpose.
export const server = setupServer()
