import { describe, expect, it } from 'vitest'
import { compareTodos, priorityRank, PRIORITIES, type Todo } from './priority'

function todo(over: Partial<Todo>): Todo {
  return {
    id: 'a',
    text: 't',
    priority: 'HIGH',
    completed: false,
    createdAt: '2026-01-01T00:00:00Z',
    ...over,
  }
}

describe('priority', () => {
  it('ranks high to low', () => {
    expect(PRIORITIES.map(priorityRank)).toEqual([0, 1, 2, 3])
  })

  it('puts completed items last, then orders by priority, then oldest first', () => {
    const items = [
      todo({ id: 'done', completed: true, priority: 'HIGH' }),
      todo({ id: 'low', priority: 'LOW' }),
      todo({ id: 'new-high', priority: 'HIGH', createdAt: '2026-02-01T00:00:00Z' }),
      todo({ id: 'old-high', priority: 'HIGH' }),
      todo({ id: 'mh', priority: 'MEDIUM_HIGH' }),
    ]
    expect([...items].sort(compareTodos).map((t) => t.id)).toEqual([
      'old-high',
      'new-high',
      'mh',
      'low',
      'done',
    ])
  })

  it('breaks ties by id and treats identical items as equal', () => {
    expect(compareTodos(todo({ id: 'a' }), todo({ id: 'b' }))).toBeLessThan(0)
    expect(compareTodos(todo({ id: 'b' }), todo({ id: 'a' }))).toBeGreaterThan(0)
    expect(compareTodos(todo({}), todo({}))).toBe(0)
  })
})
