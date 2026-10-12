# Spec Delta

## Purpose

Defines what a signed-in user sees and can do on the main TODO screen: viewing their prioritized list, completing and restoring items, and creating, editing and deleting them.

## ADDED Requirements

### Requirement: Empty state
When the user has no items, the main screen SHALL show "No tasks to do this time!" centered, together with a floating button labeled "Create TODO".

#### Scenario: No items
- **WHEN** a user with no items opens the main screen
- **THEN** the message and the "Create TODO" button are shown and no list is rendered

### Requirement: Item list display
When the user has at least one item, the screen SHALL show the items in the order returned by the server, with the "Create TODO" floating button at the bottom. Each item SHALL show a checkbox and its text, and SHALL NOT show its id.

#### Scenario: Items shown in server order
- **WHEN** the server returns several items
- **THEN** they are rendered in that order with a checkbox and text each

#### Scenario: Id not visible
- **WHEN** an item is rendered
- **THEN** its internal id appears nowhere in the visible text

### Requirement: Priority colors
Active items SHALL use a background color by priority: red for high, orange for medium-high, green for medium-low and blue for low.

#### Scenario: Colors by priority
- **WHEN** items of each priority are shown
- **THEN** their backgrounds are red, orange, green and blue respectively

### Requirement: Completed item appearance
Completed items SHALL be grayed out with a strikethrough on their text and SHALL appear after all active items.

#### Scenario: Completed styling
- **WHEN** an item is completed
- **THEN** it is shown grayed with struck-through text instead of its priority color, after the active items

### Requirement: Complete with Undo toast
Clicking an item's checkbox SHALL complete it and show a toast at the bottom reading "Task Completed - Undo", where "Undo" is an actionable link.

#### Scenario: Complete
- **WHEN** the user checks an active item
- **THEN** the item immediately appears completed at the end of the list and the toast is shown

#### Scenario: Undo
- **WHEN** the user activates "Undo" in the toast
- **THEN** the item becomes active again and returns to its priority position

#### Scenario: Toast dismissal
- **WHEN** the user does nothing for about 5 seconds
- **THEN** the toast disappears and the item stays completed

#### Scenario: Server rejects the change
- **WHEN** the completion request fails
- **THEN** the item returns to its previous state and an error message is shown

### Requirement: Restore a completed item
A long-press on a completed item SHALL open a dialog asking to confirm putting it back as incomplete.

#### Scenario: Confirm restore
- **WHEN** the user long-presses a completed item and confirms
- **THEN** the item becomes active again

#### Scenario: Cancel restore
- **WHEN** the user cancels the dialog
- **THEN** the item stays completed

### Requirement: Create dialog
Activating "Create TODO" SHALL open a dialog where the user enters text and chooses a priority, and saves the new item.

#### Scenario: Create success
- **WHEN** the user enters text, picks a priority and saves
- **THEN** the dialog closes and the new item appears in its place in the list

#### Scenario: Empty text
- **WHEN** the user tries to save with blank text
- **THEN** the dialog stays open and shows a validation message

#### Scenario: Cancel
- **WHEN** the user closes the dialog without saving
- **THEN** no item is created

### Requirement: Edit dialog
Tapping an item anywhere except its checkbox SHALL open the same form pre-filled with the item's text and priority, with a Delete action, and, for a completed item, a "Mark as incomplete" action.

#### Scenario: Edit and save
- **WHEN** the user changes the text or priority and saves
- **THEN** the dialog closes and the list shows the updated item in its new position

#### Scenario: Checkbox does not open the dialog
- **WHEN** the user clicks the checkbox
- **THEN** the item is completed or restored and no dialog opens

#### Scenario: Mark as incomplete
- **WHEN** the user opens a completed item and chooses "Mark as incomplete"
- **THEN** the dialog closes and the item is active again

#### Scenario: Mark as incomplete hidden for active items
- **WHEN** the user opens an active item
- **THEN** the dialog does not offer "Mark as incomplete"

### Requirement: Delete from the dialog
Choosing Delete in the edit dialog SHALL ask for confirmation before permanently removing the item.

#### Scenario: Confirm delete
- **WHEN** the user chooses Delete and confirms
- **THEN** the item disappears from the list, and if it was the last one the empty state is shown

#### Scenario: Cancel delete
- **WHEN** the user chooses Delete and cancels the confirmation
- **THEN** the item remains

### Requirement: Keyboard and screen-reader access
Item rows SHALL be reachable by keyboard and open the edit dialog with Enter or Space, the checkbox SHALL be a separate focusable control with an accessible name, and dialogs SHALL trap focus and close with Escape.

#### Scenario: Keyboard edit
- **WHEN** a keyboard user focuses a row and presses Enter
- **THEN** the edit dialog opens

#### Scenario: Escape closes
- **WHEN** a dialog is open and the user presses Escape
- **THEN** the dialog closes without saving
