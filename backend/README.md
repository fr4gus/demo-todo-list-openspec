# Backend

Spring Boot 3 / Java 21 API. Run the tests with `./mvnw test` (Docker is needed: PostgreSQL and Redis run in Testcontainers).

## TODO endpoints

All endpoints live under `/api/v1/todos`, require `Authorization: Bearer <token>` (from `POST /api/v1/auth/login`) and are
limited to the `USER` role. The owner is always the signed-in user: no user id appears in any path or body, and an unknown
body property such as `userId` is ignored.

| Method and path                         | Body                              | Success                   |
|-----------------------------------------|-----------------------------------|---------------------------|
| `GET /api/v1/todos`                     | none                              | `200` array of items      |
| `POST /api/v1/todos`                    | `{"text", "priority"}`            | `201` the new item        |
| `PUT /api/v1/todos/{id}`                | `{"text", "priority"}` (both required) | `200` the updated item |
| `PATCH /api/v1/todos/{id}/completion`   | `{"completed": true or false}`    | `200` the item            |
| `DELETE /api/v1/todos/{id}`             | none                              | `204` no content          |

### Item

```json
{"id":"b762d1e8-0c1a-48ad-af03-2266e31db5ca","text":"Buy milk","priority":"HIGH","completed":false,"createdAt":"2026-10-12T13:18:38.401929Z"}
```

- `id` is internal and not meant to be displayed.
- `text` is trimmed and must be 1 to 500 characters after trimming.
- `priority` is one of `HIGH`, `MEDIUM_HIGH`, `MEDIUM_LOW`, `LOW`.
- Completed items stay in the list with `completed: true`; `DELETE` is the only way to remove an item for good.

### Ordering

`GET /api/v1/todos` returns the caller's items ordered by:

1. not completed before completed,
2. priority `HIGH`, `MEDIUM_HIGH`, `MEDIUM_LOW`, `LOW`,
3. oldest creation time first.

Editing an item changes its position when its priority changes; completing it moves it after all active items;
restoring it returns it to its priority position. The list is cached in Redis per user (key `todos::<userId>`, 10 minute
TTL) and every create, edit, completion change and delete is visible in the next list response.

### Examples

Create (`201`):

```
POST /api/v1/todos
{"text":"  Buy milk  ","priority":"HIGH"}
```
```json
{"id":"…","text":"Buy milk","priority":"HIGH","completed":false,"createdAt":"…"}
```

Edit (`200`; the completed state is kept):

```
PUT /api/v1/todos/{id}
{"text":"Buy oat milk","priority":"LOW"}
```

Complete (`200`), then restore (`200`). Repeating the current state is allowed and changes nothing:

```
PATCH /api/v1/todos/{id}/completion
{"completed":true}
```
```
PATCH /api/v1/todos/{id}/completion
{"completed":false}
```

Delete (`204`, empty body):

```
DELETE /api/v1/todos/{id}
```

### Errors

Errors are RFC 7807 problem details (`application/problem+json`) with a machine-readable `code`.

| Status | When                                                                                       | `code`              |
|--------|--------------------------------------------------------------------------------------------|---------------------|
| `400`  | blank or over-long text, missing or unknown priority, missing `completed`, malformed id    | `VALIDATION_FAILED` for body problems |
| `401`  | no valid token                                                                             | `UNAUTHENTICATED`   |
| `403`  | an `ADMIN` token (administrators cannot use these endpoints)                               | `FORBIDDEN`         |
| `404`  | the id does not exist **or belongs to another user** (the two are indistinguishable)       | `TODO_NOT_FOUND`    |

Invalid body (`400`), here `POST /api/v1/todos` with `{"text":"","priority":"HIGH"}`:

```json
{"type":"https://todo.example.com/problems/validation-failed","title":"Bad Request","status":400,"detail":"Request validation failed.","instance":"/api/v1/todos","code":"VALIDATION_FAILED","errors":[{"message":"must not be blank","field":"text"}]}
```

An unknown priority such as `{"text":"x","priority":"URGENT"}` is reported the same way with `"field":"priority"` and
`"message":"has an invalid value"`.

Unknown or foreign id (`404`), here `DELETE /api/v1/todos/3f2b8c1e-5a47-4d8e-9a60-1b7c2d9e4f10`:

```json
{"type":"https://todo.example.com/problems/todo-not-found","title":"Not Found","status":404,"detail":"TODO item not found.","instance":"/api/v1/todos/3f2b8c1e-5a47-4d8e-9a60-1b7c2d9e4f10","code":"TODO_NOT_FOUND"}
```

Access to another user's item is logged as a warning containing the two user/item ids and never the item's text.

Every example above is executed by `TodoApiExamplesTest`.
