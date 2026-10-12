export type Priority = 'HIGH' | 'MEDIUM_HIGH' | 'MEDIUM_LOW' | 'LOW'

export interface Todo {
  id: string
  text: string
  priority: Priority
  completed: boolean
  createdAt: string
}

/** Highest priority first; matches the order of the server's enum. */
export const PRIORITIES: Priority[] = ['HIGH', 'MEDIUM_HIGH', 'MEDIUM_LOW', 'LOW']

export const PRIORITY_LABELS: Record<Priority, string> = {
  HIGH: 'High',
  MEDIUM_HIGH: 'Medium-high',
  MEDIUM_LOW: 'Medium-low',
  LOW: 'Low',
}

/** Row background and text colors for active items (red, orange, green, blue). */
export const PRIORITY_CLASSES: Record<Priority, string> = {
  HIGH: 'bg-red-500 text-white',
  MEDIUM_HIGH: 'bg-orange-500 text-black',
  MEDIUM_LOW: 'bg-green-500 text-black',
  LOW: 'bg-blue-500 text-white',
}

export const COMPLETED_CLASSES = 'bg-gray-200 text-gray-500 line-through'

export function priorityRank(priority: Priority): number {
  return PRIORITIES.indexOf(priority)
}

/** Same rule as the server: active first, then priority, then oldest first. */
export function compareTodos(a: Todo, b: Todo): number {
  if (a.completed !== b.completed) return a.completed ? 1 : -1
  const byPriority = priorityRank(a.priority) - priorityRank(b.priority)
  if (byPriority !== 0) return byPriority
  if (a.createdAt !== b.createdAt) return a.createdAt < b.createdAt ? -1 : 1
  return a.id < b.id ? -1 : a.id > b.id ? 1 : 0
}
