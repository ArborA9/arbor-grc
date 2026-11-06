import type { ReactNode } from 'react'

/** AppShell header state for embedded SoA / Gap workspace (kit Screens 3–4). */
export type AssessmentShellHeaderMeta = {
  title: string
  breadcrumbTrail: string
  breadcrumbRoot?: { label: string; onClick: () => void }
  tabs?: ReactNode
}

/** @deprecated Use AssessmentShellHeaderMeta */
export type GapShellHeaderMeta = AssessmentShellHeaderMeta
