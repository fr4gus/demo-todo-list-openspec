import { act, fireEvent, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { signedInState } from '../../test/fixtures'
import { renderWithProviders } from '../../test/renderWithProviders'
import { server } from '../../test/server'
import { compareTodos, type Todo } from './priority'
import { TodoListPage } from './TodoListPage'

const API = 'http://localhost:8080/api/v1'

function todo(over: Partial<Todo> & { id: string }): Todo {
  return {
    text: `Task ${over.id}`,
    priority: 'MEDIUM_LOW',
    completed: false,
    createdAt: '2026-01-01T00:00:00Z',
    ...over,
  }
}

/** A tiny in-memory stand-in for the todo API, sorted by the server's rule. */
function fakeApi(initial: Todo[], opts: { failCompletion?: boolean } = {}) {
  let items = [...initial]
  let seq = 0
  const calls: string[] = []
  server.use(
    http.get(`${API}/todos`, () => HttpResponse.json([...items].sort(compareTodos))),
    http.post(`${API}/todos`, async ({ request }) => {
      const body = (await request.json()) as Pick<Todo, 'text' | 'priority'>
      const created = todo({
        id: `new-${++seq}`,
        ...body,
        createdAt: `2026-06-0${seq}T00:00:00Z`,
      })
      items.push(created)
      calls.push('create')
      return HttpResponse.json(created, { status: 201 })
    }),
    http.put(`${API}/todos/:id`, async ({ request, params }) => {
      const body = (await request.json()) as Pick<Todo, 'text' | 'priority'>
      items = items.map((t) => (t.id === params.id ? { ...t, ...body } : t))
      calls.push('update')
      return HttpResponse.json(items.find((t) => t.id === params.id))
    }),
    http.patch(`${API}/todos/:id/completion`, async ({ request, params }) => {
      if (opts.failCompletion) return HttpResponse.json({ status: 500 }, { status: 500 })
      const { completed } = (await request.json()) as { completed: boolean }
      items = items.map((t) => (t.id === params.id ? { ...t, completed } : t))
      calls.push(`completion:${completed}`)
      return HttpResponse.json(items.find((t) => t.id === params.id))
    }),
    http.delete(`${API}/todos/:id`, ({ params }) => {
      items = items.filter((t) => t.id !== params.id)
      calls.push('delete')
      return new HttpResponse(null, { status: 204 })
    }),
  )
  return { calls }
}

function renderPage() {
  return renderWithProviders(<TodoListPage />, { preloadedState: signedInState() })
}

const rowTexts = () => screen.getAllByRole('listitem').map((li) => li.textContent ?? '')

describe('TodoListPage', () => {
  describe('list', () => {
    it('shows the empty state and the Create TODO button', async () => {
      fakeApi([])
      renderPage()

      expect(await screen.findByText('No tasks to do this time!')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Create TODO' })).toBeInTheDocument()
      expect(screen.queryByRole('list')).not.toBeInTheDocument()
    })

    it('renders items in server order without their ids', async () => {
      fakeApi([
        todo({ id: 'id-c', text: 'Third', priority: 'LOW' }),
        todo({ id: 'id-a', text: 'First', priority: 'HIGH' }),
        todo({ id: 'id-d', text: 'Done', completed: true, priority: 'HIGH' }),
        todo({ id: 'id-b', text: 'Second', priority: 'MEDIUM_HIGH' }),
      ])
      const { container } = renderPage()

      await screen.findByText('First')
      expect(rowTexts()).toEqual(['First', 'Second', 'Third', 'Done'])
      expect(container.textContent).not.toMatch(/id-[a-d]/)
    })

    it('shows an error with Retry when loading fails', async () => {
      server.use(
        http.get(`${API}/todos`, () =>
          HttpResponse.json({ status: 500 }, { status: 500 }),
        ),
      )
      renderPage()

      expect(await screen.findByText('Could not load your tasks.')).toBeInTheDocument()
      fakeApi([todo({ id: '1', text: 'Back' })])
      await userEvent.click(screen.getByRole('button', { name: 'Retry' }))
      expect(await screen.findByText('Back')).toBeInTheDocument()
    })
  })

  describe('rows', () => {
    it.each([
      ['HIGH', 'bg-red-500'],
      ['MEDIUM_HIGH', 'bg-orange-500'],
      ['MEDIUM_LOW', 'bg-green-500'],
      ['LOW', 'bg-blue-500'],
    ] as const)('colors a %s item with %s', async (priority, cls) => {
      fakeApi([todo({ id: '1', text: 'Colored', priority })])
      renderPage()

      const row = await screen.findByRole('button', { name: 'Edit Colored' })
      expect(row).toHaveClass(cls)
    })

    it('grays out completed items with a strikethrough', async () => {
      fakeApi([todo({ id: '1', text: 'Done', completed: true, priority: 'HIGH' })])
      renderPage()

      const row = await screen.findByRole('button', { name: 'Edit Done' })
      expect(row).toHaveClass('bg-gray-200', 'text-gray-500', 'line-through')
      expect(row).not.toHaveClass('bg-red-500')
    })

    it.each([['{Enter}'], [' ']])('opens edit with %j on a focused row', async (key) => {
      fakeApi([todo({ id: '1', text: 'Keyed' })])
      renderPage()
      const user = userEvent.setup()

      const row = await screen.findByRole('button', { name: 'Edit Keyed' })
      row.focus()
      await user.keyboard(key)

      expect(await screen.findByRole('dialog', { name: 'Edit TODO' })).toBeInTheDocument()
    })

    it('keeps the checkbox a separate control that does not open the dialog', async () => {
      fakeApi([todo({ id: '1', text: 'Box' })])
      renderPage()
      const user = userEvent.setup()

      await user.click(await screen.findByRole('checkbox', { name: 'Complete Box' }))

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })

  describe('completion', () => {
    it('completes with a toast, and Undo restores the item', async () => {
      const { calls } = fakeApi([
        todo({ id: 'a', text: 'Alpha', priority: 'HIGH' }),
        todo({ id: 'b', text: 'Beta', priority: 'LOW' }),
      ])
      renderPage()
      const user = userEvent.setup()

      await user.click(await screen.findByRole('checkbox', { name: 'Complete Alpha' }))

      expect(await screen.findByText('Task Completed -')).toBeInTheDocument()
      await vi.waitFor(() => expect(rowTexts()).toEqual(['Beta', 'Alpha']))

      await user.click(screen.getByRole('button', { name: 'Undo' }))

      await vi.waitFor(() => expect(rowTexts()).toEqual(['Alpha', 'Beta']))
      expect(screen.queryByText('Task Completed -')).not.toBeInTheDocument()
      expect(calls).toEqual(['completion:true', 'completion:false'])
    })

    it('reverts the item and shows an error when the request fails', async () => {
      fakeApi([todo({ id: 'a', text: 'Alpha' })], { failCompletion: true })
      renderPage()
      const user = userEvent.setup()

      await user.click(await screen.findByRole('checkbox', { name: 'Complete Alpha' }))

      expect(await screen.findByRole('alert')).toHaveTextContent(
        'Could not update the task',
      )
      expect(screen.getByRole('checkbox', { name: 'Complete Alpha' })).not.toBeChecked()
      expect(screen.queryByText('Task Completed -')).not.toBeInTheDocument()
    })

    it('unchecking a completed item restores it without a toast', async () => {
      const { calls } = fakeApi([todo({ id: 'a', text: 'Alpha', completed: true })])
      renderPage()
      const user = userEvent.setup()

      await user.click(
        await screen.findByRole('checkbox', { name: 'Mark Alpha as incomplete' }),
      )

      await screen.findByRole('checkbox', { name: 'Complete Alpha' })
      expect(calls).toEqual(['completion:false'])
      expect(screen.queryByText('Task Completed -')).not.toBeInTheDocument()
    })
  })

  describe('long-press restore', () => {
    beforeEach(() => vi.useFakeTimers({ shouldAdvanceTime: true }))
    afterEach(() => vi.useRealTimers())

    async function longPress(name: string) {
      const row = await screen.findByRole('button', { name })
      fireEvent.pointerDown(row, { clientX: 1, clientY: 1 })
      act(() => void vi.advanceTimersByTime(500))
      fireEvent.pointerUp(row)
      fireEvent.click(row)
    }

    it('confirms and restores, without opening the edit dialog', async () => {
      const { calls } = fakeApi([todo({ id: 'a', text: 'Alpha', completed: true })])
      renderPage()

      await longPress('Edit Alpha')

      const dialog = await screen.findByRole('dialog', { name: 'Mark as incomplete' })
      expect(screen.queryByRole('dialog', { name: 'Edit TODO' })).not.toBeInTheDocument()
      fireEvent.click(within(dialog).getByRole('button', { name: 'Mark as incomplete' }))

      await screen.findByRole('checkbox', { name: 'Complete Alpha' })
      expect(calls).toEqual(['completion:false'])
    })

    it('leaves the item completed when cancelled', async () => {
      const { calls } = fakeApi([todo({ id: 'a', text: 'Alpha', completed: true })])
      renderPage()

      await longPress('Edit Alpha')
      const dialog = await screen.findByRole('dialog', { name: 'Mark as incomplete' })
      fireEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(
        screen.getByRole('checkbox', { name: 'Mark Alpha as incomplete' }),
      ).toBeChecked()
      expect(calls).toEqual([])
    })
  })

  describe('create', () => {
    it('creates an item and shows it in its place', async () => {
      const { calls } = fakeApi([todo({ id: 'a', text: 'Low one', priority: 'LOW' })])
      renderPage()
      const user = userEvent.setup()

      await user.click(await screen.findByRole('button', { name: 'Create TODO' }))
      const dialog = await screen.findByRole('dialog', { name: 'Create TODO' })
      await user.type(within(dialog).getByLabelText('Text'), '  Urgent thing ')
      await user.selectOptions(within(dialog).getByLabelText('Priority'), 'HIGH')
      await user.click(within(dialog).getByRole('button', { name: 'Save' }))

      await vi.waitFor(() => expect(rowTexts()).toEqual(['Urgent thing', 'Low one']))
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(calls).toEqual(['create'])
    })

    it('rejects blank text and keeps the dialog open', async () => {
      const { calls } = fakeApi([])
      renderPage()
      const user = userEvent.setup()

      await user.click(await screen.findByRole('button', { name: 'Create TODO' }))
      const dialog = await screen.findByRole('dialog', { name: 'Create TODO' })
      await user.type(within(dialog).getByLabelText('Text'), '   ')
      await user.click(within(dialog).getByRole('button', { name: 'Save' }))

      expect(within(dialog).getByRole('alert')).toHaveTextContent('Text is required')
      expect(screen.getByRole('dialog', { name: 'Create TODO' })).toBeInTheDocument()
      expect(calls).toEqual([])
    })

    it('shows the server problem when saving fails', async () => {
      fakeApi([])
      server.use(
        http.post(`${API}/todos`, () =>
          HttpResponse.json({ status: 400, detail: 'Text is too long' }, { status: 400 }),
        ),
      )
      renderPage()
      const user = userEvent.setup()

      await user.click(await screen.findByRole('button', { name: 'Create TODO' }))
      const dialog = await screen.findByRole('dialog', { name: 'Create TODO' })
      await user.type(within(dialog).getByLabelText('Text'), 'x')
      await user.click(within(dialog).getByRole('button', { name: 'Save' }))

      expect(await within(dialog).findByText('Text is too long')).toBeInTheDocument()
    })

    it('creates nothing on cancel', async () => {
      const { calls } = fakeApi([])
      renderPage()
      const user = userEvent.setup()

      await user.click(await screen.findByRole('button', { name: 'Create TODO' }))
      const dialog = await screen.findByRole('dialog', { name: 'Create TODO' })
      await user.type(within(dialog).getByLabelText('Text'), 'Never saved')
      await user.click(within(dialog).getByRole('button', { name: 'Cancel' }))

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(calls).toEqual([])
    })
  })

  describe('edit and delete', () => {
    async function openEdit(name: string) {
      const user = userEvent.setup()
      await user.click(await screen.findByRole('button', { name }))
      const dialog = await screen.findByRole('dialog', { name: 'Edit TODO' })
      return { user, dialog }
    }

    it('pre-fills the form and saves changes to the new position', async () => {
      const { calls } = fakeApi([
        todo({ id: 'a', text: 'Alpha', priority: 'LOW' }),
        todo({ id: 'b', text: 'Beta', priority: 'MEDIUM_HIGH' }),
      ])
      renderPage()

      const { user, dialog } = await openEdit('Edit Alpha')
      expect(within(dialog).getByLabelText('Text')).toHaveValue('Alpha')
      expect(within(dialog).getByLabelText('Priority')).toHaveValue('LOW')

      await user.clear(within(dialog).getByLabelText('Text'))
      await user.type(within(dialog).getByLabelText('Text'), 'Alpha 2')
      await user.selectOptions(within(dialog).getByLabelText('Priority'), 'HIGH')
      await user.click(within(dialog).getByRole('button', { name: 'Save' }))

      await vi.waitFor(() => expect(rowTexts()).toEqual(['Alpha 2', 'Beta']))
      expect(calls).toEqual(['update'])
    })

    it('offers Mark as incomplete only for completed items', async () => {
      fakeApi([
        todo({ id: 'a', text: 'Active' }),
        todo({ id: 'b', text: 'Finished', completed: true }),
      ])
      renderPage()

      let { dialog } = await openEdit('Edit Active')
      expect(
        within(dialog).queryByRole('button', { name: 'Mark as incomplete' }),
      ).not.toBeInTheDocument()
      fireEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))

      ;({ dialog } = await openEdit('Edit Finished'))
      expect(
        within(dialog).getByRole('button', { name: 'Mark as incomplete' }),
      ).toBeInTheDocument()
    })

    it('marks a completed item as incomplete from the dialog', async () => {
      const { calls } = fakeApi([todo({ id: 'b', text: 'Finished', completed: true })])
      renderPage()

      const { user, dialog } = await openEdit('Edit Finished')
      await user.click(within(dialog).getByRole('button', { name: 'Mark as incomplete' }))

      await screen.findByRole('checkbox', { name: 'Complete Finished' })
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(calls).toEqual(['completion:false'])
    })

    it('keeps the item when delete is cancelled', async () => {
      const { calls } = fakeApi([todo({ id: 'a', text: 'Alpha' })])
      renderPage()

      const { user, dialog } = await openEdit('Edit Alpha')
      await user.click(within(dialog).getByRole('button', { name: 'Delete' }))
      const confirm = await screen.findByRole('dialog', { name: 'Delete TODO' })
      await user.click(within(confirm).getByRole('button', { name: 'Cancel' }))

      expect(
        screen.queryByRole('dialog', { name: 'Delete TODO' }),
      ).not.toBeInTheDocument()
      expect(screen.getByRole('dialog', { name: 'Edit TODO' })).toBeInTheDocument()
      expect(calls).toEqual([])
    })

    it('deletes after confirmation and shows the empty state for the last item', async () => {
      const { calls } = fakeApi([todo({ id: 'a', text: 'Alpha' })])
      renderPage()

      const { user, dialog } = await openEdit('Edit Alpha')
      await user.click(within(dialog).getByRole('button', { name: 'Delete' }))
      const confirm = await screen.findByRole('dialog', { name: 'Delete TODO' })
      await user.click(within(confirm).getByRole('button', { name: 'Delete' }))

      expect(await screen.findByText('No tasks to do this time!')).toBeInTheDocument()
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(calls).toEqual(['delete'])
    })
  })
})
