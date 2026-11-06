import { Box, Button, Card, Stack, Typography } from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { brand } from '../theme'

export default function FocusModeToolbar({
  focus,
  backLabel,
  onBack,
  title,
  subtitle,
  actions,
}: {
  focus: boolean
  backLabel?: string
  onBack?: () => void
  title: string
  subtitle?: string
  actions?: React.ReactNode
}) {
  return (
    <Card sx={{ px: 2, py: 1.25, flexShrink: 0 }}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'center' }}>
        {focus && onBack ? (
          <Button
            size="small"
            variant="outlined"
            startIcon={<ArrowBackIcon fontSize="small" />}
            onClick={onBack}
            sx={{ fontWeight: 700, borderColor: brand.accent, color: brand.accent }}
          >
            {backLabel ?? 'Back'}
          </Button>
        ) : (
          <>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="subtitle1">{title}</Typography>
              {subtitle && (
                <Typography variant="caption" color="text.secondary">
                  {subtitle}
                </Typography>
              )}
            </Box>
            {actions}
          </>
        )}
      </Stack>
    </Card>
  )
}
