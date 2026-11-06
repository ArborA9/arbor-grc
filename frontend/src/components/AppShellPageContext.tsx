import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type DependencyList,
  type ReactNode,
} from 'react'

/** Per-page AppShell topbar state (kit: one bar: title, breadcrumb back-link, tabs, CTAs). */
export type AppShellPageConfig = {
  title: string
  breadcrumbTrail?: string
  breadcrumbRoot?: { label: string; onClick: () => void }
  /** Page title click. Same job as the breadcrumb back link when the title is what people click. */
  onTitleClick?: () => void
  tabs?: ReactNode
  /** Status / dates / assignee in the topbar, left of page actions. */
  headerMeta?: ReactNode
  actions?: ReactNode
  hideRoleChips?: boolean
  hideContextLabel?: boolean
  /** Hide primary module sidebar (e.g. internal auditor engagement workspace). */
  hideSidebar?: boolean
  /** Remove main content padding for edge-to-edge workspace layouts. */
  flushContent?: boolean
}

type AppShellPageContextValue = {
  page: AppShellPageConfig | null
  setPage: (config: AppShellPageConfig | null) => void
}

const AppShellPageContext = createContext<AppShellPageContextValue | null>(null)

export function AppShellPageProvider({
  children,
  onPageChange,
}: {
  children: ReactNode
  onPageChange: (config: AppShellPageConfig | null) => void
}) {
  const [page, setPageState] = useState<AppShellPageConfig | null>(null)
  const onPageChangeRef = useRef(onPageChange)
  onPageChangeRef.current = onPageChange

  const setPage = useCallback((config: AppShellPageConfig | null) => {
    setPageState(config)
    onPageChangeRef.current(config)
  }, [])

  const value = useMemo(() => ({ page, setPage }), [page, setPage])

  return <AppShellPageContext.Provider value={value}>{children}</AppShellPageContext.Provider>
}

/**
 * Register this page's AppShell chrome. Clears on unmount.
 * Pass `null` when the page should fall back to Dashboard defaults.
 */
export function useAppShellPage(config: AppShellPageConfig | null, deps: DependencyList = []) {
  const ctx = useContext(AppShellPageContext)
  const setPage = ctx?.setPage
  useEffect(() => {
    if (!setPage) return undefined
    setPage(config)
    return () => setPage(null)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [setPage, ...deps])
}

export function useAppShellPageSetter() {
  const ctx = useContext(AppShellPageContext)
  if (!ctx) {
    throw new Error('useAppShellPageSetter requires AppShellPageProvider')
  }
  return ctx.setPage
}
