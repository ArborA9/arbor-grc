import { Box, Typography } from '@mui/material'
import type { WorkTaskSummary } from '../../api'
import { brand, semantic } from '../../theme'
import { daysUntilDue, greetingName, greetingTime, isOverdue } from './contribUtils'
import { countActiveAssignments, nextDueAssignmentLabel } from './assignmentGrouping'
import { ContributorRail } from './ContributorShell'

function computeStats(tasks: WorkTaskSummary[]) {
  const active = tasks.filter((t) => t.status !== 'CANCELLED')
  const completed = active.filter((t) => t.status === 'SUBMITTED').length
  const open = active.filter((t) => t.status !== 'SUBMITTED')
  const overdue = open.filter((t) => isOverdue(t)).length
  const total = active.length
  const pct = total === 0 ? 0 : Math.round((completed / total) * 100)
  return { total, completed, inProgress: open.length, overdue, openCount: open.length, pct }
}

function nextDueTask(tasks: WorkTaskSummary[]): WorkTaskSummary | null {
  const open = tasks.filter((t) => t.status !== 'SUBMITTED' && t.dueDate)
  if (open.length === 0) return null
  return [...open].sort((a, b) => (a.dueDate ?? '').localeCompare(b.dueDate ?? ''))[0] ?? null
}

export default function ContributorSummaryBanner({
  email,
  tasks,
}: {
  email: string
  tasks: WorkTaskSummary[]
}) {
  const stats = computeStats(tasks)
  const next = nextDueTask(tasks)
  const activeAssignments = countActiveAssignments(tasks)
  const openCount = stats.openCount

  return (
    <Box
      sx={{
        bgcolor: brand.paper,
        borderBottom: `1px solid ${brand.border}`,
        py: 2.25,
        flexShrink: 0,
      }}
    >
      <ContributorRail>
        <Box
          sx={{
            display: 'flex',
            alignItems: 'flex-start',
            justifyContent: 'space-between',
            gap: 2,
            mb: 1.75,
            flexWrap: 'wrap',
          }}
        >
          <Box>
            <Typography sx={{ fontSize: '0.875rem', fontWeight: 700, color: brand.ink }}>
              {greetingTime()}, {greetingName(email).split(' ')[0]}
            </Typography>
            <Typography sx={{ fontSize: '0.6875rem', color: brand.muted, mt: 0.25 }}>
              You have {openCount} task{openCount === 1 ? '' : 's'} across {activeAssignments} active
              assignment{activeAssignments === 1 ? '' : 's'}
            </Typography>
          </Box>
          {next && (
            <Box
              sx={{
                fontSize: '0.65625rem',
                color: brand.muted,
                bgcolor: brand.hover,
                px: 1.5,
                py: 0.75,
                borderRadius: 1,
                border: `1px solid ${brand.border}`,
                flexShrink: 0,
              }}
            >
              Next due:{' '}
              <Box component="strong" sx={{ color: brand.goldDark, fontWeight: 700 }}>
                {nextDueAssignmentLabel(tasks) ?? next.title}
              </Box>
              {next.dueDate && (
                <>
                  {' '}
                  ·{' '}
                  {(() => {
                    const d = daysUntilDue(next.dueDate)
                    if (d == null) return null
                    if (d < 0) return `${Math.abs(d)} day${Math.abs(d) === 1 ? '' : 's'} overdue`
                    if (d === 0) return 'due today'
                    return `${d} day${d === 1 ? '' : 's'} remaining`
                  })()}
                </>
              )}
            </Box>
          )}
        </Box>

        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: 'repeat(2, 1fr)', sm: 'repeat(4, 1fr)' },
            gap: 1.25,
            mb: 1.75,
          }}
        >
          {[
            { val: stats.total, lbl: 'Total Assigned', tone: 'default' as const },
            { val: stats.completed, lbl: 'Completed', tone: 'done' as const },
            { val: stats.inProgress, lbl: 'In Progress', tone: 'gold' as const },
            { val: stats.overdue, lbl: 'Overdue', tone: 'over' as const },
          ].map((s) => (
            <Box
              key={s.lbl}
              sx={{
                bgcolor: brand.hover,
                border: `1px solid ${s.tone === 'over' ? '#FECACA' : brand.border}`,
                borderRadius: 1,
                py: 1.5,
                px: 1.75,
                textAlign: 'center',
              }}
            >
              <Typography
                sx={{
                  fontSize: '1.625rem',
                  fontWeight: 700,
                  lineHeight: 1,
                  color:
                    s.tone === 'done'
                      ? semantic.risk.low
                      : s.tone === 'over'
                        ? semantic.risk.critical
                        : s.tone === 'gold'
                          ? brand.gold
                          : brand.ink,
                }}
              >
                {s.val}
              </Typography>
              <Typography
                sx={{
                  fontSize: '0.59375rem',
                  color: brand.mutedLight,
                  mt: 0.375,
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em',
                }}
              >
                {s.lbl}
              </Typography>
            </Box>
          ))}
        </Box>

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <Typography sx={{ fontSize: '0.6875rem', color: brand.mutedLight, flexShrink: 0 }}>
            Overall completion
          </Typography>
          <Box
            sx={{
              flex: 1,
              height: 7,
              bgcolor: brand.border,
              borderRadius: 0.5,
              overflow: 'hidden',
            }}
          >
            <Box
              sx={{
                height: '100%',
                width: `${stats.pct}%`,
                bgcolor: brand.gold,
                borderRadius: 0.5,
                transition: 'width .2s ease',
              }}
            />
          </Box>
          <Typography sx={{ fontSize: '0.6875rem', fontWeight: 700, color: brand.goldDark, minWidth: 36 }}>
            {stats.pct}%
          </Typography>
        </Box>
      </ContributorRail>
    </Box>
  )
}
