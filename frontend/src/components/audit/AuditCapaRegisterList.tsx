import { useMemo, useState } from 'react'
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
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import type { CapaRegisterRecord } from '../../api'
import {
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

export default function AuditCapaRegisterList({
  ready,
  registers,
  onSelect,
}: {
  ready: boolean
  registers: CapaRegisterRecord[]
  onSelect: (r: CapaRegisterRecord) => void
}) {
  const [query, setQuery] = useState('')
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return registers
    return registers.filter(
      (r) => r.name.toLowerCase().includes(q) || String(r.year).includes(q),
    )
  }, [registers, query])
  const paged = usePagedRows(filtered)

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, p: 1.5 }}>
      <Card
        sx={{
          flex: 1,
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
          gap={1.25}
          sx={{ px: 1.75, pt: 1.5, pb: 1 }}
        >
          <Typography sx={{ fontSize: '0.9375rem', fontWeight: 650, letterSpacing: '-0.01em' }}>
            CAPA registers
          </Typography>
          <TextField
            size="small"
            placeholder="Year or name"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            inputProps={{ 'aria-label': 'Search CAPA registers' }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchRoundedIcon sx={{ fontSize: 16, color: brand.mutedLight }} />
                </InputAdornment>
              ),
            }}
            sx={{
              width: 280,
              maxWidth: 280,
              flex: '0 0 280px',
              '& .MuiOutlinedInput-root': {
                height: 32,
                bgcolor: brand.paper,
              },
            }}
          />
        </Stack>
        <RegisterListFrame
          ready={ready}
          count={paged.count}
          emptyMessage={
            registers.length === 0
              ? 'No CAPA registers yet. Add a register for the year, then add CAPAs.'
              : 'No registers match the current search.'
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
                  <TableCell sx={{ ...registerHeadCellSx, ...compactTableCellSx }}>Year</TableCell>
                  <TableCell sx={{ ...registerHeadCellSx, ...primaryTableCellSx }}>Name</TableCell>
                  <TableCell sx={{ ...registerHeadCellSx, ...lastTableCellSx }}>Total findings</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((r) => (
                  <TableRow key={r.id} hover sx={{ cursor: 'pointer' }} onClick={() => onSelect(r)}>
                    <TableCell>{r.year}</TableCell>
                    <TableCell>
                      <Box sx={primaryRegisterNameSx}>{r.name}</Box>
                    </TableCell>
                    <TableCell>{r.capaCount}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </RegisterListFrame>
      </Card>
    </Stack>
  )
}
