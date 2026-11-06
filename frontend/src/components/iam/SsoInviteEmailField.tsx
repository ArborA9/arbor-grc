import { useEffect, useState } from 'react'
import { Autocomplete, Box, TextField, Typography } from '@mui/material'
import { api, type EmployeeDirectoryEntry } from '../../api'
import FormFieldLabel, { compactInputSx } from '../ui/FormFieldLabel'

/**
 * SSO invite email: suggestions from Entra as the operator types a name or address.
 * Selecting a row fills the address; the parent copies name and department.
 */
export default function SsoInviteEmailField({
  value,
  onChange,
  onPick,
  error,
  helperText,
  onBlur,
  disabled,
}: {
  value: string
  onChange: (email: string) => void
  onPick: (person: EmployeeDirectoryEntry) => void
  error?: boolean
  helperText?: string
  onBlur?: () => void
  disabled?: boolean
}) {
  const [options, setOptions] = useState<EmployeeDirectoryEntry[]>([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)

  useEffect(() => {
    const q = value.trim()
    if (q.length < 2) {
      setOptions([])
      setLoading(false)
      return
    }
    let cancelled = false
    setLoading(true)
    const timer = window.setTimeout(() => {
      void api.searchEmployees(q).then((res) => {
        if (cancelled) return
        setOptions(res.available ? res.results : [])
      }).catch(() => {
        if (!cancelled) setOptions([])
      }).finally(() => {
        if (!cancelled) setLoading(false)
      })
    }, 200)
    return () => {
      cancelled = true
      window.clearTimeout(timer)
    }
  }, [value])

  return (
    <Box sx={{ minWidth: 0 }}>
      <FormFieldLabel htmlFor="invite-email" required>
        Email
      </FormFieldLabel>
      <Autocomplete
        freeSolo
        id="invite-email"
        disabled={disabled}
        open={open && value.trim().length >= 2}
        onOpen={() => setOpen(true)}
        onClose={() => setOpen(false)}
        options={options}
        loading={loading}
        filterOptions={(rows) => rows}
        inputValue={value}
        getOptionLabel={(option) => (typeof option === 'string' ? option : option.email || option.displayName)}
        isOptionEqualToValue={(a, b) =>
          typeof a !== 'string' && typeof b !== 'string' && a.directoryId === b.directoryId
        }
        noOptionsText={loading ? 'Searching Entra…' : 'No matching people'}
        onInputChange={(_, next, reason) => {
          if (reason === 'input' || reason === 'clear') onChange(next)
        }}
        onChange={(_, next) => {
          if (next && typeof next !== 'string') onPick(next)
        }}
        forcePopupIcon={false}
        clearOnBlur={false}
        slotProps={{
          popper: { sx: { minWidth: 320 } },
        }}
        renderOption={(props, option) => {
          const { key, ...rest } = props
          const dept = option.department?.trim()
          return (
            <Box component="li" key={key} {...rest} sx={{ display: 'block !important', py: 0.75 }}>
              <Typography variant="body2" sx={{ fontSize: '0.8125rem', fontWeight: 650, lineHeight: 1.3 }}>
                {option.displayName || option.email || '-'}
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', lineHeight: 1.3 }}>
                {[option.email, dept].filter(Boolean).join(' · ') || '-'}
              </Typography>
            </Box>
          )
        }}
        renderInput={(params) => (
          <TextField
            {...params}
            id="invite-email"
            placeholder="Name or email"
            error={error}
            helperText={helperText}
            onBlur={onBlur}
            autoComplete="off"
            sx={compactInputSx}
          />
        )}
      />
    </Box>
  )
}
