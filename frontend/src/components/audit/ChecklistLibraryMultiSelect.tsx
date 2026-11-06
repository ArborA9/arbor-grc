import { Autocomplete, Box, TextField } from '@mui/material'
import FormFieldLabel, { compactInputSx, CONTROL_HEIGHT } from '../ui/FormFieldLabel'
import CompactOverflowChips from '../bcms/CompactOverflowChips'

export default function ChecklistLibraryMultiSelect({
  id,
  label,
  required,
  options,
  value,
  onChange,
  error,
  helperText,
}: {
  id?: string
  label: string
  required?: boolean
  options: { id: number; label: string }[]
  value: number[]
  onChange: (next: number[]) => void
  error?: boolean
  helperText?: string
}) {
  const selected = options.filter((o) => value.includes(o.id))
  return (
    <Box sx={{ width: '100%', minWidth: 0 }}>
      <FormFieldLabel htmlFor={id} required={required} wrap>
        {label}
      </FormFieldLabel>
      <Autocomplete
        multiple
        options={options}
        value={selected}
        onChange={(_, next) => onChange(next.map((o) => o.id))}
        disableCloseOnSelect
        getOptionLabel={(o) => o.label}
        isOptionEqualToValue={(a, b) => a.id === b.id}
        renderTags={() => null}
        renderInput={(params) => (
          <TextField
            {...params}
            id={id}
            placeholder={value.length ? `${value.length} selected. Search to add` : 'Select checklists'}
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
        items={selected.map((o) => ({ key: String(o.id), label: o.label }))}
        noun="checklists"
        onRemove={(key) => onChange(value.filter((id) => String(id) !== key))}
      />
    </Box>
  )
}
