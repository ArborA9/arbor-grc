import { Stack, Tab, Tabs } from '@mui/material'
import { KpiCard, KpiCardGrid } from '../ui'
import { ENGAGEMENT_KPI_ACCENTS } from './auditKit'

export type AuditWorkTab = 'checklist' | 'findings' | 'summary'

export default function AuditEngagementChrome({
  testId,
  lines,
  reviewed,
  findings,
  daysRemaining,
  progressPct,
  workTab,
  onWorkTab,
  showFindingsTab,
}: {
  testId: string
  lines: number
  reviewed: number
  findings: number
  daysRemaining: number | null
  progressPct: number
  workTab: AuditWorkTab
  onWorkTab: (tab: AuditWorkTab) => void
  showFindingsTab: boolean
}) {
  return (
    <Stack spacing={1.25} data-testid={testId}>
      <KpiCardGrid columns={4} dense>
        <KpiCard
          compact
          label="Controls in scope"
          value={lines}
          accentColor={ENGAGEMENT_KPI_ACCENTS.neutral}
          onClick={() => onWorkTab('checklist')}
        />
        <KpiCard
          compact
          label="Reviewed"
          value={reviewed}
          accentColor={ENGAGEMENT_KPI_ACCENTS.success}
          sub={`${reviewed} of ${lines}`}
          onClick={() => onWorkTab('checklist')}
        />
        {showFindingsTab ? (
          <KpiCard
            compact
            label="Findings raised"
            value={findings}
            accentColor={ENGAGEMENT_KPI_ACCENTS.danger}
            onClick={() => onWorkTab('findings')}
          />
        ) : (
          <KpiCard
            compact
            label="Progress"
            value={`${progressPct}%`}
            accentColor={ENGAGEMENT_KPI_ACCENTS.accent}
          />
        )}
        <KpiCard
          compact
          label="Days remaining"
          value={daysRemaining == null ? '-' : daysRemaining}
          accentColor={ENGAGEMENT_KPI_ACCENTS.accent}
          sub={daysRemaining == null ? 'No end date' : undefined}
        />
      </KpiCardGrid>

      <Tabs
        value={workTab}
        onChange={(_, v: AuditWorkTab) => onWorkTab(v)}
        sx={{
          minHeight: 36,
          borderBottom: 1,
          borderColor: 'divider',
          '& .MuiTab-root': {
            textTransform: 'none',
            minHeight: 36,
            py: 0.5,
            px: 1.5,
            fontWeight: 600,
            fontSize: '0.8125rem',
          },
        }}
      >
        <Tab value="checklist" label="Checklist" />
        {showFindingsTab && (
          <Tab value="findings" label={findings > 0 ? `Findings (${findings})` : 'Findings'} />
        )}
        <Tab value="summary" label="Summary" />
      </Tabs>
    </Stack>
  )
}
