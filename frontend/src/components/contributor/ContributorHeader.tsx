import { Box, Button, Typography } from '@mui/material'
import type { Session } from '../../api'
import LogoMark from '../ui/LogoMark'
import { brand } from '../../theme'
import { greetingName, initials } from './contribUtils'
import { roleLabel } from '../../uiQuality/roleLabels'
import { ContributorRail } from './ContributorShell'

export default function ContributorHeader({
  session,
  onLogout,
  onReportIncident,
  backLabel,
  onBack,
  compact,
}: {
  session: Session
  onLogout: () => void
  onReportIncident?: () => void
  backLabel?: string
  onBack?: () => void
  /** Task detail: workspace pill + avatar only (mock). */
  compact?: boolean
}) {
  const ws = (session.tenantId ?? 'Workspace').toUpperCase()
  const displayName = greetingName(session.email)

  return (
    <Box
      sx={{
        bgcolor: brand.sidebar,
        borderBottom: `1px solid ${brand.border}`,
        flexShrink: 0,
      }}
    >
      <ContributorRail
        sx={{
          height: 54,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 2,
        }}
      >
        {backLabel && onBack ? (
          <Box
            component="button"
            type="button"
            onClick={onBack}
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 1,
              border: 0,
              p: 0,
              m: 0,
              font: 'inherit',
              cursor: 'pointer',
              color: brand.muted,
              fontSize: '0.6875rem',
              fontWeight: 500,
              bgcolor: 'transparent',
              '&:hover': { color: brand.goldDark },
            }}
          >
            ← {backLabel}
          </Box>
        ) : (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.125 }}>
            <LogoMark size={28} />
            <Box>
              <Typography sx={{ fontSize: '0.8125rem', fontWeight: 700, color: brand.ink, lineHeight: 1.2 }}>
                Arbor GRC
              </Typography>
            </Box>
          </Box>
        )}

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexShrink: 0 }}>
          <Typography
            sx={{
              fontSize: '0.6875rem',
              fontWeight: 600,
              color: brand.muted,
              px: 1.5,
              py: 0.5,
              borderRadius: 20,
              bgcolor: 'rgba(184, 134, 11, 0.1)',
            }}
          >
            {ws}
          </Typography>
          {!compact && onReportIncident && (
            <Button
              size="small"
              variant="outlined"
              onClick={onReportIncident}
              sx={{ display: { xs: 'none', sm: 'inline-flex' }, fontSize: '0.6875rem' }}
            >
              Report Incident
            </Button>
          )}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.875 }}>
            <Box
              sx={{
                width: 28,
                height: 28,
                borderRadius: '50%',
                bgcolor: brand.gold,
                color: '#fff',
                display: 'grid',
                placeItems: 'center',
                fontSize: '0.6875rem',
                fontWeight: 700,
              }}
            >
              {initials(session.email)}
            </Box>
            {!compact && (
              <Box sx={{ display: { xs: 'none', sm: 'block' }, minWidth: 0 }}>
                <Typography sx={{ fontSize: '0.6875rem', fontWeight: 600, color: brand.ink, lineHeight: 1.2 }}>
                  {displayName}
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.625, mt: 0.125 }}>
                  <Typography
                    component="span"
                    sx={{
                      fontSize: '0.5625rem',
                      fontWeight: 600,
                      px: 1,
                      py: 0.25,
                      borderRadius: 20,
                      bgcolor: brand.goldLight,
                      color: brand.goldDark,
                    }}
                  >
                    {roleLabel(session.roles.find((r) => r === 'CONTRIBUTOR') ?? session.roles[0] ?? 'CONTRIBUTOR')}
                  </Typography>
                </Box>
              </Box>
            )}
          </Box>
          {!compact && (
            <Button
              size="small"
              onClick={onLogout}
              sx={{ fontSize: '0.6875rem', color: brand.muted, minWidth: 0, px: 1 }}
            >
              Log Out
            </Button>
          )}
        </Box>
      </ContributorRail>
    </Box>
  )
}
