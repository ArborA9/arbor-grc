import { Box, Chip, Stack, Typography } from '@mui/material'
import AccountTreeRoundedIcon from '@mui/icons-material/AccountTreeRounded'
import CheckRoundedIcon from '@mui/icons-material/CheckRounded'
import GppGoodRoundedIcon from '@mui/icons-material/GppGoodRounded'
import LayersRoundedIcon from '@mui/icons-material/LayersRounded'
import { LogoMark } from '../ui'
import { brand } from '../../theme'
import LoginProductVisuals from './LoginProductVisuals'
import { loginChipSx, loginFadeIn, loginPillarIconSx, loginDecorSizes } from './loginStyles'

const FRAMEWORKS: { label: string; bg: string; border: string; color: string }[] = [
  { label: 'ISO 27001', bg: brand.goldLight, border: brand.goldMid, color: brand.goldDark },
  { label: 'UAE IA V2.1', bg: '#FFF7ED', border: '#FDBA74', color: '#9A3412' },
  { label: 'NCEMA V3', bg: '#FEF9C3', border: brand.goldMid, color: brand.goldDark },
  { label: 'ISO 22301', bg: '#ECFDF5', border: '#86EFAC', color: '#166534' },
  { label: 'ISO 31000', bg: '#F5F3FF', border: '#C4B5FD', color: '#5B21B6' },
]

const MODULE_TILES: { label: string; bg: string; color: string }[] = [
  { label: 'ISMS', bg: brand.gold, color: '#fff' },
  { label: 'ERM', bg: brand.goldDark, color: '#fff' },
  { label: 'BCMS', bg: '#CA8A04', color: '#fff' },
  { label: 'Audit', bg: brand.goldMid, color: brand.goldDark },
  { label: 'TPRM', bg: brand.goldLight, color: brand.goldDark },
]

const PILLARS = [
  {
    icon: <LayersRoundedIcon />,
    title: 'Single GRC engine',
    body: 'Maintain one authoritative control library and evidence store: so teams stop reconciling duplicate work across spreadsheets and siloed tools.',
  },
  {
    icon: <AccountTreeRoundedIcon />,
    title: 'Multi-framework compliance',
    body: 'Map controls once across ISO 27001, UAE IA, NCEMA and aligned frameworks: reducing repeat effort when regulatory requirements overlap.',
  },
  {
    icon: <GppGoodRoundedIcon />,
    title: 'Integrated risk & assurance',
    body: 'Connect risk registers, audit findings and assurance activities in one workflow: giving leadership a clearer view of organisational posture.',
  },
]

const TRUST_ITEMS = [
  'UAE Data Residency',
  'Microsoft Entra ID',
  'SAML 2.0 Ready',
  'Multi-Factor Authentication',
  'Role-Based Access Control',
]

