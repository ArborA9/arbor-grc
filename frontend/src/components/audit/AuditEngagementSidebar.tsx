import { Box, List, ListItemButton, ListItemIcon, ListItemText, Typography } from '@mui/material'
import AssignmentRoundedIcon from '@mui/icons-material/AssignmentRounded'
import DashboardRoundedIcon from '@mui/icons-material/DashboardRounded'
import FactCheckRoundedIcon from '@mui/icons-material/FactCheckRounded'
import FolderOpenRoundedIcon from '@mui/icons-material/FolderOpenRounded'
import ReportProblemRoundedIcon from '@mui/icons-material/ReportProblemRounded'
import TaskAltRoundedIcon from '@mui/icons-material/TaskAltRounded'
import type { GrcAuditDetail } from '../../api'
import { brand } from '../../theme'
import { auditFrameworkLabel, engagementDaysRemaining } from './auditEngagementUtils'

export type EngagementWorkspaceSection =
  | 'overview'
  | 'engagements'
  | 'controls'
  | 'evidence'
  | 'findings'
  | 'capa'

const FIELDWORK_NAV: { id: EngagementWorkspaceSection; label: string; icon: React.ReactNode }[] = [
  { id: 'engagements', label: 'My Engagements', icon: <AssignmentRoundedIcon fontSize="small" /> },
  { id: 'overview', label: 'Overview', icon: <DashboardRoundedIcon fontSize="small" /> },
  { id: 'controls', label: 'Checklists', icon: <FactCheckRoundedIcon fontSize="small" /> },
  { id: 'evidence', label: 'Evidence Review', icon: <FolderOpenRoundedIcon fontSize="small" /> },
  { id: 'findings', label: 'My Findings', icon: <ReportProblemRoundedIcon fontSize="small" /> },
  { id: 'capa', label: 'CAPA Register', icon: <TaskAltRoundedIcon fontSize="small" /> },
]

const ADMIN_NAV: { id: EngagementWorkspaceSection; label: string; icon: React.ReactNode }[] = [
  { id: 'overview', label: 'Overview', icon: <DashboardRoundedIcon fontSize="small" /> },
  { id: 'controls', label: 'Checklists', icon: <FactCheckRoundedIcon fontSize="small" /> },
  { id: 'evidence', label: 'Evidence Review', icon: <FolderOpenRoundedIcon fontSize="small" /> },
  { id: 'findings', label: 'My Findings', icon: <ReportProblemRoundedIcon fontSize="small" /> },
  { id: 'capa', label: 'CAPA Register', icon: <TaskAltRoundedIcon fontSize="small" /> },
]

export default function AuditEngagementSidebar({
  detail,
  section,
  onSectionChange,
  findingsCount,
  iaFieldwork = false,
  activeEngagementId,
}: {
  detail: GrcAuditDetail | null
  section: EngagementWorkspaceSection
  onSectionChange: (s: EngagementWorkspaceSection) => void
  findingsCount: number
  iaFieldwork?: boolean
  activeEngagementId?: number
}) {
  const nav = iaFieldwork ? FIELDWORK_NAV : ADMIN_NAV
  const days = detail ? engagementDaysRemaining(detail.audit.endDate) : null
  const urgent = days != null && days <= 14

  return (
    <Box
      component="nav"
      aria-label="Engagement workspace"
      data-testid="audit-engagement-sidebar"
      sx={{
        width: 200,
        flexShrink: 0,
        bgcolor: brand.sidebar,
        borderRight: `1px solid ${brand.border}`,
        py: 1.5,
        px: 1,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <Typography
        variant="caption"
        sx={{
          px: 1,
          py: 0.5,
          fontWeight: 700,
          letterSpacing: '0.1em',
          textTransform: 'uppercase',
          color: 'text.disabled',
          fontSize: 9,
        }}
      >
        Audit fieldwork
      </Typography>
      <List dense disablePadding sx={{ flex: 1 }}>
        {nav.map((item) => {
          const active = section === item.id
          let label = item.label
          if (item.id === 'findings' && findingsCount > 0) {
            label += ` (${findingsCount})`
          }
          return (
            <ListItemButton
              key={item.id}
              selected={active}
              onClick={() => onSectionChange(item.id)}
              data-testid={item.id === 'engagements' ? 'audit-nav-my-engagements' : undefined}
              sx={{
                borderRadius: 1,
                mb: 0.25,
                py: 0.75,
                pl: 1.25,
                position: 'relative',
                '&.Mui-selected': {
                  bgcolor: brand.sidebarSelected,
                  color: brand.goldDark,
                  '&::before': {
                    content: '""',
                    position: 'absolute',
                    left: 0,
                    top: 6,
                    bottom: 6,
                    width: 3,
                    borderRadius: '0 2px 2px 0',
                    bgcolor: brand.gold,
                  },
                },
              }}
            >
              <ListItemIcon sx={{ minWidth: 28, color: 'inherit', opacity: 0.75 }}>
                {item.icon}
              </ListItemIcon>
              <ListItemText
                primary={label}
                primaryTypographyProps={{ fontSize: 12, fontWeight: active ? 600 : 500 }}
              />
            </ListItemButton>
          )
        })}
      </List>
      {detail && (
        <Box
          sx={{
            mt: 1.5,
            mx: 0.5,
            p: 1.25,
            borderRadius: 1,
            bgcolor: brand.sidebarSelected,
            border: `1px solid ${brand.goldMid}`,
          }}
        >
          <Typography variant="caption" fontWeight={600} color={brand.goldDark} display="block">
            {auditFrameworkLabel(detail.audit)}
          </Typography>
          <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.25 }}>
            {detail.audit.name}
          </Typography>
          <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.25 }}>
            {detail.audit.auditType === 'INTERNAL' ? 'Internal audit' : 'External audit'}
          </Typography>
          {detail.audit.endDate && (
            <Typography
              variant="caption"
              display="block"
              sx={{ mt: 0.25, color: urgent ? 'error.main' : 'text.secondary' }}
            >
              Ends {detail.audit.endDate}
              {days != null ? ` · ${days}d left` : ''}
            </Typography>
          )}
        </Box>
      )}
      {!detail && activeEngagementId == null && (
        <Box sx={{ mt: 1.5, mx: 0.5, p: 1.25, borderRadius: 1, bgcolor: brand.sidebarSelected }}>
          <Typography variant="caption" color="text.secondary">
            Select an engagement from My engagements
          </Typography>
        </Box>
      )}
    </Box>
  )
}
