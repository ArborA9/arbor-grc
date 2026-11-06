import { type ReactNode, type Ref } from 'react'
import { Box, Button, Drawer, IconButton, Stack, Tooltip, Typography } from '@mui/material'
import ChevronLeftRoundedIcon from '@mui/icons-material/ChevronLeftRounded'
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded'
import CloseRoundedIcon from '@mui/icons-material/CloseRounded'
import { brand } from '../../theme'

export default function ReviewDossierDrawer({
  open,
  onClose,
  code,
  title,
  titleWeight = 650,
  chips,
  banner,
  toolbar,
  index,
  total,
  onPrev,
  onNext,
  busy = false,
  workingOverlay,
  children,
  footer,
  width,
  bodyRef,
}: {
  open: boolean
  onClose: () => void
  code?: string
  title: string
  /** Drawer heading weight. Gap uses normal weight for the control title. */
  titleWeight?: number
  chips?: ReactNode
  banner?: ReactNode
  /** Stays above the scroll body (e.g. Gap Control / Assign / Response / Decision). */
  toolbar?: ReactNode
  index: number
  total: number
  onPrev: () => void
  onNext: () => void
  busy?: boolean
  workingOverlay?: ReactNode
  children: ReactNode
  footer?: ReactNode
  width?: { xs?: string | number; sm?: string | number; md?: string | number }
  /** Scroll body, so a parent can follow which section is in view. */
  bodyRef?: Ref<HTMLDivElement>
}) {
  const hasPrev = index > 0
  const hasNext = index >= 0 && index < total - 1

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={busy ? undefined : onClose}
      PaperProps={{
        sx: {
          width: width ?? { xs: '100%', sm: 500, md: 540 },
          maxWidth: '100vw',
          bgcolor: brand.canvas,
          display: 'flex',
          flexDirection: 'column',
        },
      }}
    >
      <Stack sx={{ height: '100%', position: 'relative' }}>
        {workingOverlay}
        <Box
          sx={{
            px: 2.25,
            pt: 1.75,
            pb: 1.5,
            bgcolor: brand.paper,
            borderBottom: `1px solid ${brand.border}`,
            position: 'relative',
            flexShrink: 0,
            '&::before': {
              content: '""',
              position: 'absolute',
              left: 0,
              top: 0,
              bottom: 0,
              width: 3,
              bgcolor: brand.gold,
            },
          }}
        >
          <Stack direction="row" alignItems="flex-start" justifyContent="space-between" gap={1}>
            <Box sx={{ minWidth: 0, flex: 1, pl: 0.5 }}>
              {code ? (
                <Typography
                  sx={{
                    fontFamily: 'ui-monospace, Menlo, monospace',
                    fontWeight: 700,
                    fontSize: '0.75rem',
                    color: brand.goldDark,
                    letterSpacing: '0.02em',
                  }}
                >
                  {code}
                </Typography>
              ) : null}
              <Typography
                sx={{
                  fontWeight: titleWeight,
                  fontSize: '1.0625rem',
                  letterSpacing: '-0.02em',
                  lineHeight: 1.35,
                  mt: code ? 0.5 : 0,
                  color: brand.ink,
                }}
              >
                {title || '-'}
              </Typography>
              {chips ? (
                <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap sx={{ mt: 1 }}>
                  {chips}
                </Stack>
              ) : null}
            </Box>
            <IconButton
              size="small"
              onClick={onClose}
              aria-label="Close"
              disabled={busy}
              sx={{ mt: -0.25 }}
            >
              <CloseRoundedIcon fontSize="small" />
            </IconButton>
          </Stack>
        </Box>

        {banner ? <Box sx={{ px: 2.25, pt: 1.25, flexShrink: 0 }}>{banner}</Box> : null}
        {toolbar ? (
          <Box
            sx={{
              px: 2.25,
              pt: banner ? 1 : 1.5,
              pb: 1,
              flexShrink: 0,
              bgcolor: brand.canvas,
              borderBottom: `1px solid ${brand.borderLight}`,
            }}
          >
            {toolbar}
          </Box>
        ) : null}

        <Box ref={bodyRef} sx={{ flex: 1, overflow: 'auto', px: 2.25, py: 2, minHeight: 0 }}>{children}</Box>

        <Stack
          direction="row"
          alignItems="center"
          justifyContent="space-between"
          gap={1}
          sx={{
            px: 2.25,
            py: 1.25,
            bgcolor: brand.paper,
            borderTop: `1px solid ${brand.border}`,
            flexShrink: 0,
          }}
        >
          <Stack direction="row" spacing={0.5} alignItems="center">
            <Tooltip title="Previous">
              <span>
                <IconButton
                  size="small"
                  aria-label="Previous"
                  disabled={!hasPrev || busy}
                  onClick={onPrev}
                  sx={{
                    width: 32,
                    height: 32,
                    border: `1px solid ${brand.border}`,
                    borderRadius: 1,
                    color: brand.inkSoft,
                  }}
                >
                  <ChevronLeftRoundedIcon fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>
            <Tooltip title="Next">
              <span>
                <IconButton
                  size="small"
                  aria-label="Next"
                  disabled={!hasNext || busy}
                  onClick={onNext}
                  sx={{
                    width: 32,
                    height: 32,
                    border: `1px solid ${brand.border}`,
                    borderRadius: 1,
                    color: brand.inkSoft,
                  }}
                >
                  <ChevronRightRoundedIcon fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>
            {total > 0 ? (
              <Typography sx={{ fontSize: '0.75rem', color: brand.muted, ml: 0.5, fontVariantNumeric: 'tabular-nums' }}>
                {index + 1} of {total}
              </Typography>
            ) : null}
          </Stack>
          <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
            {footer ?? (
              <Button size="small" onClick={onClose} disabled={busy}>
                Close
              </Button>
            )}
          </Stack>
        </Stack>
      </Stack>
    </Drawer>
  )
}
