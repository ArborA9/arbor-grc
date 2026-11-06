import { Box, Checkbox, Typography } from '@mui/material'
import type { SoaItem } from '../../api'
import { brand } from '../../theme'
import { DomainBadge } from '../ui'
import { narrativeReadScrollSx } from '../ui/formLayout'
import { soaApplicabilityBadge } from './soaUi'

const BASE_COLS = '80px 1fr 120px minmax(160px, 1.2fr) minmax(140px, 1fr)'
const SELECT_COL = '40px'

export default function SoaControlTable({
  items,
  showPriority,
  clickable,
  onRowClick,
  showApplicabilityHint = true,
  isVisible = () => true,
  gridTemplateColumns,
  selectable = false,
  selectedIds,
  onToggleSelect,
  onToggleSelectAll,
}: {
  items: SoaItem[]
  showPriority?: boolean
  clickable: boolean
  onRowClick: (item: SoaItem) => void
  showApplicabilityHint?: boolean
  isVisible?: (columnId: string) => boolean
  gridTemplateColumns?: string
  selectable?: boolean
  selectedIds?: Set<number>
  onToggleSelect?: (itemId: number) => void
  onToggleSelectAll?: () => void
}) {
  const dataCols =
    gridTemplateColumns ??
    (showPriority && isVisible('priority') ? `${BASE_COLS} 90px` : BASE_COLS)
  const gridCols = selectable ? `${SELECT_COL} ${dataCols}` : dataCols

  const allSelected =
    selectable && items.length > 0 && items.every((i) => selectedIds?.has(i.id))
  const someSelected =
    selectable && !allSelected && items.some((i) => selectedIds?.has(i.id))

  if (items.length === 0) {
    return (
      <Typography color="text.secondary" sx={{ py: 3, textAlign: 'center' }}>
        No controls match this filter.
      </Typography>
    )
  }

  return (
    <Box
      sx={{
        bgcolor: brand.paper,
        border: `1px solid ${brand.border}`,
        borderRadius: 1.125,
        overflow: 'hidden',
        boxShadow: brand.shadowCard,
      }}
    >
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: gridCols,
          gap: 1.5,
          alignItems: 'center',
          px: 2,
          py: 1.125,
          bgcolor: brand.tableHeader,
          borderBottom: `1px solid ${brand.border}`,
          fontSize: '0.59375rem',
          fontWeight: 700,
          letterSpacing: '0.07em',
          textTransform: 'uppercase',
          color: brand.mutedLight,
        }}
      >
        {selectable && (
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Checkbox
              size="small"
              checked={allSelected}
              indeterminate={someSelected}
              onChange={() => onToggleSelectAll?.()}
              inputProps={{ 'aria-label': 'Select all visible controls' }}
              sx={{ p: 0.25 }}
            />
          </Box>
        )}
        <Box>Control</Box>
        {isVisible('title') && <Box>Title</Box>}
        {isVisible('applicable') && <Box>Applicable?</Box>}
        {isVisible('justification') && <Box>Justification</Box>}
        {isVisible('remarks') && <Box>Remarks</Box>}
        {showPriority && isVisible('priority') && <Box>Priority</Box>}
      </Box>

      {items.map((item) => {
        const badge = soaApplicabilityBadge(item.applicability)
        const selected = selectedIds?.has(item.id) ?? false
        return (
          <Box
            key={item.id}
            role={clickable ? 'button' : undefined}
            tabIndex={clickable ? 0 : undefined}
            onClick={clickable ? () => onRowClick(item) : undefined}
            onKeyDown={
              clickable
                ? (e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault()
                      onRowClick(item)
                    }
                  }
                : undefined
            }
            sx={{
              display: 'grid',
              gridTemplateColumns: gridCols,
              gap: 1.5,
              alignItems: 'center',
              width: '100%',
              px: 2,
              py: 1.25,
              border: 0,
              borderBottom: `1px solid ${brand.borderLight}`,
              bgcolor: selected ? brand.hover : brand.paper,
              font: 'inherit',
              textAlign: 'left',
              cursor: clickable ? 'pointer' : 'default',
              contentVisibility: 'auto',
              containIntrinsicSize: '0 52px',
              '&:hover': clickable ? { bgcolor: brand.hover } : undefined,
              '&:last-child': { borderBottom: 0 },
            }}
          >
            {selectable && (
              <Box
                sx={{ display: 'flex', alignItems: 'center' }}
                onClick={(e) => e.stopPropagation()}
                onMouseDown={(e) => e.stopPropagation()}
              >
                <Checkbox
                  size="small"
                  checked={selected}
                  onChange={() => onToggleSelect?.(item.id)}
                  inputProps={{ 'aria-label': `Select ${item.nodeCode}` }}
                  sx={{ p: 0.25 }}
                />
              </Box>
            )}
            <Typography sx={{ fontSize: '0.6875rem', fontWeight: 700, color: brand.goldDark }}>
              {item.nodeCode}
            </Typography>
            {isVisible('title') && (
            <Box sx={{ minWidth: 0 }}>
              <Typography
                sx={{
                  fontSize: '0.6875rem',
                  fontWeight: 500,
                  color: brand.ink,
                  lineHeight: 1.35,
                  ...narrativeReadScrollSx,
                }}
              >
                {item.title}
              </Typography>
            </Box>
            )}
            {isVisible('applicable') && (
            <Box>
              {badge.variant === 'soa' ? (
                <DomainBadge variant="soa" status={badge.status} label={badge.label} />
              ) : (
                <DomainBadge variant="gap" status={badge.status} label={badge.label} />
              )}
            </Box>
            )}
            {isVisible('justification') && (
            <Box sx={{ minWidth: 0 }}>
              <Typography
                sx={{
                  fontSize: '0.6875rem',
                  color: item.justification ? brand.muted : brand.mutedLight,
                  lineHeight: 1.4,
                  ...narrativeReadScrollSx,
                }}
              >
                {item.justification || '-'}
              </Typography>
            </Box>
            )}
            {isVisible('remarks') && (
            <Box sx={{ minWidth: 0 }}>
              <Typography
                sx={{
                  fontSize: '0.6875rem',
                  color: item.remarks ? brand.muted : brand.mutedLight,
                  lineHeight: 1.4,
                  ...narrativeReadScrollSx,
                }}
              >
                {item.remarks || '-'}
              </Typography>
            </Box>
            )}
            {showPriority && isVisible('priority') && (
              <Typography sx={{ fontSize: '0.6875rem', color: brand.muted }}>
                {item.priority || '-'}
              </Typography>
            )}
          </Box>
        )
      })}

      {clickable && showApplicabilityHint && (
        <Box sx={{ px: 2, py: 1, borderTop: `1px solid ${brand.borderLight}`, bgcolor: brand.hover }}>
          <Typography sx={{ fontSize: '0.6875rem', color: brand.muted }}>
            Implementation status is recorded on Gap Assessment. Click a row to edit applicability.
          </Typography>
        </Box>
      )}
    </Box>
  )
}
