import Box from '@mui/material/Box'
import FormControl from '@mui/material/FormControl'
import FormControlLabel from '@mui/material/FormControlLabel'
import FormLabel from '@mui/material/FormLabel'
import Radio from '@mui/material/Radio'
import RadioGroup from '@mui/material/RadioGroup'
import Typography from '@mui/material/Typography'
import { brand } from '../../theme'
import {
  SAMA_GATE_LABELS,
  SAMA_MATURITY_LABELS,
  type GateAnswer,
  maturityFromGates,
  samaMaturityTone,
} from './samaMaturityGates'

export default function SamaMaturityGatesEditor({
  gates,
  onChange,
  disabled,
  showComputed = true,
}: {
  gates: GateAnswer[]
  onChange: (next: GateAnswer[]) => void
  disabled?: boolean
  showComputed?: boolean
}) {
  const derived = maturityFromGates(gates)

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.25 }}>
      <Typography sx={{ fontSize: '0.75rem', color: brand.muted, lineHeight: 1.4 }}>
        Answer Yes/No for each evidence gate (SAMA maturity workbook). Maturity is the count of
        consecutive Yes answers from Level 1 (target 3).
      </Typography>
      {SAMA_GATE_LABELS.map((label, i) => (
        <FormControl key={i} component="fieldset" disabled={disabled} sx={{ m: 0 }}>
          <FormLabel
            sx={{
              fontSize: '0.6875rem',
              color: brand.ink,
              fontWeight: 600,
              mb: 0.25,
              '&.Mui-focused': { color: brand.ink },
            }}
          >
            {label}
          </FormLabel>
          <RadioGroup
            row
            value={gates[i] === true ? 'yes' : gates[i] === false ? 'no' : ''}
            onChange={(e) => {
              const v = e.target.value
              const next = [...gates] as GateAnswer[]
              next[i] = v === 'yes' ? true : v === 'no' ? false : null
              onChange(next)
            }}
          >
            <FormControlLabel
              value="yes"
              control={<Radio size="small" />}
              label="Yes"
              sx={{ '& .MuiFormControlLabel-label': { fontSize: '0.75rem' } }}
            />
            <FormControlLabel
              value="no"
              control={<Radio size="small" />}
              label="No"
              sx={{ '& .MuiFormControlLabel-label': { fontSize: '0.75rem' } }}
            />
          </RadioGroup>
        </FormControl>
      ))}
      {showComputed && (
        <Typography sx={{ fontSize: '0.8125rem', fontWeight: 600, color: brand.ink }}>
          Computed maturity:{' '}
          {derived == null ? (
            '-'
          ) : (
            <Box
              component="span"
              sx={{
                display: 'inline-block',
                px: 0.75,
                py: 0.15,
                borderRadius: '4px',
                bgcolor: samaMaturityTone(derived).bg,
                color: samaMaturityTone(derived).text,
                fontWeight: 700,
              }}
            >
              {SAMA_MATURITY_LABELS[derived] ?? String(derived)}
            </Box>
          )}
        </Typography>
      )}
    </Box>
  )
}
