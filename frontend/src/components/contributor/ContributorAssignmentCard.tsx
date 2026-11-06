import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import ShieldOutlinedIcon from '@mui/icons-material/ShieldOutlined'
import { Box, Typography } from '@mui/material'
import { brand, semantic } from '../../theme'
import type { ContributorAssignment } from './assignmentGrouping'
import {
  assignmentDueLabel,
  assignmentProgress,
  assignmentVisualStatus,
} from './assignmentGrouping'
import { statusBadge } from './contribUtils'

function borderAccent(status: ReturnType<typeof assignmentVisualStatus>): string {
  if (status === 'RETURNED') return semantic.risk.medium
  if (status === 'OVERDUE') return semantic.risk.critical
  if (status === 'IN_PROGRESS') return brand.gold
  return brand.border
}

function moduleIcon(visual: ReturnType<typeof assignmentVisualStatus>) {
  const grey = visual === 'NOT_STARTED'
  return (
    <Box
      sx={{
        width: 32,
        height: 32,
        borderRadius: '7px',
        bgcolor: grey ? '#F3F4F6' : brand.goldLight,
        display: 'grid',
        placeItems: 'center',
        flexShrink: 0,
        mr: 1.25,
      }}
    >
      <ShieldOutlinedIcon sx={{ fontSize: 16, color: grey ? '#9CA3AF' : brand.goldDark }} />
    </Box>
  )
}

export default function ContributorAssignmentCard({
  assignment,
  onOpen,
}: {
  assignment: ContributorAssignment
  onOpen: () => void
}) {
  const visual = assignmentVisualStatus(assignment)
  const badge = statusBadge(visual)
  const due = assignmentDueLabel(assignment)
  const pct = assignmentProgress(assignment)
  const barColor = visual === 'OVERDUE' ? semantic.risk.critical : brand.gold
  const itemWord =
    assignment.tab === 'RISK' ? 'risk' : assignment.tab === 'ACTIVITY' ? 'activity' : 'control'
  const total = assignment.tasks.length
  const done = assignment.tasks.filter((t) => t.status === 'SUBMITTED').length

  return (
    <Box
      component="button"
      type="button"
      onClick={onOpen}
      sx={{
        display: 'block',
        width: '100%',
        textAlign: 'left',
        font: 'inherit',
        cursor: 'pointer',
        bgcolor: brand.paper,
        border: `1px solid ${brand.border}`,
        borderLeft: `3px solid ${borderAccent(visual)}`,
        borderRadius: 1,
        p: '16px 18px',
        mb: 0,
        boxShadow: brand.shadowCard,
        transition: 'box-shadow .15s ease',
        height: '100%',
        '&:hover': { boxShadow: '0 3px 12px rgba(0,0,0,0.1)' },
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', mb: 1.25 }}>
        <Box sx={{ display: 'flex', alignItems: 'flex-start', flex: 1, minWidth: 0 }}>
          {moduleIcon(visual)}
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 700, color: brand.ink }}>
              {assignment.title}
            </Typography>
            <Typography sx={{ fontSize: '0.6875rem', color: brand.muted, mt: 0.25 }}>
              {assignment.subtitle}
            </Typography>
          </Box>
        </Box>
        <Typography
          component="span"
          sx={{
            fontSize: '0.59375rem',
            fontWeight: 700,
            px: 1.25,
            py: 0.375,
            borderRadius: 20,
            bgcolor: badge.bg,
            color: badge.color,
            flexShrink: 0,
            ml: 1,
          }}
        >
          {badge.label}
        </Typography>
      </Box>

      {visual === 'RETURNED' && (
        <Typography sx={{ fontSize: '0.65625rem', color: semantic.risk.medium, mb: 1, fontWeight: 600 }}>
          Returned for review
          {assignment.tasks.find((t) => t.returnReason)?.returnReason
            ? `: ${assignment.tasks.find((t) => t.returnReason)?.returnReason}`
            : ''}
        </Typography>
      )}

      <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 2, rowGap: 0.75 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.625 }}>
          <AssignmentOutlinedIcon sx={{ fontSize: 12, color: brand.mutedLight }} />
          <Typography sx={{ fontSize: '0.65625rem', color: brand.muted }}>
            {total} {itemWord}{total === 1 ? '' : 's'} assigned
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.625 }}>
          <CheckCircleOutlineIcon sx={{ fontSize: 12, color: brand.mutedLight }} />
          <Typography sx={{ fontSize: '0.65625rem', color: brand.muted }}>
            {done} of {total} completed
          </Typography>
        </Box>
        <Typography
          sx={{
            fontSize: '0.6875rem',
            fontWeight: 600,
            color:
              due.tone === 'over'
                ? semantic.risk.critical
                : due.tone === 'soon'
                  ? '#CA8A04'
                  : semantic.risk.low,
          }}
        >
          {due.tone === 'over' ? '⚠ ' : due.tone === 'soon' ? '⏰ ' : due.tone === 'ok' && done < total ? '✓ ' : ''}
          {due.text}
        </Typography>
      </Box>

      <Box
        sx={{
          height: 4,
          bgcolor: brand.border,
          borderRadius: 0.25,
          overflow: 'hidden',
          mt: 1.25,
        }}
      >
        <Box sx={{ height: '100%', width: `${pct}%`, bgcolor: barColor, borderRadius: 0.25 }} />
      </Box>
    </Box>
  )
}
