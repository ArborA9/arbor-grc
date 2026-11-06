import { type ReactNode } from 'react'
import { Box, Card } from '@mui/material'
import { brand } from '../../theme'
import { SIMPLE_FORM_MAX_WIDTH } from './formLayout'

export type SimpleFormLayoutProps = {
  children: ReactNode
  /** Footer row (Cancel / Save): rendered below children inside the width constraint. */
  actions?: ReactNode
  /** Default 640: kit simple-form column. */
  maxWidth?: number
}

/**
 * Centered simple CRUD shell: Assets, KRI create, BCMS dept profile pattern.
 * Pair with SettingsCard compact + CompactTextField / FormSelect inside.
 */
export default function SimpleFormLayout({
  children,
  actions,
  maxWidth = SIMPLE_FORM_MAX_WIDTH,
}: SimpleFormLayoutProps) {
  return (
    <Card
      sx={{
        flex: 1,
        minHeight: 0,
        overflow: 'auto',
        p: { xs: 1.25, sm: 1.5 },
        bgcolor: brand.canvas,
        boxShadow: brand.shadowCard,
      }}
    >
      <Box sx={{ maxWidth, mx: 'auto' }}>
        {children}
        {actions ? <Box sx={{ mt: 1.25 }}>{actions}</Box> : null}
      </Box>
    </Card>
  )
}
