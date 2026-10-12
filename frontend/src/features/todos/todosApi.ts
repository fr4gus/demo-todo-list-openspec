import { baseApi } from '../../store/baseApi'
import { compareTodos, type Priority, type Todo } from './priority'

export interface TodoInput {
  text: string
  priority: Priority
}

export const todosApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getTodos: build.query<Todo[], void>({
      query: () => '/todos',
      providesTags: ['Todos'],
    }),
    createTodo: build.mutation<Todo, TodoInput>({
      query: (body) => ({ url: '/todos', method: 'POST', body }),
      invalidatesTags: ['Todos'],
    }),
    updateTodo: build.mutation<Todo, TodoInput & { id: string }>({
      query: ({ id, ...body }) => ({ url: `/todos/${id}`, method: 'PUT', body }),
      invalidatesTags: ['Todos'],
    }),
    deleteTodo: build.mutation<void, string>({
      query: (id) => ({ url: `/todos/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Todos'],
    }),
    setCompletion: build.mutation<Todo, { id: string; completed: boolean }>({
      query: ({ id, completed }) => ({
        url: `/todos/${id}/completion`,
        method: 'PATCH',
        body: { completed },
      }),
      // Flip the flag and re-sort in the cache right away; roll back if the server refuses.
      async onQueryStarted({ id, completed }, { dispatch, queryFulfilled }) {
        const patch = dispatch(
          todosApi.util.updateQueryData('getTodos', undefined, (draft) => {
            const item = draft.find((t) => t.id === id)
            if (item) item.completed = completed
            draft.sort(compareTodos)
          }),
        )
        try {
          await queryFulfilled
        } catch {
          patch.undo()
        }
      },
      invalidatesTags: ['Todos'],
    }),
  }),
})

export const {
  useGetTodosQuery,
  useCreateTodoMutation,
  useUpdateTodoMutation,
  useDeleteTodoMutation,
  useSetCompletionMutation,
} = todosApi
