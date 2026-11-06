import type { ElementType } from 'react'
import { Box, Stack, Tooltip, Typography } from '@mui/material'
import WorkspacePremiumRoundedIcon from '@mui/icons-material/WorkspacePremiumRounded'
import AdminPanelSettingsRoundedIcon from '@mui/icons-material/AdminPanelSettingsRounded'
import EditNoteRoundedIcon from '@mui/icons-material/EditNoteRounded'
import FactCheckRoundedIcon from '@mui/icons-material/FactCheckRounded'
import ShieldRoundedIcon from '@mui/icons-material/ShieldRounded'
import VisibilityRoundedIcon from '@mui/icons-material/VisibilityRounded'
import PersonRoundedIcon from '@mui/icons-material/PersonRounded'
import AppsRoundedIcon from '@mui/icons-material/AppsRounded'
import { brand } from '../../theme'
import { roleLabel } from '../../uiQuality/roleLabels'
import { moduleDescription, moduleShortLabel } from '../../moduleDisplay'

type RoleStyle = { icon: ElementType; fg: string; bg: string; border: string }

const AUDIT_STYLE: RoleStyle = { icon: FactCheckRoundedIcon, fg: '#6D28D9', bg: '#F5F3FF', border: '#DDD6FE' }
const READER_STYLE: RoleStyle = { icon: VisibilityRoundedIcon, fg: '#475569', bg: '#F8FAFC', border: '#E2E8F0' }

const ROLE_STYLES: Record<string, RoleStyle> = {
  OWNER: { icon: WorkspacePremiumRoundedIcon, fg: brand.goldDark, bg: '#FFFBEB', border: '#FDE68A' },
  ADMIN: { icon: AdminPanelSettingsRoundedIcon, fg: '#3730A3', bg: '#EEF2FF', border: '#C7D2FE' },
  CONTRIBUTOR: { icon: EditNoteRoundedIcon, fg: '#0F766E', bg: '#F0FDFA', border: '#99F6E4' },
  RISK_OWNER: { icon: ShieldRoundedIcon, fg: '#C2410C', bg: '#FFF7ED', border: '#FED7AA' },
  INTERNAL_AUDITOR: AUDIT_STYLE,
  EXTERNAL_AUDITOR: AUDIT_STYLE,
  AUDIT_MANAGER: AUDIT_STYLE,
  GLOBAL_READER: READER_STYLE,
  VIEWER: READER_STYLE,
}

const FALLBACK_STYLE: RoleStyle = { icon: PersonRoundedIcon, fg: brand.muted, bg: '#FAFAF9', border: '#E7E5E4' }

const ROLE_ORDER = [
  'OWNER',
  'ADMIN',
  'AUDIT_MANAGER',
  'INTERNAL_AUDITOR',
  'EXTERNAL_AUDITOR',
  'RISK_OWNER',
  'CONTRIBUTOR',
  'GLOBAL_READER',
  'VIEWER',
]

const MAX_INLINE_MODULES = 3

function RoleBadge({ role }: { role: string }) {
  const s = ROLE_STYLES[role] ?? FALLBACK_STYLE
  const Icon = s.icon
  return (
    <Box
      component="span"
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 0.5,
        height: 22,
        pl: 0.75,
        pr: 1,
        borderRadius: '6px',
        border: `1px solid ${s.border}`,
        bgcolor: s.bg,
        color: s.fg,
        fontSize: '0.75rem',
        fontWeight: 650,
        lineHeight: 1,
        whiteSpace: 'nowrap',
      }}
    >
      <Icon sx={{ fontSize: 14 }} />
      {roleLabel(role)}
    </Box>
  )
}

