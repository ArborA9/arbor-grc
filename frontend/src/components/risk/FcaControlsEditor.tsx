import { Box, Stack, Typography } from '@mui/material'
import FormFieldLabel from '../ui/FormFieldLabel'
import FormSelect from '../ui/FormSelect'
import {
  FCA_CONTROL_AXIS_LABEL,
  fcaControlScore,
  fcaControlWord,
  type FcaControlAxis,
} from './fcaRiskMatrix'

const AXIS_OPTIONS = (Object.keys(FCA_CONTROL_AXIS_LABEL) as FcaControlAxis[]).map((v) => ({
  value: v,
  label: FCA_CONTROL_AXIS_LABEL[v],
}))

export function fcaScoreToRiskLevel(score: number): 'VERY_LOW' | 'LOW' | 'MEDIUM' | 'HIGH' | 'VERY_HIGH' {
  switch (score) {
    case 1:
      return 'VERY_LOW'
    case 2:
      return 'LOW'
    case 3:
      return 'MEDIUM'
    case 4:
      return 'HIGH'
    default:
      return 'VERY_HIGH'
  }
}

/** Two closed Design / Performance selects keyed into the 3×3 FCA control grid. */
export function FcaControlsEditor({
  design,
  performance,
  onDesignChange,
  onPerformanceChange,
  disabled,
}: {
  design: FcaControlAxis
  performance: FcaControlAxis
  onDesignChange: (v: FcaControlAxis) => void
  onPerformanceChange: (v: FcaControlAxis) => void
  disabled?: boolean
}) {
  const score = fcaControlScore(design, performance)

  return (
    <Stack spacing={1.25}>
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: 'repeat(2, minmax(0, max-content))',
          columnGap: 1.25,
          alignItems: 'start',
        }}
      >
        <FormSelect
          label="Design"
          wrapLabel
          minWidth={168}
          maxWidth={200}
          value={design}
          options={AXIS_OPTIONS}
          disabled={disabled}
          onChange={(v) => onDesignChange(v as FcaControlAxis)}
        />
        <FormSelect
          label="Performance"
          wrapLabel
          minWidth={168}
          maxWidth={200}
          value={performance}
          options={AXIS_OPTIONS}
          disabled={disabled}
          onChange={(v) => onPerformanceChange(v as FcaControlAxis)}
        />
      </Box>
      <Box>
        <FormFieldLabel wrap>Control Effectiveness</FormFieldLabel>
        <Box sx={{ minHeight: 32, display: 'flex', alignItems: 'center' }}>
          <Typography variant="body2">{`${score} · ${fcaControlWord(score)}`}</Typography>
        </Box>
      </Box>
    </Stack>
  )
}
