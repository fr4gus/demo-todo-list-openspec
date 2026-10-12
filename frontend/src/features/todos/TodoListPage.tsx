import { useCallback, useState } from 'react'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { UndoToast } from '../../components/UndoToast'
import type { Todo } from './priority'
import { TodoFormDialog } from './TodoFormDialog'
import { TodoRow } from './TodoRow'
import { useGetTodosQuery, useSetCompletionMutation } from './todosApi'

/** `'new'` opens the create form; a todo opens the edit form for it. */
type FormTarget = 'new' | Todo | null

export function TodoListPage() {
  const { data: todos, isLoading, isError, refetch } = useGetTodosQuery()
  const [setCompletion] = useSetCompletionMutation()

  const [form, setForm] = useState<FormTarget>(null)
  const [restoring, setRestoring] = useState<Todo | null>(null)
  const [toastFor, setToastFor] = useState<string | null>(null)
  const [error, setError] = useState<string>()

  const dismissToast = useCallback(() => setToastFor(null), [])

  async function changeCompletion(id: string, completed: boolean) {
    setError(undefined)
    try {
      await setCompletion({ id, completed }).unwrap()
      return true
    } catch {
      setError('Could not update the task. Please try again.')
      setToastFor(null)
      return false
    }
  }

  async function onToggle(todo: Todo) {
    const completed = !todo.completed
    // The toast appears with the optimistic update; a failure clears it again.
    if (completed) setToastFor(todo.id)
    await changeCompletion(todo.id, completed)
  }

  async function onUndo() {
    const id = toastFor
    setToastFor(null)
    if (id) await changeCompletion(id, false)
  }

  async function onConfirmRestore() {
    const target = restoring
    setRestoring(null)
    if (target) await changeCompletion(target.id, false)
  }

  // Keep the edit form bound to the latest data for that item.
  const editing =
    form && form !== 'new' ? (todos?.find((t) => t.id === form.id) ?? form) : undefined

  let body
  if (isLoading) {
    body = <p role="status">Loading…</p>
  } else if (isError) {
    body = (
      <div role="alert" className="text-center">
        <p className="mb-2 text-red-600">Could not load your tasks.</p>
        <button
          type="button"
          onClick={() => void refetch()}
          className="rounded border border-gray-300 px-4 py-2"
        >
          Retry
        </button>
      </div>
    )
  } else if (!todos || todos.length === 0) {
    body = (
      <p className="py-24 text-center text-lg text-gray-600">No tasks to do this time!</p>
    )
  } else {
    body = (
      <ul className="flex flex-col gap-2">
        {todos.map((todo) => (
          <TodoRow
            key={todo.id}
            todo={todo}
            onToggle={(t) => void onToggle(t)}
            onOpen={setForm}
            onRestoreRequest={setRestoring}
          />
        ))}
      </ul>
    )
  }

  return (
    <main className="mx-auto max-w-xl p-6 pb-28">
      {error && (
        <p role="alert" className="mb-4 rounded bg-red-100 px-4 py-2 text-red-800">
          {error}
        </p>
      )}
      {body}

      <button
        type="button"
        onClick={() => setForm('new')}
        className="fixed right-6 bottom-6 rounded-full bg-blue-600 px-6 py-3 font-medium text-white shadow-lg"
      >
        Create TODO
      </button>

      <UndoToast
        toastKey={toastFor}
        onUndo={() => void onUndo()}
        onDismiss={dismissToast}
      />

      <TodoFormDialog open={form !== null} todo={editing} onClose={() => setForm(null)} />
      <ConfirmDialog
        open={restoring !== null}
        title="Mark as incomplete"
        message="Put this task back as incomplete?"
        confirmLabel="Mark as incomplete"
        onConfirm={() => void onConfirmRestore()}
        onCancel={() => setRestoring(null)}
      />
    </main>
  )
}