export default function LoginMarketingPanel() {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        height: { xs: 'auto', md: '100%' },
        minHeight: 0,
        width: '100%',
        minWidth: 0,
        py: { xs: 2, md: 2.5, lg: 3.5 },
        pr: { md: 1, lg: 2 },
        position: 'relative',
        zIndex: 1,
        ...loginFadeIn,
      }}
    >
      <Stack direction="row" spacing={1.25} alignItems="center" sx={{ flexShrink: 0 }}>
        <LogoMark size={32} />
        <Box>
          <Typography sx={{ fontWeight: 700, fontSize: '1.1rem', letterSpacing: '-0.02em', color: brand.ink, lineHeight: 1.2 }}>
            Arbor GRC
          </Typography>
          <Typography
            sx={{
              fontSize: '0.65rem',
              color: brand.navMuted,
              letterSpacing: '0.06em',
              textTransform: 'uppercase',
              mt: '3px',
            }}
          >
            by A9 Advisory
          </Typography>
        </Box>
      </Stack>

      <Box
        sx={{
          flex: { xs: 'none', md: 1 },
          minHeight: 0,
          display: 'flex',
          flexDirection: { xs: 'column', lg: 'row' },
          alignItems: { lg: 'center' },
          gap: { lg: 3, xl: 4 },
          justifyContent: 'center',
          py: { xs: 2, md: 1.5, lg: 2 },
          width: '100%',
          position: 'relative',
        }}
      >
        <Box
          sx={{
            flex: '1 1 auto',
            minWidth: 0,
            maxWidth: { lg: 600, xl: 640 },
            position: 'relative',
            zIndex: 2,
          }}
        >
          <Typography
            variant="overline"
            sx={{ display: 'block', mb: 1.75, color: brand.goldDark, letterSpacing: '0.16em', fontWeight: 700 }}
          >
            Governance · Risk · Compliance
          </Typography>
          <Typography
            component="h1"
            sx={{
              fontWeight: 700,
              fontSize: { xs: '2rem', md: '2.5rem', lg: '2.85rem' },
              lineHeight: 1.08,
              letterSpacing: '-0.04em',
              mb: 1.5,
              color: brand.ink,
              maxWidth: { md: 620, lg: 680, xl: 720 },
            }}
          >
            One platform for every framework you licence.
          </Typography>
          <Typography
            sx={{
              color: brand.inkSoft,
              mb: 2.5,
              fontSize: { xs: '0.9375rem', md: '1rem' },
              maxWidth: { md: 600, lg: 620, xl: 640 },
              lineHeight: 1.55,
            }}
          >
            Centralize governance, risk and compliance in a single platform designed for highly regulated enterprises.
            Replace fragmented spreadsheets with automated controls, evidence management and audit-ready workflows.
          </Typography>

          <Stack spacing={1.1} sx={{ mb: { xs: 2.5, md: 3.25 } }}>
            <Stack direction="row" flexWrap="wrap" gap={0.85} role="list" aria-label="Supported frameworks">
              {FRAMEWORKS.map((fw) => (
                <Chip
                  key={fw.label}
                  label={fw.label}
                  size="small"
                  role="listitem"
                  sx={{
                    ...loginChipSx,
                    bgcolor: fw.bg,
                    border: `1px solid ${fw.border}`,
                    color: fw.color,
                    boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
                    '&:hover': {
                      ...loginChipSx['&:hover'],
                      borderColor: fw.border,
                    },
                  }}
                />
              ))}
            </Stack>
            <Stack direction="row" flexWrap="wrap" gap={0.85} role="list" aria-label="Licensed modules">
              {MODULE_TILES.map((m) => (
                <Box
                  key={m.label}
                  role="listitem"
                  sx={{
                    px: 1.25,
                    py: 0.5,
                    borderRadius: 0.75,
                    fontSize: '0.625rem',
                    fontWeight: 800,
                    letterSpacing: '0.06em',
                    textTransform: 'uppercase',
                    bgcolor: m.bg,
                    color: m.color,
                    boxShadow: '0 1px 4px rgba(146, 83, 10, 0.15)',
                    transition: 'transform 0.18s ease, box-shadow 0.18s ease',
                    '&:hover': {
                      transform: 'translateY(-1px)',
                      boxShadow: '0 4px 12px rgba(146, 83, 10, 0.2)',
                    },
                  }}
                >
                  {m.label}
                </Box>
              ))}
            </Stack>
          </Stack>

          <Stack
            spacing={2.25}
            sx={{
              display: { xs: 'none', sm: 'flex' },
              position: 'relative',
              zIndex: 1,
            }}
          >
            {PILLARS.map((p) => (
              <Stack key={p.title} direction="row" spacing={1.5} alignItems="flex-start">
                <Box sx={loginPillarIconSx} aria-hidden>
                  {p.icon}
                </Box>
                <Box>
                  <Typography component="h2" sx={{ fontWeight: 700, fontSize: '0.875rem', mb: 0.35, color: brand.ink }}>
                    {p.title}
                  </Typography>
                  <Typography sx={{ color: brand.muted, fontSize: '0.8125rem', lineHeight: 1.5 }}>
                    {p.body}
                  </Typography>
                </Box>
              </Stack>
            ))}
          </Stack>
        </Box>

        <Box
          sx={{
            display: { xs: 'none', lg: 'block' },
            flex: '0 0 auto',
            width: loginDecorSizes.large.width,
            minHeight: 508,
            position: 'relative',
            zIndex: 0,
            alignSelf: 'center',
          }}
        >
          <LoginProductVisuals />
        </Box>
      </Box>

      <Stack
        component="ul"
        direction="row"
        spacing={0}
        flexWrap="wrap"
        useFlexGap
        sx={{
          color: brand.muted,
          flexShrink: 0,
          pt: { xs: 2, md: 3 },
          gap: { xs: 1, sm: 1.25 },
          listStyle: 'none',
          m: 0,
          p: 0,
        }}
        aria-label="Platform capabilities"
      >
        {TRUST_ITEMS.map((item) => (
          <Stack
            key={item}
            component="li"
            direction="row"
            spacing={0.5}
            alignItems="center"
            sx={{ minWidth: { xs: '46%', sm: 'auto' } }}
          >
            <CheckRoundedIcon sx={{ fontSize: 14, color: brand.gold }} aria-hidden />
            <Typography variant="caption" sx={{ fontSize: '0.7rem', fontWeight: 600 }}>
              {item}
            </Typography>
          </Stack>
        ))}
      </Stack>
    </Box>
  )
}
