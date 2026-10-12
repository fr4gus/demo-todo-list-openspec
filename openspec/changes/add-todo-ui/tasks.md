# Tasks

## 1. Data layer

- [ ] 1.1 Add `priority.ts` (labels, colors, rank) and the shared comparator; verify unit tests for rank order and for the comparator matching the server rule (completed last, priority, oldest first)
- [ ] 1.2 Add `todosApi` endpoints with tag invalidation and the optimistic `setCompletion` with rollback; verify MSW tests for the optimistic update, rollback on 500 and refetch after create/update/delete

## 2. Shared UI primitives

- [ ] 2.1 Build the dialog wrapper on `<dialog>` and `ConfirmDialog`, with a jsdom polyfill in test setup; verify RTL tests for open, Escape close and focus return
- [ ] 2.2 Build `UndoToast` with timer and `aria-live`; verify tests with fake timers for the 5-second dismissal and replacement by a newer toast
- [ ] 2.3 Build `useLongPress`; verify tests for firing after 500 ms, cancelling on early release or movement, and suppressing the follow-up click

## 3. List screen

- [ ] 3.1 Build `TodoListPage` with loading, error and empty states and the floating "Create TODO" button; verify RTL + MSW tests for "No tasks to do this time!" and for a populated list in server order
- [ ] 3.2 Build `TodoRow` with priority colors, completed styling, separate checkbox hit area and keyboard activation; verify tests for each color class, completed styling, no id in the DOM text, and Enter/Space opening edit
- [ ] 3.3 Wire completion with the toast and Undo; verify a test: check item, toast text "Task Completed - Undo" appears, Undo restores the item, and a failed request reverts it
- [ ] 3.4 Wire long-press restore with its confirmation; verify tests for confirm and cancel, and that long-press does not also open the edit dialog

## 4. Create, edit and delete

- [ ] 4.1 Build `TodoFormDialog` used for create and edit with validation of blank text; verify tests for create success, blank text, cancel and pre-filled edit values
- [ ] 4.2 Add Delete with confirmation and "Mark as incomplete" (completed items only) to the edit dialog; verify tests for delete confirm and cancel, last-item delete showing the empty state, and the action's visibility by state
- [ ] 4.3 Run `npm run lint` and `npm run test:coverage`; verify both pass with the 80% threshold

## 5. Integration

- [ ] 5.1 Exercise the full screen against the Compose stack (register, create items of each priority, complete, undo, long-press restore, edit, delete); verify each step manually and note the result, including colors and keyboard use
