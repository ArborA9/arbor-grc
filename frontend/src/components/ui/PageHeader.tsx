import { type ReactNode } from 'react'
import { Box, Stack, Typography } from '@mui/material'
import { brand } from '../../theme'

export interface BreadcrumbSegment {
  label: string
  onClick?: () => void
}

export interface PageHeaderProps {
  title: string
  /**
   * Full breadcrumb trail; last segment is the current context (bold).
   * Page title sits above this line (kit pattern).
   */
  breadcrumb?: BreadcrumbSegment[]
  /** Shorthand: renders `parent › title` under the page title. */
  breadcrumbParent?: string
  actions?: ReactNode
  subtitle?: string
}

function BreadcrumbLine({ segments }: { segments: BreadcrumbSegment[] }) {
  return (
    <Typography
      component="div"
      sx={{ fontSize: '0.6875rem', color: brand.mutedLight, mt: 0.35, lineHeight: 1.3 }}
    >
      {segments.map((seg, i) => {
        const isLast = i === segments.length - 1
        const clickable = !!seg.onClick && !isLast
        return (
          <Box component="span" key={`${seg.label}-${i}`}>
            {i > 0 && (
              <Box component="span" sx={{ mx: 0.5 }}>
                ›
              </Box>
            )}
            {clickable ? (
              <Box
                component="button"
                type="button"
                onClick={seg.onClick}
                sx={{
                  border: 0,
                  p: 0,
                  m: 0,
                  font: 'inherit',
                  color: brand.mutedLight,
                  cursor: 'pointer',
                  bgcolor: 'transparent',
                  '&:hover': { color: brand.goldDark, textDecoration: 'underline' },
                }}
              >
                {seg.label}
              </Box>
            ) : (
              <Box
                component="span"
                sx={{
                  color: isLast ? brand.muted : brand.mutedLight,
                  fontWeight: isLast ? 500 : 400,
                }}
              >
                {seg.label}
              </Box>
            )}
          </Box>
        )
      })}
    </Typography>
  )
}

export default function PageHeader({
  title,
  breadcrumb,
  breadcrumbParent,
  actions,
  subtitle,
}: PageHeaderProps) {
  const breadcrumbSegments: BreadcrumbSegment[] | null = breadcrumb?.length
    ? breadcrumb
    : breadcrumbParent
      ? [{ label: breadcrumbParent }, { label: title }]
      : null

  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      spacing={1}
      alignItems={{ sm: 'center' }}
      justifyContent="space-between"
      sx={{ mb: 2 }}
    >
      <Box sx={{ minWidth: 0 }}>
        <Typography
          sx={{
            fontSize: '0.9375rem',
            fontWeight: 600,
            color: brand.ink,
            lineHeight: 1.25,
            letterSpacing: '-0.01em',
          }}
        >
          {title}
        </Typography>
        {subtitle && (
          <Typography sx={{ fontSize: '0.6875rem', color: brand.muted, mt: 0.25 }}>
            {subtitle}
          </Typography>
        )}
        {breadcrumbSegments && <BreadcrumbLine segments={breadcrumbSegments} />}
      </Box>
      {actions && (
        <Stack direction="row" spacing={1} alignItems="center" flexShrink={0} useFlexGap>
          {actions}
        </Stack>
      )}
    </Stack>
  )
}
