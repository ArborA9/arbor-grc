import { Box, Stack, Typography } from '@mui/material'
import { MODULE_CODES, type ModuleLicense } from '../../api'
import { moduleShortLabel } from '../../moduleDisplay'
import { DomainBadge } from '../ui'
import { brand } from '../../theme'

/** Licensed modules only, in catalog order. Unlicensed modules are not shown. */
export default function ActiveModulesList({
  modules,
  layout = 'rows',
}: {
  modules: ModuleLicense[]
  /** rows: one line per module. compact: wrapping status chips for a summary rail. */
  layout?: 'rows' | 'compact'
}) {
  const byCode = new Map(modules.map((m) => [m.code, m]))
  const rows = MODULE_CODES.map((code) => byCode.get(code)).filter((m): m is ModuleLicense => Boolean(m?.enabled))

  if (rows.length === 0) {
    return (
      <Typography variant="body2" color="text.secondary">
        No modules licensed yet. Contact Arbor to enable one.
      </Typography>
    )
  }

  if (layout === 'compact') {
    return (
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.75 }}>
        {rows.map((m) => {
          const label = m.expired ? 'Expired' : m.status === 'EXPIRING' ? 'Expiring' : 'Active'
          return (
            <Box
              key={m.code}
              sx={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 0.75,
                px: 1,
                py: 0.4,
                borderRadius: 0.75,
                border: `1px solid ${brand.border}`,
                bgcolor: brand.hover,
              }}
            >
              <Typography sx={{ fontSize: '0.75rem', fontWeight: 650, color: brand.ink }}>
                {moduleShortLabel(m.code)}
              </Typography>
              <Typography sx={{ fontSize: '0.6875rem', color: brand.muted }}>{label}</Typography>
            </Box>
          )
        })}
      </Box>
    )
  }

  return (
    <Stack spacing={1}>
      {rows.map((m) => (
        <Box
          key={m.code}
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 1,
            px: 1.5,
            py: 1.1,
            bgcolor: brand.hover,
            border: `1px solid ${brand.border}`,
            borderRadius: 0.75,
          }}
        >
          <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: brand.ink }}>
            {moduleShortLabel(m.code)}
          </Typography>
          <DomainBadge
            variant="lifecycle"
            status={m.expired ? 'draft' : 'closed'}
            label={m.expired ? 'EXPIRED' : m.status === 'EXPIRING' ? 'EXPIRING' : 'ACTIVE'}
          />
        </Box>
      ))}
    </Stack>
  )
}
