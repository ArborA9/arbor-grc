import { useState, type ReactNode } from 'react'
import { Box, IconButton, InputAdornment, TextField, Typography, type SxProps, type TextFieldProps, type Theme } from '@mui/material'
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined'
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined'
import { brand } from '../../theme'
import FormFieldLabel, { compactInputSx } from './FormFieldLabel'
import { inferInputLimit } from './inputLimits'
import { DATE_FIELD_WIDTH, DATETIME_FIELD_WIDTH, NARRATIVE_MAX_ROWS } from './formLayout'

export function fieldId(label: string, prefix = 'field') {
  return `${prefix}-${label.replace(/[^a-z0-9]+/gi, '-').toLowerCase()}`
}

function htmlInputMaxLength(slotProps: TextFieldProps['slotProps']): number | undefined {
  const html = slotProps?.htmlInput
  if (html && typeof html === 'object' && 'maxLength' in html && typeof html.maxLength === 'number') {
    return html.maxLength
  }
  return undefined
}

const UNBOUNDED_TYPES = new Set(['date', 'datetime-local', 'time', 'number', 'file'])

/** Kit-style text input: caption label always visible above control. */
export default function CompactTextField({
  label,
  required,
  id: idProp,
  helperText,
  fieldIdPrefix,
  maxWidth,
  wrapLabel,
  labelColor,
  maxLength: maxLengthProp,
  showPasswordToggle,
  hint,
  hintPlacement,
  onFocus,
  onBlur,
  ...tfProps
}: TextFieldProps & {
  label: string
  fieldIdPrefix?: string
  maxWidth?: number | string
  wrapLabel?: boolean
  /** Caption color when the label uses the uppercase section style. */
  labelColor?: string
  hint?: ReactNode
  /** `inline` keeps the hint next to the label. Default parks it on the far side of the caption. */
  hintPlacement?: 'end' | 'inline'
  /** `false` disables the cap. Omit to infer from label / multiline. */
  maxLength?: number | false
  showPasswordToggle?: boolean
}) {
  const [focused, setFocused] = useState(false)
  const [revealPassword, setRevealPassword] = useState(false)
  const id = idProp ?? fieldId(label, fieldIdPrefix)
  const raw = tfProps.value
  const text = raw == null ? '' : String(raw)
  const inputType = showPasswordToggle ? (revealPassword ? 'text' : 'password') : tfProps.type
  const dateFieldWidth =
    inputType === 'date' ? DATE_FIELD_WIDTH : inputType === 'datetime-local' ? DATETIME_FIELD_WIDTH : undefined
  const unboundedType = inputType != null && UNBOUNDED_TYPES.has(inputType)
  const limit =
    maxLengthProp === false || unboundedType
      ? undefined
      : (maxLengthProp ?? htmlInputMaxLength(tfProps.slotProps) ?? inferInputLimit(label, tfProps.multiline))
  const len = [...text].length
  const atMax = limit != null && len >= limit
  /** Floor of 8 would make a 6-digit OTP always show 0/6 and drop sibling buttons. */
  const shortLimit = limit != null && limit <= 12
  const warnWindow =
    limit == null || shortLimit ? 0 : Math.max(8, Math.min(40, Math.ceil(limit * 0.1)))
  const nearMax = limit != null && !atMax && warnWindow > 0 && len >= limit - warnWindow
  const showCount =
    limit != null &&
    !shortLimit &&
    (atMax || nearMax || (Boolean(tfProps.multiline) && (focused || len > 0)))

  const legacyInputProps =
    tfProps.inputProps && typeof tfProps.inputProps === 'object' ? tfProps.inputProps : {}
  const passedHtmlInput =
    typeof tfProps.slotProps?.htmlInput === 'object' && tfProps.slotProps.htmlInput != null
      ? tfProps.slotProps.htmlInput
      : {}
  const multilineLocked = Boolean(tfProps.multiline && tfProps.disabled)
  const resolvedMaxRows = tfProps.multiline ? (tfProps.maxRows ?? NARRATIVE_MAX_ROWS) : tfProps.maxRows
  const rowCap =
    typeof resolvedMaxRows === 'number'
      ? resolvedMaxRows
      : typeof resolvedMaxRows === 'string'
        ? Number(resolvedMaxRows)
        : undefined
  const lineCap =
    rowCap && Number.isFinite(rowCap) ? `calc(0.75rem * 1.4375 * ${rowCap})` : undefined
  const multilineCapSx = lineCap
    ? {
        '& .MuiInputBase-inputMultiline': {
          maxHeight: `${lineCap} !important`,
          overflowY: 'auto !important',
          pointerEvents: 'auto',
        },
      }
    : undefined

  const statusColor = tfProps.error ? '#DC2626' : atMax ? '#B45309' : brand.mutedLight
  const statusMessage = tfProps.error
    ? helperText
    : atMax && !shortLimit
      ? `Maximum ${limit} characters: cannot add more`
      : helperText

  return (
    <Box
      sx={{
        // Date inputs stay a fixed width. The caption may be wider than the box.
        width: dateFieldWidth != null ? 'max-content' : '100%',
        minWidth: 0,
        maxWidth: dateFieldWidth != null ? '100%' : maxWidth,
        flex: dateFieldWidth != null ? '0 0 auto' : undefined,
      }}
    >
      <FormFieldLabel
        htmlFor={id}
        required={required}
        wrap={wrapLabel}
        hint={hint}
        hintPlacement={hintPlacement}
        color={labelColor}
      >
        {label}
      </FormFieldLabel>
      <TextField
        {...tfProps}
        id={id}
        type={inputType}
        size="small"
        margin="none"
        fullWidth={dateFieldWidth == null}
        hiddenLabel
        maxRows={resolvedMaxRows}
        disabled={multilineLocked ? false : tfProps.disabled}
        onFocus={(e) => {
          setFocused(true)
          onFocus?.(e)
        }}
        onBlur={(e) => {
          setFocused(false)
          onBlur?.(e)
        }}
        onChange={(e) => {
          if (multilineLocked) return
          if (limit != null) {
            const next = [...e.target.value].slice(0, limit).join('')
            if (next !== e.target.value) e.target.value = next
          }
          tfProps.onChange?.(e)
        }}
        slotProps={{
          ...tfProps.slotProps,
          input: {
            notched: false,
            ...(typeof tfProps.slotProps?.input === 'object' ? tfProps.slotProps.input : {}),
            ...(showPasswordToggle
              ? {
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        size="small"
                        aria-label={revealPassword ? 'Hide password' : 'Show password'}
                        onClick={() => setRevealPassword((v) => !v)}
                        edge="end"
                      >
                        {revealPassword ? (
                          <VisibilityOffOutlinedIcon fontSize="small" />
                        ) : (
                          <VisibilityOutlinedIcon fontSize="small" />
                        )}
                      </IconButton>
                    </InputAdornment>
                  ),
                }
              : {}),
          },
          htmlInput: {
            dir: tfProps.multiline ? 'auto' : undefined,
            maxLength: limit,
            ...legacyInputProps,
            ...passedHtmlInput,
            ...(lineCap
              ? {
                  style: {
                    ...(typeof passedHtmlInput === 'object' &&
                    passedHtmlInput &&
                    'style' in passedHtmlInput &&
                    passedHtmlInput.style &&
                    typeof passedHtmlInput.style === 'object'
                      ? passedHtmlInput.style
                      : {}),
                    maxHeight: lineCap,
                    overflowY: 'auto',
                  },
                }
              : {}),
            readOnly:
              multilineLocked ||
              Boolean('readOnly' in passedHtmlInput && passedHtmlInput.readOnly),
          },
        }}
        sx={
          [
            compactInputSx,
            dateFieldWidth != null ? { width: dateFieldWidth, maxWidth: dateFieldWidth } : undefined,
            multilineCapSx,
            multilineLocked
              ? {
                  '& .MuiOutlinedInput-root': { bgcolor: brand.tableHeader },
                  '& .MuiInputBase-input': { cursor: 'default' },
                }
              : undefined,
            tfProps.sx,
          ] as SxProps<Theme>
        }
      />
      {(statusMessage || showCount) && (
        <Box
          sx={{
            display: 'flex',
            alignItems: 'flex-start',
            justifyContent: 'space-between',
            gap: 1,
            mt: 0.35,
          }}
        >
          {statusMessage ? (
            <Typography sx={{ fontSize: '0.6875rem', color: statusColor, lineHeight: 1.35, minWidth: 0 }}>
              {statusMessage}
            </Typography>
          ) : (
            <span />
          )}
          {showCount && limit != null ? (
            <Typography
              sx={{
                fontSize: '0.6875rem',
                color: atMax ? '#B45309' : nearMax ? brand.goldDark : brand.mutedLight,
                lineHeight: 1.35,
                flexShrink: 0,
                fontVariantNumeric: 'tabular-nums',
              }}
              aria-live="polite"
            >
              {len} / {limit}
            </Typography>
          ) : null}
        </Box>
      )}
    </Box>
  )
}
