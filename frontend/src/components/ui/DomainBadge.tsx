import { Box } from '@mui/material'
import { brand, semantic } from '../../theme'

type BadgePair = { bg: string; text: string }

const gapLabels: Record<GapStatus, string> = {
  compliant: 'Compliant',
  partial: 'Partial',
  planned: 'Planned',
  nonCompliant: 'Non-compliant',
  notAssessed: 'Not assessed',
  na: 'N/A',
}

const soaLabels: Record<SoaStatus, string> = {
  yes: 'Yes',
  no: 'No',
  na: 'N/A',
}

const implLabels: Record<ImplStatus, string> = {
  full: 'Full',
  partial: 'Partial',
  planned: 'Planned',
  none: 'None',
}

const lifecycleLabels: Record<LifecycleStatus, string> = {
  draft: 'Draft',
  open: 'Open',
  closed: 'Closed',
  treat: 'In treatment',
}

const riskLabels: Record<RiskLevel, string> = {
  critical: 'Critical',
  high: 'High',
  medium: 'Medium',
  low: 'Low',
  veryLow: 'Very low',
}

export type GapStatus = keyof typeof semantic.gap
export type SoaStatus = keyof typeof semantic.soa
export type ImplStatus = keyof typeof semantic.impl
export type LifecycleStatus = keyof typeof semantic.lifecycle
export type WorkflowStatus = keyof typeof semantic.workflow
export type RiskLevel = keyof typeof semantic.risk

export type DomainBadgeProps =
  | { variant: 'gap'; status: GapStatus; label?: string; dot?: boolean }
  | { variant: 'soa'; status: SoaStatus; label?: string }
  | { variant: 'impl'; status: ImplStatus; label?: string }
  | { variant: 'lifecycle'; status: LifecycleStatus; label?: string; wrap?: boolean }
  | { variant: 'workflow'; status: WorkflowStatus; label?: string }
  | { variant: 'risk'; level: RiskLevel; label?: string; dot?: boolean; wrap?: boolean }
  | { variant: 'domain'; label: string }

function badgeStyle(pair: BadgePair) {
  return { bgcolor: pair.bg, color: pair.text }
}

function titleCaseLabel(s: string): string {
  return s
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase())
}

function resolveLabel(props: DomainBadgeProps): string {
  if (props.variant === 'gap') return props.label ?? gapLabels[props.status]
  if (props.variant === 'soa') return props.label ?? soaLabels[props.status]
  if (props.variant === 'impl') return props.label ?? implLabels[props.status]
  if (props.variant === 'lifecycle') return props.label ?? lifecycleLabels[props.status]
  if (props.variant === 'workflow') return props.label ?? props.status
  if (props.variant === 'risk') return titleCaseLabel(props.label ?? riskLabels[props.level])
  return props.label
}

function resolveColors(props: DomainBadgeProps): BadgePair {
  switch (props.variant) {
    case 'gap':
      return semantic.gap[props.status]
    case 'soa':
      return semantic.soa[props.status]
    case 'impl':
      return semantic.impl[props.status]
    case 'lifecycle':
      return semantic.lifecycle[props.status]
    case 'workflow':
      return semantic.workflow[props.status]
    case 'risk':
      return semantic.riskChip[props.level]
    case 'domain':
      return { bg: brand.goldLight, text: brand.goldDark }
    default:
      return { bg: brand.hover, text: brand.muted }
  }
}

function resolveDotColor(props: DomainBadgeProps): string | undefined {
  if (props.variant === 'risk') return semantic.risk[props.level]
  if (props.variant === 'gap' && props.dot) {
    const map: Record<GapStatus, string> = {
      compliant: semantic.risk.low,
      partial: semantic.risk.medium,
      planned: semantic.impl.planned.text,
      nonCompliant: semantic.risk.critical,
      notAssessed: brand.mutedLight,
      na: brand.muted,
    }
    return map[props.status]
  }
  return undefined
}

/** Semantic status badge aligned to Option A kit (gap, SoA, risk, lifecycle). */
export default function DomainBadge(props: DomainBadgeProps) {
  const label = resolveLabel(props)
  const colors = resolveColors(props)
  const dotColor = resolveDotColor(props)
  const showDot = props.variant === 'gap' && props.dot
  const uppercase = props.variant === 'lifecycle'
  const riskStyle = props.variant === 'risk'
  const riskBorder =
    props.variant === 'risk' ? `1px solid ${semantic.risk[props.level]}33` : undefined

  const wrapChip =
    (props.variant === 'risk' && props.wrap) || (props.variant === 'lifecycle' && props.wrap)

  return (
    <Box
      component="span"
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 0.5,
        fontSize: riskStyle ? '0.75rem' : '0.6875rem',
        fontWeight: riskStyle ? 700 : props.variant === 'lifecycle' ? 700 : 600,
        letterSpacing: props.variant === 'lifecycle' ? '0.04em' : riskStyle ? '0.01em' : 0,
        textTransform: uppercase ? 'uppercase' : 'none',
        px: 1.25,
        py: riskStyle ? 0.4 : 0.375,
        borderRadius: 20,
        lineHeight: 1.2,
        whiteSpace: wrapChip ? 'normal' : 'nowrap',
        maxWidth: '100%',
        overflow: wrapChip ? 'visible' : 'hidden',
        textOverflow: wrapChip ? undefined : 'ellipsis',
        boxSizing: 'border-box',
        border: riskBorder,
        ...badgeStyle(colors),
      }}
    >
      {showDot && dotColor && (
        <Box
          component="span"
          sx={{
            width: 6,
            height: 6,
            borderRadius: '50%',
            bgcolor: dotColor,
            flexShrink: 0,
          }}
        />
      )}
      {label}
    </Box>
  )
}
