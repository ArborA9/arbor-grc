import { Box } from '@mui/material'
import { brand } from '../../theme'

/** Kit gold “A” mark: sidebar, login, marketing panels. */
export default function LogoMark({ size = 30 }: { size?: number }) {
  return (
    <Box
      sx={{
        width: size,
        height: size,
        borderRadius: 1,
        display: 'grid',
        placeItems: 'center',
        bgcolor: brand.gold,
        color: '#fff',
        fontSize: size * 0.47,
        fontWeight: 800,
        letterSpacing: '-0.5px',
        flexShrink: 0,
        boxShadow: '0 1px 3px rgba(146, 83, 10, 0.28)',
      }}
    >
      A
    </Box>
  )
}
