import { useState } from 'react'
import { Box, Button, Drawer, IconButton, Link, Stack, Typography } from '@mui/material'
import MenuRoundedIcon from '@mui/icons-material/MenuRounded'
import CloseRoundedIcon from '@mui/icons-material/CloseRounded'
import { LogoMark } from './components/ui'
import { brand, semantic } from './theme'

const NAVY = '#1a1a1a'
const SUB = '#555555'
const MUTED = '#888888'
const PAGE_BG = '#f9f9f9'
const BORDER = '#e0e0e0'

const NAV = [
  { id: 'modules', label: 'Modules', href: '/#modules' },
  { id: 'benefits', label: 'Benefits', href: '/#benefits' },
  { id: 'platform', label: 'Platform', href: '/#platform' },
  { id: 'security', label: 'Security', href: '/#security' },
] as const

const FOOTER_LINKS = [
  { label: 'Modules', href: '/#modules' },
  { label: 'Benefits', href: '/#benefits' },
  { label: 'Security', href: '/#security' },
  { label: 'Sign In', href: '/signin' },
] as const

const MODULES = [
  {
    name: 'Frameworks and compliance',
    detail: 'Statement of Applicability and gap assessments across the frameworks you activate. One control can satisfy more than one framework.',
  },
  {
    name: 'Information security risk',
    detail: 'Identify, score, and treat information security risks on the register your team already uses, with owners and treatment plans.',
  },
  {
    name: 'Enterprise risk',
    detail: 'The same discipline for enterprise risks: scoring, acceptance, and a treatment action plan before work is assigned.',
  },
  {
    name: 'Third-party risk',
    detail: 'Vendor projects, questionnaires, and reviews so supplier risk sits next to the rest of the register.',
  },
  {
    name: 'Business continuity',
    detail: 'Business impact analysis, recovery plans, exercises, and NCEMA gap work for continuity obligations.',
  },
  {
    name: 'Privacy',
    detail: 'Records of processing, data subject requests, DPIA, consent, and breach handling for UAE personal data duties.',
  },
  {
    name: 'Audit and CAPA',
    detail: 'Plan engagements, collect evidence, log findings, and track corrective actions through to close.',
  },
  {
    name: 'Assets, policy, and incidents',
    detail: 'Asset register with classification, policy lifecycle, key risk indicators, and incident records in the same workspace.',
  },
] as const

const BENEFITS = [
  {
    title: 'Licence only what you use',
    body: 'Each workspace shows the modules that are turned on. Navigation, reports, and screens for a module that is off stay hidden.',
  },
  {
    title: 'One control library',
    body: 'Framework content sits on a single engine. Implement a control once and it is credited wherever that control applies.',
  },
  {
    title: 'Work reaches the right person',
    body: 'Owners and admins run the workspace. Contributors see assigned tasks. Auditors work the engagement they were given.',
  },
  {
    title: 'Evidence stays classified',
    body: 'Uploads carry a classification. Downloads wait until a malware scan marks the file clean.',
  },
  {
    title: 'An audit trail on the record',
    body: 'Status changes, assignments, and sign-in method are logged, so a review does not depend on a side spreadsheet.',
  },
  {
    title: 'Reports from the live register',
    body: 'Gap, risk, audit, and management views read the same records the team is updating, not a copy pasted for the board.',
  },
] as const

const FRAMEWORK_BARS = [
  { name: 'ISO 27001', value: 82, color: brand.gold },
  { name: 'UAE IA', value: 74, color: '#C4A35A' },
  { name: 'NCEMA', value: 68, color: '#8C6A3D' },
  { name: 'ISO 31000', value: 91, color: '#3F3F46' },
] as const

const RISK_BARS = [
  { name: 'Low', count: 18, color: semantic.risk.low },
  { name: 'Medium', count: 11, color: semantic.risk.medium },
  { name: 'High', count: 5, color: semantic.risk.high },
  { name: 'Critical', count: 2, color: semantic.risk.critical },
] as const

