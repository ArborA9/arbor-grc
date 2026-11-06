import { useRef, useState } from 'react'
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { api, type BiaProcessImportResult } from '../../api'
import { BusyButton, ConfirmActionDialog } from '../ui'
import { controlActionSx } from '../ui/formLayout'

export default function BiaProcessExcelBar({
  cycleId,
  busy,
  onError,
  onImported,
}: {
  cycleId: number
  busy: boolean
  onError: (m: string) => void
  onImported: (result: BiaProcessImportResult) => void
}) {
  const fileRef = useRef<HTMLInputElement>(null)
  const [uploadBusy, setUploadBusy] = useState(false)
  const [pendingFile, setPendingFile] = useState<File | null>(null)
  const [result, setResult] = useState<BiaProcessImportResult | null>(null)

  const locked = busy || uploadBusy

  const download = () => {
    void api.downloadBiaTemplate(cycleId).catch((e) => onError(String(e)))
  }

  const runUpload = async (file: File) => {
    setUploadBusy(true)
    try {
      const report = await api.importBiaProcesses(cycleId, file)
      setResult(report)
      onImported(report)
    } catch (e) {
      onError(String(e))
    } finally {
      setUploadBusy(false)
      setPendingFile(null)
    }
  }

  return (
    <>
      <BusyButton
        size="small"
        variant="outlined"
        disabled={locked}
        disabledReason={locked ? 'Wait until the current action finishes' : undefined}
        onClick={download}
        sx={controlActionSx}
      >
        Download Template
      </BusyButton>
      <BusyButton
        size="small"
        variant="outlined"
        busy={uploadBusy}
        disabled={locked}
        disabledReason={busy && !uploadBusy ? 'Wait until the current action finishes' : undefined}
        onClick={() => fileRef.current?.click()}
        sx={controlActionSx}
      >
        {uploadBusy ? 'Importing…' : 'Import'}
      </BusyButton>
      <input
        ref={fileRef}
        type="file"
        hidden
        accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        onChange={(e) => {
          const f = e.target.files?.[0]
          e.target.value = ''
          if (!f) return
          if (!f.name.toLowerCase().endsWith('.xlsx')) {
            onError('Import an .xlsx file from the Arbor template')
            return
          }
          setPendingFile(f)
        }}
      />
      <ConfirmActionDialog
        open={pendingFile != null}
        title="Import BIA processes?"
        message="Import adds new processes from this file (one row per process). The SAMPLE example row is ignored if you leave it. Existing processes are not changed. Duplicate Process Codes are rejected."
        confirmLabel="Import and Add"
        confirmColor="primary"
        busy={uploadBusy}
        onCancel={() => !uploadBusy && setPendingFile(null)}
        onConfirm={() => {
          if (pendingFile) void runUpload(pendingFile)
        }}
      />
      <Dialog open={result != null} onClose={() => setResult(null)} fullWidth maxWidth="sm">
        <DialogTitle>Process Import Report</DialogTitle>
        <DialogContent>
          {result && (
            <Stack spacing={1.25} sx={{ mt: 0.5 }}>
              <Typography variant="body2">
                Added {result.createdCount}
                {result.ignoredCount > 0 ? ` · ${result.ignoredCount} sample row(s) ignored` : ''}
                {result.failedCount > 0 ? ` · ${result.failedCount} row(s) could not be imported` : ''}
              </Typography>
              <TableContainer sx={{ maxHeight: 320 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell>Row</TableCell>
                      <TableCell>Code</TableCell>
                      <TableCell>Result</TableCell>
                      <TableCell>Detail</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {result.rows.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={4}>
                          <Typography color="text.secondary">No data rows.</Typography>
                        </TableCell>
                      </TableRow>
                    )}
                    {result.rows.map((r) => (
                      <TableRow key={`${r.excelRow}-${r.processCode ?? ''}`}>
                        <TableCell>{r.excelRow}</TableCell>
                        <TableCell>{r.processCode ?? '-'}</TableCell>
                        <TableCell>{r.ignored ? 'Ignored' : r.success ? 'Added' : 'Failed'}</TableCell>
                        <TableCell>
                          {r.ignored || !r.success
                            ? (r.error ?? '-')
                            : (r.completenessLabel ?? '-')}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Stack>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 2.5, pb: 2 }}>
          <Button size="small" onClick={() => setResult(null)}>Close</Button>
        </DialogActions>
      </Dialog>
    </>
  )
}