function ModuleScope({ modules, licensed }: { modules: string[]; licensed: string[] }) {
  const sorted = [...modules].sort((a, b) => moduleShortLabel(a).localeCompare(moduleShortLabel(b)))
  const all = licensed.length > 0 && licensed.every((m) => modules.includes(m))
  if (sorted.length === 0) {
    return (
      <Typography variant="caption" sx={{ color: 'warning.dark', display: 'block', mt: 0.4 }}>
        No modules assigned
      </Typography>
    )
  }
  const inline = sorted.slice(0, MAX_INLINE_MODULES).map(moduleShortLabel)
  const extra = sorted.length - inline.length
  const summary = all ? 'All modules' : inline.join(' · ')
  return (
    <Tooltip
      arrow
      placement="bottom-start"
      slotProps={{
        tooltip: {
          sx: {
            bgcolor: '#FFFFFF',
            color: brand.ink,
            border: `1px solid ${brand.border}`,
            borderRadius: '10px',
            boxShadow: '0 12px 28px rgba(23,23,23,0.12)',
            p: 1.25,
            maxWidth: 320,
          },
        },
        arrow: { sx: { color: '#FFFFFF', '&::before': { border: `1px solid ${brand.border}` } } },
      }}
      title={
        <Box>
          <Typography sx={{ fontSize: '0.6875rem', fontWeight: 650, letterSpacing: '0.06em', color: brand.muted, mb: 0.75 }}>
            CAN MANAGE {sorted.length} MODULE{sorted.length === 1 ? '' : 'S'}
          </Typography>
          <Stack spacing={0.6}>
            {sorted.map((code) => (
              <Stack key={code} direction="row" spacing={0.75} alignItems="flex-start">
                <Box
                  sx={{ width: 6, height: 6, borderRadius: '50%', bgcolor: '#6366F1', mt: '6px', flexShrink: 0 }}
                />
                <Box sx={{ minWidth: 0 }}>
                  <Typography sx={{ fontSize: '0.8125rem', fontWeight: 650, lineHeight: 1.35, color: brand.ink }}>
                    {moduleShortLabel(code)}
                  </Typography>
                  {moduleDescription(code) && moduleDescription(code) !== moduleShortLabel(code) && (
                    <Typography sx={{ fontSize: '0.75rem', lineHeight: 1.35, color: brand.mutedLight }}>
                      {moduleDescription(code)}
                    </Typography>
                  )}
                </Box>
              </Stack>
            ))}
          </Stack>
        </Box>
      }
    >
      <Stack
        direction="row"
        spacing={0.5}
        alignItems="center"
        tabIndex={0}
        aria-label={`Manages ${sorted.map(moduleShortLabel).join(', ')}`}
        sx={{ mt: 0.4, width: 'fit-content', maxWidth: '100%', cursor: 'default', color: 'text.secondary' }}
      >
        <AppsRoundedIcon sx={{ fontSize: 13, opacity: 0.7 }} />
        <Typography variant="caption" noWrap sx={{ color: 'inherit' }}>
          {summary}
        </Typography>
        {!all && extra > 0 && (
          <Box
            component="span"
            sx={{
              px: 0.6,
              borderRadius: '99px',
              bgcolor: '#F1F5F9',
              color: '#475569',
              fontSize: '0.6875rem',
              fontWeight: 650,
              lineHeight: '16px',
            }}
          >
            +{extra}
          </Box>
        )}
      </Stack>
    </Tooltip>
  )
}

/** Users & Access row: one badge per role, then what that access covers. */
export default function UserAccessCell({
  roles,
  adminModules,
  licensedModules,
}: {
  roles: string[]
  adminModules: string[]
  licensedModules: string[]
}) {
  const ordered = [...roles].sort((a, b) => {
    const ia = ROLE_ORDER.indexOf(a)
    const ib = ROLE_ORDER.indexOf(b)
    return (ia < 0 ? 99 : ia) - (ib < 0 ? 99 : ib)
  })
  const isOwner = roles.includes('OWNER')
  const scopedAdmin = roles.includes('ADMIN') && !isOwner
  return (
    <Box sx={{ minWidth: 0 }}>
      <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
        {ordered.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            -
          </Typography>
        ) : (
          ordered.map((r) => <RoleBadge key={r} role={r} />)
        )}
      </Stack>
      {isOwner && (
        <Stack direction="row" spacing={0.5} alignItems="center" sx={{ mt: 0.4, color: 'text.secondary' }}>
          <AppsRoundedIcon sx={{ fontSize: 13, opacity: 0.7 }} />
          <Typography variant="caption" sx={{ color: 'inherit' }}>
            Full workspace
          </Typography>
        </Stack>
      )}
      {scopedAdmin && <ModuleScope modules={adminModules} licensed={licensedModules} />}
    </Box>
  )
}
