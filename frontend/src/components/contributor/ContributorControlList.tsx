import { useMemo } from 'react'
import {
  Card,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import type { WorkTaskSummary } from '../../api'
import { brand } from '../../theme'
import StatusChip from '../StatusChip'
import {
  KpiCard,
  KpiCardGrid,
  RegisterListFrame,
  compactTableCellSx,
  lastTableCellSx,
  primaryRegisterNameSx,
  primaryTableCellSx,
  registerTableSx,
} from '../ui'
import { formatTenantDate } from '../../uiQuality/tenantLocale'
import { usePagedRows } from '../../uiQuality/usePagedRows'
import type { ContributorAssignment } from './assignmentGrouping'
import { cardVisualStatus, dueLabel, statusBadge } from './contribUtils'

function visualTone(
  visual: ReturnType<typeof cardVisualStatus>,
): 'success' | 'warning' | 'info' | 'neutral' | 'danger' {
  if (visual === 'DONE') return 'success'
  if (visual === 'RETURNED' || visual === 'IN_PROGRESS') return visual === 'RETURNED' ? 'warning' : 'info'
  if (visual === 'OVERDUE') return 'danger'
  return 'neutral'
}

function workRank(task: WorkTaskSummary): number {
  const visual = cardVisualStatus(task)
  if (visual === 'RETURNED') return 0
  if (visual === 'OVERDUE') return 1
  if (visual === 'NOT_STARTED') return 2
  if (visual === 'IN_PROGRESS') return 3
  return 4
}

export default function ContributorControlList({
  assignment,
  onOpenTask,
}: {
  assignment: ContributorAssignment
  onOpenTask: (taskId: number) => void
}) {
  const noun =
    assignment.tab === 'RISK' ? 'risk' : assignment.tab === 'ACTIVITY' ? 'activity' : 'control'
  const nounPlural = noun === 'activity' ? 'activities' : `${noun}s`
  const sorted = useMemo(
    () =>
      [...assignment.tasks].sort((a, b) => {
        const rank = workRank(a) - workRank(b)
        if (rank !== 0) return rank
        return (a.dueDate ?? '9999').localeCompare(b.dueDate ?? '9999')
      }),
    [assignment.tasks],
  )
  const paged = usePagedRows(sorted)
  const toDo = assignment.tasks.filter((t) => t.status !== 'SUBMITTED').length
  const submitted = assignment.tasks.length - toDo
  const overdue = assignment.tasks.filter((t) => cardVisualStatus(t) === 'OVERDUE').length
  const returned = assignment.tasks.filter((t) => cardVisualStatus(t) === 'RETURNED').length

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%', p: { xs: 1.5, sm: 2 } }}>
      <KpiCardGrid columns={3} dense>
        <KpiCard
          label="To do"
          value={toDo}
          sub={`Of ${assignment.tasks.length} ${assignment.tasks.length === 1 ? noun : nounPlural}`}
          accentColor={toDo > 0 ? brand.gold : brand.gold}
          compact
        />
        <KpiCard
          label="Submitted"
          value={submitted}
          sub="Waiting on Admin"
          compact
        />
        <KpiCard
          label={returned > 0 ? 'Returned' : 'Overdue'}
          value={returned > 0 ? returned : overdue}
          sub={returned > 0 ? 'Admin sent back' : overdue > 0 ? 'Past due date' : 'None waiting'}
          accentColor={returned > 0 || overdue > 0 ? '#CA8A04' : '#16A34A'}
          compact
        />
      </KpiCardGrid>

      {assignment.assignerName && assignment.assignerName !== '-' ? (
        <Typography sx={{ fontSize: '0.75rem', color: brand.mutedLight, px: 0.25 }}>
          Assigned by {assignment.assignerName}. Open a row to answer questions and add evidence.
        </Typography>
      ) : (
        <Typography sx={{ fontSize: '0.75rem', color: brand.mutedLight, px: 0.25 }}>
          Open a row to answer questions and add evidence.
        </Typography>
      )}

      <Card
        sx={{
          flex: 1,
          minHeight: 0,
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: brand.shadowCard,
        }}
      >
        <RegisterListFrame
          ready
          count={paged.count}
          emptyMessage={`No ${nounPlural} assigned to you in this project.`}
          page={paged.page}
          rowsPerPage={paged.rowsPerPage}
          onPageChange={paged.setPage}
          onRowsPerPageChange={paged.setRowsPerPage}
        >
          <TableContainer sx={{ flex: 1 }}>
            <Table size="small" stickyHeader sx={registerTableSx}>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ ...primaryTableCellSx, pl: 1.5 }}>
                    {noun === 'control' ? 'Control' : noun === 'risk' ? 'Risk' : 'Activity'}
                  </TableCell>
                  <TableCell sx={compactTableCellSx}>Due</TableCell>
                  <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx }}>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((task) => {
                  const visual = cardVisualStatus(task)
                  const badge = statusBadge(visual)
                  const due = dueLabel(task)
                  return (
                    <TableRow
                      key={task.id}
                      hover
                      onClick={() => onOpenTask(task.id)}
                      sx={{ cursor: 'pointer' }}
                    >
                      <TableCell sx={{ ...primaryTableCellSx, pl: 1.5 }}>
                        <Typography noWrap sx={primaryRegisterNameSx}>
                          {task.title}
                        </Typography>
                        {visual === 'RETURNED' && (
                          <Typography sx={{ fontSize: '0.6875rem', color: brand.goldDark, mt: 0.25 }}>
                            Returned{task.returnReason ? `: ${task.returnReason}` : ' for more detail'}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell sx={compactTableCellSx}>
                        <Typography
                          variant="caption"
                          color={due.tone === 'over' ? 'error' : 'text.secondary'}
                          noWrap
                        >
                          {task.dueDate ? formatTenantDate(task.dueDate) : '-'}
                        </Typography>
                      </TableCell>
                      <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx }}>
                        <StatusChip label={badge.label} tone={visualTone(visual)} />
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </RegisterListFrame>
      </Card>
    </Stack>
  )
}
