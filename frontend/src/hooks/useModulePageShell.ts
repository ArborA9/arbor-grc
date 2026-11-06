import { type ReactNode } from 'react'
import { useAppShellPage } from '../components/AppShellPageContext'

/** Standard kit topbar for a module list page (one bar, no duplicate PageHeader). */
export function useModuleListShell({
  title,
  breadcrumbTrail,
  actions,
}: {
  title: string
  breadcrumbTrail?: string
  actions?: ReactNode
}) {
  useAppShellPage(
    {
      title,
      breadcrumbTrail: breadcrumbTrail ?? title,
      hideContextLabel: true,
      hideRoleChips: true,
      actions,
    },
    [title, breadcrumbTrail, actions],
  )
}

/** Detail drill-in: breadcrumb root links back to the list (kit back-link pattern). */
export function useDetailShell({
  title,
  breadcrumbRootLabel,
  breadcrumbTrail,
  onBack,
  actions,
}: {
  title: string
  breadcrumbRootLabel: string
  breadcrumbTrail: string
  onBack: () => void
  actions?: ReactNode
}) {
  useAppShellPage(
    {
      title,
      breadcrumbTrail,
      breadcrumbRoot: { label: breadcrumbRootLabel, onClick: onBack },
      hideContextLabel: true,
      hideRoleChips: true,
      actions,
    },
    [title, breadcrumbRootLabel, breadcrumbTrail, onBack, actions],
  )
}

/** Renders nothing: use under a conditional to register list shell. */
export function ModuleListShell(props: Parameters<typeof useModuleListShell>[0]) {
  useModuleListShell(props)
  return null
}

/** Renders nothing: use under a conditional to register detail shell. */
export function ModuleDetailShell(props: Parameters<typeof useDetailShell>[0]) {
  useDetailShell(props)
  return null
}
