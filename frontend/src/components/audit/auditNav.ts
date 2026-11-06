export type AuditModuleSection =
  | 'register'
  | 'checklists'
  | 'capa'
  | 'grants'
  | 'accessLog'
  | 'findings'

export const AUDIT_NAV_REGISTER = 'audit-register'
export const AUDIT_NAV_ENGAGEMENTS = 'audit-engagements'
export const AUDIT_NAV_CHECKLISTS = 'audit-checklists'
export const AUDIT_NAV_CAPA = 'audit-capa'
export const AUDIT_NAV_REGISTER2 = 'audit-register2'
export const AUDIT_NAV_REGISTER3 = 'audit-register3'
export const AUDIT_NAV_GRANTS = 'audit-grants'
export const AUDIT_NAV_ACCESS_LOG = 'audit-access-log'
export const AUDIT_NAV_FINDINGS = 'audit-findings'

const REGISTER_IDS = new Set([AUDIT_NAV_REGISTER, AUDIT_NAV_ENGAGEMENTS])
const CAPA_IDS = new Set([AUDIT_NAV_CAPA, AUDIT_NAV_REGISTER2, AUDIT_NAV_REGISTER3])

export function isAuditNavId(id: string): boolean {
  return (
    REGISTER_IDS.has(id) ||
    CAPA_IDS.has(id) ||
    id === AUDIT_NAV_CHECKLISTS ||
    id === AUDIT_NAV_GRANTS ||
    id === AUDIT_NAV_ACCESS_LOG ||
    id === AUDIT_NAV_FINDINGS
  )
}

export function auditNavIdToSection(navId: string): AuditModuleSection {
  if (navId === AUDIT_NAV_CHECKLISTS) return 'checklists'
  if (CAPA_IDS.has(navId)) return 'capa'
  if (navId === AUDIT_NAV_GRANTS) return 'grants'
  if (navId === AUDIT_NAV_ACCESS_LOG) return 'accessLog'
  if (navId === AUDIT_NAV_FINDINGS) return 'findings'
  return 'register'
}

export function auditSectionToNavId(section: AuditModuleSection): string {
  switch (section) {
    case 'checklists':
      return AUDIT_NAV_CHECKLISTS
    case 'capa':
      return AUDIT_NAV_CAPA
    case 'grants':
      return AUDIT_NAV_GRANTS
    case 'accessLog':
      return AUDIT_NAV_ACCESS_LOG
    case 'findings':
      return AUDIT_NAV_FINDINGS
    default:
      return AUDIT_NAV_REGISTER
  }
}

export function auditRegisterToNavId(_register: 'INTERNAL_AUDIT' | 'EXTERNAL_AUDIT'): string {
  return AUDIT_NAV_CAPA
}
