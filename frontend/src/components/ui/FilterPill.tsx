import { Box, Stack } from '@mui/material'
import type { ReactNode } from 'react'
import { brand } from '../../theme'

export interface FilterPillProps {
  label: string
  active?: boolean
  count?: number
  onClick: () => void
  disabled?: boolean
  dense?: boolean
  /** Optional leading icon (e.g. filter glyph before All). */
  startIcon?: ReactNode
}

/** Kit-style rounded filter pill (SoA / Gap filters). */
export default function FilterPill({
  label,
  active = false,
  count,
  onClick,
  disabled = false,
  dense = false,
  startIcon,
}: FilterPillProps) {
  return (
    <Box
      component="button"
      type="button"
      disabled={disabled}
      onClick={onClick}
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 0.5,
        border: `1px solid ${active ? brand.goldMid : brand.border}`,
        bgcolor: active ? brand.goldLight : brand.paper,
        color: active ? brand.goldDark : brand.muted,
        borderRadius: 20,
        px: dense ? 1.25 : 1.75,
        py: dense ? 0.375 : 0.625,
        fontSize: '0.6875rem',
        fontWeight: active ? 600 : 500,
        cursor: disabled ? 'not-allowed' : 'pointer',
        fontFamily: 'inherit',
        whiteSpace: 'nowrap',
        opacity: disabled ? 0.5 : 1,
        transition: 'background-color .12s ease, border-color .12s ease, color .12s ease',
        '&:hover': disabled
          ? undefined
          : {
              borderColor: active ? brand.goldMid : brand.goldMid,
              bgcolor: active ? brand.goldLight : brand.hover,
              color: active ? brand.goldDark : brand.inkSoft,
            },
        '&:focus-visible': {
          outline: `2px solid ${brand.gold}`,
          outlineOffset: 1,
        },
      }}
    >
      {startIcon}
      <Box component="span">
        {label}
        {count != null ? ` (${count})` : ''}
      </Box>
    </Box>
  )
}

export type FilterPillOption<T extends string> = {
  value: T
  label: string
  count?: number
  disabled?: boolean
}

export function FilterPillRow<T extends string>({
  value,
  options,
  onChange,
  dense = false,
  nowrap = false,
}: {
  value: T
  options: FilterPillOption<T>[]
  onChange: (value: T) => void
  dense?: boolean
  nowrap?: boolean
}) {
  return (
    <Stack
      direction="row"
      spacing={dense ? 0.5 : 1}
      flexWrap={nowrap ? 'nowrap' : 'wrap'}
      useFlexGap
      sx={nowrap ? { flexShrink: 0 } : undefined}
    >
      {options.map((opt) => (
        <FilterPill
          key={opt.value}
          label={opt.label}
          count={opt.count}
          disabled={opt.disabled}
          active={value === opt.value}
          dense={dense}
          onClick={() => onChange(opt.value)}
        />
      ))}
    </Stack>
  )
}
