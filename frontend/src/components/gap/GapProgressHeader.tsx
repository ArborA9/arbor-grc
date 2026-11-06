import { type ReactNode } from 'react'
import { Box, LinearProgress, Stack, Typography } from '@mui/material'
import { brand, semantic } from '../../theme'
import type { GapProgressStats } from './gapUi'
import { formatGapScorePct } from './gapUi'
import { TYPE_CAPTION } from '../ui/FormFieldLabel'
import SettingsCard from '../ui/SettingsCard'

export interface GapProgressHeaderProps {
  frameworkTitle: string
  subtitle?: string
  stats: GapProgressStats
  actions?: ReactNode
  /** Bar label. UAE IA uses "UAE IA score"; ISO uses "Overall completion". */
  scoreLabel?: string
  /** When set, shows assessed / in-scope under the score bar. */
  showCoverage?: boolean
  /** Hide title/subtitle/actions when identity tiles already show them. */
  hideIdentity?: boolean
}

const STATS: {
  key: 'compliant' | 'partial' | 'planned' | 'nonCompliant' | 'notAssessed'
  label: string
  tone: keyof typeof semantic.gap
}[] = [
  { key: 'compliant', label: 'Compliant', tone: 'compliant' },
  { key: 'partial', label: 'Partial', tone: 'partial' },
  { key: 'planned', label: 'Planned', tone: 'planned' },
  { key: 'nonCompliant', label: 'Non-compliant', tone: 'nonCompliant' },
  { key: 'notAssessed', label: 'Not assessed', tone: 'notAssessed' },
]

function StatCell({ value, label, tone }: { value: number; label: string; tone: keyof typeof semantic.gap }) {
  const t = semantic.gap[tone]
  return (
    <Box
      sx={{
        textAlign: 'center',
        minWidth: 0,
        flex: '1 1 0',
        px: 0.5,
        py: 0.7,
        borderRadius: 0.75,
        bgcolor: t.bg,
      }}
    >
      <Typography sx={{ fontSize: '0.9375rem', fontWeight: 700, color: t.text, lineHeight: 1 }}>
        {value}
      </Typography>
      <Typography
        sx={{
          fontSize: TYPE_CAPTION,
          fontWeight: 650,
          color: t.text,
          mt: 0.3,
          lineHeight: 1.2,
        }}
      >
        {label}
      </Typography>
    </Box>
  )
}

function ScoreBody({
  stats,
  scoreLabel,
  showCoverage,
  actions,
}: {
  stats: GapProgressStats
  scoreLabel: string
  showCoverage: boolean
  actions?: ReactNode
}) {
  const barValue = Math.min(100, Math.max(0, stats.completionPct))
  return (
    <Stack spacing={1} sx={{ minWidth: 0 }}>
      {actions ? (
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          {actions}
        </Stack>
      ) : null}
      <LinearProgress
        variant="determinate"
        value={barValue}
        aria-label={scoreLabel}
        sx={{
          height: 10,
          borderRadius: 1,
          bgcolor: brand.goldLight,
          '& .MuiLinearProgress-bar': { bgcolor: brand.gold, borderRadius: 1 },
        }}
      />
      {showCoverage ? (
        <Typography sx={{ fontSize: TYPE_CAPTION, fontWeight: 650, color: brand.muted }}>
          Assessed {stats.assessedCount} of {stats.inScopeCount}
        </Typography>
      ) : null}
      <Stack direction="row" spacing={0.6} sx={{ width: '100%' }}>
        {STATS.map((s) => (
          <StatCell key={s.key} value={stats[s.key]} label={s.label} tone={s.tone} />
        ))}
      </Stack>
    </Stack>
  )
}

/** Kit `.progress-header`: framework summary, score bar, status counts. */
export default function GapProgressHeader({
  frameworkTitle,
  subtitle,
  stats,
  actions,
  scoreLabel = 'Overall completion',
  showCoverage = false,
  hideIdentity = false,
}: GapProgressHeaderProps) {
  const pct = `${formatGapScorePct(stats.completionPct)}%`
  const scoreAction = (
    <Typography sx={{ fontSize: '1.375rem', fontWeight: 700, color: brand.goldDark, lineHeight: 1, letterSpacing: '-0.02em' }}>
      {pct}
    </Typography>
  )

  if (hideIdentity) {
    return (
      <SettingsCard
        compact
        heading
        title={scoreLabel}
        subtitle={showCoverage ? `Assessed ${stats.assessedCount} of ${stats.inScopeCount}` : undefined}
        accentColor={brand.gold}
        action={scoreAction}
      >
        <ScoreBody stats={stats} scoreLabel={scoreLabel} showCoverage={false} actions={actions} />
      </SettingsCard>
    )
  }

  return (
    <SettingsCard
      compact
      heading
      title={frameworkTitle}
      subtitle={subtitle}
      accentColor={brand.gold}
      action={actions ?? scoreAction}
    >
      <ScoreBody
        stats={stats}
        scoreLabel={scoreLabel}
        showCoverage={showCoverage}
        actions={actions ? scoreAction : undefined}
      />
    </SettingsCard>
  )
}
