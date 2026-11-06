import type { CapaRecord, CapaStatus } from '../../api'

/** Sheet1 CAPA Line Status, derived from stored workflow + assignment. */
export type CapaLineStatus =
  | 'Draft'
  | 'Assigned'
  | 'In Progress'
  | 'Submitted for verification'
  | 'Verified - Closed'
  | 'Reopened'
  | 'Withdrawn'

/** Sheet1 Verification Result. */
export const CAPA_VERIFICATION_RESULTS = [
  { value: 'Effective', label: 'Effective' },
  { value: 'Not effective', label: 'Not effective' },
] as const

export function capaLineStatus(c: Pick<CapaRecord, 'status' | 'ownerUserId' | 'rejectionReason'>): CapaLineStatus {
  if (c.status === 'CLOSED') return 'Verified - Closed'
  if (c.status === 'PENDING_VERIFICATION') return 'Submitted for verification'
  if (c.status === 'OVERDUE') return 'In Progress'
  if (c.rejectionReason) return 'Reopened'
  if (c.status === 'IN_PROGRESS') return 'In Progress'
  if (c.ownerUserId == null) return 'Draft'
  return 'Assigned'
}

export function capaLineStatusLabel(status: CapaStatus, c?: Pick<CapaRecord, 'ownerUserId' | 'rejectionReason'>): string {
  if (c) return capaLineStatus({ status, ownerUserId: c.ownerUserId, rejectionReason: c.rejectionReason })
  switch (status) {
    case 'OPEN':
      return 'Assigned'
    case 'IN_PROGRESS':
      return 'In Progress'
    case 'PENDING_VERIFICATION':
      return 'Submitted for verification'
    case 'OVERDUE':
      return 'Overdue'
    case 'CLOSED':
      return 'Verified - Closed'
  }
}

export function capaLineStatusTone(
  line: CapaLineStatus,
): 'success' | 'warning' | 'info' | 'danger' | 'neutral' {
  if (line === 'Verified - Closed') return 'success'
  if (line === 'Submitted for verification') return 'warning'
  if (line === 'Reopened') return 'danger'
  if (line === 'In Progress' || line === 'Assigned') return 'info'
  return 'neutral'
}

/** Compact register chip. Full words stay on the CAPA itself. */
export function capaLineStatusChipLabel(line: CapaLineStatus): string {
  if (line === 'Verified - Closed') return 'Closed'
  if (line === 'Submitted for verification') return 'Submitted'
  return line
}

export function verificationResultLabel(value: string | null | undefined): string {
  if (!value) return '-'
  const v = value.trim()
  if (v === 'PASS' || v.toUpperCase() === 'EFFECTIVE') return 'Effective'
  if (v === 'FAIL' || v.toUpperCase() === 'NOT_EFFECTIVE') return 'Not effective'
  if (v === 'PENDING' || v.toUpperCase() === 'NOT_YET_VERIFIED') return 'Not yet verified'
  return v
}

export function isVerificationEffective(value: string | null | undefined): boolean {
  if (!value) return false
  const v = value.trim().toUpperCase().replace(/_/g, ' ')
  return v === 'EFFECTIVE' || v === 'PASS'
}

export function classificationLabel(value: string | null | undefined): string {
  if (!value) return '-'
  if (value === 'MAJOR_NC') return 'Major NC'
  if (value === 'MINOR_NC') return 'Minor NC'
  if (value === 'OFI') return 'OFI'
  if (value === 'OBSERVATION') return 'Observation'
  return value.replace(/_/g, ' ')
}
