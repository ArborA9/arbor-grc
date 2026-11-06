/** Product names for role codes. BIA pack still says Champion in its own copy. */
const ROLE_LABELS: Record<string, string> = {
  OWNER: 'Owner',
  ADMIN: 'Admin',
  CONTRIBUTOR: 'Contributor',
  RISK_OWNER: 'Risk owner',
  INTERNAL_AUDITOR: 'Auditor',
  EXTERNAL_AUDITOR: 'External auditor',
  AUDIT_MANAGER: 'Audit manager',
  VIEWER: 'Viewer',
  GLOBAL_READER: 'Global reader',
}

export function roleLabel(code: string): string {
  return ROLE_LABELS[code] ?? code.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase())
}

export function roleLabels(codes: string[]): string {
  return codes.map(roleLabel).join(', ') || 'No roles'
}
