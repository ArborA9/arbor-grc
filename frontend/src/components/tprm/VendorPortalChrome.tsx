import { type ReactNode } from 'react'
import { Box, LinearProgress, Stack, Typography } from '@mui/material'
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded'
import LockRoundedIcon from '@mui/icons-material/LockRounded'
import ShieldOutlinedIcon from '@mui/icons-material/ShieldOutlined'
import { BusyButton, LogoMark, PageLoading } from '../ui'
import StatusChip from '../StatusChip'
import { brand, semantic } from '../../theme'
import { portalStatusLabel } from './vendorPortalItems'

const toolbarBtnSx = {
  height: 32,
  minHeight: 32,
  px: 1.5,
  textTransform: 'none' as const,
}

export function VendorPortalPostureTarget({ value }: { value: string }) {
  return (
    <Box
      sx={{
        display: 'inline-flex',
        alignItems: 'baseline',
        gap: 0.75,
        px: 1.25,
        py: 0.5,
        borderRadius: 1,
        bgcolor: brand.gold,
        border: `1.5px solid ${brand.goldDark}`,
        flexShrink: 0,
      }}
    >
      <Typography
        sx={{
          fontSize: '0.625rem',
          fontWeight: 700,
          letterSpacing: '0.08em',
          textTransform: 'uppercase',
          color: brand.paper,
          lineHeight: 1.2,
        }}
      >
        Posture target
      </Typography>
      <Typography sx={{ fontSize: '1rem', fontWeight: 800, color: brand.paper, lineHeight: 1.1 }}>
        {value}
      </Typography>
    </Box>
  )
}

