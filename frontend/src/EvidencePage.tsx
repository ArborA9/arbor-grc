import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Box,
  Button,
  Card,
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
import {
  api,
  EVIDENCE_CLASSIFICATIONS,
  RISK_EVIDENCE_STAGES,
  type EvidenceClassification,
  type EvidenceDetail,
  type EvidenceStatus,
  type EvidenceSummary,
  type RiskEvidenceStage,
  type Session,
} from './api'
import StatusChip from './components/StatusChip'
import { CompactTextField, FormSelect, RegisterListFrame, RegisterSortHeader, RegisterToolbar } from './components/ui'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { usePersistedState } from './uiQuality/usePersistedState'
import { compareLoose, toggleSort, type SortDir } from './uiQuality/sortRows'
import { EVIDENCE_REGISTER_COLUMNS } from './hooks/registerColumns'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'
import { ModuleDetailShell, ModuleListShell } from './hooks/useModulePageShell'
import { optionalIsoDateError } from './validation'
import EvidenceDetailView from './components/evidence/EvidenceDetailView'
import { EVIDENCE_ACCEPT, fileAcceptIssue } from './components/evidence/evidenceFileAccept'

type EvidenceStatusFilter = 'ALL' | EvidenceStatus
type EvidenceClassFilter = 'ALL' | EvidenceClassification

const MONO = 'ui-monospace, SFMono-Regular, Menlo, monospace'

