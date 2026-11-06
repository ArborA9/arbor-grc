import { useEffect, useState } from 'react'
import {
  Box,
  Chip,
  IconButton,
  List,
  ListItem,
  ListItemText,
  Popover,
  Tooltip,
  Typography,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import { brand } from '../../theme'

const VISIBLE_CHIPS = 2

export type OverflowChipItem = {
  key: string
  label: string
  secondary?: string | null
}

export default function CompactOverflowChips({
  items,
  disabled,
  noun,
  onRemove,
}: {
  items: OverflowChipItem[]
  disabled?: boolean
  noun: string
  onRemove?: (key: string) => void
}) {
  const [moreAnchor, setMoreAnchor] = useState<HTMLElement | null>(null)
  const extra = Math.max(0, items.length - VISIBLE_CHIPS)
  const visible = extra > 0 ? items.slice(0, VISIBLE_CHIPS) : items

  useEffect(() => {
    if (items.length === 0) setMoreAnchor(null)
  }, [items.length])

  if (items.length === 0) return null

  return (
    <>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 0.75, minWidth: 0 }}>
        {visible.map((item) => (
          <Tooltip key={item.key} title={item.secondary ? `${item.label} · ${item.secondary}` : item.label}>
            <Chip
              size="small"
              label={item.label}
              onDelete={disabled || !onRemove ? undefined : () => onRemove(item.key)}
              sx={{ maxWidth: 160, '& .MuiChip-label': { overflow: 'hidden', textOverflow: 'ellipsis' } }}
            />
          </Tooltip>
        ))}
        {extra > 0 && (
          <Chip
            size="small"
            label={`+${extra} more`}
            onClick={(e) => setMoreAnchor(e.currentTarget)}
            variant="outlined"
            sx={{ cursor: 'pointer' }}
          />
        )}
      </Box>
      <Popover
        open={Boolean(moreAnchor)}
        anchorEl={moreAnchor}
        onClose={() => setMoreAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        transformOrigin={{ vertical: 'top', horizontal: 'left' }}
      >
        <Box sx={{ width: 320, maxWidth: 'calc(100vw - 32px)' }}>
          <Typography sx={{ px: 1.5, pt: 1.25, pb: 0.5, fontSize: '0.75rem', fontWeight: 650, color: brand.muted }}>
            {items.length} {noun} selected
          </Typography>
          <List dense disablePadding sx={{ maxHeight: 280, overflow: 'auto', pb: 0.5 }}>
            {items.map((item) => (
              <ListItem
                key={item.key}
                secondaryAction={
                  disabled || !onRemove ? undefined : (
                    <IconButton
                      edge="end"
                      size="small"
                      aria-label={`Remove ${item.label}`}
                      onClick={() => onRemove(item.key)}
                    >
                      <CloseIcon sx={{ fontSize: 16 }} />
                    </IconButton>
                  )
                }
                sx={{ pr: disabled || !onRemove ? 1.5 : 6 }}
              >
                <ListItemText
                  primary={item.label}
                  secondary={item.secondary || undefined}
                  primaryTypographyProps={{ fontSize: '0.8125rem', noWrap: true }}
                  secondaryTypographyProps={{ fontSize: '0.6875rem', noWrap: true }}
                />
              </ListItem>
            ))}
          </List>
        </Box>
      </Popover>
    </>
  )
}
