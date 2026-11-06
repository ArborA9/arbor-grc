import { useEffect, useMemo, useState } from 'react'
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material'
import { api, type AccessGrant, type GrcAuditSummary, type UserSummary } from '../../api'
import StatusChip from '../StatusChip'
import { RegisterListFrame, RegisterToolbar } from '../ui'
import { useListReady } from '../../uiQuality/useListReady'
import { usePagedRows } from '../../uiQuality/usePagedRows'
import { auditTableContainerSx, auditTableHeadCellSx } from './auditKit'

function grantTone(status: string, independence: string): 'success' | 'warning' | 'info' | 'danger' | 'neutral' {
  if (independence.startsWith('BLOCKED')) return 'danger'
  if (status === 'REVOKED' || status === 'EXPIRED') return 'danger'
  if (status === 'Expiring' || status === 'Pending') return 'warning'
  if (status === 'Active') return 'success'
  return 'neutral'
}

export default function AuditAccessGrantsPanel({
  audits,
  users,
  onOpenAudit,
  onError,
}: {
  audits: GrcAuditSummary[]
  users: UserSummary[]
  onOpenAudit: (id: number) => void
  onError: (msg: string) => void
}) {
  const { listReady, markReady } = useListReady()
  const [rows, setRows] = useState<AccessGrant[]>([])
  const [search, setSearch] = useState('')

  useEffect(() => {
    api
      .listAllAuditGrants()
      .then(setRows)
      .catch((e) => onError(String(e)))
      .finally(markReady)
  }, [onError, markReady])

  const auditName = (id: number) => {
    const a = audits.find((x) => x.id === id)
    return a ? `${a.displayId ?? ''} ${a.name}`.trim() : `Audit ${id}`
  }
  const userName = (id: number) => users.find((u) => u.id === id)?.fullName ?? `#${id}`

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return rows
    return rows.filter((g) => {
      const hay = [
        g.displayId,
        auditName(g.auditId),
        userName(g.userId),
        g.granteeType,
        g.scopeBoundary,
        g.accessStatus,
        g.independenceCheck,
        ...(g.modules ?? []),
      ]
        .join(' ')
        .toLowerCase()
      return hay.includes(q)
    })
  }, [rows, search, audits, users])

  const paged = usePagedRows(filtered)

  return (
    <>
      <Typography variant="subtitle1" fontWeight={700}>
        Access Grants
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
        Time-boxed, read-only access to modules in scope for an audit
      </Typography>
      <RegisterToolbar
        search={search}
        searchPlaceholder="Search grants"
        onSearchChange={setSearch}
        onClearFilters={() => setSearch('')}
      />
      <RegisterListFrame
        ready={listReady}
        count={paged.count}
        emptyMessage={rows.length === 0 ? 'No access grants yet.' : 'No grants match the current filters.'}
        page={paged.page}
        rowsPerPage={paged.rowsPerPage}
        onPageChange={paged.setPage}
        onRowsPerPageChange={paged.setRowsPerPage}
      >
        <TableContainer sx={{ ...auditTableContainerSx, border: 'none', boxShadow: 'none', flex: 1 }}>
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell sx={auditTableHeadCellSx}>Grant ID</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Audit</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Grantee</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Type</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Modules</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Boundary</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Status</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Independence</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Valid</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {paged.paged.map((g) => (
                <TableRow
                  key={g.id}
                  hover
                  sx={{ cursor: 'pointer' }}
                  onClick={() => onOpenAudit(g.auditId)}
                >
                  <TableCell>{g.displayId}</TableCell>
                  <TableCell>{auditName(g.auditId)}</TableCell>
                  <TableCell>{userName(g.userId)}</TableCell>
                  <TableCell>{g.granteeType.replace(/_/g, ' ')}</TableCell>
                  <TableCell>{(g.modules ?? []).join(', ') || '-'}</TableCell>
                  <TableCell>{g.scopeBoundary || '-'}</TableCell>
                  <TableCell>
                    <StatusChip
                      label={g.accessStatus}
                      tone={grantTone(g.accessStatus, g.independenceCheck)}
                    />
                  </TableCell>
                  <TableCell>{g.independenceCheck}</TableCell>
                  <TableCell>
                    {g.validFrom} to {g.validTo}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </RegisterListFrame>
    </>
  )
}