function formatBytes(n: number) {
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / (1024 * 1024)).toFixed(1)} MB`
}

export default function EvidencePage({
  session,
  onError,
  onOk,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const canUpload = session.roles.some((r) => ['OWNER', 'ADMIN', 'CONTRIBUTOR'].includes(r))
  const canDelete = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))

  const [rows, setRows] = useState<EvidenceSummary[]>([])
  const [total, setTotal] = useState(0)
  const [q, setQ] = usePersistedState('reg-evidence-q', '')
  const [classification, setClassification] = useState<EvidenceClassFilter>('ALL')
  const [status, setStatus] = useState<EvidenceStatusFilter>('ALL')
  const [busy, setBusy] = useState(false)
  const { listReady, markReady } = useListReady()
  const [sortKey, setSortKey] = useState('title')
  const [sortDir, setSortDir] = useState<SortDir>('asc')

  const [uploadOpen, setUploadOpen] = useState(false)
  const [title, setTitle] = useState('')
  const [uploadClass, setUploadClass] = useState<EvidenceClassification>('CONFIDENTIAL')
  const [expiresOn, setExpiresOn] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [fileIssue, setFileIssue] = useState<string | null>(null)
  const [moduleCode, setModuleCode] = useState('')
  const [recordType, setRecordType] = useState('')
  const [recordId, setRecordId] = useState('')
  const [workflowStage, setWorkflowStage] = useState<RiskEvidenceStage | ''>('')

  const [detail, setDetail] = useState<EvidenceDetail | null>(null)
  const evidenceColumns = useRegisterColumnPicker(EVIDENCE_REGISTER_COLUMNS)
  const [confirmDelete, setConfirmDelete] = useState<EvidenceSummary | null>(null)

  const expiresOnError = useMemo(() => optionalIsoDateError(expiresOn), [expiresOn])

  const load = useCallback(async () => {
    setBusy(true)
    try {
      const page = await api.listEvidence({
        q: q.trim() || undefined,
        classification: classification === 'ALL' ? undefined : classification,
        status: status === 'ALL' ? undefined : status,
      })
      setRows(page.content)
      setTotal(page.totalElements)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
      markReady()
    }
  }, [q, classification, status, onError, markReady])

  useEffect(() => {
    void load()
  }, [load])

  const statusFilterOptions = useMemo(
    () => [
      { value: 'ALL' as const, label: 'All statuses' },
      { value: 'ACTIVE' as const, label: 'Active' },
      { value: 'EXPIRED' as const, label: 'Expired' },
    ],
    [],
  )

  const classFilterOptions = useMemo(
    () => [
      { value: 'ALL' as const, label: 'All classes' },
      ...EVIDENCE_CLASSIFICATIONS.map((c) => ({ value: c as EvidenceClassFilter, label: c })),
    ],
    [],
  )

  const openDetail = async (id: number) => {
    try {
      setDetail(await api.getEvidence(id))
    } catch (e) {
      onError(String(e))
    }
  }

  const pickEvidenceFile = (next: File | null) => {
    if (!next) {
      setFile(null)
      setFileIssue(null)
      return
    }
    const issue = fileAcceptIssue(next, EVIDENCE_ACCEPT)
    if (issue) {
      setFile(null)
      setFileIssue(issue)
      return
    }
    setFileIssue(null)
    setFile(next)
  }

  const runUpload = async () => {
    if (!file) {
      onError(fileIssue || 'Choose a file')
      return
    }
    if (expiresOnError) {
      onError(expiresOnError)
      return
    }
    const linking = Boolean(moduleCode.trim() && recordType.trim() && recordId.trim())
    if (linking && moduleCode.trim().toUpperCase() === 'RISK' && !workflowStage) {
      onError('Select a workflow stage when linking to RISK')
      return
    }
    setBusy(true)
    try {
      await api.uploadEvidence({
        file,
        title: title.trim() || undefined,
        classification: uploadClass,
        expiresOn: expiresOn || undefined,
        moduleCode: moduleCode.trim() || undefined,
        recordType: recordType.trim() || undefined,
        recordId: recordId.trim() || undefined,
        workflowStage: workflowStage || undefined,
      })
      setUploadOpen(false)
      setTitle('')
      setExpiresOn('')
      setFile(null)
      setFileIssue(null)
      setModuleCode('')
      setRecordType('')
      setRecordId('')
      setWorkflowStage('')
      onOk('Evidence uploaded')
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const runDelete = async () => {
    if (!confirmDelete) return
    setBusy(true)
    try {
      await api.deleteEvidence(confirmDelete.id)
      setConfirmDelete(null)
      setDetail(null)
      onOk('Evidence removed')
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const addVersion = async (id: number, next: File | null) => {
    if (!next) return
    const issue = fileAcceptIssue(next, EVIDENCE_ACCEPT)
    if (issue) {
      onError(issue)
      return
    }
    setBusy(true)
    try {
      const d = await api.addEvidenceVersion(id, next)
      setDetail(d)
      onOk(`Version ${d.currentVersion} uploaded`)
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const listActions = useMemo(
    () =>
      canUpload ? (
        <Button variant="contained" onClick={() => setUploadOpen(true)} disabled={busy}>
          Upload
        </Button>
      ) : undefined,
    [canUpload, busy],
  )

  const sortedRows = useMemo(
    () => [...rows].sort((a, b) => compareLoose(a.title, b.title, sortDir)),
    [rows, sortDir],
  )
  const paged = usePagedRows(sortedRows)

  const clearFilters = useCallback(() => {
    setQ('')
    setStatus('ALL')
    setClassification('ALL')
  }, [])

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: 0, gap: 1.25 }}>
      {detail ? (
        <ModuleDetailShell
          title={detail.title}
          breadcrumbRootLabel="Evidence Repository"
          breadcrumbTrail={detail.title}
          onBack={() => setDetail(null)}
        />
      ) : (
        <ModuleListShell title="Evidence Repository" breadcrumbTrail="Evidence" actions={listActions} />
      )}

      <Card sx={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
        {!detail ? (
          <RegisterToolbar
            search={q}
            searchPlaceholder="Search title"
            onSearchChange={setQ}
            onClearFilters={clearFilters}
            filters={[
              {
                id: 'status',
                label: 'Status',
                value: status,
                options: statusFilterOptions,
                onChange: (v) => setStatus(v as EvidenceStatusFilter),
                minWidth: 140,
              },
              {
                id: 'classification',
                label: 'Classification',
                value: classification,
                options: classFilterOptions,
                onChange: (v) => setClassification(v as EvidenceClassFilter),
                minWidth: 160,
              },
            ]}
            columnPicker={evidenceColumns.columnPicker}
          />
        ) : null}

        {!detail ? (
          <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
          <RegisterListFrame
            ready={listReady}
            count={paged.count}
            emptyMessage="No evidence yet. Upload a document to start the vault."
            page={paged.page}
            rowsPerPage={paged.rowsPerPage}
            onPageChange={paged.setPage}
            onRowsPerPageChange={paged.setRowsPerPage}
          >
          <TableContainer sx={{ flex: 1, minHeight: 0 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  {evidenceColumns.isVisible('title') && (
                    <RegisterSortHeader
                      label="Title"
                      active={sortKey === 'title'}
                      dir={sortDir}
                      onClick={() => {
                        const next = toggleSort(sortKey, sortDir, 'title')
                        setSortKey(next.key)
                        setSortDir(next.dir)
                      }}
                    />
                  )}
                  {evidenceColumns.isVisible('classification') && <TableCell width={120}>Classification</TableCell>}
                  {evidenceColumns.isVisible('status') && <TableCell width={90}>Status</TableCell>}
                  {evidenceColumns.isVisible('version') && <TableCell width={70}>Ver</TableCell>}
                  {evidenceColumns.isVisible('file') && <TableCell>File</TableCell>}
                  {evidenceColumns.isVisible('links') && <TableCell width={70}>Links</TableCell>}
                  {evidenceColumns.isVisible('expires') && <TableCell width={110}>Expires</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((row) => (
                  <TableRow
                    key={row.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => void openDetail(row.id)}
                  >
                    {evidenceColumns.isVisible('title') && (
                    <TableCell>
                      <Typography variant="body2" fontWeight={600} noWrap>
                        {row.title}
                      </Typography>
                    </TableCell>
                    )}
                    {evidenceColumns.isVisible('classification') && (
                    <TableCell>
                      <StatusChip
                        label={row.classification}
                        tone={
                          row.classification === 'OPEN'
                            ? 'success'
                            : row.classification === 'CONFIDENTIAL'
                              ? 'info'
                              : 'warning'
                        }
                      />
                    </TableCell>
                    )}
                    {evidenceColumns.isVisible('status') && (
                    <TableCell>
                      <StatusChip
                        label={row.status}
                        tone={row.status === 'EXPIRED' ? 'warning' : 'neutral'}
                      />
                    </TableCell>
                    )}
                    {evidenceColumns.isVisible('version') && (
                    <TableCell>
                      <Typography variant="caption" fontFamily={MONO}>
                        v{row.currentVersion}
                      </Typography>
                    </TableCell>
                    )}
                    {evidenceColumns.isVisible('file') && (
                    <TableCell>
                      <Typography variant="caption" noWrap display="block">
                        {row.latestFilename ?? '-'}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {formatBytes(row.latestSizeBytes)}
                      </Typography>
                    </TableCell>
                    )}
                    {evidenceColumns.isVisible('links') && <TableCell>{row.linkCount}</TableCell>}
                    {evidenceColumns.isVisible('expires') && (
                    <TableCell>
                      <Typography variant="caption" color="text.secondary">
                        {row.expiresOn ?? '-'}
                      </Typography>
                    </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          </RegisterListFrame>
          </Box>
        ) : (
          <Box sx={{ flex: 1, minHeight: 0, overflow: 'auto', p: 2 }}>
            <EvidenceDetailView
              detail={detail}
              busy={busy}
              canUpload={canUpload}
              canDelete={canDelete}
              onDownload={(version) => {
                api.downloadEvidence(detail.id, version).catch((e) => onError(String(e)))
              }}
              onUploadVersion={(file) => {
                void addVersion(detail.id, file)
              }}
              onDelete={() => {
                const row = rows.find((r) => r.id === detail.id)
                if (row) setConfirmDelete(row)
              }}
            />
          </Box>
        )}
      </Card>

      <Dialog open={uploadOpen} onClose={() => !busy && setUploadOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle sx={{ pb: 1 }}>Upload Evidence</DialogTitle>
        <DialogContent>
          <Stack spacing={1.75} sx={{ pt: 0.5 }}>
            <Typography variant="body2" color="text.secondary">
              Allowed: PDF, DOCX, XLSX, PNG, JPG · max 25MB. Classification is mandatory.
            </Typography>
            <Button variant="outlined" component="label">
              {file ? file.name : 'Choose File'}
              <input
                hidden
                type="file"
                accept={EVIDENCE_ACCEPT}
                onChange={(e) => {
                  pickEvidenceFile(e.target.files?.[0] ?? null)
                  e.target.value = ''
                }}
              />
            </Button>
            {fileIssue ? (
              <Typography variant="caption" color="error">
                {fileIssue}
              </Typography>
            ) : null}
            <CompactTextField
              label="Title"
              fieldIdPrefix="evidence"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Defaults to filename"
            />
            <FormSelect
              label="Classification"
              value={uploadClass}
              options={EVIDENCE_CLASSIFICATIONS.map((c) => ({ value: c, label: c }))}
              onChange={(v) => setUploadClass(v as EvidenceClassification)}
              fullWidth
            />
            <CompactTextField
              label="Expires on (optional)"
              type="date"
              fieldIdPrefix="evidence"
              value={expiresOn}
              onChange={(e) => setExpiresOn(e.target.value)}
              InputLabelProps={{ shrink: true }}
              error={!!expiresOnError}
              helperText={expiresOnError || 'Past dates allowed for backfilled records'}
            />
            <Typography variant="caption" color="text.secondary">
              Optional link (module / type / id). RISK links require a workflow stage.
            </Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <CompactTextField
                label="Module"
                fieldIdPrefix="evidence"
                placeholder="RISK"
                value={moduleCode}
                onChange={(e) => setModuleCode(e.target.value)}
              />
              <CompactTextField
                label="Record type"
                fieldIdPrefix="evidence"
                placeholder="RISK_RECORD"
                value={recordType}
                onChange={(e) => setRecordType(e.target.value)}
              />
              <CompactTextField
                label="Record id"
                fieldIdPrefix="evidence"
                placeholder="123"
                value={recordId}
                onChange={(e) => setRecordId(e.target.value)}
              />
            </Stack>
            {(moduleCode.trim().toUpperCase() === 'RISK' || workflowStage) && (
              <FormSelect
                label="Workflow stage"
                value={workflowStage}
                options={[
                  { value: '', label: 'Select…' },
                  ...RISK_EVIDENCE_STAGES.map((s) => ({ value: s, label: s })),
                ]}
                onChange={(v) => setWorkflowStage(v as RiskEvidenceStage | '')}
                fullWidth
              />
            )}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setUploadOpen(false)} disabled={busy}>
            Cancel
          </Button>
          <Button variant="contained" onClick={runUpload} disabled={busy || !file || !!expiresOnError}>
            {busy ? 'Uploading…' : 'Upload'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(confirmDelete)} onClose={() => !busy && setConfirmDelete(null)} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ pb: 1 }}>Delete evidence?</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary">
            Soft-delete <strong>{confirmDelete?.title}</strong>? It will leave the vault list. Files stay on disk for
            audit until hard-purge is added.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setConfirmDelete(null)} disabled={busy}>
            Cancel
          </Button>
          <Button color="error" variant="contained" onClick={runDelete} disabled={busy}>
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
