import DomainBadge, { type RiskLevel } from './DomainBadge'

/** Filled semantic risk chip (Critical / High / Medium / Low). */
export default function RiskLevelBadge({
  level,
  label,
  wrap,
}: {
  level: RiskLevel
  label?: string
  wrap?: boolean
}) {
  return <DomainBadge variant="risk" level={level} label={label} wrap={wrap} />
}
