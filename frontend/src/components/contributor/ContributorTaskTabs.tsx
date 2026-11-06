import EventNoteOutlinedIcon from '@mui/icons-material/EventNoteOutlined'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import AssignmentIcon from '@mui/icons-material/Assignment'
import HowToRegOutlinedIcon from '@mui/icons-material/HowToRegOutlined'
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined'
import { Box, Typography } from '@mui/material'
import type { ContributorTab } from './contribUtils'
import { brand } from '../../theme'
import { ContributorRail } from './ContributorShell'

const TAB_META: Record<
  ContributorTab,
  { icon: typeof CheckCircleOutlineIcon; label: string }
> = {
  GAP: { icon: CheckCircleOutlineIcon, label: 'Gap Assessments' },
  RISK: { icon: WarningAmberOutlinedIcon, label: 'Risk Reviews' },
  RISK_ACCEPTANCE: { icon: HowToRegOutlinedIcon, label: 'Risk Acceptance' },
  AUDIT: { icon: AssignmentIcon, label: 'Audit' },
  ACTIVITY: { icon: EventNoteOutlinedIcon, label: 'Activity' },
}

export default function ContributorTaskTabs({
  active,
  counts,
  onChange,
}: {
  active: ContributorTab
  counts: Record<ContributorTab, number>
  onChange: (tab: ContributorTab) => void
}) {
  return (
    <Box
      sx={{
        borderBottom: `1px solid ${brand.border}`,
        bgcolor: brand.paper,
      }}
    >
      <ContributorRail sx={{ display: 'flex' }}>
      {(Object.keys(TAB_META) as ContributorTab[]).map((tab) => {
        const { icon: Icon, label } = TAB_META[tab]
        const selected = active === tab
        const count = counts[tab]
        return (
          <Box
            key={tab}
            component="button"
            type="button"
            onClick={() => onChange(tab)}
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 0.875,
              px: 2,
              py: 1.5,
              border: 0,
              borderBottom: `2px solid ${selected ? brand.gold : 'transparent'}`,
              mb: '-1px',
              bgcolor: 'transparent',
              font: 'inherit',
              cursor: 'pointer',
              color: selected ? brand.goldDark : brand.muted,
              fontWeight: selected ? 600 : 500,
              fontSize: '0.75rem',
            }}
          >
            <Icon sx={{ fontSize: 13, opacity: selected ? 1 : 0.75 }} />
            {label}
            <Typography
              component="span"
              sx={{
                fontSize: '0.59375rem',
                fontWeight: 700,
                px: 0.875,
                py: 0.25,
                borderRadius: 20,
                bgcolor: selected ? brand.gold : count > 0 ? brand.goldLight : '#F3F4F6',
                color: selected ? '#fff' : count > 0 ? brand.goldDark : '#6B7280',
              }}
            >
              {count}
            </Typography>
          </Box>
        )
      })}
      </ContributorRail>
    </Box>
  )
}
