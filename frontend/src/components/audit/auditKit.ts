import type { SxProps, Theme } from '@mui/material'
import type { GapStatus, RiskLevel } from '../ui'
import { brand, semantic } from '../../theme'

/** Kit-aligned register / control table chrome (Option A). */
export const auditTableContainerSx: SxProps<Theme> = {
  flex: 1,
  border: `1px solid ${brand.border}`,
  borderRadius: 1.125,
  bgcolor: brand.paper,
  boxShadow: brand.shadowCard,
}

export const auditTableHeadCellSx: SxProps<Theme> = {
  fontSize: '0.6875rem',
  fontWeight: 700,
  letterSpacing: '0.08em',
  textTransform: 'uppercase',
  color: brand.muted,
  background: brand.sidebar,
  backgroundColor: brand.sidebar,
  bgcolor: brand.sidebar,
  borderBottom: `1.5px solid ${brand.border}`,
  '&.MuiTableCell-stickyHeader': {
    background: brand.sidebar,
    backgroundColor: brand.sidebar,
  },
}

export const auditControlRefSx: SxProps<Theme> = {
  fontWeight: 700,
  fontSize: '0.6875rem',
  color: brand.goldDark,
}

export function selfAssessmentGapStatus(value: string | null | undefined): GapStatus {
  switch (value) {
    case 'YES':
      return 'compliant'
    case 'PARTIAL':
      return 'partial'
    case 'NO':
      return 'nonCompliant'
    default:
      return 'notAssessed'
  }
}

export function findingSeverityRiskLevel(sev: string): RiskLevel {
  if (sev === 'MAJOR_NC') return 'critical'
  if (sev === 'MINOR_NC') return 'high'
  if (sev === 'OBSERVATION') return 'medium'
  return 'low'
}

export const CAPA_KPI_ACCENTS = {
  total: brand.muted,
  overdue: semantic.risk.critical,
  inProgress: '#CA8A04',
  closed: semantic.risk.low,
} as const

export const ENGAGEMENT_KPI_ACCENTS = {
  neutral: brand.muted,
  success: semantic.risk.low,
  danger: semantic.risk.critical,
  accent: brand.gold,
} as const
