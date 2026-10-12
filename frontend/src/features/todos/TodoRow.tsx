import type { KeyboardEvent } from 'react'
import { useLongPress } from './hooks/useLongPress'
import { COMPLETED_CLASSES, PRIORITY_CLASSES, type Todo } from './priority'

interface Props {
  todo: Todo
  onToggle: (todo: Todo) => void
  onOpen: (todo: Todo) => void
  onRestoreRequest: (todo: Todo) => void
}

export function TodoRow({ todo, onToggle, onOpen, onRestoreRequest }: Props) {
  // Restore is the only long-press action, so it applies to completed rows only.
  const longPress = useLongPress(() => onRestoreRequest(todo), todo.completed)

  const onKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    // Keys pressed on the checkbox belong to the checkbox.
    if (event.target !== event.currentTarget) return
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      onOpen(todo)
    }
  }

  return (
    <li>
      <div
        role="button"
        tabIndex={0}
        aria-label={`Edit ${todo.text}`}
        onClick={() => onOpen(todo)}
        onKeyDown={onKeyDown}
        {...longPress}
        className={`flex cursor-pointer items-center rounded-lg ${
          todo.completed ? COMPLETED_CLASSES : PRIORITY_CLASSES[todo.priority]
        }`}
      >
        <label
          className="flex cursor-pointer items-center p-4"
          onClick={(event) => event.stopPropagation()}
        >
          <input
            type="checkbox"
            checked={todo.completed}
            onChange={() => onToggle(todo)}
            aria-label={
              todo.completed ? `Mark ${todo.text} as incomplete` : `Complete ${todo.text}`
            }
            className="size-5"
          />
        </label>
        <span className="flex-1 py-4 pr-4 break-words">{todo.text}</span>
      </div>
    </li>
  )
}
