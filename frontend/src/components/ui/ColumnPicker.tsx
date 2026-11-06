import { useState } from 'react'
import {
  Box,
  Button,
  Checkbox,
  Divider,
  FormControlLabel,
  Menu,
  MenuItem,
  Typography,
} from '@mui/material'
import ViewColumnRoundedIcon from '@mui/icons-material/ViewColumnRounded'
import type { ColumnDefinition } from '../../hooks/columnVisibility'
import { brand } from '../../theme'

export default function ColumnPicker({
  columns,
  visibleIds,
  onToggle,
  onReset,
  maxVisible,
}: {
  columns: ColumnDefinition[]
  visibleIds: readonly string[]
  onToggle: (id: string) => void
  onReset: () => void
  /** When set, unchecked columns are disabled once this many are on. */
  maxVisible?: number
}) {
  const [anchor, setAnchor] = useState<null | HTMLElement>(null)
  const open = Boolean(anchor)
  const atCap = maxVisible != null && visibleIds.length >= maxVisible
  const countLabel =
    maxVisible != null ? `${visibleIds.length}/${maxVisible}` : String(visibleIds.length)

  return (
    <>
      <Button
        size="small"
        variant="outlined"
        startIcon={<ViewColumnRoundedIcon sx={{ fontSize: 15 }} />}
        onClick={(e) => setAnchor(e.currentTarget)}
        sx={{
          borderColor: brand.border,
          bgcolor: brand.paper,
          color: brand.inkSoft,
          textTransform: 'none',
          fontWeight: 600,
          fontSize: '0.75rem',
          '&:hover': {
            borderColor: brand.gold,
            bgcolor: brand.goldLight,
            color: brand.goldDark,
          },
        }}
      >
        Columns ({countLabel})
      </Button>
      <Menu
        anchorEl={anchor}
        open={open}
        onClose={() => setAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        transformOrigin={{ vertical: 'top', horizontal: 'left' }}
        slotProps={{
          paper: {
            sx: { minWidth: 260, maxHeight: 'min(70vh, 520px)', mt: 0.5 },
          },
        }}
      >
        <Box sx={{ px: 1.5, pt: 1, pb: 0.5 }}>
          <Typography sx={{ fontSize: '0.6875rem', fontWeight: 700, color: brand.mutedLight }}>
            SHOW COLUMNS
          </Typography>
          {maxVisible != null && (
            <Typography sx={{ fontSize: '0.6875rem', color: brand.mutedLight, mt: 0.25 }}>
              {visibleIds.length} of {maxVisible} selected
              {atCap ? ': uncheck one to add another' : ''}
            </Typography>
          )}
        </Box>
        {columns.map((col) => {
          const checked = visibleIds.includes(col.id)
          const blocked = !checked && atCap
          const disabled = !!col.required || blocked
          return (
            <MenuItem
              key={col.id}
              dense
              disabled={disabled}
              onClick={() => !disabled && onToggle(col.id)}
              title={blocked ? `At most ${maxVisible} columns at a time` : undefined}
              sx={{ py: 0.25 }}
            >
              <FormControlLabel
                sx={{ m: 0, width: '100%', pointerEvents: 'none' }}
                control={
                  <Checkbox
                    size="small"
                    checked={checked}
                    disabled={disabled}
                    sx={{ py: 0.5 }}
                  />
                }
                label={
                  <Typography sx={{ fontSize: '0.8125rem' }}>
                    {col.label}
                    {col.required ? ' (required)' : ''}
                  </Typography>
                }
              />
            </MenuItem>
          )
        })}
        <Divider sx={{ my: 0.5 }} />
        <MenuItem
          onClick={() => {
            onReset()
            setAnchor(null)
          }}
          sx={{ fontSize: '0.8125rem', color: brand.goldDark, fontWeight: 600 }}
        >
          Reset to Default
        </MenuItem>
      </Menu>
    </>
  )
}
