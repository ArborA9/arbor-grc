import { Box, Typography } from '@mui/material'
import type { ReactNode } from 'react'
import { NARRATIVE_MAX_ROWS, narrativeReadScrollSx, readOnlyCaptionSx, TYPE_VALUE } from '../ui'
import { brand } from '../../theme'

/** Caption-above read-only value. Same pattern as BIA process identity. */
export function CapaReadOnlyField({
  label,
  value,
  required,
  emphasis,
  well,
  children,
}: {
  label: string
  value?: string
  required?: boolean
  /** Gold value, for the finding id. */
  emphasis?: boolean
  /** Warm surface for narrative plan text. */
  well?: boolean
  children?: ReactNode
}) {
  const text = (value ?? '').trim() ? value!.trim() : '-'
  const empty = text === '-'
  return (
    <Box>
      <Typography component="label" sx={readOnlyCaptionSx}>
        {label}
        {required ? ' *' : ''}
      </Typography>
      {children ?? (
        <Typography
          variant="body2"
          sx={{
            minHeight: well ? undefined : 32,
            alignItems: well ? 'flex-start' : 'center',
            whiteSpace: 'pre-wrap',
            fontSize: TYPE_VALUE,
            fontWeight: emphasis ? 700 : 500,
            color: empty ? brand.mutedLight : emphasis ? brand.goldDark : brand.ink,
            ...(well
              ? {
                  ...narrativeReadScrollSx,
                  display: 'block',
                  bgcolor: brand.hover,
                  border: `1px solid ${brand.border}`,
                  borderRadius: 1,
                  px: 1.25,
                  py: 1,
                  lineHeight: 1.45,
                  maxHeight: `calc(${TYPE_VALUE} * 1.45 * ${NARRATIVE_MAX_ROWS} + 16px)`,
                }
              : { display: 'flex' }),
          }}
        >
          {text}
        </Typography>
      )}
    </Box>
  )
}
