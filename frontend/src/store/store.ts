import {
  combineReducers,
  configureStore,
  createListenerMiddleware,
} from '@reduxjs/toolkit'
import authReducer, { loggedIn, loggedOut } from '../features/auth/authSlice'
import { clearSession, loadSession, saveSession } from '../features/auth/authStorage'
import { baseApi } from './baseApi'

const rootReducer = combineReducers({
  auth: authReducer,
  [baseApi.reducerPath]: baseApi.reducer,
})

/** Keeps localStorage in step with the session and wipes cached API data on sign-out. */
function createSessionListener() {
  const listener = createListenerMiddleware()
  listener.startListening({
    actionCreator: loggedIn,
    effect: (action) => saveSession(action.payload),
  })
  listener.startListening({
    actionCreator: loggedOut,
    effect: (_action, api) => {
      clearSession()
      api.dispatch(baseApi.util.resetApiState())
    },
  })
  return listener.middleware
}

export function setupStore(preloadedState?: Partial<RootState>) {
  return configureStore({
    reducer: rootReducer,
    // A stored session is restored on load unless the caller supplies its own state.
    preloadedState: { auth: loadSession(), ...preloadedState },
    middleware: (getDefault) =>
      getDefault().prepend(createSessionListener()).concat(baseApi.middleware),
  })
}

export type RootState = ReturnType<typeof rootReducer>
export type AppStore = ReturnType<typeof setupStore>
export type AppDispatch = AppStore['dispatch']
