import { Box, MenuItem, Select, Typography, type SelectChangeEvent } from '@mui/material'
import { useState, type ReactNode } from 'react'
import FormFieldLabel, { compactSelectSx } from './FormFieldLabel'
import { fieldId } from './CompactTextField'
import { COMPACT_SELECT_MAX_WIDTH, COMPACT_SELECT_MIN_WIDTH } from './formLayout'
import { brand } from '../../theme'
import { RATING_TONE_COLOR, type RatingTone } from './ratingTone'

export type FormSelectOption<T extends string> = {
  value: T
  label: string
  /** Green / amber / red swatch for impact, severity, priority. */
  tone?: RatingTone
}

function ToneDot({ tone }: { tone?: RatingTone }) {
  if (!tone) return null
  return (
    <Box
      component="span"
      sx={{
        width: 8,
        height: 8,
        borderRadius: '50%',
        bgcolor: RATING_TONE_COLOR[tone],
        flexShrink: 0,
      }}
    />
  )
}

/** Compact select with visible caption label (kit Organisation pattern). */
export default function FormSelect<T extends string>({
  label,
  value,
  options,
  onChange,
  fullWidth,
  minWidth,
  maxWidth,
  disabled,
  required,
  wrapLabel,
  hint,
  error,
  helperText,
  placeholder,
  'data-testid': dataTestId,
}: {
  label: string
  value: T
  options: FormSelectOption<T>[]
  onChange: (value: T) => void
  fullWidth?: boolean
  minWidth?: number
  maxWidth?: number
  disabled?: boolean
  required?: boolean
  wrapLabel?: boolean
  hint?: ReactNode
  error?: boolean
  helperText?: string
  /** Shown in the closed control when value is empty. Not listed as a choice. */
  placeholder?: string
  'data-testid'?: string
}) {
  const id = fieldId(label, 'fs')
  const handle = (e: SelectChangeEvent<T>) => onChange(e.target.value as T)
  const [menuWidth, setMenuWidth] = useState(0)
  const [menuMaxHeight, setMenuMaxHeight] = useState(320)
  const [openAbove, setOpenAbove] = useState(false)

  const placeMenu = () => {
    const el = document.getElementById(id)
    if (!el) return
    const rect = el.getBoundingClientRect()
    setMenuWidth(el.offsetWidth)
    const spaceBelow = window.innerHeight - rect.bottom - 16
    const spaceAbove = rect.top - 16
    const above = spaceBelow < 160 && spaceAbove > spaceBelow
    setOpenAbove(above)
    const room = above ? spaceAbove : spaceBelow
    setMenuMaxHeight(Math.max(96, Math.min(320, room)))
  }

  return (
    <Box
      sx={{
        width: fullWidth ? '100%' : 'max-content',
        minWidth: fullWidth ? 0 : (minWidth ?? 0),
        maxWidth: fullWidth ? undefined : (maxWidth ?? COMPACT_SELECT_MAX_WIDTH),
      }}
    >
      <FormFieldLabel htmlFor={id} required={required} wrap={wrapLabel} hint={hint}>
        {label}
      </FormFieldLabel>
      <Select
        id={id}
        size="small"
        fullWidth={!!fullWidth}
        disabled={disabled}
        value={value}
        onChange={handle}
        displayEmpty
        notched={false}
        error={error}
        onOpen={placeMenu}
        sx={{
          ...compactSelectSx,
          minWidth: fullWidth ? 0 : (minWidth ?? COMPACT_SELECT_MIN_WIDTH),
          width: fullWidth ? '100%' : 'max-content',
          maxWidth: fullWidth ? undefined : (maxWidth ?? COMPACT_SELECT_MAX_WIDTH),
          ...(error
            ? {
                '& .MuiOutlinedInput-notchedOutline': { borderColor: '#DC2626' },
              }
            : {}),
        }}
        {...(disabled ? { IconComponent: () => null } : {})}
        MenuProps={{
          disablePortal: false,
          marginThreshold: null,
          anchorOrigin: { vertical: openAbove ? 'top' : 'bottom', horizontal: 'left' },
          transformOrigin: { vertical: openAbove ? 'bottom' : 'top', horizontal: 'left' },
          PaperProps: {
            sx: {
              maxHeight: menuMaxHeight,
              boxSizing: 'border-box',
              ...(menuWidth > 0
                ? { width: menuWidth, maxWidth: menuWidth }
                : {}),
            },
          },
        }}
        SelectDisplayProps={{
          ...(dataTestId ? { 'data-testid': dataTestId } : {}),
          title: options.find((o) => o.value === value)?.label ?? '',
        } as object}
        renderValue={(selected) => {
          const current = options.find((o) => o.value === selected)
          if (!current) {
            return (
              <Box component="span" sx={{ color: brand.mutedLight }}>
                {placeholder ?? ''}
              </Box>
            )
          }
          return (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, minWidth: 0 }} title={current.label}>
              <ToneDot tone={current.tone} />
              <Box
                component="span"
                sx={{
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                  minWidth: 0,
                }}
              >
                {current.label}
              </Box>
            </Box>
          )
        }}
      >
        {placeholder ? <MenuItem value="" sx={{ display: 'none' }} /> : null}
        {options.map((o) => (
          <MenuItem
            key={o.value || '__empty'}
            value={o.value}
            sx={{ fontSize: '0.75rem', gap: 1, whiteSpace: 'normal', alignItems: 'flex-start' }}
          >
            <ToneDot tone={o.tone} />
            {o.label}
          </MenuItem>
        ))}
      </Select>
      {helperText ? (
        <Typography sx={{ fontSize: '0.6875rem', color: error ? '#DC2626' : brand.mutedLight, mt: 0.35, lineHeight: 1.35 }}>
          {helperText}
        </Typography>
      ) : null}
    </Box>
  )
}
