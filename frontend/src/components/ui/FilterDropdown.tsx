import type { ReactNode } from 'react'
import { Box, FormControl, MenuItem, Select } from '@mui/material'
import { brand } from '../../theme'

export type FilterDropdownOption<T extends string> = {
  value: T
  label: string
}

/** Kit-style filter select: visible `Label: value` in the control (UI Kit Screen 3). */
export default function FilterDropdown<T extends string>({
  label,
  value,
  options,
  onChange,
  minWidth = 150,
  hideLabel,
  startIcon,
}: {
  label: string
  value: T
  options: FilterDropdownOption<T>[]
  onChange: (value: T) => void
  minWidth?: number
  /** Show only the current option (icon toolbars). `label` stays the aria-label. */
  hideLabel?: boolean
  startIcon?: ReactNode
}) {
  const current = options.find((o) => o.value === value)?.label ?? options[0]?.label ?? 'All'

  return (
    <FormControl size="small" sx={{ minWidth }}>
      <Select
        value={value}
        onChange={(e) => onChange(e.target.value as T)}
        displayEmpty
        inputProps={{ 'aria-label': label }}
        renderValue={() => (
          <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5 }}>
            {startIcon}
            {!hideLabel && (
              <Box component="span" sx={{ color: brand.muted, fontWeight: 500 }}>
                {label}:
              </Box>
            )}
            <Box component="span" sx={{ fontWeight: 600, color: brand.ink }}>
              {current}
            </Box>
          </Box>
        )}
        sx={{
          fontSize: '0.75rem',
          height: 32,
          bgcolor: brand.hover,
          '& .MuiSelect-select': {
            py: 0,
            display: 'flex',
            alignItems: 'center',
            fontSize: '0.75rem',
          },
          '& .MuiOutlinedInput-notchedOutline': { borderColor: brand.border },
        }}
      >
        {options.map((o) => (
          <MenuItem key={o.value} value={o.value} sx={{ fontSize: '0.75rem' }}>
            {o.label}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  )
}
