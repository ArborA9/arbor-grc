import { useEffect, useState } from 'react'

/** Remember register search / filter across nav (H-14). */
export function usePersistedState<T>(key: string, initial: T): [T, (next: T) => void] {
  const [value, setValue] = useState<T>(() => {
    try {
      const raw = sessionStorage.getItem(key)
      if (raw == null) return initial
      return JSON.parse(raw) as T
    } catch {
      return initial
    }
  })

  useEffect(() => {
    try {
      sessionStorage.setItem(key, JSON.stringify(value))
    } catch {
      /* ignore quota */
    }
  }, [key, value])

  return [value, setValue]
}
