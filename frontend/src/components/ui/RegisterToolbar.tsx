import {
  Box,
  InputAdornment,
  Stack,
  TextField,
} from '@mui/material'
import type { ReactNode } from 'react'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import { brand } from '../../theme'
import FilterDropdown, { type FilterDropdownOption } from './FilterDropdown'

export type RegisterToolbarFilter<T extends string = string> = {
  id: string
  label: string
  value: T
  options: FilterDropdownOption<T>[]
  onChange: (value: T) => void
  minWidth?: number
}

/** Kit table toolbar: search, dropdown filters, trailing actions. */
export default function RegisterToolbar({
  search,
  searchPlaceholder = 'Search…',
  searchWidth = 200,
  onSearchChange,
  filters = [],
  columnPicker,
  trailing,
}: {
  search?: string
  searchPlaceholder?: string
  searchWidth?: number
  onSearchChange?: (value: string) => void
  onClearFilters?: () => void
  filters?: RegisterToolbarFilter[]
  /** Column visibility dropdown (kit register tables). */
  columnPicker?: ReactNode
  trailing?: ReactNode
}) {
  return (
    <Box
      sx={{
        px: 1.5,
        py: 0.85,
        borderBottom: `1px solid ${brand.border}`,
        flexShrink: 0,
        bgcolor: brand.tableHeader,
      }}
    >
      <Stack
        direction="row"
        spacing={1}
        alignItems="center"
        flexWrap="nowrap"
        useFlexGap
        sx={{ overflowX: 'auto' }}
      >
        {onSearchChange != null && (
          <TextField
            size="small"
            placeholder={searchPlaceholder}
            value={search ?? ''}
            onChange={(e) => onSearchChange(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchRoundedIcon sx={{ fontSize: 16, color: brand.mutedLight }} />
                </InputAdornment>
              ),
            }}
            sx={{
              flex: { xs: '1 1 100%', sm: `0 0 ${searchWidth}px` },
              width: { sm: searchWidth },
              maxWidth: { sm: searchWidth },
              '& .MuiOutlinedInput-root': { bgcolor: brand.hover },
            }}
          />
        )}

        {filters.map((f) => (
          <FilterDropdown
            key={f.id}
            label={f.label}
            value={f.value}
            options={f.options}
            onChange={f.onChange}
            minWidth={f.minWidth}
          />
        ))}

        {(columnPicker || trailing) ? (
          <Stack
            direction="row"
            spacing={1}
            alignItems="center"
            flexWrap="nowrap"
            useFlexGap
            sx={{ ml: 'auto', flexShrink: 0 }}
          >
            {columnPicker}
            {trailing}
          </Stack>
        ) : null}
      </Stack>
    </Box>
  )
}
