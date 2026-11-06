import { type ReactNode } from 'react'
import {
  Box,
  Button,
  InputAdornment,
  Stack,
  TextField,
} from '@mui/material'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined'
import { brand } from '../../theme'
import { compactInputSx, FilterDropdown, FilterPill } from '../ui'
import { fmtLevel } from '../../riskLevelLabels'
import { parseGridSize, priorityBandsForGrid, type MatrixGridSize } from './riskMatrix'
import type { RiskPriorityFilter, RiskStatusFilter } from './riskUi'

const STATUS_OPTIONS: { value: RiskStatusFilter; label: string }[] = [
  { value: 'ALL', label: 'All' },
  { value: 'OPEN', label: 'Open' },
  { value: 'MITIGATING', label: 'In treatment' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'CLOSED', label: 'Closed' },
  { value: 'TOLERATED', label: 'Tolerated' },
  { value: 'AVOIDED', label: 'Avoided' },
  { value: 'TRANSFERRED', label: 'Transferred' },
  { value: 'AUTO_CLOSED', label: 'Auto-closed' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

function priorityOptions(gridSize: MatrixGridSize): { value: RiskPriorityFilter; label: string }[] {
  return [
    { value: 'ALL', label: 'All' },
    ...priorityBandsForGrid(gridSize).map((band) => ({
      value: band as RiskPriorityFilter,
      label: fmtLevel(band),
    })),
  ]
}

const quietBtnSx = {
  borderColor: brand.border,
  color: brand.muted,
  textTransform: 'none' as const,
  fontWeight: 500,
  fontSize: '0.75rem',
  flexShrink: 0,
}

export default function RiskRegisterToolbar({
  search,
  onSearchChange,
  statusFilter,
  priorityFilter,
  onStatusFilterChange,
  onPriorityFilterChange,
  onClearFilters,
  filtersActive,
  waitingMyApproval,
  waitingMyApprovalCount,
  onWaitingMyApprovalChange,
  canExport,
  exportBusy,
  onExport,
  columnPicker,
  extraActions,
  hideStatusFilter,
  gridSize,
}: {
  search: string
  onSearchChange: (value: string) => void
  statusFilter: RiskStatusFilter
  priorityFilter: RiskPriorityFilter
  onStatusFilterChange: (value: RiskStatusFilter) => void
  onPriorityFilterChange: (value: RiskPriorityFilter) => void
  onClearFilters: () => void
  filtersActive?: boolean
  waitingMyApproval?: boolean
  waitingMyApprovalCount?: number
  onWaitingMyApprovalChange?: (value: boolean) => void
  canExport: boolean
  exportBusy?: boolean
  onExport: () => void
  columnPicker?: ReactNode
  extraActions?: ReactNode
  hideStatusFilter?: boolean
  gridSize?: MatrixGridSize
}) {
  const priorityOpts = priorityOptions(parseGridSize(gridSize))
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
        <TextField
          size="small"
          placeholder="Search risks…"
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchRoundedIcon sx={{ fontSize: 16, color: brand.mutedLight }} />
              </InputAdornment>
            ),
          }}
          sx={{
            ...compactInputSx,
            flex: '0 0 200px',
            width: 200,
            maxWidth: 200,
            '& .MuiOutlinedInput-root': { bgcolor: brand.paper },
          }}
        />

        {!hideStatusFilter && (
          <FilterDropdown
            label="Status"
            value={statusFilter}
            options={STATUS_OPTIONS}
            onChange={onStatusFilterChange}
            minWidth={128}
          />
        )}

        <FilterDropdown
          label="Priority"
          value={priorityFilter}
          options={priorityOpts}
          onChange={onPriorityFilterChange}
          minWidth={120}
        />

        {onWaitingMyApprovalChange && (
          <FilterPill
            label="Waiting my approval"
            active={!!waitingMyApproval}
            count={waitingMyApprovalCount}
            onClick={() => onWaitingMyApprovalChange(!waitingMyApproval)}
          />
        )}

        {filtersActive ? (
          <Button size="small" variant="text" onClick={onClearFilters} sx={{ ...quietBtnSx, border: 0 }}>
            Clear
          </Button>
        ) : null}

        <Stack
          direction="row"
          spacing={1}
          alignItems="center"
          flexWrap="nowrap"
          useFlexGap
          sx={{ ml: 'auto', flexShrink: 0 }}
        >
          {extraActions}
          {columnPicker}
          {canExport && (
            <Button
              size="small"
              variant="outlined"
              disabled={exportBusy}
              startIcon={<FileDownloadOutlinedIcon sx={{ fontSize: 16 }} />}
              onClick={onExport}
              sx={{ ...quietBtnSx, color: brand.inkSoft }}
            >
              Export
            </Button>
          )}
        </Stack>
      </Stack>
    </Box>
  )
}
