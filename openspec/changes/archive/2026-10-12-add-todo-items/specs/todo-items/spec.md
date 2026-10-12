# Spec Delta

## Purpose

Defines how signed-in users create and manage their own TODO items, how the list is ordered, and the guarantee that one user's items are never visible to or changeable by anyone else.

## ADDED Requirements

### Requirement: Item shape
Each item SHALL have an internal id (never meant for display), text of 1 to 500 characters after trimming, a priority (`HIGH`, `MEDIUM_HIGH`, `MEDIUM_LOW`, `LOW`), a completed flag, and creation and update times.

#### Scenario: Create an item
- **WHEN** a `USER` calls `POST /api/v1/todos` with valid text and priority
- **THEN** the system responds `201` with the new item, not completed, owned by that user

#### Scenario: Invalid item
- **WHEN** the text is blank or over 500 characters, or the priority is not one of the four values
- **THEN** the system responds `400` with the invalid fields listed and creates nothing

### Requirement: List own items in display order
`GET /api/v1/todos` SHALL return only the caller's items, ordered by: not completed before completed, then priority `HIGH`, `MEDIUM_HIGH`, `MEDIUM_LOW`, `LOW`, then oldest creation time first.

#### Scenario: Mixed list
- **WHEN** a user has an old low-priority active item, a new high-priority active item and a completed high-priority item
- **THEN** the response lists the high active item, then the low active item, then the completed item

#### Scenario: Same priority
- **WHEN** two active items share a priority
- **THEN** the one created earlier comes first

#### Scenario: Empty list
- **WHEN** the user has no items
- **THEN** the system responds `200` with an empty list

### Requirement: Edit an item
`PUT /api/v1/todos/{id}` SHALL change an item's text and priority, leaving its completed state and owner unchanged.

#### Scenario: Successful edit
- **WHEN** the owner submits new valid text and priority
- **THEN** the system responds `200` with the updated item and the next list shows it in its new position

#### Scenario: Edit a completed item
- **WHEN** the owner edits a completed item
- **THEN** the item stays completed with the new text and priority

### Requirement: Complete and restore
`PATCH /api/v1/todos/{id}/completion` with `{"completed": true|false}` SHALL mark the item completed or active again. Completed items SHALL remain in the list.

#### Scenario: Complete
- **WHEN** the owner sets `completed` to true
- **THEN** the item is returned as completed and appears after all active items in the list

#### Scenario: Undo or restore
- **WHEN** the owner sets `completed` to false on a completed item
- **THEN** the item is active again and returns to its priority position in the list

#### Scenario: Repeating a state
- **WHEN** the owner sets the state the item is already in
- **THEN** the system responds `200` and nothing else changes

### Requirement: Delete an item
`DELETE /api/v1/todos/{id}` SHALL permanently remove an item, whether active or completed.

#### Scenario: Successful delete
- **WHEN** the owner deletes an item
- **THEN** the system responds `204` and the item no longer appears in any list or count

### Requirement: Items are private to their owner
Every item operation SHALL act only on the caller's own items. The caller's identity SHALL come from the token, never from a request parameter. Administrators SHALL NOT be able to use these endpoints.

#### Scenario: Another user's item
- **WHEN** a user edits, completes or deletes an item id owned by someone else
- **THEN** the system responds `404` and changes nothing

#### Scenario: Lists are separate
- **WHEN** two users each create items
- **THEN** each user's list contains only their own items

#### Scenario: Admin access
- **WHEN** an `ADMIN` calls any `/api/v1/todos` endpoint
- **THEN** the system responds `403`

#### Scenario: Not signed in
- **WHEN** a request has no valid token
- **THEN** the system responds `401`

### Requirement: Cached list consistency
The system SHALL cache each user's list and SHALL make every create, edit, completion change and delete visible in the next list response.

#### Scenario: Read after write
- **WHEN** a user changes an item and then fetches the list
- **THEN** the response reflects the change

#### Scenario: Caches are per user
- **WHEN** one user's list is cached and another user changes their own items
- **THEN** the first user's cached list is unaffected
