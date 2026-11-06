import { useMemo, useState } from 'react'
import {
  Card,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import type { CapaRecord, CapaStatus, UserSummary } from '../../api'
import StatusChip from '../StatusChip'
import {
  KpiCard,
  KpiCardGrid,
  RegisterListFrame,
  RegisterToolbar,
  compactTableCellSxWidth,
  lastTableCellSx,
  mediumTableCellSx,
  primaryRegisterNameSx,
  primaryTableCellSx,
  registerHeadCellSx,
  registerTableSx,
} from '../ui'
import { auditControlRefSx, CAPA_KPI_ACCENTS } from './auditKit'
import {
  capaRef,
  capaRegisterStats,
  capaSourceLabel,
  capaSourceLineage,
  REGISTER_META,
  type AuditCapaRegister,
} from './capaRegisterUtils'
import { capaLineStatus, capaLineStatusChipLabel, capaLineStatusLabel, capaLineStatusTone } from './capaJourney'
import { CAPA_EXCEL } from './capaExcelFields'
import { usePagedRows } from '../../uiQuality/usePagedRows'
import { formatTenantDate } from '../../uiQuality/tenantLocale'
import { brand } from '../../theme'

/** Head cells that must show full short titles (no ellipsis mid-word). */
const capaHeadFitSx = {
  ...registerHeadCellSx,
  whiteSpace: 'normal' as const,
  lineHeight: 1.25,
  overflow: 'visible' as const,
  textOverflow: 'clip' as const,
}

const CAPA_STATUS_FILTERS: CapaStatus[] = [
  'OPEN',
  'IN_PROGRESS',
  'PENDING_VERIFICATION',
  'OVERDUE',
  'CLOSED',
]

type CapaStatusFilter = 'ALL' | CapaStatus

export default function AuditCapaRegisterPanel({
  register,
  capas,
  users,
  audits = [],
  engagementId,
  ready = true,
  onSelect,
}: {
  register?: AuditCapaRegister
  capas: CapaRecord[]
  users: UserSummary[]
  audits?: { id: number; name: string }[]
  /** When set, lock the register to this engagement and hide the filter. */
  engagementId?: number
  ready?: boolean
  onSelect: (c: CapaRecord) => void
}) {
  const tracker = register == null
  const meta = register ? REGISTER_META[register] : null
  const [engagementFilter, setEngagementFilter] = useState<number | ''>(engagementId ?? '')
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<CapaStatusFilter>('ALL')
  const [registerFilter, setRegisterFilter] = useState<'ALL' | AuditCapaRegister | 'OTHER'>('ALL')

  const filteredCapas = useMemo(() => {
    const filterId = engagementId ?? engagementFilter
    const q = search.trim().toLowerCase()
    return capas.filter((c) => {
      if (filterId !== '' && c.sourceAuditId !== filterId) return false
      if (statusFilter !== 'ALL' && c.status !== statusFilter) return false
      if (tracker && registerFilter !== 'ALL' && c.registerCode !== registerFilter) return false
      if (!q) return true
      return (
        c.title.toLowerCase().includes(q) ||
        capaRef(c.id).toLowerCase().includes(q) ||
        (c.controlRef?.toLowerCase().includes(q) ?? false) ||
        capaSourceLineage(c).toLowerCase().includes(q) ||
        capaSourceLabel(c).toLowerCase().includes(q) ||
        (c.correctiveAction?.toLowerCase().includes(q) ?? false) ||
        (c.recommendedAction?.toLowerCase().includes(q) ?? false)
      )
    })
  }, [capas, engagementFilter, engagementId, search, statusFilter, tracker, registerFilter])

  const scopedCapas = useMemo(() => {
    const filterId = engagementId ?? engagementFilter
    if (filterId === '') return capas
    return capas.filter((c) => c.sourceAuditId === filterId)
  }, [capas, engagementFilter, engagementId])

  const engagementOptions = useMemo(
    () => [
      { value: 'ALL' as const, label: tracker ? 'All audits' : 'All engagements', count: capas.length },
      ...audits.map((a) => ({
        value: a.id,
        label: a.name,
        count: capas.filter((c) => c.sourceAuditId === a.id).length,
      })),
    ],
    [audits, capas, tracker],
  )

  const statusOptions = useMemo(
    () => [
      { value: 'ALL' as const, label: 'All statuses', count: scopedCapas.length },
      ...CAPA_STATUS_FILTERS.map((status) => ({
        value: status,
        label: capaLineStatusLabel(status),
        count: scopedCapas.filter((c) => c.status === status).length,
      })),
    ],
    [scopedCapas],
  )

  const clearFilters = () => {
    setSearch('')
    setStatusFilter('ALL')
    setRegisterFilter('ALL')
    if (engagementId == null) setEngagementFilter('')
  }
  const stats = useMemo(() => capaRegisterStats(scopedCapas), [scopedCapas])
  const paged = usePagedRows(filteredCapas)
  const ownerLabel = (id: number | null) =>
    id == null ? 'Unassigned' : users.find((u) => u.id === id)?.fullName ?? `#${id}`

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0 }}>
      {!tracker && meta && (
        <>
          <Typography sx={{ fontSize: '0.9375rem', fontWeight: 650, letterSpacing: '-0.01em' }}>
            {meta.title}
          </Typography>
          <Typography sx={{ fontSize: '0.75rem', color: brand.mutedLight }}>
            {meta.subtitle}
          </Typography>
        </>
      )}

      <KpiCardGrid columns={4} dense>
        <KpiCard compact label="Total CAPAs" value={stats.total} accentColor={CAPA_KPI_ACCENTS.total} />
        <KpiCard compact label="Overdue" value={stats.overdue} accentColor={CAPA_KPI_ACCENTS.overdue} />
        <KpiCard compact label="In progress" value={stats.inProgress} accentColor={CAPA_KPI_ACCENTS.inProgress} />
        <KpiCard compact label="Closed" value={stats.closed} accentColor={CAPA_KPI_ACCENTS.closed} />
      </KpiCardGrid>

      <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        <RegisterToolbar
          search={search}
          searchPlaceholder="Search CAPAs"
          onSearchChange={setSearch}
          onClearFilters={clearFilters}
          filters={[
            ...(engagementId == null && audits.length > 0
              ? [
                  {
                    id: 'engagement',
                    label: tracker ? 'Audit' : 'Engagement',
                    value: engagementFilter === '' ? 'ALL' : String(engagementFilter),
                    options: engagementOptions.map((o) => ({
                      value: o.value === 'ALL' ? 'ALL' : String(o.value),
                      label: `${o.label} (${o.count})`,
                    })),
                    onChange: (v: string) =>
                      setEngagementFilter(v === 'ALL' ? '' : Number(v)),
                    minWidth: 200,
                  },
                ]
              : []),
            ...(tracker && engagementId == null
              ? [
                  {
                    id: 'source',
                    label: 'Source',
                    value: registerFilter,
                    options: [
                      {
                        value: 'ALL',
                        label: `All (${capas.length})`,
                      },
                      {
                        value: 'INTERNAL_AUDIT',
                        label: `Internal (${capas.filter((c) => c.registerCode === 'INTERNAL_AUDIT').length})`,
                      },
                      {
                        value: 'EXTERNAL_AUDIT',
                        label: `External (${capas.filter((c) => c.registerCode === 'EXTERNAL_AUDIT').length})`,
                      },
                      {
                        value: 'OTHER',
                        label: `Other (${capas.filter((c) => c.registerCode === 'OTHER').length})`,
                      },
                    ],
                    onChange: (v: string) => setRegisterFilter(v as 'ALL' | AuditCapaRegister | 'OTHER'),
                    minWidth: 140,
                  },
                ]
              : []),
            {
              id: 'status',
              label: 'Status',
              value: statusFilter,
              options: statusOptions.map((o) => ({
                value: o.value,
                label: `${o.label} (${o.count})`,
              })),
              onChange: (v) => setStatusFilter(v as CapaStatusFilter),
              minWidth: 160,
            },
          ]}
        />

        <RegisterListFrame
          ready={ready}
          count={paged.count}
          emptyMessage={
            capas.length === 0
              ? tracker
                ? 'No CAPAs yet. Use Add CAPA to create one.'
                : `No CAPAs in ${meta?.title ?? 'this register'} yet. Items open automatically when an engagement is locked (Admin second sign) with non-OFI findings.`
              : 'No CAPAs match the current filters.'
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
                  <TableCell sx={{ ...capaHeadFitSx, ...compactTableCellSxWidth('8%') }}>
                    {CAPA_EXCEL.capaId}
                  </TableCell>
                  {tracker && (
                    <TableCell sx={{ ...capaHeadFitSx, ...compactTableCellSxWidth('8%') }}>
                      Source
                    </TableCell>
                  )}
                  <TableCell sx={{ ...capaHeadFitSx, ...primaryTableCellSx, width: tracker ? '20%' : '24%' }}>
                    {CAPA_EXCEL.findingTitle}
                  </TableCell>
                  <TableCell sx={{ ...capaHeadFitSx, ...mediumTableCellSx, width: '10%' }}>
                    {CAPA_EXCEL.clauseRef}
                  </TableCell>
                  <TableCell sx={{ ...capaHeadFitSx, ...mediumTableCellSx, width: '16%' }}>
                    {CAPA_EXCEL.correctiveActionPlan}
                  </TableCell>
                  <TableCell sx={{ ...capaHeadFitSx, minWidth: 110, width: '12%' }}>
                    Assignee
                  </TableCell>
                  <TableCell sx={{ ...capaHeadFitSx, minWidth: 100, width: '11%' }}>
                    Target date
                  </TableCell>
                  <TableCell
                    sx={{
                      ...capaHeadFitSx,
                      ...lastTableCellSx,
                      minWidth: 108,
                      width: '11%',
                      overflow: 'visible',
                    }}
                  >
                    Status
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((c) => {
                  const line = capaLineStatus(c)
                  return (
                  <TableRow key={c.id} hover sx={{ cursor: 'pointer' }} onClick={() => onSelect(c)}>
                    <TableCell>
                      <Typography sx={auditControlRefSx}>{capaRef(c.id)}</Typography>
                    </TableCell>
                    {tracker && (
                      <TableCell sx={compactTableCellSxWidth('8%')} title={capaSourceLabel(c)}>
                        {capaSourceLabel(c)}
                      </TableCell>
                    )}
                    <TableCell sx={{ whiteSpace: 'normal' }}>
                      <Typography sx={primaryRegisterNameSx}>{c.title}</Typography>
                      <Typography sx={{ fontSize: '0.6875rem', color: brand.muted, mt: 0.15 }}>
                        {capaSourceLineage(c)}
                      </Typography>
                    </TableCell>
                    <TableCell
                      title={c.controlRef ?? undefined}
                      sx={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', verticalAlign: 'middle' }}
                    >
                      {c.controlRef ?? '-'}
                    </TableCell>
                    <TableCell sx={{ whiteSpace: 'normal' }}>
                      <Typography variant="body2" noWrap title={c.correctiveAction ?? c.recommendedAction ?? ''}>
                        {c.correctiveAction ?? c.recommendedAction ?? '-'}
                      </Typography>
                    </TableCell>
                    <TableCell sx={{ whiteSpace: 'normal' }}>{ownerLabel(c.ownerUserId)}</TableCell>
                    <TableCell>
                      <Typography
                        variant="caption"
                        color={c.status === 'OVERDUE' ? 'error.main' : 'text.secondary'}
                        fontWeight={c.status === 'OVERDUE' ? 600 : 400}
                      >
                        {c.targetDate
                          ? formatTenantDate(c.targetDate)
                          : c.status === 'CLOSED'
                            ? 'Closed'
                            : '-'}
                      </Typography>
                    </TableCell>
                    <TableCell sx={{ overflow: 'visible', whiteSpace: 'nowrap' }}>
                      <StatusChip
                        label={capaLineStatusChipLabel(line)}
                        tone={capaLineStatusTone(line)}
                        title={line}
                      />
                    </TableCell>
                  </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </RegisterListFrame>
      </Card>
    </Stack>
  )
}
