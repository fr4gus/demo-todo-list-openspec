import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { makeSession } from '../../test/fixtures'
import { server } from '../../test/server'
import { setupStore } from '../../store/store'
import type { Todo } from './priority'
import { todosApi } from './todosApi'

const API = 'http://localhost:8080/api/v1'

const a: Todo = {
  id: 'a',
  text: 'A',
  priority: 'HIGH',
  completed: false,
  createdAt: '2026-01-01T00:00:00Z',
}
const b: Todo = { ...a, id: 'b', text: 'B', priority: 'LOW' }

function store() {
  const { token, expiresAt, user } = makeSession()
  return setupStore({ auth: { token, expiresAt, user } })
}

const cached = (s: ReturnType<typeof store>) =>
  todosApi.endpoints.getTodos.select()(s.getState()).data

describe('todosApi', () => {
  it('completes optimistically, re-sorts, and keeps the result', async () => {
    let release!: () => void
    const gate = new Promise<void>((r) => (release = r))
    server.use(
      http.get(`${API}/todos`, () => HttpResponse.json([a, b])),
      http.patch(`${API}/todos/a/completion`, async () => {
        await gate
        return HttpResponse.json({ ...a, completed: true })
      }),
    )
    const s = store()
    await s.dispatch(todosApi.endpoints.getTodos.initiate())

    const pending = s.dispatch(
      todosApi.endpoints.setCompletion.initiate({ id: 'a', completed: true }),
    )
    await new Promise((r) => setTimeout(r, 20))
    expect(cached(s)?.map((t) => [t.id, t.completed])).toEqual([
      ['b', false],
      ['a', true],
    ])
    release()
    await pending
  })

  it('rolls back when the server answers 500', async () => {
    server.use(
      http.get(`${API}/todos`, () => HttpResponse.json([a, b])),
      http.patch(`${API}/todos/a/completion`, () =>
        HttpResponse.json({ status: 500 }, { status: 500 }),
      ),
    )
    const s = store()
    await s.dispatch(todosApi.endpoints.getTodos.initiate())

    const result = await s.dispatch(
      todosApi.endpoints.setCompletion.initiate({ id: 'a', completed: true }),
    )

    expect('error' in result).toBe(true)
    expect(cached(s)?.map((t) => [t.id, t.completed])).toEqual([
      ['a', false],
      ['b', false],
    ])
  })

  it.each([
    [
      'create',
      () => http.post(`${API}/todos`, () => HttpResponse.json(a, { status: 201 })),
      () => todosApi.endpoints.createTodo.initiate({ text: 'A', priority: 'HIGH' }),
    ],
    [
      'update',
      () => http.put(`${API}/todos/a`, () => HttpResponse.json(a)),
      () =>
        todosApi.endpoints.updateTodo.initiate({ id: 'a', text: 'A', priority: 'HIGH' }),
    ],
    [
      'delete',
      () => http.delete(`${API}/todos/a`, () => new HttpResponse(null, { status: 204 })),
      () => todosApi.endpoints.deleteTodo.initiate('a'),
    ],
  ])('refetches the list after %s', async (_name, handler, action) => {
    let gets = 0
    server.use(
      http.get(`${API}/todos`, () => {
        gets += 1
        return HttpResponse.json([a])
      }),
      handler(),
    )
    const s = store()
    const subscription = s.dispatch(todosApi.endpoints.getTodos.initiate())
    await subscription
    expect(gets).toBe(1)

    await (s.dispatch as (a: unknown) => Promise<unknown>)(action())

    await expect.poll(() => gets).toBe(2)
    subscription.unsubscribe()
  })
})
