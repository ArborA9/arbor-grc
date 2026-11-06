/** Staff auditor (INTERNAL_AUDITOR code kept; product label is Auditor). Not Owner/Admin. */
export function isInternalAuditorFieldwork(roles: string[]): boolean {
  return (
    roles.includes('INTERNAL_AUDITOR') &&
    !roles.some((r) => r === 'OWNER' || r === 'ADMIN')
  )
}

/** Platform-login EXTERNAL_AUDITOR leftover (OTP portal is separate). */
export function isExternalAuditorOnly(roles: string[]): boolean {
  return (
    roles.includes('EXTERNAL_AUDITOR') &&
    !roles.some((r) => ['OWNER', 'ADMIN', 'INTERNAL_AUDITOR', 'GLOBAL_READER'].includes(r))
  )
}

/** Auditor console: Admin chrome, grant-scoped view, audit fieldwork write. */
export function isAuditorConsole(roles: string[]): boolean {
  return isInternalAuditorFieldwork(roles) || isExternalAuditorOnly(roles)
}

export function defaultAuditHomeNav(roles: string[]): string | null {
  if (isAuditorConsole(roles)) return 'audit-register'
  return null
}

export function auditorNavIds(granted: string[]): Set<string> {
  const m = new Set(granted)
  const ids = new Set([
    'audit',
    'audit-register',
    'audit-engagements',
    'audit-capa',
    'audit-register2',
    'audit-register3',
  ])
  if (m.has('DASHBOARD')) ids.add('overview')
  if (m.has('GAP')) ids.add('assessments')
  if (m.has('SOA')) ids.add('soa')
  if (m.has('RISK') || m.has('ERM')) {
    ids.add('risk-manager')
  }
  if (m.has('ASSET')) ids.add('assets')
  if (m.has('RISK')) {
    ids.add('is-risk-nav')
    ids.add('risk')
    ids.add('risk-acceptance')
  }
  if (m.has('ERM')) {
    ids.add('erm-risk-nav')
    ids.add('erm-risk')
    ids.add('erm-risk-acceptance')
    ids.add('tprm')
    ids.add('tprm-projects')
    ids.add('tprm-vendors')
    ids.add('tprm-questionnaires')
    ids.add('kri')
    ids.add('incidents')
  }
  if (m.has('EVIDENCE')) ids.add('evidence')
  if (m.has('BCMS')) {
    ids.add('business-continuity')
    ids.add('bcms-bia')
    ids.add('bcms-plans')
  }
  if (m.has('PRIVACY')) {
    ids.add('privacy')
    ids.add('privacy-inventory')
    ids.add('privacy-ropa')
    ids.add('privacy-dsr')
    ids.add('privacy-dpia')
    ids.add('privacy-consent')
    ids.add('privacy-breach')
  }
  return ids
}

export function filterNavForAuditor<T extends { id: string; children?: T[] }>(
  items: T[],
  granted: string[],
): T[] {
  const allow = auditorNavIds(granted)
  return items
    .map((item) => {
      if (!allow.has(item.id)) return null
      if (!item.children?.length) return item
      const children = filterNavForAuditor(item.children, granted)
      if (!children.length) return null
      return { ...item, children }
    })
    .filter((item): item is T => item != null)
}
