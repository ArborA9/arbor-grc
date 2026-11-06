import { useCallback, useEffect, useState, type RefObject } from 'react'

export type ScrollSpySection = { id: string; label?: string }

/** Tracks which section is visible inside a scroll container; scroll-to-section for tab nav. */
export function useSectionScrollSpy(
  sections: ScrollSpySection[],
  scrollRootRef: RefObject<HTMLElement | null>,
  resetKey?: string | number | null,
  options?: { rootMargin?: string },
) {
  const [activeSection, setActiveSection] = useState(sections[0]?.id ?? '')

  const scrollToSection = useCallback((sectionId: string) => {
    const el = document.getElementById(sectionId)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
      setActiveSection(sectionId)
    }
  }, [])

  useEffect(() => {
    setActiveSection(sections[0]?.id ?? '')
    scrollRootRef.current?.scrollTo({ top: 0 })
  }, [resetKey, sections, scrollRootRef])

  useEffect(() => {
    const root = scrollRootRef.current
    if (!root || sections.length === 0) return

    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((e) => e.isIntersecting)
          .sort((a, b) => b.intersectionRatio - a.intersectionRatio)
        if (visible[0]?.target.id) {
          setActiveSection(visible[0].target.id)
        }
      },
      {
        root,
        rootMargin: options?.rootMargin ?? '-120px 0px -55% 0px',
        threshold: [0, 0.15, 0.5],
      },
    )

    for (const s of sections) {
      const el = document.getElementById(s.id)
      if (el) observer.observe(el)
    }
    return () => observer.disconnect()
  }, [resetKey, sections, scrollRootRef, options?.rootMargin])

  return { activeSection, scrollToSection }
}
