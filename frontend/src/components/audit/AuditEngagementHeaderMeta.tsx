import type { ReactNode } from 'react'
import { Box, Divider, Stack, Typography } from '@mui/material'
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined'
import PersonOutlineRoundedIcon from '@mui/icons-material/PersonOutlineRounded'
import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined'
import StatusChip from '../StatusChip'
import { TYPE_CAPTION, TYPE_CONTROL } from '../ui'
import { brand } from '../../theme'
import { auditStatusLabel, auditStatusTone } from './auditEngagementUtils'

function MetaFact({
  icon,
  label,
  value,
}: {
  icon: ReactNode
  label: string
  value: string
}) {
  return (
    <Stack direction="row" spacing={0.75} alignItems="center" sx={{ minWidth: 0, flexShrink: 1 }}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: 28,
          height: 28,
          borderRadius: 1,
          bgcolor: brand.hover,
          border: `1px solid ${brand.border}`,
          color: brand.muted,
          flexShrink: 0,
        }}
      >
        {icon}
      </Box>
      <Box sx={{ minWidth: 0 }}>
        <Typography
          sx={{
            fontSize: TYPE_CAPTION,
            fontWeight: 650,
            color: brand.muted,
            lineHeight: 1.2,
          }}
          noWrap
        >
          {label}
        </Typography>
        <Typography
          sx={{
            fontSize: TYPE_CONTROL,
            fontWeight: 600,
            color: brand.ink,
            lineHeight: 1.25,
          }}
          noWrap
          title={value}
        >
          {value}
        </Typography>
      </Box>
    </Stack>
  )
}

/** Compact engagement identity for AppShell headerMeta. */
export default function AuditEngagementHeaderMeta({
  status,
  findingCount,
  framework,
  period,
  assignee,
}: {
  status: string
  findingCount?: number
  framework: string
  period: string
  assignee: string
}) {
  return (
    <Stack
      direction="row"
      spacing={1.25}
      alignItems="center"
      sx={{
        minWidth: 0,
        width: '100%',
        py: 0.25,
        px: 1,
        borderRadius: 1.25,
        bgcolor: brand.hover,
        border: `1px solid ${brand.border}`,
      }}
    >
      <StatusChip label={auditStatusLabel(status, findingCount)} tone={auditStatusTone(status)} />
      <Divider orientation="vertical" flexItem sx={{ borderColor: brand.border, my: 0.35 }} />
      <Stack
        direction="row"
        spacing={1.5}
        alignItems="center"
        divider={<Divider orientation="vertical" flexItem sx={{ borderColor: brand.border, my: 0.35 }} />}
        sx={{ minWidth: 0, flex: 1, overflow: 'hidden' }}
      >
        <MetaFact
          icon={<AccountTreeOutlinedIcon sx={{ fontSize: 15 }} />}
          label="Framework"
          value={framework}
        />
        <MetaFact
          icon={<CalendarMonthOutlinedIcon sx={{ fontSize: 15 }} />}
          label="Period"
          value={period}
        />
        <MetaFact
          icon={<PersonOutlineRoundedIcon sx={{ fontSize: 15 }} />}
          label="Lead auditor"
          value={assignee}
        />
      </Stack>
    </Stack>
  )
}
