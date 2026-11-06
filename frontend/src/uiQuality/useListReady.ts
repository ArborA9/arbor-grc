import { useCallback, useState } from 'react'

/** First fetch vs later refresh: avoids empty flash (G-03 / G-05). */
export function useListReady() {
  const [listReady, setListReady] = useState(false)
  const markReady = useCallback(() => setListReady(true), [])
  return { listReady, markReady }
}
