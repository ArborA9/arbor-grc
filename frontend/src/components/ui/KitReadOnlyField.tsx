import { Box, Typography } from '@mui/material'
import { brand } from '../../theme'
import { CONTROL_HEIGHT, readOnlyCaptionSx } from './FormFieldLabel'
import { NARRATIVE_FIELD_MAX_WIDTH, narrativeReadScrollSx } from './formLayout'

/** Caption-above plain value for locked fields. Prefer this over a disabled select. */
export default function KitReadOnlyField({
  label,
  value,
  wrapLabel,
  multiline,
}: {
  label: string
  value: string | null | undefined
  wrapLabel?: boolean
  multiline?: boolean
}) {
  const text = (value ?? '').trim() || '-'
  return (
    <Box sx={{ minWidth: 0, maxWidth: multiline ? NARRATIVE_FIELD_MAX_WIDTH : undefined }}>
      <Typography
        sx={{
          ...readOnlyCaptionSx,
          mb: 0.5,
          ...(wrapLabel
            ? { whiteSpace: 'normal', wordBreak: 'break-word' }
            : { whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }),
        }}
      >
        {label}
      </Typography>
      <Box
        role="textbox"
        aria-readonly="true"
        aria-label={label}
        sx={{
          width: '100%',
          textAlign: 'left',
          fontSize: '0.75rem',
          lineHeight: 1.4,
          color: text === '-' ? brand.muted : brand.ink,
          bgcolor: brand.hover,
          border: `1px solid ${brand.border}`,
          borderRadius: '6px',
          px: 1,
          ...(multiline
            ? {
                minHeight: 40,
                py: 0.75,
                ...narrativeReadScrollSx,
              }
            : {
                minHeight: CONTROL_HEIGHT,
                display: 'flex',
                alignItems: 'center',
                py: 0.5,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }),
        }}
      >
        {text}
      </Box>
    </Box>
  )
}
