import { useMemo, useState, type ReactNode } from 'react'
import {
  Box,
  Card,
  InputAdornment,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import FilterListRoundedIcon from '@mui/icons-material/FilterListRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import type { AuditFinding, CapaRecord, UserSummary } from '../../api'
import StatusChip from '../StatusChip'
import {
  DomainBadge,
  FilterDropdown,
  RegisterListFrame,
  compactTableCellSx,
  lastTableCellSx,
  primaryRegisterNameSx,
  primaryTableCellSx,
  registerHeadCellSx,
  registerTableSx,
} from '../ui'
import { usePagedRows } from '../../uiQuality/usePagedRows'
import { brand } from '../../theme'
import { findingSeverityRiskLevel } from './auditKit'
import {
  findingSeverityShort,
  findingTableLabel,
  findingWorkflowStatus,
  formatEngagementDate,
} from './auditEngagementUtils'
import { capaRef } from './capaRegisterUtils'

type SeverityFilter = 'ALL' | AuditFinding['severity']

const SEVERITY_OPTIONS: { value: SeverityFilter; label: string }[] = [
  { value: 'ALL', label: 'All severities' },
  { value: 'MAJOR_NC', label: 'Major NC' },
  { value: 'MINOR_NC', label: 'Minor NC' },
  { value: 'OBSERVATION', label: 'Observation' },
  { value: 'OFI', label: 'OFI' },
]

/** Engagement Findings tab: register table chrome (search, severity filter, pagination). */
export default function AuditEngagementFindingsPanel({
  findings,
  capasById,
  users,
  emptyMessage,
  trailing,
}: {
  findings: AuditFinding[]
  capasById?: Map<number, CapaRecord>
  users: UserSummary[]
  emptyMessage: string
  trailing?: ReactNode
}) {
  const [query, setQuery] = useState('')
  const [severityFilter, setSeverityFilter] = useState<SeverityFilter>('ALL')

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return findings.filter((f) => {
      if (severityFilter !== 'ALL' && f.severity !== severityFilter) return false
      if (!q) return true
      const capa = f.capaId != null ? capasById?.get(f.capaId) : undefined
      const workflow = findingWorkflowStatus(f, capa)
      const hay = [
        f.displayCode,
        f.title,
        f.description,
        f.controlRef,
        f.severity,
        findingSeverityShort(f.severity),
        workflow.label,
        f.capaId != null ? capaRef(f.capaId) : '',
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
      return hay.includes(q)
    })
  }, [findings, query, severityFilter, capasById])

  const paged = usePagedRows(filtered)

  return (
    <Card
      sx={{
        flex: 1,
        minHeight: 0,
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
        border: `1px solid ${brand.border}`,
        boxShadow: brand.shadowCard,
      }}
    >
      <Stack
        direction="row"
        alignItems="center"
        justifyContent="space-between"
        flexWrap="wrap"
        gap={1}
        sx={{ px: 1.75, pt: 1.5, pb: 1 }}
      >
        <Typography sx={{ fontSize: '0.9375rem', fontWeight: 650, letterSpacing: '-0.01em' }}>
          Findings
        </Typography>
        <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
          {trailing}
          <TextField
            size="small"
            placeholder="Code, title, or control"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            inputProps={{ 'aria-label': 'Search findings' }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchRoundedIcon sx={{ fontSize: 16, color: brand.mutedLight }} />
                </InputAdornment>
              ),
            }}
            sx={{
              width: 240,
              maxWidth: 240,
              flex: '0 0 240px',
              '& .MuiOutlinedInput-root': {
                height: 32,
                bgcolor: brand.paper,
              },
            }}
          />
          <FilterDropdown
            label="Severity"
            hideLabel
            startIcon={<FilterListRoundedIcon sx={{ fontSize: 16, color: brand.mutedLight }} />}
            value={severityFilter}
            options={SEVERITY_OPTIONS}
            onChange={(v) => setSeverityFilter(v as SeverityFilter)}
            minWidth={148}
          />
        </Stack>
      </Stack>
      <RegisterListFrame
        ready
        count={paged.count}
        emptyMessage={
          findings.length === 0
            ? emptyMessage
            : 'No findings match the current search or filter.'
        }
        page={paged.page}
        rowsPerPage={paged.rowsPerPage}
        onPageChange={paged.setPage}
        onRowsPerPageChange={paged.setRowsPerPage}
      >
        <TableContainer>
          <Table size="small" sx={registerTableSx}>
            <TableHead>
              <TableRow>
                <TableCell sx={{ ...registerHeadCellSx, ...compactTableCellSx }}>Code</TableCell>
                <TableCell sx={{ ...registerHeadCellSx, ...compactTableCellSx }}>Severity</TableCell>
                <TableCell sx={{ ...registerHeadCellSx, ...primaryTableCellSx }}>Title</TableCell>
                <TableCell sx={{ ...registerHeadCellSx, ...compactTableCellSx }}>Control</TableCell>
                <TableCell sx={{ ...registerHeadCellSx, ...compactTableCellSx }}>Raised</TableCell>
                <TableCell sx={{ ...registerHeadCellSx, ...compactTableCellSx }}>Status</TableCell>
                <TableCell sx={{ ...registerHeadCellSx, ...lastTableCellSx }}>CAPA</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {paged.paged.map((f) => {
                const capa = f.capaId != null ? capasById?.get(f.capaId) : undefined
                const workflow = findingWorkflowStatus(f, capa)
                const ownerName =
                  capa?.ownerUserId != null
                    ? users.find((u) => u.id === capa.ownerUserId)?.fullName
                    : undefined
                return (
                  <TableRow key={f.id} hover>
                    <TableCell sx={compactTableCellSx}>
                      <Typography
                        sx={{
                          fontSize: '0.8125rem',
                          fontWeight: 650,
                          fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
                          color: brand.ink,
                        }}
                      >
                        {f.displayCode}
                      </Typography>
                    </TableCell>
                    <TableCell sx={compactTableCellSx}>
                      <DomainBadge
                        variant="risk"
                        level={findingSeverityRiskLevel(f.severity)}
                        label={findingTableLabel(f.severity)}
                      />
                    </TableCell>
                    <TableCell sx={primaryRegisterNameSx}>
                      <Box sx={{ minWidth: 0 }}>
                        <Typography
                          sx={{
                            fontSize: '0.8125rem',
                            fontWeight: 600,
                            color: brand.ink,
                            lineHeight: 1.35,
                          }}
                          noWrap
                          title={f.title}
                        >
                          {f.title || '-'}
                        </Typography>
                        {f.description?.trim() ? (
                          <Typography
                            sx={{
                              fontSize: '0.75rem',
                              color: brand.muted,
                              lineHeight: 1.35,
                              mt: 0.15,
                            }}
                            noWrap
                            title={f.description}
                          >
                            {f.description}
                          </Typography>
                        ) : null}
                      </Box>
                    </TableCell>
                    <TableCell sx={compactTableCellSx}>{f.controlRef || '-'}</TableCell>
                    <TableCell sx={compactTableCellSx}>{formatEngagementDate(f.createdAt)}</TableCell>
                    <TableCell sx={compactTableCellSx}>
                      <StatusChip label={workflow.label} tone={workflow.tone} />
                    </TableCell>
                    <TableCell sx={lastTableCellSx}>
                      {f.capaId != null ? (
                        <Box>
                          <Typography sx={{ fontSize: '0.8125rem', fontWeight: 600, color: brand.ink }}>
                            {capaRef(f.capaId)}
                          </Typography>
                          {ownerName ? (
                            <Typography sx={{ fontSize: '0.6875rem', color: brand.muted }} noWrap>
                              {ownerName}
                            </Typography>
                          ) : null}
                        </Box>
                      ) : (
                        <Typography sx={{ fontSize: '0.8125rem', color: brand.muted }}>-</Typography>
                      )}
                    </TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        </TableContainer>
      </RegisterListFrame>
    </Card>
  )
}
