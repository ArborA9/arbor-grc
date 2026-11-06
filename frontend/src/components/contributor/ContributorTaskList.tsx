import { Box, Typography } from '@mui/material'
import type { WorkTaskSummary } from '../../api'
import { brand } from '../../theme'
import ContributorAssignmentCard from './ContributorAssignmentCard'
import { groupIntoAssignments, type ContributorTab } from './assignmentGrouping'
import { ContributorRail } from './ContributorShell'

export default function ContributorTaskList({
  tasks,
  tab,
  tabs,
  sortAsc,
  onToggleSort,
  onOpenAssignment,
}: {
  tasks: WorkTaskSummary[]
  tab?: ContributorTab
  /** When set, cards from each tab are listed together (My Tasks home). */
  tabs?: ContributorTab[]
  sortAsc: boolean
  onToggleSort: () => void
  onOpenAssignment: (assignmentId: string) => void
}) {
  const tabList = tabs?.length ? tabs : tab ? [tab] : []
  let assignments = tabList.flatMap((t) => groupIntoAssignments(tasks, t))
  if (!sortAsc) {
    assignments = [...assignments].reverse()
  }

  const multiCol = assignments.length > 1

  return (
    <ContributorRail sx={{ py: 2.5, flex: 1, display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.75 }}>
        <Typography sx={{ fontSize: '0.65625rem', color: brand.mutedLight }}>
          {assignments.length} assignment{assignments.length === 1 ? '' : 's'} · sorted by due date
        </Typography>
        <Box
          component="button"
          type="button"
          onClick={onToggleSort}
          sx={{
            border: 0,
            p: 0,
            m: 0,
            font: 'inherit',
            cursor: 'pointer',
            fontSize: '0.65625rem',
            color: brand.goldDark,
            fontWeight: 600,
            bgcolor: 'transparent',
          }}
        >
          Sort ↕
        </Box>
      </Box>

      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: {
            xs: '1fr',
            md: multiCol ? 'repeat(2, minmax(0, 1fr))' : '1fr',
            xl: multiCol && assignments.length >= 3 ? 'repeat(2, minmax(0, 1fr))' : undefined,
          },
          gap: 1.25,
          flex: assignments.length > 0 ? undefined : 1,
          alignContent: 'start',
        }}
      >
        {assignments.map((assignment) => (
          <ContributorAssignmentCard
            key={assignment.id}
            assignment={assignment}
            onOpen={() => onOpenAssignment(assignment.id)}
          />
        ))}
      </Box>

      {assignments.length === 0 && (
        <Box
          sx={{
            flex: 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            py: 6,
          }}
        >
          <Typography sx={{ fontSize: '0.75rem', color: brand.muted, textAlign: 'center' }}>
            No assignments in this category
          </Typography>
        </Box>
      )}

      <Typography
        sx={{
          textAlign: 'center',
          pt: 1.25,
          mt: 'auto',
          fontSize: '0.6875rem',
          color: brand.mutedLight,
          borderTop: `1px solid ${brand.borderLight}`,
        }}
      >
        Click any assignment to see your{' '}
        {tabList.length > 1
          ? 'assigned work'
          : tabList[0] === 'RISK'
            ? 'risks'
            : tabList[0] === 'ACTIVITY'
              ? 'activities'
              : 'controls'}{' '}
        and start working ·{' '}
        <Box component="span" sx={{ color: brand.goldDark, fontWeight: 600 }}>
          → opens task detail view
        </Box>
      </Typography>
    </ContributorRail>
  )
}
