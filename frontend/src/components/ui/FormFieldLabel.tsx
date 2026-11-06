import { type ReactNode } from 'react'
import { Box, Typography, type SxProps, type Theme } from '@mui/material'
import { brand } from '../../theme'

/** Matches theme.ts INPUT_HEIGHT: text, select, and small buttons share this. */
export const CONTROL_HEIGHT = 32

/** Product type floor: never smaller on forms, tables, or chrome. */
export const TYPE_CAPTION = '0.6875rem'
export const TYPE_CONTROL = '0.75rem'
/** Value under a read-only caption (dossier / review drawers). */
export const TYPE_VALUE = '0.875rem'

/** Sentence-case caption above a read-only value. Prefer this over 9px shouty uppercase. */
export const readOnlyCaptionSx: SxProps<Theme> = {
  display: 'block',
  fontSize: TYPE_CAPTION,
  fontWeight: 650,
  color: brand.muted,
  mb: 0.35,
  lineHeight: 1.3,
}

/**
 * One control chrome for outlined text and select.
 * `notched={false}` + hidden legend removes the 5px outline offset that
 * makes TextField sit lower than Select in the same row.
 */
export const compactControlSx: SxProps<Theme> = {
  boxSizing: 'border-box',
  height: CONTROL_HEIGHT,
  minHeight: CONTROL_HEIGHT,
  maxHeight: CONTROL_HEIGHT,
  fontSize: '0.75rem',
  bgcolor: brand.hover,
  '& .MuiOutlinedInput-notchedOutline': { borderColor: brand.border, top: 0 },
  '& legend': { display: 'none' },
  '& .MuiSelect-select, & .MuiOutlinedInput-input': {
    boxSizing: 'border-box',
    height: CONTROL_HEIGHT - 2,
    minHeight: CONTROL_HEIGHT - 2,
    py: 0,
    px: 1.25,
    display: 'flex',
    alignItems: 'center',
    fontSize: '0.78125rem',
    fontWeight: 400,
    color: brand.ink,
    WebkitTextFillColor: brand.ink,
    lineHeight: 1.25,
  },
  '&.Mui-disabled': {
    opacity: 1,
    bgcolor: brand.tableHeader,
    '& .MuiSelect-icon': { display: 'none' },
    '& .MuiSelect-select, & .MuiOutlinedInput-input': {
      WebkitTextFillColor: brand.ink,
      color: brand.ink,
      opacity: 1,
    },
  },
}

/** Same caption as FormFieldLabel: use on section headings next to kit fields. */
export const sectionLabelSx: SxProps<Theme> = {
  display: 'block',
  height: 16,
  mb: 0.4,
  fontSize: '0.6875rem',
  fontWeight: 600,
  color: brand.muted,
  textTransform: 'uppercase',
  letterSpacing: '0.06em',
  lineHeight: '16px',
}

/** Kit-style field caption: always visible above control (Organisation settings pattern). */
export default function FormFieldLabel({
  children,
  htmlFor,
  required,
  wrap,
  hint,
  hintPlacement = 'end',
  color,
}: {
  children: ReactNode
  htmlFor?: string
  required?: boolean
  /** Full wording (e.g. BIA Excel headers): do not clip or force uppercase. */
  wrap?: boolean
  /** Who fills the field (Excel row 4) or other caption-right mark. */
  hint?: ReactNode
  /** `end` keeps the hint on the far side of a wide caption. `inline` sits it next to the label. */
  hintPlacement?: 'end' | 'inline'
  /** Caption color. Defaults to the kit section label. */
  color?: string
}) {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: hintPlacement === 'inline' ? 'flex-start' : 'space-between',
        gap: hintPlacement === 'inline' ? 1 : 0.75,
        mb: 0.4,
        minHeight: 16,
      }}
    >
      <Typography
        component="label"
        htmlFor={htmlFor}
        sx={
          wrap
            ? {
                display: 'block',
                minWidth: 0,
                fontSize: '0.6875rem',
                fontWeight: 600,
                color: brand.muted,
                letterSpacing: '0.02em',
                lineHeight: 1.3,
              }
            : {
                ...sectionLabelSx,
                mb: 0,
                minWidth: 0,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
                ...(color ? { color, fontWeight: 700 } : {}),
              }
        }
      >
        {children}
        {required ? (
          <Typography
            component="span"
            sx={{ color: brand.goldDark, ml: 0.25, fontSize: 'inherit', fontWeight: 700, lineHeight: 'inherit' }}
          >
            *
          </Typography>
        ) : null}
      </Typography>
      {hint ? (
        <Typography
          component="span"
          sx={{
            flexShrink: 0,
            mt: wrap ? 0.1 : 0,
            fontSize: '0.6875rem',
            fontWeight: 600,
            color: brand.muted,
            letterSpacing: '0.02em',
            lineHeight: 1.3,
          }}
        >
          {hint}
        </Typography>
      ) : null}
    </Box>
  )
}

export const compactSelectSx = compactControlSx

export const compactInputSx: SxProps<Theme> = {
  m: 0,
  width: '100%',
  verticalAlign: 'top',
  '& .MuiOutlinedInput-root:not(.MuiInputBase-multiline)': compactControlSx,
  '& .MuiOutlinedInput-root.MuiInputBase-multiline': {
    height: 'auto',
    minHeight: CONTROL_HEIGHT,
    bgcolor: brand.hover,
    overflow: 'hidden',
    '& .MuiOutlinedInput-notchedOutline': { borderColor: brand.border, top: 0 },
    '& legend': { display: 'none' },
    // HTML disabled textareas swallow wheel; keep inner scroll usable.
    '&.Mui-disabled': {
      pointerEvents: 'auto',
      bgcolor: brand.tableHeader,
    },
  },
  '& .MuiInputBase-inputMultiline': {
    py: 0.75,
    fontSize: '0.75rem',
    fontWeight: 400,
    overflowY: 'auto',
    '&.Mui-disabled': { pointerEvents: 'auto', cursor: 'default' },
  },
}
