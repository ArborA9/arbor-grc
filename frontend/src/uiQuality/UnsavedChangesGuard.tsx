import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import DiscardChangesDialog from '../components/DiscardChangesDialog'
import { useBeforeUnload } from './useBeforeUnload'

export type UnsavedChangesGuardConfig = {
  /** True when the open form has edits that would be lost. */
  dirty: boolean
  title?: string
  message?: string
  /** Optional draft save before leaving (e.g. BIA process). */
  onSaveDraft?: () => void | Promise<void>
  saveDraftLabel?: string
}

type GuardContextValue = {
  /** Register the active form's dirty state. Pass null on unmount / when not editing. */
  setGuard: (config: UnsavedChangesGuardConfig | null) => void
  /**
   * Run `proceed` immediately if clean; otherwise open the leave dialog and run
   * `proceed` only after Save draft or Discard.
   */
  requestNavigation: (proceed: () => void) => void
}

const UnsavedChangesGuardContext = createContext<GuardContextValue | null>(null)

/**
 * App-shell guard for unsaved create/edit forms.
 * Intercept left-nav (and any caller of `requestNavigation`) so half-filled work is not lost.
 */
export function UnsavedChangesGuardProvider({ children }: { children: ReactNode }) {
  const [guard, setGuardState] = useState<UnsavedChangesGuardConfig | null>(null)
  const [leaveOpen, setLeaveOpen] = useState(false)
  const [saveDraftBusy, setSaveDraftBusy] = useState(false)
  const pendingRef = useRef<(() => void) | null>(null)
  const guardRef = useRef(guard)
  guardRef.current = guard

  const setGuard = useCallback((config: UnsavedChangesGuardConfig | null) => {
    setGuardState(config)
  }, [])

  const requestNavigation = useCallback((proceed: () => void) => {
    const g = guardRef.current
    if (!g?.dirty) {
      proceed()
      return
    }
    pendingRef.current = proceed
    setLeaveOpen(true)
  }, [])

  useBeforeUnload(Boolean(guard?.dirty))

  const value = useMemo(() => ({ setGuard, requestNavigation }), [setGuard, requestNavigation])

  const closeDialog = () => {
    setLeaveOpen(false)
    pendingRef.current = null
    setSaveDraftBusy(false)
  }

  const runPending = () => {
    const next = pendingRef.current
    pendingRef.current = null
    setLeaveOpen(false)
    setSaveDraftBusy(false)
    next?.()
  }

  return (
    <UnsavedChangesGuardContext.Provider value={value}>
      {children}
      <DiscardChangesDialog
        open={leaveOpen}
        title={guard?.title ?? 'Leave without saving?'}
        message={
          guard?.message
          ?? 'You have unsaved changes. Save a draft to keep your work, or discard to leave without saving.'
        }
        keepEditingLabel="Keep editing"
        saveDraftLabel={guard?.saveDraftLabel ?? 'Save Draft'}
        discardLabel="Discard Changes"
        saveDraftBusy={saveDraftBusy}
        onKeepEditing={closeDialog}
        onSaveDraft={
          guard?.onSaveDraft
            ? () => {
                void (async () => {
                  setSaveDraftBusy(true)
                  try {
                    await guard.onSaveDraft?.()
                    runPending()
                  } catch {
                    setSaveDraftBusy(false)
                  }
                })()
              }
            : undefined
        }
        onDiscard={runPending}
      />
    </UnsavedChangesGuardContext.Provider>
  )
}

export function useUnsavedChangesGuardApi() {
  const ctx = useContext(UnsavedChangesGuardContext)
  if (!ctx) {
    throw new Error('useUnsavedChangesGuardApi requires UnsavedChangesGuardProvider')
  }
  return ctx
}

/** Optional API when the page may render outside the shell (returns no-ops). */
export function useOptionalUnsavedChangesGuardApi(): GuardContextValue {
  const ctx = useContext(UnsavedChangesGuardContext)
  return useMemo(
    () =>
      ctx
      ?? {
        setGuard: () => {},
        requestNavigation: (proceed) => proceed(),
      },
    [ctx],
  )
}

export function useRegisterUnsavedChanges(config: UnsavedChangesGuardConfig | null) {
  const { setGuard } = useOptionalUnsavedChangesGuardApi()
  const onSaveDraftRef = useRef(config?.onSaveDraft)
  onSaveDraftRef.current = config?.onSaveDraft

  const dirty = config?.dirty ?? false
  const title = config?.title
  const message = config?.message
  const saveDraftLabel = config?.saveDraftLabel
  const hasSaveDraft = Boolean(config?.onSaveDraft)
  const active = config != null

  useEffect(() => {
    if (!active) {
      setGuard(null)
      return () => setGuard(null)
    }
    setGuard({
      dirty,
      title,
      message,
      saveDraftLabel,
      onSaveDraft: hasSaveDraft ? () => onSaveDraftRef.current?.() : undefined,
    })
    return () => setGuard(null)
  }, [setGuard, active, dirty, title, message, saveDraftLabel, hasSaveDraft])
}