const HEAT = [
  [0, 0, 1, 0, 0],
  [0, 2, 1, 1, 0],
  [1, 3, 2, 1, 0],
  [2, 4, 2, 0, 0],
  [6, 3, 1, 0, 0],
] as const

const HEAT_COLORS = [
  semantic.heatmap.veryLow,
  semantic.heatmap.low,
  semantic.heatmap.medium,
  semantic.heatmap.high,
  semantic.heatmap.critical,
]

const ctaPrimarySx = {
  bgcolor: brand.gold,
  color: '#fff',
  fontWeight: 700,
  fontSize: '0.9375rem',
  px: 4,
  py: 1.75,
  minHeight: 48,
  borderRadius: '6px',
  boxShadow: 'none',
  textTransform: 'none' as const,
  transition: 'background-color 200ms ease, transform 200ms ease, box-shadow 200ms ease',
  '&:hover': {
    bgcolor: brand.goldDark,
    boxShadow: '0 4px 14px rgba(146, 83, 10, 0.22)',
    transform: 'scale(1.02)',
  },
  '&:active': { bgcolor: brand.goldDark, transform: 'scale(1)' },
}

const ctaSecondarySx = {
  bgcolor: 'transparent',
  color: brand.goldDark,
  fontWeight: 700,
  fontSize: '0.9375rem',
  px: 4,
  py: 1.75,
  minHeight: 48,
  borderRadius: '6px',
  border: `2px solid ${brand.gold}`,
  boxShadow: 'none',
  textTransform: 'none' as const,
  transition: 'background-color 200ms ease, transform 200ms ease, border-color 200ms ease',
  '&:hover': {
    bgcolor: brand.goldLight,
    borderColor: brand.goldDark,
    transform: 'scale(1.02)',
  },
}

const sectionTitleSx = {
  fontWeight: 700,
  fontSize: { xs: '1.75rem', md: '2.25rem' },
  letterSpacing: '-0.03em',
  lineHeight: 1.2,
  color: NAVY,
}

function goSignIn() {
  window.location.assign('/signin')
}

function BrandMark({ size = 32 }: { size?: number }) {
  return (
    <Stack
      component="a"
      href="/"
      direction="row"
      spacing={1}
      alignItems="center"
      sx={{ textDecoration: 'none', color: NAVY, flexShrink: 0 }}
    >
      <LogoMark size={size} />
      <Box sx={{ display: { xs: 'none', sm: 'block' } }}>
        <Typography sx={{ fontWeight: 700, fontSize: '0.9375rem', lineHeight: 1.15, letterSpacing: '-0.02em', color: NAVY }}>
          Arbor GRC
        </Typography>
        <Typography sx={{ fontSize: '0.5625rem', letterSpacing: '0.08em', color: MUTED, textTransform: 'uppercase' }}>
          By A9 Advisory
        </Typography>
      </Box>
    </Stack>
  )
}

function NavLinks({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <>
      {NAV.map((item) => (
        <Link
          key={item.id}
          href={item.href}
          underline="none"
          onClick={onNavigate}
          sx={{
            fontSize: '0.875rem',
            fontWeight: 600,
            color: NAVY,
            px: 1.25,
            py: 0.5,
            transition: 'color 150ms ease',
            '&:hover': { color: brand.goldDark, textDecoration: 'underline' },
          }}
        >
          {item.label}
        </Link>
      ))}
    </>
  )
}

function SectionLabel({ children }: { children: string }) {
  return (
    <Typography
      sx={{
        fontSize: '0.75rem',
        fontWeight: 700,
        letterSpacing: '0.12em',
        textTransform: 'uppercase',
        color: brand.goldDark,
        mb: 1.25,
      }}
    >
      {children}
    </Typography>
  )
}

