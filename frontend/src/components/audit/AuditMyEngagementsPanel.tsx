import {
  Box,
  Button,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import type { GrcAuditStatus, GrcAuditSummary, GrcAuditType } from '../../api'
import StatusChip from '../StatusChip'
import { RegisterToolbar } from '../ui'
import AuditFrameworkLabel from './AuditFrameworkLabel'
import { auditTableContainerSx, auditTableHeadCellSx } from './auditKit'
import { brand } from '../../theme'

const ENGAGEMENT_STATUSES: GrcAuditStatus[] = [
  'DRAFT',
  'ACCESS_GRANTED',
  'IN_PROGRESS',
  'REPORT_ISSUED',
  'CLOSED',
  'CANCELLED',
]

type EngagementStatusFilter = 'ALL' | GrcAuditStatus
type EngagementTypeFilter = 'ALL' | GrcAuditType

function tone(s: string): 'success' | 'warning' | 'info' | 'danger' | 'neutral' {
  if (s === 'CLOSED' || s === 'COMPLETED') return 'success'
  if (s === 'REPORT_ISSUED' || s === 'PENDING_ADMIN' || s === 'PENDING_VERIFICATION') return 'warning'
  if (s === 'IN_PROGRESS' || s === 'ACCESS_GRANTED' || s === 'OPEN') return 'info'
  if (s === 'CANCELLED' || s === 'RETURNED' || s === 'OVERDUE') return 'danger'
  return 'neutral'
}

export default function AuditMyEngagementsPanel({
  audits,
  search,
  statusFilter,
  typeFilter,
  onSearchChange,
  onStatusFilterChange,
  onTypeFilterChange,
  onClearFilters,
  onOpen,
  onCreate,
  canCreate,
  embedded = false,
  activeEngagementId,
}: {
  audits: GrcAuditSummary[]
  search: string
  statusFilter: EngagementStatusFilter
  typeFilter: EngagementTypeFilter
  onSearchChange: (v: string) => void
  onStatusFilterChange: (v: EngagementStatusFilter) => void
  onTypeFilterChange: (v: EngagementTypeFilter) => void
  onClearFilters: () => void
  onOpen: (id: number) => void
  onCreate?: () => void
  canCreate?: boolean
  /** Render inside IA fieldwork shell (no full-page chrome). */
  embedded?: boolean
  activeEngagementId?: number
}) {
  const q = search.trim().toLowerCase()
  const filtered = audits.filter((a) => {
    if (statusFilter !== 'ALL' && a.status !== statusFilter) return false
    if (typeFilter !== 'ALL' && a.auditType !== typeFilter) return false
    if (!q) return true
    return (
      a.name.toLowerCase().includes(q) ||
      a.frameworkCode.toLowerCase().includes(q) ||
      (a.frameworkName?.toLowerCase().includes(q) ?? false)
    )
  })

  const statusOptions = [
    { value: 'ALL' as const, label: 'All', count: audits.length },
    ...ENGAGEMENT_STATUSES.map((status) => ({
      value: status,
      label: status.replace(/_/g, ' '),
      count: audits.filter((a) => a.status === status).length,
    })),
  ]

  const typeOptions = [
    { value: 'ALL' as const, label: 'All types', count: audits.length },
    {
      value: 'INTERNAL' as const,
      label: 'Internal',
      count: audits.filter((a) => a.auditType === 'INTERNAL').length,
    },
    {
      value: 'EXTERNAL' as const,
      label: 'External',
      count: audits.filter((a) => a.auditType === 'EXTERNAL').length,
    },
  ]

  return (
    <Box
      data-testid="audit-my-engagements"
      sx={{
        flex: 1,
        minHeight: 0,
        display: 'flex',
        flexDirection: 'column',
        bgcolor: embedded ? 'transparent' : brand.canvas,
        overflow: 'hidden',
      }}
    >
      {!embedded && (
      <Box sx={{ px: 2, pt: 2, pb: 1 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" gap={1} flexWrap="wrap">
          <Box>
            <Typography variant="h6" fontWeight={700}>
              My engagements
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.25 }}>
              Engagements you are assigned to as lead or team member. Open one to start fieldwork.
            </Typography>
          </Box>
          {canCreate && onCreate && (
            <Button variant="contained" size="small" onClick={onCreate}>
              New Engagement
            </Button>
          )}
        </Stack>
      </Box>
      )}

      <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', px: embedded ? 2 : 2, pt: embedded ? 2 : 0, pb: 2 }}>
        <RegisterToolbar
          search={search}
          searchPlaceholder="Search engagements…"
          onSearchChange={onSearchChange}
          onClearFilters={onClearFilters}
          filters={[
            {
              id: 'status',
              label: 'Status',
              value: statusFilter,
              options: statusOptions.map((o) => ({
                value: o.value,
                label: `${o.label} (${o.count})`,
              })),
              onChange: (v) => onStatusFilterChange(v as EngagementStatusFilter),
              minWidth: 160,
            },
            {
              id: 'type',
              label: 'Type',
              value: typeFilter,
              options: typeOptions.map((o) => ({
                value: o.value,
                label: `${o.label} (${o.count})`,
              })),
              onChange: (v) => onTypeFilterChange(v as EngagementTypeFilter),
              minWidth: 140,
            },
          ]}
        />

        <TableContainer
          sx={{
            ...auditTableContainerSx,
            flex: 1,
            bgcolor: 'background.paper',
            borderRadius: 1,
            boxShadow: brand.shadowCard,
          }}
        >
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell sx={auditTableHeadCellSx}>Name</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Framework</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Type</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filtered.map((a) => (
                <TableRow
                  key={a.id}
                  hover
                  selected={activeEngagementId === a.id}
                  sx={{ cursor: 'pointer' }}
                  onClick={() => onOpen(a.id)}
                  data-testid={`audit-engagement-row-${a.id}`}
                >
                  <TableCell>
                    <Typography variant="body2" fontWeight={600}>
                      {a.name}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <AuditFrameworkLabel audit={a} />
                  </TableCell>
                  <TableCell>{a.auditType === 'INTERNAL' ? 'Internal' : 'External'}</TableCell>
                  <TableCell>
                    <StatusChip label={a.status} tone={tone(a.status)} />
                  </TableCell>
                </TableRow>
              ))}
              {filtered.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4}>
                    <Stack spacing={1.5} alignItems="center" sx={{ py: 4, px: 2, textAlign: 'center' }}>
                      <Typography variant="body1" fontWeight={600}>
                        {audits.length === 0 ? 'No engagements yet' : 'No engagements match your filters'}
                      </Typography>
                      <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 420 }}>
                        {audits.length === 0
                          ? canCreate
                            ? 'Plan a new internal audit engagement, or ask an Admin to add you to an existing team.'
                            : 'When an Admin adds you to an audit engagement team, it will appear here.'
                          : 'Try clearing filters or broadening your search.'}
                      </Typography>
                      {audits.length === 0 && canCreate && onCreate && (
                        <Button variant="contained" size="small" onClick={onCreate} sx={{ mt: 0.5 }}>
                          Plan Engagement
                        </Button>
                      )}
                    </Stack>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Box>
    </Box>
  )
}
