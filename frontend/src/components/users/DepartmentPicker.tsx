import { Box, Checkbox, FormControlLabel, FormGroup, Typography } from '@mui/material'
import type { OrgUnitNode } from '../../api'
import { brand } from '../../theme'

/** Departments a Contributor works in. Shown when the Contributor role is selected. */
export default function DepartmentPicker(props: {
  units: OrgUnitNode[]
  selected: number[]
  hint: string
  disabled?: boolean
  onChange: (next: number[]) => void
}) {
  const { units, selected, hint, disabled, onChange } = props
  return (
    <Box sx={{ p: 1.25, borderRadius: 1, bgcolor: brand.canvas, border: `1px solid ${brand.hairline}` }}>
      <Typography variant="body2" fontWeight={650}>
        Department <Box component="span" sx={{ color: 'error.main' }}>*</Box>
      </Typography>
      <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.75 }}>
        {hint}
      </Typography>
      <Box sx={{ maxHeight: 140, overflow: 'auto', pr: 0.5 }}>
        <FormGroup>
          {units.map((u) => (
            <FormControlLabel
              key={u.id}
              sx={{ my: 0 }}
              control={
                <Checkbox
                  size="small"
                  disabled={disabled}
                  checked={selected.includes(u.id)}
                  onChange={() =>
                    onChange(selected.includes(u.id) ? selected.filter((x) => x !== u.id) : [...selected, u.id])
                  }
                />
              }
              label={<Typography variant="caption">{u.name}</Typography>}
            />
          ))}
          {units.length === 0 && (
            <Typography variant="caption" color="warning.main">
              Add a department under Organisation, Structure first.
            </Typography>
          )}
        </FormGroup>
      </Box>
    </Box>
  )
}
