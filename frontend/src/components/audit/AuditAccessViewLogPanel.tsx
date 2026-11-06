import { useEffect, useMemo, useState } from 'react'
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material'
import { api, type AccessViewLog } from '../../api'
import { RegisterListFrame, RegisterToolbar } from '../ui'
import { useListReady } from '../../uiQuality/useListReady'
import { usePagedRows } from '../../uiQuality/usePagedRows'
import { formatTenantDateTime } from '../../uiQuality/tenantLocale'
import { auditTableContainerSx, auditTableHeadCellSx } from './auditKit'

export default function AuditAccessViewLogPanel({ onError }: { onError: (msg: string) => void }) {
  const { listReady, markReady } = useListReady()
  const [rows, setRows] = useState<AccessViewLog[]>([])
  const [search, setSearch] = useState('')

  useEffect(() => {
    api
      .listAuditAccessLog()
      .then(setRows)
      .catch((e) => onError(String(e)))
      .finally(markReady)
  }, [onError, markReady])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return rows
    return rows.filter((r) =>
      [
        r.grantDisplayId,
        r.auditDisplayId,
        r.auditName,
        r.userName,
        r.moduleCode,
        r.recordRef,
        r.action,
        r.documentName,
        r.withinScope,
      ]
        .join(' ')
        .toLowerCase()
        .includes(q),
    )
  }, [rows, search])

  const paged = usePagedRows(filtered)

  return (
    <>
      <Typography variant="subtitle1" fontWeight={700}>
        Access View Log
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
        What was opened under an access grant
      </Typography>
      <RegisterToolbar
        search={search}
        searchPlaceholder="Search access log"
        onSearchChange={setSearch}
        onClearFilters={() => setSearch('')}
      />
      <RegisterListFrame
        ready={listReady}
        count={paged.count}
        emptyMessage={rows.length === 0 ? 'No access views recorded yet.' : 'No log rows match the current filters.'}
        page={paged.page}
        rowsPerPage={paged.rowsPerPage}
        onPageChange={paged.setPage}
        onRowsPerPageChange={paged.setRowsPerPage}
      >
        <TableContainer sx={{ ...auditTableContainerSx, border: 'none', boxShadow: 'none', flex: 1 }}>
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell sx={auditTableHeadCellSx}>When</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Grant</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Audit</TableCell>
                <TableCell sx={auditTableHeadCellSx}>User</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Module</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Record</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Action</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Document</TableCell>
                <TableCell sx={auditTableHeadCellSx}>Scope</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {paged.paged.map((r) => (
                <TableRow key={r.id} hover>
                  <TableCell>{formatTenantDateTime(r.viewedAt)}</TableCell>
                  <TableCell>{r.grantDisplayId}</TableCell>
                  <TableCell>
                    {r.auditDisplayId}
                    {r.auditName && r.auditName !== '-' ? ` ${r.auditName}` : ''}
                  </TableCell>
                  <TableCell>{r.userName}</TableCell>
                  <TableCell>{r.moduleCode}</TableCell>
                  <TableCell>{r.recordRef}</TableCell>
                  <TableCell>{r.action.replace(/_/g, ' ')}</TableCell>
                  <TableCell>{r.documentName || '-'}</TableCell>
                  <TableCell>{r.withinScope}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </RegisterListFrame>
    </>
  )
}
