import type { FrameworkNode } from '../api'

/** Locked Framework Import V2 §2.10: Super Admin catalog viewer profiles. */
export type FrameworkViewerProfile = 'ISO_PACK' | 'UAE_IA' | 'SAMA' | 'NIST_800_53'

export function resolveViewerProfile(frameworkCode: string | null | undefined): FrameworkViewerProfile {
  const c = (frameworkCode || '').toUpperCase().replace(/[\s-]+/g, '_')
  if (c.includes('UAE_IA') || c === 'UAE_IA') return 'UAE_IA'
  if (c.includes('SAMA')) return 'SAMA'
  if (c.includes('800_53') || c.includes('NIST_800')) return 'NIST_800_53'
  return 'ISO_PACK'
}

function attr(n: FrameworkNode, key: string): string | null {
  const v = n.attributes?.[key]
  return v && v.trim() ? v.trim() : null
}

/** Primary body label for the node's definition text (Excel “Description / Requirement / …” column). */
export function bodyFieldLabel(profile: FrameworkViewerProfile, frameworkCode: string | null | undefined): string {
  const c = (frameworkCode || '').toUpperCase()
  switch (profile) {
    case 'UAE_IA':
      return 'Description'
    case 'SAMA':
      return 'Control consideration'
    case 'NIST_800_53':
      return 'Control statement'
    case 'ISO_PACK':
    default:
      if (c.includes('NCEMA')) return 'Requirement'
      if (c.includes('PDPL') || c.includes('GDPR')) return 'Legal requirement'
      if (c.includes('CSF') || c.includes('NIST_CSF')) return 'Outcome'
      return 'Description'
  }
}

export function evidenceOf(n: FrameworkNode): string | null {
  return attr(n, 'evidence') || attr(n, 'evidence_examples')
}

export function whatItMeansOf(n: FrameworkNode): string | null {
  return attr(n, 'what_it_means')
}

/** NIST discussion/guidance when stored as attribute; otherwise unused. */
export function guidanceOf(n: FrameworkNode): string | null {
  return attr(n, 'discussion') || attr(n, 'guidance') || attr(n, 'discussion_guidance')
}

export function applicabilityOf(n: FrameworkNode): string | null {
  return attr(n, 'applicability') || attr(n, 'default_applicability')
}

export function statusPercentOf(n: FrameworkNode): string | null {
  return attr(n, 'status_percent') || attr(n, 'status_%')
}
