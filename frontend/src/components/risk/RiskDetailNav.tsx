import {
  FormControl,
  IconButton,
  MenuItem,
  Select,
  Stack,
  Typography,
} from '@mui/material'
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft'
import ChevronRightIcon from '@mui/icons-material/ChevronRight'
import type { RiskSummary } from '../../api'

export default function RiskDetailNav({
  rows,
  detail,
  detailIndex,
  onSelectRisk,
}: {
  rows: RiskSummary[]
  detail: RiskSummary
  detailIndex: number
  onSelectRisk: (id: number) => void
}) {
  return (
    <Stack direction="row" spacing={0.25} alignItems="center">
      <IconButton
        size="small"
        disabled={detailIndex <= 0}
        onClick={() => {
          const prev = rows[detailIndex - 1]
          if (prev) onSelectRisk(prev.id)
        }}
        aria-label="Previous risk"
      >
        <ChevronLeftIcon />
      </IconButton>
      <FormControl size="small" sx={{ minWidth: { xs: 100, sm: 88 } }}>
        <Select
          value={detail.id}
          onChange={(e) => onSelectRisk(Number(e.target.value))}
          renderValue={() => (
            <Typography variant="caption" noWrap>
              {detailIndex + 1} / {rows.length}
            </Typography>
          )}
        >
          {rows.map((r) => (
            <MenuItem key={r.id} value={r.id}>
              <Stack direction="row" spacing={1} alignItems="center" sx={{ width: '100%' }}>
                <Typography variant="caption" fontWeight={700} sx={{ minWidth: 72 }}>
                  {r.displayId}
                </Typography>
                <Typography variant="body2" noWrap sx={{ flex: 1 }}>
                  {r.title}
                </Typography>
              </Stack>
            </MenuItem>
          ))}
        </Select>
      </FormControl>
      <IconButton
        size="small"
        disabled={detailIndex < 0 || detailIndex >= rows.length - 1}
        onClick={() => {
          const next = rows[detailIndex + 1]
          if (next) onSelectRisk(next.id)
        }}
        aria-label="Next risk"
      >
        <ChevronRightIcon />
      </IconButton>
    </Stack>
  )
}
