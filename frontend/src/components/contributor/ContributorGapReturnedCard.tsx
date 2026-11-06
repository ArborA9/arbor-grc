import { Box, Typography } from '@mui/material'
import { brand } from '../../theme'
import { sectionLabelSx } from '../ui/FormFieldLabel'
import { narrativeReadScrollSx } from '../ui/formLayout'

/** Blocking instruction when Admin returns a Gap control for more Contributor work. */
export default function ContributorGapReturnedCard({ reason }: { reason?: string | null }) {
  const text = reason?.trim() || ''
  return (
    <Box
      role="status"
      sx={{
        p: '12px 14px',
        borderRadius: 1,
        border: '1px solid #F5D0A8',
        bgcolor: '#FFF7ED',
      }}
    >
      <Typography sx={{ fontSize: '0.8125rem', fontWeight: 700, color: '#9A3412', lineHeight: 1.35 }}>
        Returned for more work
      </Typography>
      <Typography sx={{ fontSize: '0.75rem', color: '#9A3412', mt: 0.5, lineHeight: 1.5 }}>
        Admin asked you to update this control. Change your answers or evidence, then submit again.
      </Typography>
      {text ? (
        <Box sx={{ mt: 1.25 }}>
          <Typography sx={{ ...sectionLabelSx, color: '#9A3412', mb: 0.5 }}>Send-back reason</Typography>
          <Box
            sx={{
              px: 1.25,
              py: 1,
              borderRadius: 0.75,
              bgcolor: '#FFFFFF',
              border: `1px solid ${brand.goldMid}`,
            }}
          >
            <Typography sx={{ fontSize: '0.8125rem', color: brand.ink, ...narrativeReadScrollSx, lineHeight: 1.5 }}>
              {text}
            </Typography>
          </Box>
        </Box>
      ) : null}
    </Box>
  )
}
