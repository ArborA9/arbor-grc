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
import { Box, Card, Stack } from '@mui/material'
import { brand } from '../../theme'

export type FormSection = { id: string; label: string }

type FormSectionShellContextValue = {
  mode: 'scroll' | 'panels'
  activeSection: string
}

const FormSectionShellContext = createContext<FormSectionShellContextValue>({
  mode: 'scroll',
  activeSection: '',
})

/** Sections can hide themselves in panels mode when not the active tab. */
export function useFormSectionShell() {
  return useContext(FormSectionShellContext)
}

/**
 * Full-page multi-section form shell: section tab nav + scroll area.
 * - scroll (default): all sections stacked; tabs scroll to anchors.
 * - panels: only the active tab’s section is visible (enterprise review / focused work).
 */
export default function FormSectionShell({
  sections,
  children,
  fill = true,
  initialSectionId,
  mode = 'scroll',
  activeSectionId,
  onActiveSectionChange,
}: {
  sections: FormSection[]
  children: ReactNode
  /** False = stacked in a scrolling page (do not collapse to the leftover viewport). */
  fill?: boolean
  /** When set and present in sections, land on this tab instead of the first. */
  initialSectionId?: string
  mode?: 'scroll' | 'panels'
  /** Controlled active tab (panels). When set, overrides internal state. */
  activeSectionId?: string
  onActiveSectionChange?: (sectionId: string) => void
}) {
  const scrollRef = useRef<HTMLDivElement>(null)
  const sectionKey = `${mode}|${sections.map((s) => s.id).join('|')}`
  const resolvedInitial =
    initialSectionId && sections.some((s) => s.id === initialSectionId)
      ? initialSectionId
      : (sections[0]?.id ?? '')
  const [internalActive, setInternalActive] = useState(resolvedInitial)
  const controlled = activeSectionId != null
  const activeSection = controlled ? activeSectionId : internalActive

  const setActiveSection = useCallback((sectionId: string) => {
    if (!controlled) setInternalActive(sectionId)
    onActiveSectionChange?.(sectionId)
  }, [controlled, onActiveSectionChange])

  const scrollToSection = useCallback((sectionId: string, behavior: ScrollBehavior = 'smooth') => {
    setActiveSection(sectionId)
    if (mode === 'panels') {
      scrollRef.current?.scrollTo({ top: 0 })
      return
    }
    const el = document.getElementById(sectionId)
    const root = scrollRef.current
    if (el && root && fill) {
      const nextTop = root.scrollTop + el.getBoundingClientRect().top - root.getBoundingClientRect().top
      root.scrollTo({ top: Math.max(0, nextTop), behavior })
      return
    }
    if (el && !fill) {
      el.scrollIntoView({ behavior, block: 'start' })
    }
  }, [fill, mode, setActiveSection])

  useEffect(() => {
    if (controlled) return
    setInternalActive(resolvedInitial)
    // fill=false lives in a parent page scroller. scrollIntoView would pull
    // Identification to the top and hide chrome above the shell (Your action).
    if (!fill) {
      return
    }
    if (!resolvedInitial) {
      scrollRef.current?.scrollTo({ top: 0 })
      return
    }
    const t = window.setTimeout(() => scrollToSection(resolvedInitial, 'auto'), 0)
    return () => window.clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps -- sectionKey is the stable identity for sections
  }, [sectionKey, resolvedInitial, scrollToSection, controlled, fill])

  useEffect(() => {
    if (mode === 'panels' || controlled) return
    if (sections.length === 0) return
    // fill=false has no shell scroller: track against the viewport so the page
    // scroller drives which section tab is highlighted.
    const root = fill ? scrollRef.current : null
    if (fill && !root) return

    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((e) => e.isIntersecting)
          .sort((a, b) => b.intersectionRatio - a.intersectionRatio)
        if (visible[0]?.target.id) {
          setActiveSection(visible[0].target.id)
        }
      },
      { root, rootMargin: '-100px 0px -55% 0px', threshold: [0, 0.15, 0.5] },
    )

    for (const s of sections) {
      const el = document.getElementById(s.id)
      if (el) observer.observe(el)
    }
    return () => observer.disconnect()
    // eslint-disable-next-line react-hooks/exhaustive-deps -- sectionKey is the stable identity for sections
  }, [sectionKey, mode, controlled, setActiveSection])

  const ctx = useMemo(
    () => ({ mode, activeSection }),
    [mode, activeSection],
  )

  return (
    <FormSectionShellContext.Provider value={ctx}>
      <Card
        sx={{
          flex: fill ? 1 : 'none',
          flexShrink: fill ? undefined : 0,
          minHeight: fill ? 0 : 360,
          height: fill ? undefined : 'auto',
          display: 'flex',
          flexDirection: 'column',
          overflow: fill ? 'hidden' : 'visible',
          boxShadow: brand.shadowCard,
        }}
      >
        <Box
          sx={{
            flexShrink: 0,
            px: 2.5,
            py: 1,
            borderBottom: `1px solid ${brand.hairline}`,
            bgcolor: brand.accentMuted,
            overflowX: 'auto',
            // In page-scroll mode the tab bar stays pinned while sections scroll.
            ...(fill
              ? {}
              : { position: 'sticky', top: 0, zIndex: 6, boxShadow: '0 1px 4px rgba(28, 25, 23, 0.08)' }),
          }}
        >
          <Stack direction="row" spacing={0.75} sx={{ minWidth: 'max-content' }}>
            {sections.map((s) => (
              <Box
                key={s.id}
                component="button"
                type="button"
                onClick={() => scrollToSection(s.id)}
                sx={{
                  border: `1px solid ${activeSection === s.id ? brand.accent : brand.hairlineStrong}`,
                  bgcolor: activeSection === s.id ? brand.paper : '#fff',
                  color: activeSection === s.id ? brand.accentHover : brand.muted,
                  borderRadius: 1,
                  px: 1.25,
                  py: 0.45,
                  fontSize: '0.75rem',
                  fontWeight: 650,
                  cursor: 'pointer',
                  fontFamily: 'inherit',
                  whiteSpace: 'nowrap',
                }}
              >
                {s.label}
              </Box>
            ))}
          </Stack>
        </Box>

        <Box
          ref={scrollRef}
          sx={{
            flex: fill ? 1 : 'none',
            minHeight: fill ? 0 : undefined,
            overflow: fill ? 'auto' : 'visible',
            px: { xs: 1.25, sm: 1.75 },
            py: 1.5,
            bgcolor: brand.canvas,
          }}
        >
          {children}
        </Box>
      </Card>
    </FormSectionShellContext.Provider>
  )
}
