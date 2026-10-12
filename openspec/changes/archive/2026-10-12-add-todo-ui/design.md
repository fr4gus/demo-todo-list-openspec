# Design

## Context

Uses the frontend foundation (store, base API slice, Tailwind, test harness) and the session and routing from `add-user-auth`. API contract is in `add-todo-items`. Behavior is in `specs/todo-list-screen/spec.md`.

## Goals / Non-Goals

**Goals:**
- Responsive-feeling list (optimistic completion) with a faithful implementation of the product's screen flow.
- One form component for create and edit.
- Accessible without adding visible controls beyond the spec.

**Non-Goals:**
- Client-side sorting logic beyond what is needed for optimistic updates; the server order is the source of truth.
- Animations beyond simple transitions, offline queueing, theming.

## Decisions

**Structure.** `features/todos/`: `todosApi.ts`, `TodoListPage.tsx`, `TodoRow.tsx`, `TodoFormDialog.tsx`, `ConfirmDialog.tsx`, `UndoToast.tsx`, `priority.ts` (labels, colors, rank), and `hooks/useLongPress.ts`. Shared dialog and toast primitives live in `components/`.

**RTK Query.** Endpoints: `getTodos` (provides tag `Todos`), `createTodo`, `updateTodo`, `deleteTodo` (each invalidates `Todos`), `setCompletion`. `setCompletion` uses `onQueryStarted` to patch the cached list optimistically (flip the flag and re-sort with the same comparator as the server: completed, priority rank, createdAt) and rolls back on error. Alternative: refetch only; rejected because the checkbox should feel instant. All hooks work with the Bearer token from the base API.

**Toast.** A single toast slice or local state in the page: it holds the id of the last completed item and a timer (5 s). Starting a new completion replaces the toast. Undo calls `setCompletion(id, false)`. The toast is an `aria-live="polite"` region.

**Long press.** `useLongPress` (pointer events, 500 ms, cancels on movement beyond a small threshold) applied to completed rows only, since restore is the only long-press action. It suppresses the click that would otherwise follow so the edit dialog does not also open. Keyboard and mouse users use the "Mark as incomplete" action in the edit dialog.

**Row interaction.** The row is a focusable element (`role="button"`, `tabIndex=0`, Enter/Space opens edit). The checkbox sits in its own padded hit area and stops click propagation, so toggling never opens the dialog.

**Priority colors.** Tailwind classes mapped in `priority.ts`: red-500, orange-500, green-500, blue-500 backgrounds with text colors chosen for readable contrast; completed rows use gray-200 background, gray-500 text and `line-through`. Contrast is checked against WCAG AA for the chosen pairs.

**Dialogs.** Built on the native `<dialog>` element (focus handling and Escape built in) wrapped in a small component, avoiding a UI library. Delete confirmation is a second small confirm dialog.

**Error display.** Mutation failures show an inline message in the dialog or a transient error toast; a 401 is handled globally by the auth change.

## Risks / Trade-offs

- [Optimistic re-sort could briefly disagree with the server order] → Same comparator, plus tag invalidation after each mutation to resync.
- [Long-press is unreliable on some devices and invisible to desktop users] → Redundant path through the edit dialog's "Mark as incomplete".
- [Whole-row tap target can cause accidental dialogs on touch] → Separate padded checkbox area and a click-suppression window after long press.
- [Native `<dialog>` styling and test support in jsdom are limited] → Polyfill the `showModal` methods in the test setup.
