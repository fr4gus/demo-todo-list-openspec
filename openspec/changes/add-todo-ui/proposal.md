# Proposal

## Why

Users need screens to see and manage their TODO list. The product spec defines these in detail: empty state, priority colors, completion with Undo, restore, and a create dialog; edit and delete were added during planning.

## What Changes

- Main screen at `/` for signed-in users: list in server order, empty state "No tasks to do this time!", and a floating "Create TODO" button.
- Each item shows a checkbox and its text on a background color by priority: high red, medium-high orange, medium-low green, low blue. Completed items are grayed out with a strikethrough and sit at the end.
- Completing an item shows a bottom toast "Task Completed - Undo"; Undo restores the item.
- Long-press on an item opens a confirmation dialog to put a completed item back as incomplete.
- Tapping an item (anywhere except the checkbox) opens the edit dialog: change text and priority, delete (with confirmation), and "Mark as incomplete" for completed items. Rows are keyboard-operable.
- The create dialog (same form as edit) opens from the floating button.
- RTK Query endpoints and cache tags for the todo API with optimistic completion toggling.

## Capabilities

### New Capabilities
- `todo-list-screen`: The main list screen: empty state, priority colors, ordering display, completion, toast/Undo, restore, create/edit/delete dialogs and keyboard access.

### Modified Capabilities
<!-- None: no specs exist yet. -->

## Impact

- Depends on `add-platform-scaffold` (frontend foundation), `add-user-auth` (session, `ProtectedRoute`, sign-out control) and `add-todo-items` (API).
- New frontend feature folder `features/todos/`.
- No backend changes.
- Out of scope: drag-and-drop reordering, search or filtering, offline use.
