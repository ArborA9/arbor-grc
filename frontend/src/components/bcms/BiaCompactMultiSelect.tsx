import { Autocomplete, Box, TextField, Typography } from '@mui/material'
import FormFieldLabel, { compactInputSx, CONTROL_HEIGHT } from '../ui/FormFieldLabel'
import CompactOverflowChips from './CompactOverflowChips'

export default function BiaCompactMultiSelect({
  id,
  label,
  required,
  options,
  value,
  onChange,
  disabled,
  error,
  helperText,
  emptyPlaceholder = 'Select one or more',
  noun = 'items',
}: {
  id?: string
  label: string
  required?: boolean
  options: string[]
  value: string[]
  onChange: (next: string[]) => void
  disabled?: boolean
  error?: boolean
  helperText?: string
  emptyPlaceholder?: string
  noun?: string
}) {
  return (
    <Box sx={{ width: '100%', minWidth: 0 }}>
      <FormFieldLabel htmlFor={id} required={required} wrap>
        {label}
      </FormFieldLabel>
      {disabled ? (
        value.length === 0 ? (
          <Typography variant="body2" sx={{ minHeight: CONTROL_HEIGHT, display: 'flex', alignItems: 'center' }}>
            -
          </Typography>
        ) : (
          <CompactOverflowChips
            items={value.map((v) => ({ key: v, label: v }))}
            disabled
            noun={noun}
          />
        )
      ) : (
        <>
          <Autocomplete
            multiple
            options={options}
            value={value}
            onChange={(_, next) => onChange(next)}
            disableCloseOnSelect
            renderTags={() => null}
            renderInput={(params) => (
              <TextField
                {...params}
                id={id}
                placeholder={value.length ? `${value.length} selected. Search to add` : emptyPlaceholder}
                error={error}
                helperText={helperText}
                size="small"
                sx={{
                  ...compactInputSx,
                  '& .MuiOutlinedInput-root': {
                    flexWrap: 'nowrap',
                    height: CONTROL_HEIGHT,
                    minHeight: CONTROL_HEIGHT,
                    maxHeight: CONTROL_HEIGHT,
                  },
                }}
              />
            )}
          />
          <CompactOverflowChips
            items={value.map((v) => ({ key: v, label: v }))}
            noun={noun}
            onRemove={(key) => onChange(value.filter((v) => v !== key))}
          />
        </>
      )}
    </Box>
  )
}