export function VendorPortalShell({
  trailing,
  toolbar,
  subbar,
  brandTitle = 'Arbor GRC',
  brandSubtitle = 'Secure vendor workspace',
  lockViewport = false,
  children,
}: {
  trailing?: ReactNode
  toolbar?: ReactNode
  subbar?: ReactNode
  brandTitle?: string
  brandSubtitle?: string
  /** Questionnaire workspace: children fill the remaining height and manage their own scroll. */
  lockViewport?: boolean
  children: ReactNode
}) {
  return (
    <Box
      sx={{
        ...(lockViewport
          ? { height: '100dvh', maxHeight: '100dvh', overflow: 'hidden' }
          : { minHeight: '100vh' }),
        bgcolor: brand.canvas,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <Box
        sx={{
          flexShrink: 0,
          zIndex: 12,
          bgcolor: brand.paper,
          borderBottom: `1px solid ${brand.hairline}`,
          boxShadow: toolbar ? '0 4px 12px rgba(28, 25, 23, 0.06)' : undefined,
        }}
      >
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1}
          alignItems={{ sm: 'center' }}
          justifyContent="space-between"
          sx={{ px: { xs: 2, md: 3 }, py: 1.25 }}
        >
          <Stack direction="row" spacing={1.25} alignItems="center" sx={{ minWidth: 0 }}>
            <LogoMark size={32} />
            <Box sx={{ minWidth: 0 }}>
              <Typography sx={{ fontWeight: 700, fontSize: '0.9375rem', color: brand.ink, lineHeight: 1.25, letterSpacing: '-0.02em' }} noWrap>
                {brandTitle}
              </Typography>
              <Typography sx={{ fontSize: '0.6875rem', color: brand.mutedLight, mt: 0.15 }} noWrap>
                {brandSubtitle}
              </Typography>
            </Box>
          </Stack>
          {toolbar ?? (
            <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap justifyContent="flex-end">
              <Stack direction="row" spacing={0.5} alignItems="center" sx={{ color: brand.mutedLight }}>
                <LockRoundedIcon sx={{ fontSize: 14 }} />
                <Typography sx={{ fontSize: '0.6875rem', fontWeight: 600, display: { xs: 'none', sm: 'block' } }}>
                  One-time access
                </Typography>
              </Stack>
              {trailing}
            </Stack>
          )}
        </Stack>
        {subbar}
      </Box>
      <Box sx={{ flex: 1, minHeight: 0, overflow: lockViewport ? 'hidden' : 'auto', display: 'flex', flexDirection: 'column' }}>
        {children}
      </Box>
    </Box>
  )
}

export function VendorPortalAuthLayout({ children }: { children: ReactNode }) {
  return (
    <Box
      sx={{
        minHeight: '100%',
        display: 'flex',
        alignItems: { xs: 'flex-start', md: 'center' },
        justifyContent: 'center',
        px: { xs: 2, md: 3 },
        py: { xs: 3, md: 6 },
      }}
    >
      <Box sx={{ width: '100%', maxWidth: 440 }}>{children}</Box>
    </Box>
  )
}

export function VendorPortalAuthCard({
  eyebrow,
  title,
  subtitle,
  footer,
  children,
}: {
  eyebrow?: string
  title: string
  subtitle?: string
  footer?: string
  children: ReactNode
}) {
  return (
    <Box
      sx={{
        bgcolor: brand.paper,
        border: `1px solid ${brand.border}`,
        borderRadius: 2,
        boxShadow: '0 12px 40px rgba(28, 25, 23, 0.08)',
        overflow: 'hidden',
      }}
    >
      <Box sx={{ height: 4, bgcolor: brand.gold }} />
      <Box sx={{ px: { xs: 2.25, md: 3 }, py: { xs: 2.25, md: 3 } }}>
        {eyebrow && (
          <Typography sx={{ fontSize: '0.6875rem', fontWeight: 600, letterSpacing: '0.08em', textTransform: 'uppercase', color: brand.goldDark, mb: 0.75 }}>
            {eyebrow}
          </Typography>
        )}
        <Typography sx={{ fontSize: '1.25rem', fontWeight: 700, color: brand.ink, letterSpacing: '-0.02em', lineHeight: 1.25 }}>
          {title}
        </Typography>
        {subtitle && (
          <Typography sx={{ fontSize: '0.8125rem', color: brand.muted, mt: 0.75, lineHeight: 1.5 }}>
            {subtitle}
          </Typography>
        )}
        <Box sx={{ mt: 2.25 }}>{children}</Box>
      </Box>
      <Box sx={{ px: 3, py: 1.25, bgcolor: brand.hover, borderTop: `1px solid ${brand.border}` }}>
        <Stack direction="row" spacing={0.75} alignItems="center">
          <ShieldOutlinedIcon sx={{ fontSize: 14, color: brand.goldDark }} />
          <Typography sx={{ fontSize: '0.6875rem', color: brand.mutedLight }}>
            {footer ?? 'No vendor account. Passcode is sent only to the invited email.'}
          </Typography>
        </Stack>
      </Box>
    </Box>
  )
}

export function VendorPortalLoading() {
  return (
    <VendorPortalAuthLayout>
      <PageLoading label="Opening secure workspace…" />
    </VendorPortalAuthLayout>
  )
}

export function VendorPortalSubmitted({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <VendorPortalAuthLayout>
      <Box
        sx={{
          bgcolor: brand.paper,
          border: `1px solid ${brand.border}`,
          borderRadius: 2,
          boxShadow: '0 12px 40px rgba(28, 25, 23, 0.08)',
          px: 3,
          py: 4,
          textAlign: 'center',
        }}
      >
        <Box
          sx={{
            width: 56,
            height: 56,
            borderRadius: '50%',
            bgcolor: '#DCFCE7',
            color: semantic.risk.low,
            display: 'grid',
            placeItems: 'center',
            mx: 'auto',
            mb: 2,
          }}
        >
          <CheckCircleRoundedIcon sx={{ fontSize: 32 }} />
        </Box>
        <Typography sx={{ fontSize: '1.25rem', fontWeight: 700, color: brand.ink, letterSpacing: '-0.02em' }}>
          Response Submitted
        </Typography>
        <Typography sx={{ fontSize: '0.875rem', color: brand.muted, mt: 1, lineHeight: 1.5 }}>
          Thank you. Your response has been successfully submitted
          {title ? ` for ${title}` : ''}.
        </Typography>
        {subtitle && (
          <Typography sx={{ fontSize: '0.75rem', color: brand.mutedLight, mt: 1.25, fontFamily: 'ui-monospace, Menlo, monospace' }}>
            {subtitle}
          </Typography>
        )}
      </Box>
    </VendorPortalAuthLayout>
  )
}

export function VendorPortalStatusChip({ status, unsaved }: { status?: string; unsaved?: boolean }) {
  if (unsaved) return <StatusChip label="Unsaved changes" tone="warning" />
  if (!status) return null
  const tone = status === 'RETURNED' ? 'warning' : status === 'SUBMITTED' || status === 'COMPLETE' ? 'success' : 'info'
  return <StatusChip label={portalStatusLabel(status)} tone={tone} />
}

/** Sticky header actions: progress + Save / Submit stay at the top while scrolling. */
export function VendorPortalTopActions({
  status,
  unsaved,
  answered,
  total,
  evidenceOutstanding,
  formDirty,
  busy,
  canSubmit,
  postureTarget,
  onSave,
  onSubmit,
}: {
  status?: string
  unsaved?: boolean
  answered: number
  total: number
  evidenceOutstanding: number
  formDirty: boolean
  busy: boolean
  canSubmit: boolean
  postureTarget?: string | null
  onSave: () => void
  onSubmit: () => void
}) {
  const pct = total === 0 ? 0 : Math.round((answered / total) * 100)
  return (
    <Box sx={{ width: { xs: '100%', sm: 'auto' }, minWidth: 0, flex: { md: 1 } }}>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={0.75}
        alignItems={{ md: 'center' }}
        justifyContent="flex-end"
      >
        {postureTarget ? <VendorPortalPostureTarget value={postureTarget} /> : null}
        <Box sx={{ flex: 1, minWidth: 0, maxWidth: { md: 320 }, ml: { md: 0 } }}>
          <Stack direction="row" justifyContent="space-between" spacing={1} sx={{ mb: 0.4 }}>
            <Typography sx={{ fontSize: '0.75rem', color: brand.muted, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {answered} of {total} answered
              {evidenceOutstanding > 0 ? ` · ${evidenceOutstanding} evidence file${evidenceOutstanding === 1 ? '' : 's'} still needed` : ''}
            </Typography>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 700, color: brand.goldDark, flexShrink: 0 }}>{pct}%</Typography>
          </Stack>
          <LinearProgress
            variant="determinate"
            value={pct}
            aria-label="Questionnaire progress"
            sx={{ height: 6, borderRadius: 1, bgcolor: brand.border, '& .MuiLinearProgress-bar': { bgcolor: brand.gold } }}
          />
        </Box>
        <Stack direction="row" spacing={1} alignItems="center" justifyContent="flex-end" flexShrink={0} flexWrap="wrap" useFlexGap>
          <VendorPortalStatusChip status={status} unsaved={unsaved} />
          <BusyButton
            size="small"
            variant="outlined"
            sx={toolbarBtnSx}
            busy={busy}
            disabled={!formDirty}
            disabledReason={!formDirty ? 'No unsaved changes' : undefined}
            onClick={onSave}
          >
            Save Progress
          </BusyButton>
          <BusyButton
            size="small"
            variant="contained"
            sx={toolbarBtnSx}
            busy={busy}
            disabled={!canSubmit}
            disabledReason={!canSubmit ? 'Only the Vendor primary SPOC can submit' : undefined}
            onClick={onSubmit}
          >
            Submit
          </BusyButton>
        </Stack>
      </Stack>
    </Box>
  )
}

export { toolbarBtnSx }
