import { useState, type FormEvent } from 'react'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { Dialog } from '../../components/Dialog'
import { TextField } from '../../components/TextField'
import { getProblem } from '../auth/problem'
import { PRIORITIES, PRIORITY_LABELS, type Priority, type Todo } from './priority'
import {
  useCreateTodoMutation,
  useDeleteTodoMutation,
  useSetCompletionMutation,
  useUpdateTodoMutation,
} from './todosApi'

interface Props {
  open: boolean
  /** The item being edited; absent when creating. */
  todo?: Todo
  onClose: () => void
}

const DEFAULT_PRIORITY: Priority = 'MEDIUM_LOW'

export function TodoFormDialog({ open, todo, onClose }: Props) {
  return (
    <Dialog open={open} onClose={onClose} title={todo ? 'Edit TODO' : 'Create TODO'}>
      <TodoForm todo={todo} onClose={onClose} />
    </Dialog>
  )
}

function TodoForm({ todo, onClose }: { todo?: Todo; onClose: () => void }) {
  const [text, setText] = useState(todo?.text ?? '')
  const [priority, setPriority] = useState<Priority>(todo?.priority ?? DEFAULT_PRIORITY)
  const [validation, setValidation] = useState<string>()
  const [failure, setFailure] = useState<string>()
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  const [createTodo, { isLoading: creating }] = useCreateTodoMutation()
  const [updateTodo, { isLoading: updating }] = useUpdateTodoMutation()
  const [deleteTodo] = useDeleteTodoMutation()
  const [setCompletion] = useSetCompletionMutation()

  async function run(action: () => Promise<unknown>) {
    setFailure(undefined)
    try {
      await action()
      onClose()
    } catch (error) {
      setFailure(getProblem(error)?.detail ?? 'Something went wrong. Please try again.')
    }
  }

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!text.trim()) {
      setValidation('Text is required')
      return
    }
    setValidation(undefined)
    const input = { text: text.trim(), priority }
    void run(() =>
      todo ? updateTodo({ id: todo.id, ...input }).unwrap() : createTodo(input).unwrap(),
    )
  }

  return (
    <>
      <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
        <TextField
          label="Text"
          value={text}
          onChange={(event) => setText(event.target.value)}
          maxLength={500}
          autoFocus
          aria-invalid={validation ? true : undefined}
          aria-describedby={validation ? 'todo-text-error' : undefined}
        />
        {validation && (
          <p id="todo-text-error" role="alert" className="text-sm text-red-600">
            {validation}
          </p>
        )}
        <div className="flex flex-col gap-1">
          <label htmlFor="todo-priority" className="text-sm font-medium">
            Priority
          </label>
          <select
            id="todo-priority"
            value={priority}
            onChange={(event) => setPriority(event.target.value as Priority)}
            className="rounded border border-gray-300 px-3 py-2"
          >
            {PRIORITIES.map((p) => (
              <option key={p} value={p}>
                {PRIORITY_LABELS[p]}
              </option>
            ))}
          </select>
        </div>
        {failure && (
          <p role="alert" className="text-sm text-red-600">
            {failure}
          </p>
        )}
        <div className="flex flex-wrap justify-end gap-2">
          {todo && (
            <button
              type="button"
              onClick={() => setConfirmingDelete(true)}
              className="mr-auto rounded border border-red-600 px-4 py-2 text-red-600"
            >
              Delete
            </button>
          )}
          {todo?.completed && (
            <button
              type="button"
              onClick={() =>
                void run(() => setCompletion({ id: todo.id, completed: false }).unwrap())
              }
              className="rounded border border-gray-300 px-4 py-2"
            >
              Mark as incomplete
            </button>
          )}
          <button
            type="button"
            onClick={onClose}
            className="rounded border border-gray-300 px-4 py-2"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={creating || updating}
            className="rounded bg-blue-600 px-4 py-2 font-medium text-white disabled:opacity-50"
          >
            Save
          </button>
        </div>
      </form>
      {todo && (
        <ConfirmDialog
          open={confirmingDelete}
          title="Delete TODO"
          message="Delete this TODO permanently?"
          confirmLabel="Delete"
          destructive
          onCancel={() => setConfirmingDelete(false)}
          onConfirm={() => {
            setConfirmingDelete(false)
            void run(() => deleteTodo(todo.id).unwrap())
          }}
        />
      )}
    </>
  )
}