function Heatmap() {
  return (
    <Box>
      <Stack direction="row" spacing={0.5}>
        <Typography sx={{ fontSize: '0.65rem', color: MUTED, writingMode: 'vertical-rl', transform: 'rotate(180deg)', alignSelf: 'center' }}>
          Impact
        </Typography>
        <Box>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: 'repeat(5, 28px)',
              gap: '3px',
            }}
            aria-hidden
          >
            {HEAT.map((row, r) =>
              row.map((cell, c) => (
                <Box
                  key={`${r}-${c}`}
                  sx={{
                    width: 28,
                    height: 28,
                    borderRadius: '3px',
                    bgcolor: HEAT_COLORS[Math.min(r + c, HEAT_COLORS.length - 1)],
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '0.65rem',
                    fontWeight: 700,
                    color: r + c >= 6 ? '#fff' : NAVY,
                  }}
                >
                  {cell || ''}
                </Box>
              )),
            )}
          </Box>
          <Typography sx={{ fontSize: '0.65rem', color: MUTED, textAlign: 'center', mt: 0.5 }}>Likelihood</Typography>
        </Box>
      </Stack>
    </Box>
  )
}

export default function HomeLanding() {
  const [menuOpen, setMenuOpen] = useState(false)
  const maxRisk = Math.max(...RISK_BARS.map((b) => b.count))

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: PAGE_BG, color: NAVY, display: 'flex', flexDirection: 'column' }}>
      <Box
        component="header"
        sx={{
          position: 'sticky',
          top: 0,
          zIndex: 10,
          height: 64,
          px: { xs: 2, md: 4 },
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          bgcolor: '#fff',
          borderBottom: `1px solid ${BORDER}`,
          boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
        }}
      >
        <BrandMark />
        <Stack direction="row" spacing={0.5} alignItems="center" sx={{ display: { xs: 'none', md: 'flex' } }}>
          <NavLinks />
          <Button variant="contained" onClick={goSignIn} sx={{ ...ctaPrimarySx, ml: 1, minHeight: 36, py: 0.75, px: 2.5 }}>
            Sign In
          </Button>
        </Stack>
        <IconButton
          aria-label="Open menu"
          onClick={() => setMenuOpen(true)}
          sx={{ display: { xs: 'inline-flex', md: 'none' }, color: NAVY }}
        >
          <MenuRoundedIcon />
        </IconButton>
      </Box>

      <Drawer anchor="right" open={menuOpen} onClose={() => setMenuOpen(false)}>
        <Box sx={{ width: 280, p: 2 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
            <Typography sx={{ fontWeight: 700 }}>Menu</Typography>
            <IconButton aria-label="Close menu" onClick={() => setMenuOpen(false)}>
              <CloseRoundedIcon />
            </IconButton>
          </Stack>
          <Stack spacing={1.25} alignItems="flex-start">
            <NavLinks onNavigate={() => setMenuOpen(false)} />
            <Button fullWidth variant="contained" onClick={goSignIn} sx={{ ...ctaPrimarySx, mt: 1 }}>
              Sign In
            </Button>
          </Stack>
        </Box>
      </Drawer>

      <Box
        component="section"
        id="home"
        sx={{
          bgcolor: PAGE_BG,
          px: { xs: 3, md: 7.5 },
          py: { xs: 7, md: 10 },
        }}
      >
        <Stack
          direction={{ xs: 'column', md: 'row' }}
          spacing={{ xs: 5, md: 8 }}
          alignItems="center"
          sx={{ maxWidth: 1120, mx: 'auto' }}
        >
          <Box sx={{ flex: 1, textAlign: { xs: 'center', md: 'left' } }}>
            <SectionLabel>Governance, risk, and compliance</SectionLabel>
            <Typography
              component="h1"
              sx={{
                fontWeight: 700,
                fontSize: { xs: '2rem', sm: '2.75rem', md: '3.25rem' },
                lineHeight: 1.15,
                color: NAVY,
                letterSpacing: '-0.03em',
                mb: 2,
              }}
            >
              One workspace for the frameworks you licence.
            </Typography>
            <Typography sx={{ fontSize: { xs: '1rem', md: '1.125rem' }, color: SUB, lineHeight: 1.65, maxWidth: 560, mb: 3.5, mx: { xs: 'auto', md: 0 } }}>
              Arbor GRC replaces separate spreadsheets with a single register: controls, risks, evidence, audits, and continuity, scoped to the modules on your subscription.
            </Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} justifyContent={{ xs: 'center', md: 'flex-start' }}>
              <Button variant="contained" onClick={goSignIn} sx={{ ...ctaPrimarySx, width: { xs: '100%', sm: 'auto' } }}>
                Sign In
              </Button>
              <Button variant="outlined" href="/#modules" sx={{ ...ctaSecondarySx, width: { xs: '100%', sm: 'auto' } }}>
                See the Modules
              </Button>
            </Stack>
          </Box>

          <Box
            sx={{
              width: { xs: '100%', md: 380 },
              flexShrink: 0,
              bgcolor: '#fff',
              border: `1px solid ${brand.border}`,
              borderRadius: '8px',
              p: 2.5,
              boxShadow: brand.shadowCard,
            }}
          >
            <Typography sx={{ fontSize: '0.7rem', fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: MUTED, mb: 0.5 }}>
              Example workspace
            </Typography>
            <Typography sx={{ fontWeight: 700, mb: 2 }}>Open risk by rating</Typography>
            <Stack spacing={1.25}>
              {RISK_BARS.map((bar) => (
                <Box key={bar.name}>
                  <Stack direction="row" justifyContent="space-between" sx={{ mb: 0.4 }}>
                    <Typography sx={{ fontSize: '0.8125rem', fontWeight: 600 }}>{bar.name}</Typography>
                    <Typography sx={{ fontSize: '0.8125rem', color: SUB, fontVariantNumeric: 'tabular-nums' }}>{bar.count}</Typography>
                  </Stack>
                  <Box sx={{ height: 8, bgcolor: '#F3F0EA', borderRadius: 99 }}>
                    <Box sx={{ width: `${(bar.count / maxRisk) * 100}%`, height: '100%', bgcolor: bar.color, borderRadius: 99 }} />
                  </Box>
                </Box>
              ))}
            </Stack>
            <Typography sx={{ mt: 2, fontSize: '0.75rem', color: MUTED, lineHeight: 1.5 }}>
              Sample figures for layout only. Your workspace shows the live register after sign-in.
            </Typography>
          </Box>
        </Stack>
      </Box>

      <Box component="section" id="modules" sx={{ scrollMarginTop: 80, bgcolor: '#fff', px: { xs: 3, md: 7.5 }, py: { xs: 7, md: 9 }, borderTop: `1px solid ${BORDER}` }}>
        <Box sx={{ maxWidth: 1120, mx: 'auto' }}>
          <SectionLabel>Modules</SectionLabel>
          <Typography component="h2" sx={{ ...sectionTitleSx, mb: 1.5, maxWidth: 640 }}>
            Turn on the work your organisation actually has.
          </Typography>
          <Typography sx={{ color: SUB, maxWidth: 640, mb: 4, lineHeight: 1.6 }}>
            A module that is not licensed does not appear in the menu, the dashboard, or the API. These are the areas Arbor GRC covers when they are on.
          </Typography>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: '1fr 1fr 1fr 1fr' },
              gap: 2,
            }}
          >
            {MODULES.map((mod) => (
              <Box key={mod.name} sx={{ p: 2.25, borderRadius: '8px', border: `1px solid ${brand.border}`, bgcolor: brand.canvas, minHeight: 160 }}>
                <Typography sx={{ fontWeight: 700, fontSize: '0.95rem', mb: 1, letterSpacing: '-0.02em' }}>{mod.name}</Typography>
                <Typography sx={{ fontSize: '0.8125rem', color: SUB, lineHeight: 1.55 }}>{mod.detail}</Typography>
              </Box>
            ))}
          </Box>
        </Box>
      </Box>

      <Box component="section" id="benefits" sx={{ scrollMarginTop: 80, bgcolor: PAGE_BG, px: { xs: 3, md: 7.5 }, py: { xs: 7, md: 9 } }}>
        <Box sx={{ maxWidth: 1120, mx: 'auto' }}>
          <SectionLabel>Business benefits</SectionLabel>
          <Typography component="h2" sx={{ ...sectionTitleSx, mb: 4, maxWidth: 680 }}>
            Less copying between files. A clearer picture for audit and management.
          </Typography>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr 1fr' }, gap: 2.5 }}>
            {BENEFITS.map((item) => (
              <Box key={item.title} sx={{ bgcolor: '#fff', border: `1px solid ${brand.border}`, borderRadius: '8px', p: 2.5 }}>
                <Box sx={{ width: 28, height: 3, bgcolor: brand.gold, mb: 1.75, borderRadius: 99 }} />
                <Typography sx={{ fontWeight: 700, mb: 1 }}>{item.title}</Typography>
                <Typography sx={{ fontSize: '0.875rem', color: SUB, lineHeight: 1.6 }}>{item.body}</Typography>
              </Box>
            ))}
          </Box>
        </Box>
      </Box>

      <Box component="section" id="platform" sx={{ scrollMarginTop: 80, bgcolor: '#fff', px: { xs: 3, md: 7.5 }, py: { xs: 7, md: 9 }, borderTop: `1px solid ${BORDER}` }}>
        <Box sx={{ maxWidth: 1120, mx: 'auto' }}>
          <SectionLabel>Platform</SectionLabel>
          <Typography component="h2" sx={{ ...sectionTitleSx, mb: 1.5 }}>
            What leadership sees after sign-in.
          </Typography>
          <Typography sx={{ color: SUB, maxWidth: 640, mb: 4, lineHeight: 1.6 }}>
            The dashboard follows the modules on the subscription. The picture below is an example, using the same rating colours as the product.
          </Typography>
          <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2.5}>
            <Box sx={{ flex: 1.2, border: `1px solid ${brand.border}`, borderRadius: '8px', p: 2.5 }}>
              <Typography sx={{ fontWeight: 700, mb: 0.5 }}>Framework coverage</Typography>
              <Typography sx={{ fontSize: '0.75rem', color: MUTED, mb: 2 }}>Example percent of applicable controls marked implemented</Typography>
              <Stack spacing={1.75}>
                {FRAMEWORK_BARS.map((bar) => (
                  <Box key={bar.name}>
                    <Stack direction="row" justifyContent="space-between" sx={{ mb: 0.5 }}>
                      <Typography sx={{ fontSize: '0.875rem', fontWeight: 600 }}>{bar.name}</Typography>
                      <Typography sx={{ fontSize: '0.875rem', fontVariantNumeric: 'tabular-nums' }}>{bar.value}%</Typography>
                    </Stack>
                    <Box sx={{ height: 10, bgcolor: '#F3F0EA', borderRadius: 99 }}>
                      <Box sx={{ width: `${bar.value}%`, height: '100%', bgcolor: bar.color, borderRadius: 99 }} />
                    </Box>
                  </Box>
                ))}
              </Stack>
            </Box>
            <Box sx={{ flex: 1, border: `1px solid ${brand.border}`, borderRadius: '8px', p: 2.5 }}>
              <Typography sx={{ fontWeight: 700, mb: 0.5 }}>Inherent risk heatmap</Typography>
              <Typography sx={{ fontSize: '0.75rem', color: MUTED, mb: 2 }}>Example count of open risks in each cell</Typography>
              <Heatmap />
            </Box>
            <Box sx={{ flex: 0.9, bgcolor: brand.sidebar, borderRadius: '8px', p: 2.5 }}>
              <Typography sx={{ fontWeight: 700, mb: 2 }}>Example open work</Typography>
              {[
                ['Treatment plans due', '7'],
                ['Audit findings open', '4'],
                ['CAPA awaiting response', '3'],
                ['Vendor reviews in progress', '2'],
              ].map(([label, value]) => (
                <Stack key={label} direction="row" justifyContent="space-between" sx={{ py: 1.1, borderBottom: `1px solid rgba(0,0,0,0.06)` }}>
                  <Typography sx={{ fontSize: '0.875rem' }}>{label}</Typography>
                  <Typography sx={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>{value}</Typography>
                </Stack>
              ))}
            </Box>
          </Stack>
        </Box>
      </Box>

      <Box component="section" id="security" sx={{ scrollMarginTop: 80, bgcolor: '#1C1917', color: '#FAFAF8', px: { xs: 3, md: 7.5 }, py: { xs: 7, md: 9 } }}>
        <Box sx={{ maxWidth: 1120, mx: 'auto' }}>
          <Typography sx={{ fontSize: '0.75rem', fontWeight: 700, letterSpacing: '0.12em', textTransform: 'uppercase', color: brand.goldMid, mb: 1.25 }}>
            Security
          </Typography>
          <Typography component="h2" sx={{ ...sectionTitleSx, color: '#fff', mb: 1.5, maxWidth: 680 }}>
            Sign-in, residency, and access match a regulated workspace.
          </Typography>
          <Typography sx={{ color: '#D6D3D1', maxWidth: 640, mb: 4, lineHeight: 1.65 }}>
            Company users sign in with Microsoft Entra. Local passwords are the fallback, limited once single sign-on is on. Each organisation has its own database.
          </Typography>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: '1fr 1fr 1fr' }, gap: 2 }}>
            {[
              ['Company SSO', 'Microsoft Entra is the primary sign-in. Disabling the company account removes Arbor access.'],
              ['MFA for privileged roles', 'Owners and admins complete an authenticator step when the identity provider does not already require one.'],
              ['UAE North residency', 'Tenant data is held in Azure UAE North. Evidence files are stored in that region and scanned before download.'],
              ['Classification on files', 'Every upload is tagged. Who can open it follows the classification, not a shared folder.'],
              ['Session limits', 'Idle timeout, a short access token, and a cap on concurrent sessions are enforced per workspace.'],
              ['Separate operator access', 'Platform administration is not on the tenant sign-in page. Staff use the workspace sign-in only.'],
            ].map(([title, body]) => (
              <Box key={title} sx={{ border: '1px solid rgba(255,255,255,0.12)', borderRadius: '8px', p: 2.25 }}>
                <Typography sx={{ fontWeight: 700, mb: 1, color: '#fff' }}>{title}</Typography>
                <Typography sx={{ fontSize: '0.875rem', color: '#D6D3D1', lineHeight: 1.6 }}>{body}</Typography>
              </Box>
            ))}
          </Box>
        </Box>
      </Box>

      <Box sx={{ bgcolor: '#fff', px: { xs: 3, md: 7.5 }, py: { xs: 6, md: 7 }, textAlign: 'center' }}>
        <Typography component="h2" sx={{ ...sectionTitleSx, mb: 1.5 }}>
          Open your workspace.
        </Typography>
        <Typography sx={{ color: SUB, mb: 3, maxWidth: 480, mx: 'auto', lineHeight: 1.6 }}>
          Use the workspace ID from your administrator, then continue with company SSO or a local account.
        </Typography>
        <Button variant="contained" onClick={goSignIn} sx={ctaPrimarySx}>
          Sign In
        </Button>
      </Box>

      <Box component="footer" sx={{ bgcolor: '#fff', borderTop: `1px solid ${BORDER}`, px: { xs: 3, md: 4 }, py: 5 }}>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={2}
          justifyContent="space-between"
          alignItems={{ xs: 'flex-start', sm: 'center' }}
        >
          <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap>
            {FOOTER_LINKS.map((l) => (
              <Link key={l.label} href={l.href} underline="hover" sx={{ fontSize: '0.8125rem', color: SUB, '&:hover': { color: brand.goldDark } }}>
                {l.label}
              </Link>
            ))}
          </Stack>
          <Typography sx={{ fontSize: '0.75rem', color: MUTED }}>
            © {new Date().getFullYear()} Arbor GRC. All rights reserved.
          </Typography>
        </Stack>
      </Box>
    </Box>
  )
}
