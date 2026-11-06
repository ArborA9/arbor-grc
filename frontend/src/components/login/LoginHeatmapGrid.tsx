import { Box } from '@mui/material'
import { semantic } from '../../theme'

/** Decorative 3×3 risk heatmap: illustrative counts only. */
const CELLS: { bg: string; text: string; count: number }[] = [
  { bg: semantic.heatmap.low, text: '#065F46', count: 2 },
  { bg: semantic.heatmap.medium, text: '#92400E', count: 5 },
  { bg: semantic.heatmap.high, text: '#fff', count: 3 },
  { bg: semantic.heatmap.medHigh, text: '#92400E', count: 8 },
  { bg: semantic.heatmap.medium, text: '#92400E', count: 12 },
  { bg: semantic.heatmap.low, text: '#065F46', count: 4 },
  { bg: semantic.heatmap.veryLow, text: '#64748B', count: 1 },
  { bg: semantic.heatmap.medium, text: '#92400E', count: 6 },
  { bg: semantic.heatmap.high, text: '#fff', count: 2 },
]

export default function LoginHeatmapGrid() {
  return (
    <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(3, 22px)', gap: 0.5 }}>
      {CELLS.map((cell, i) => (
        <Box
          key={i}
          sx={{
            width: 22,
            height: 22,
            borderRadius: 0.5,
            bgcolor: cell.bg,
            opacity: 0.92,
            display: 'grid',
            placeItems: 'center',
            fontSize: '0.5rem',
            fontWeight: 700,
            lineHeight: 1,
            color: cell.text,
            fontVariantNumeric: 'tabular-nums',
          }}
        >
          {cell.count}
        </Box>
      ))}
    </Box>
  )
}
