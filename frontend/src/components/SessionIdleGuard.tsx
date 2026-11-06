import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material'
import { api } from '../api'

const WARNING_SECONDS = 60
const ACTIVITY_THROTTLE_MS = 1000

/**
 * Path C B3: client-side idle logout using tenant session policy.
 * JWT may outlive idle; logout clears the SPA session (minimal approach).
 */
export default function SessionIdleGuard({
  onLogout,
  children,
}: {
  onLogout: () => void
  children: ReactNode
}) {
  const [idleMinutes, setIdleMinutes] = useState(5)
  const [warningOpen, setWarningOpen] = useState(false)
  const [secondsLeft, setSecondsLeft] = useState(WARNING_SECONDS)

  const lastActivityRef = useRef(Date.now())
  const lastTouchRef = useRef(0)
  const idleMinutesRef = useRef(5)
  const loggedOutRef = useRef(false)
  const onLogoutRef = useRef(onLogout)
  onLogoutRef.current = onLogout

  const bump = useCallback(() => {
    const now = Date.now()
    if (now - lastTouchRef.current < ACTIVITY_THROTTLE_MS) return
    lastTouchRef.current = now
    lastActivityRef.current = now
    setWarningOpen(false)
  }, [])

  useEffect(() => {
    let cancelled = false
    api
      .getSessionPolicy()
      .then((p) => {
        if (cancelled) return
        const m = Math.min(480, Math.max(1, p.idleTimeoutMinutes || 5))
        setIdleMinutes(m)
        idleMinutesRef.current = m
      })
      .catch(() => {
        /* keep default 5 */
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    const events: (keyof WindowEventMap)[] = [
      'mousemove',
      'mousedown',
      'keydown',
      'scroll',
      'touchstart',
      'click',
    ]
    for (const ev of events) {
      window.addEventListener(ev, bump, { passive: true })
    }
    return () => {
      for (const ev of events) {
        window.removeEventListener(ev, bump)
      }
    }
  }, [bump])

  useEffect(() => {
    const tick = window.setInterval(() => {
      if (loggedOutRef.current) return
      const idleMs = idleMinutesRef.current * 60_000
      const elapsed = Date.now() - lastActivityRef.current
      const remainingMs = idleMs - elapsed
      if (remainingMs <= 0) {
        loggedOutRef.current = true
        window.clearInterval(tick)
        onLogoutRef.current()
        return
      }
      if (remainingMs <= WARNING_SECONDS * 1000) {
        setWarningOpen(true)
        setSecondsLeft(Math.max(1, Math.ceil(remainingMs / 1000)))
      } else {
        setWarningOpen(false)
      }
    }, 1000)
    return () => window.clearInterval(tick)
  }, [])

  return (
    <>
      {children}
      <Dialog open={warningOpen} disableEscapeKeyDown>
        <DialogTitle>Still there?</DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            Your session will end in <strong>{secondsLeft}</strong> second
            {secondsLeft === 1 ? '' : 's'} due to inactivity
            {idleMinutes ? ` (idle limit ${idleMinutes} min)` : ''}.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button
            variant="contained"
            onClick={() => {
              bump()
              lastTouchRef.current = 0
              lastActivityRef.current = Date.now()
              setWarningOpen(false)
            }}
          >
            Stay Signed In
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}
