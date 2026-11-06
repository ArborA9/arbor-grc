import { Tooltip, Typography } from '@mui/material'
import type { FrameworkRoutingCounts } from '../api'

const MODULES: { key: keyof FrameworkRoutingCounts; label: string }[] = [
  { key: 'soa', label: 'SoA' },
  { key: 'gap', label: 'Gap' },
  { key: 'audit', label: 'Audit' },
  { key: 'risk', label: 'Risk' },
]

/**
 * Where a framework's content can actually be used. Not every framework feeds every module: ISO
 * 22301's clauses are audit criteria with no SoA, ISO 31000 is an ERM reference: and an admin who
 * only sees a total control count has no way to know that before a module refuses to start.
 */
export default function FrameworkRoutingSummary({
  counts,
}: {
  counts?: FrameworkRoutingCounts | null
}) {
  if (!counts) return null

  const used = MODULES.filter((m) => counts[m.key] > 0)
  if (used.length === 0) {
    return (
      <Tooltip title="No node in this framework is flagged for SoA, Gap, Audit or Risk, so no module can seed from it. Check the import report.">
        <Typography variant="caption" color="warning.main" data-testid="framework-routing-summary">
          not routed to any module
        </Typography>
      </Tooltip>
    )
  }

  return (
    <Tooltip
      title={MODULES.map((m) => `${m.label}: ${counts[m.key]}`).join(' · ')}
      placement="top"
    >
      <Typography variant="caption" color="text.secondary" data-testid="framework-routing-summary">
        {used.map((m) => `${m.label} ${counts[m.key]}`).join(' · ')}
      </Typography>
    </Tooltip>
  )
}
