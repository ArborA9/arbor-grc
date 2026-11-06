import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Box,
  Button,
  Card,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  InputAdornment,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import {
  api,
  ApiError,
  type AuditChecklistLibrary,
  type AuditChecklistLibraryItem,
  type GrcAuditDetail,
  type GrcAuditSummary,
  type Session,
} from './api'
import { useAppShellPage } from './components/AppShellPageContext'
import AuditWorkingPaperPanel from './components/audit/AuditWorkingPaperPanel'
import { isAuditorConsole } from './components/audit/auditRoles'
import {
  BusyButton,
  CompactTextField,
  ConfirmActionDialog,
  EmptyRegister,
  INPUT_LIMIT,
  NARRATIVE_FIELD_MAX_WIDTH,
  NARRATIVE_MAX_ROWS,
  narrativeReadScrollSx,
  PageLoading,
  RegisterListFrame,
  controlActionSx,
  formFieldGridSx,
  lastTableCellSx,
  compactTableCellSx,
  compactTableCellSxWidth,
  mediumTableCellSx,
  primaryRegisterNameSx,
  primaryTableCellSx,
  registerHeadCellSx,
  registerTableSx,
} from './components/ui'
import StatusChip from './components/StatusChip'
import { brand } from './theme'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { useOptionalUnsavedChangesGuardApi, useRegisterUnsavedChanges } from './uiQuality/UnsavedChangesGuard'

type DraftItem = {
  clientId: string
  id?: number
  clauseRef: string
  section: string
  title: string
  testFocus: string
  soaApplicability: string
  evidenceLives: string
}

type RowForm = {
  clauseRef: string
  section: string
  title: string
  testFocus: string
  soaApplicability: string
  evidenceLives: string
}

const EMPTY_FORM: RowForm = {
  clauseRef: '',
  section: '',
  title: '',
  testFocus: '',
  soaApplicability: '',
  evidenceLives: '',
}

function newClientId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function toDraft(it: AuditChecklistLibraryItem): DraftItem {
  return {
    clientId: `row-${it.id}`,
    id: it.id,
    clauseRef: it.clauseRef ?? '',
    section: it.section ?? '',
    title: it.title ?? '',
    testFocus: it.testFocus ?? '',
    soaApplicability: it.soaApplicability ?? '',
    evidenceLives: it.evidenceLives ?? '',
  }
}

function snapshot(name: string, items: DraftItem[]) {
  return JSON.stringify({
    name: name.trim(),
    items: items.map((i) => ({
      id: i.id ?? null,
      clauseRef: i.clauseRef.trim(),
      section: i.section.trim(),
      title: i.title.trim(),
      testFocus: i.testFocus.trim(),
      soaApplicability: i.soaApplicability.trim(),
      evidenceLives: i.evidenceLives.trim(),
    })),
  })
}

function pickXlsx(): Promise<File | null> {
  return new Promise((resolve) => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = '.xlsx,.xls,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    input.style.position = 'fixed'
    input.style.left = '-9999px'
    document.body.appendChild(input)
    input.addEventListener(
      'change',
      () => {
        const file = input.files?.[0] ?? null
        input.remove()
        resolve(file)
      },
      { once: true },
    )
    input.click()
  })
}

function duplicateClause(items: DraftItem[], code: string, exceptClientId: string | null) {
  const key = code.trim().toLowerCase()
  if (!key) return null
  const clash = items.find(
    (d) => d.clientId !== exceptClientId && d.clauseRef.trim().toLowerCase() === key,
  )
  return clash ? clash.clauseRef.trim() : null
}

function templateLabel(key: string) {
  if (key === 'ISO_27001') return 'ISO 27001'
  if (key === 'ISO_20000') return 'ISO 20000'
  if (key === 'UAE_IA') return 'UAE IA'
  return 'Custom'
}

export default function AuditChecklistsPage({
  session,
  onError,
  onOk,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const canAdmin = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const auditorConsole = isAuditorConsole(session.roles)
  if (auditorConsole && !canAdmin) {
    return <AuditorChecklistFill session={session} onError={onError} onOk={onOk} />
  }
  return <AdminChecklistLibrary canAdmin={canAdmin} onError={onError} onOk={onOk} />
}

function AuditorChecklistFill({
  session,
  onError,
  onOk,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const [audits, setAudits] = useState<GrcAuditSummary[]>([])
  const [detail, setDetail] = useState<GrcAuditDetail | null>(null)
  const { listReady, markReady } = useListReady()
  const paged = usePagedRows(audits)
  void session

  const load = useCallback(async () => {
    try {
      setAudits(await api.listAudits())
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
      setAudits([])
    } finally {
      markReady()
    }
  }, [onError, markReady])

  useEffect(() => {
    void load()
  }, [load])

  const openAudit = async (id: number) => {
    try {
      setDetail(await api.getAudit(id))
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
    }
  }

  const refresh = async () => {
    if (!detail) return
    setDetail(await api.getAudit(detail.audit.id))
  }

  useAppShellPage(
    {
      title: detail ? detail.audit.name : 'Audit Checklist',
      breadcrumbTrail: detail ? detail.audit.name : 'Audit › Audit Checklist',
      breadcrumbRoot: detail
        ? { label: 'Audit Checklist', onClick: () => setDetail(null) }
        : undefined,
      hideContextLabel: true,
      hideRoleChips: true,
    },
    [detail],
  )

  if (detail) {
    return (
      <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, p: 1.5 }}>
        <Box sx={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
          <AuditWorkingPaperPanel
            detail={detail}
            canEdit
            onRefresh={refresh}
            onError={onError}
            onOk={onOk}
          />
        </Box>
      </Stack>
    )
  }

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, p: 1.5 }}>
      <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        <Box sx={{ px: 1.5, pt: 1.25, pb: 0.5 }}>
          <Typography variant="subtitle2">Assigned audits</Typography>
          <Typography variant="body2" color="text.secondary">
            Open an audit to fill the working-paper checklist.
          </Typography>
        </Box>
        <RegisterListFrame
          ready={listReady}
          count={paged.count}
          emptyMessage="No assigned audits yet."
          page={paged.page}
          rowsPerPage={paged.rowsPerPage}
          onPageChange={paged.setPage}
          onRowsPerPageChange={paged.setRowsPerPage}
        >
          <TableContainer>
            <Table size="small" sx={registerTableSx}>
              <TableHead>
                <TableRow>
                  <TableCell sx={primaryTableCellSx}>Audit Title</TableCell>
                  <TableCell sx={compactTableCellSx}>Type</TableCell>
                  <TableCell sx={lastTableCellSx}>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((a) => (
                  <TableRow
                    key={a.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => void openAudit(a.id)}
                  >
                    <TableCell sx={primaryRegisterNameSx}>
                      {a.displayId ? `${a.displayId}  ` : ''}
                      {a.name}
                    </TableCell>
                    <TableCell sx={compactTableCellSx}>{a.auditType}</TableCell>
                    <TableCell sx={lastTableCellSx}>{a.status.replace(/_/g, ' ')}</TableCell>
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

function AdminChecklistLibrary({
  canAdmin,
  onError,
  onOk,
}: {
  canAdmin: boolean
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const { requestNavigation } = useOptionalUnsavedChangesGuardApi()
  const [rows, setRows] = useState<AuditChecklistLibrary[]>([])
  const [name, setName] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [rename, setRename] = useState('')
  const [drafts, setDrafts] = useState<DraftItem[]>([])
  const [baseline, setBaseline] = useState('')
  const [form, setForm] = useState<RowForm>(EMPTY_FORM)
  const [formBaseline, setFormBaseline] = useState('')
  const [editingClientId, setEditingClientId] = useState<string | null>(null)
  const [rowOpen, setRowOpen] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<DraftItem | null>(null)
  const [deleteLibrary, setDeleteLibrary] = useState<AuditChecklistLibrary | null>(null)
  const [confirmUpload, setConfirmUpload] = useState(false)
  const [query, setQuery] = useState('')
  const { listReady, markReady } = useListReady()
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return rows
    return rows.filter((r) => {
      const hay = [
        r.name,
        templateLabel(r.templateKey),
        r.systemSeeded ? 'Starter template' : 'Custom',
      ]
        .join(' ')
        .toLowerCase()
      return hay.includes(q)
    })
  }, [rows, query])
  const paged = usePagedRows(filtered)
  const pagedDrafts = usePagedRows(drafts)
  const creating = createOpen && name.trim().length > 0 && selectedId == null

  const closeCreate = () => {
    setCreateOpen(false)
    setName('')
  }

  const selected = useMemo(() => rows.find((r) => r.id === selectedId) ?? null, [rows, selectedId])
  const currentSnap = snapshot(rename, drafts)
  const dirtyEditor = selected != null && currentSnap !== baseline
  const formDirty = rowOpen && JSON.stringify(form) !== formBaseline
  const rowWhy = !form.clauseRef.trim()
    ? 'Enter a Clause / Annex A Ref'
    : !form.title.trim()
      ? 'Enter a Clause / Control Title'
      : duplicateClause(drafts, form.clauseRef, editingClientId)
        ? `Clause ref "${form.clauseRef.trim()}" already exists on this checklist`
        : undefined

  useRegisterUnsavedChanges(
    creating && !busy
      ? {
          dirty: true,
          onSaveDraft: () => create(),
          saveDraftLabel: 'Create Checklist',
        }
      : selected && formDirty && !busy
        ? {
            dirty: true,
            onSaveDraft: () => submitRow(),
            saveDraftLabel: editingClientId ? 'Save Row' : 'Add Row',
          }
        : selected && dirtyEditor && !busy
          ? {
              dirty: true,
              onSaveDraft: async () => {
                const ok = await persist(drafts)
                if (!ok) throw new Error('save failed')
              },
              saveDraftLabel: 'Save',
            }
          : null,
  )

  const applyLoaded = (q: AuditChecklistLibrary) => {
    const next = (q.items ?? []).map(toDraft)
    setRename(q.name)
    setDrafts(next)
    setBaseline(snapshot(q.name, next))
  }

  const load = useCallback(async () => {
    try {
      const list = await api.listAuditChecklistLibraries()
      setRows(list)
      return list
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
      setRows([])
      return [] as AuditChecklistLibrary[]
    } finally {
      markReady()
    }
  }, [onError, markReady])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    if (!selected) {
      setRename('')
      setDrafts([])
      setBaseline('')
      setRowOpen(false)
      setForm(EMPTY_FORM)
      setEditingClientId(null)
      return
    }
    applyLoaded(selected)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected?.id, selected?.updatedAt])

  const create = async () => {
    const n = name.trim()
    if (!n) {
      onError('Checklist name is required')
      return
    }
    setBusy(true)
    try {
      const q = await api.createAuditChecklistLibrary(n)
      closeCreate()
      onOk('Checklist created')
      const list = await load()
      const created = list.find((r) => r.id === q.id) ?? q
      setSelectedId(created.id)
      applyLoaded(created)
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const persist = async (next: DraftItem[]) => {
    if (!selected) return false
    const n = (rename.trim() || selected.name).trim()
    if (!n) {
      onError('Checklist name is required')
      return false
    }
    const seen = new Set<string>()
    for (const d of next) {
      const key = d.clauseRef.trim().toLowerCase()
      if (!key) {
        onError('Each row needs a Clause / Annex A Ref')
        return false
      }
      if (seen.has(key)) {
        onError(`Clause ref "${d.clauseRef.trim()}" is duplicated`)
        return false
      }
      seen.add(key)
      if (!d.title.trim()) {
        onError('Each row needs a Clause / Control Title')
        return false
      }
    }
    setBusy(true)
    try {
      const q = await api.saveAuditChecklistLibrary(selected.id, {
        name: n,
        items: next.map((d) => ({
          id: d.id,
          clauseRef: d.clauseRef.trim(),
          section: d.section.trim(),
          title: d.title.trim(),
          testFocus: d.testFocus.trim(),
          soaApplicability: d.soaApplicability.trim(),
          evidenceLives: d.evidenceLives.trim(),
        })),
      })
      const list = await load()
      const latest = list.find((r) => r.id === q.id) ?? q
      applyLoaded(latest)
      onOk('Checklist saved')
      return true
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
      return false
    } finally {
      setBusy(false)
    }
  }

  const closeRowDialog = () => {
    setRowOpen(false)
    setEditingClientId(null)
    setForm(EMPTY_FORM)
    setFormBaseline(JSON.stringify(EMPTY_FORM))
  }

  const openAddRow = () => {
    setEditingClientId(null)
    setForm(EMPTY_FORM)
    setFormBaseline(JSON.stringify(EMPTY_FORM))
    setRowOpen(true)
  }

  const openEditRow = (item: DraftItem) => {
    const next: RowForm = {
      clauseRef: item.clauseRef,
      section: item.section,
      title: item.title,
      testFocus: item.testFocus,
      soaApplicability: item.soaApplicability,
      evidenceLives: item.evidenceLives,
    }
    setEditingClientId(item.clientId)
    setForm(next)
    setFormBaseline(JSON.stringify(next))
    setRowOpen(true)
  }

  const submitRow = async () => {
    if (rowWhy) {
      onError(rowWhy)
      throw new Error(rowWhy)
    }
    const nextItem: DraftItem = {
      clientId: editingClientId ?? newClientId(),
      id: editingClientId ? drafts.find((d) => d.clientId === editingClientId)?.id : undefined,
      clauseRef: form.clauseRef.trim(),
      section: form.section.trim(),
      title: form.title.trim(),
      testFocus: form.testFocus.trim(),
      soaApplicability: form.soaApplicability.trim(),
      evidenceLives: form.evidenceLives.trim(),
    }
    const next = editingClientId
      ? drafts.map((d) => (d.clientId === editingClientId ? nextItem : d))
      : [...drafts, nextItem]
    const ok = await persist(next)
    if (!ok) throw new Error('save failed')
    closeRowDialog()
  }

  const runUpload = async () => {
    if (!selected) return
    const file = await pickXlsx()
    if (!file) return
    setBusy(true)
    try {
      const q = await api.uploadAuditChecklistLibrary(selected.id, file)
      onOk('Template uploaded')
      const list = await load()
      const latest = list.find((r) => r.id === q.id) ?? q
      applyLoaded(latest)
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
    } finally {
      setBusy(false)
      setConfirmUpload(false)
    }
  }

  const requestUpload = () => {
    if (dirtyEditor || formDirty) {
      setConfirmUpload(true)
      return
    }
    void runUpload()
  }

  const closeEditor = () => {
    requestNavigation(() => {
      setSelectedId(null)
      closeRowDialog()
    })
  }

  const removeLibrary = async () => {
    if (!deleteLibrary) return
    setBusy(true)
    try {
      await api.deleteAuditChecklistLibrary(deleteLibrary.id)
      onOk('Checklist deleted')
      setDeleteLibrary(null)
      if (selectedId === deleteLibrary.id) {
        setSelectedId(null)
        closeRowDialog()
      }
      await load()
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  useAppShellPage(
    {
      title: selected ? selected.name : 'Audit Checklist',
      breadcrumbTrail: selected ? selected.name : 'Audit › Audit Checklist',
      breadcrumbRoot: selected ? { label: 'Audit Checklist', onClick: closeEditor } : undefined,
      hideContextLabel: true,
      hideRoleChips: true,
      actions:
        canAdmin && selected ? (
          <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
            <Button size="small" variant="outlined" sx={controlActionSx} onClick={closeEditor}>
              Back
            </Button>
            <BusyButton size="small" variant="contained" busy={busy} onClick={openAddRow} sx={controlActionSx}>
              Add a Row
            </BusyButton>
            <BusyButton
              size="small"
              variant="outlined"
              busy={busy}
              onClick={() => void api.downloadAuditChecklistTemplate()}
              sx={controlActionSx}
            >
              Download Template
            </BusyButton>
            <BusyButton size="small" variant="outlined" busy={busy} onClick={requestUpload} sx={controlActionSx}>
              Upload
            </BusyButton>
          </Stack>
        ) : canAdmin ? (
          <BusyButton
            size="small"
            variant="contained"
            busy={busy}
            disabled={createOpen}
            onClick={() => setCreateOpen(true)}
            sx={controlActionSx}
          >
            Create Checklist
          </BusyButton>
        ) : undefined,
    },
    [canAdmin, selected, busy, dirtyEditor, formDirty, createOpen],
  )

  if (selected) {
    return (
      <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, p: 1.5 }}>
        <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          {selected.lineCount > 0 && drafts.length === 0 ? (
            <PageLoading />
          ) : drafts.length === 0 ? (
            <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <EmptyRegister
                message="No rows"
                action={
                  canAdmin ? (
                    <Button size="small" variant="contained" onClick={openAddRow}>
                      Add a Row
                    </Button>
                  ) : undefined
                }
              />
            </Box>
          ) : (
            <RegisterListFrame
              ready
              count={pagedDrafts.count}
              emptyMessage="No rows"
              page={pagedDrafts.page}
              rowsPerPage={pagedDrafts.rowsPerPage}
              onPageChange={pagedDrafts.setPage}
              onRowsPerPageChange={pagedDrafts.setRowsPerPage}
            >
              <TableContainer sx={{ flex: 1 }}>
                <Table size="small" sx={registerTableSx}>
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ ...compactTableCellSxWidth('11%'), whiteSpace: 'normal', overflow: 'visible' }}>
                        Clause / Annex A Ref
                      </TableCell>
                      <TableCell sx={{ ...compactTableCellSxWidth('10%'), whiteSpace: 'normal', overflow: 'visible' }}>
                        Section
                      </TableCell>
                      <TableCell sx={{ ...primaryTableCellSx, width: '20%', whiteSpace: 'normal', overflow: 'visible' }}>
                        Clause / Control Title
                      </TableCell>
                      <TableCell sx={{ ...mediumTableCellSx, width: '22%', whiteSpace: 'normal', overflow: 'visible' }}>
                        What the Auditor Is Testing
                      </TableCell>
                      <TableCell sx={{ ...compactTableCellSxWidth('12%'), whiteSpace: 'normal', overflow: 'visible' }}>
                        SoA Applicability
                      </TableCell>
                      <TableCell sx={{ ...compactTableCellSxWidth('12%'), whiteSpace: 'normal', overflow: 'visible' }}>
                        Where the Evidence Lives
                      </TableCell>
                      {canAdmin && (
                        <TableCell sx={{ ...lastTableCellSx, ...compactTableCellSxWidth('13%'), overflow: 'visible' }}>
                          Actions
                        </TableCell>
                      )}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {pagedDrafts.paged.map((it) => (
                      <TableRow key={it.clientId} hover>
                        <TableCell sx={{ ...compactTableCellSxWidth('11%'), whiteSpace: 'normal' }}>
                          {it.clauseRef || '-'}
                        </TableCell>
                        <TableCell sx={{ ...compactTableCellSxWidth('10%'), whiteSpace: 'normal' }}>
                          {it.section || '-'}
                        </TableCell>
                        <TableCell sx={{ ...primaryRegisterNameSx, width: '20%', whiteSpace: 'normal' }}>
                          <Box sx={narrativeReadScrollSx}>{it.title || '-'}</Box>
                        </TableCell>
                        <TableCell sx={{ ...mediumTableCellSx, width: '22%', whiteSpace: 'normal' }}>
                          <Box sx={narrativeReadScrollSx}>{it.testFocus || '-'}</Box>
                        </TableCell>
                        <TableCell sx={{ ...compactTableCellSxWidth('12%'), whiteSpace: 'normal' }}>
                          {it.soaApplicability || '-'}
                        </TableCell>
                        <TableCell sx={{ ...compactTableCellSxWidth('12%'), whiteSpace: 'normal' }}>
                          <Box sx={narrativeReadScrollSx}>{it.evidenceLives || '-'}</Box>
                        </TableCell>
                        {canAdmin && (
                          <TableCell sx={{ ...lastTableCellSx, ...compactTableCellSxWidth('13%'), overflow: 'visible' }}>
                            <Stack direction="row" spacing={0.5}>
                              <Button
                                size="small"
                                variant="outlined"
                                sx={controlActionSx}
                                onClick={() => openEditRow(it)}
                              >
                                Edit
                              </Button>
                              <Button
                                size="small"
                                color="error"
                                variant="outlined"
                                sx={controlActionSx}
                                onClick={() => setDeleteTarget(it)}
                              >
                                Delete
                              </Button>
                            </Stack>
                          </TableCell>
                        )}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </RegisterListFrame>
          )}
        </Card>

        <Dialog
          open={rowOpen}
          onClose={busy
            ? undefined
            : () => {
                if (formDirty) return
                closeRowDialog()
              }}
          fullWidth
          maxWidth="sm"
        >
          <DialogTitle sx={{ pb: 1 }}>{editingClientId ? 'Edit Row' : 'Add a Row'}</DialogTitle>
          <DialogContent>
            <Stack spacing={1.25} sx={{ mt: 0.5 }}>
              <Box sx={formFieldGridSx}>
                <CompactTextField
                  label="Clause / Annex A Ref"
                  required
                  fieldIdPrefix="audit-cl-item"
                  value={form.clauseRef}
                  onChange={(e) => setForm({ ...form, clauseRef: e.target.value })}
                  maxLength={80}
                />
                <CompactTextField
                  label="Section"
                  fieldIdPrefix="audit-cl-item"
                  value={form.section}
                  onChange={(e) => setForm({ ...form, section: e.target.value })}
                  maxLength={120}
                />
                <CompactTextField
                  label="SoA Applicability"
                  fieldIdPrefix="audit-cl-item"
                  value={form.soaApplicability}
                  onChange={(e) => setForm({ ...form, soaApplicability: e.target.value })}
                  maxLength={120}
                />
              </Box>
              <Box sx={{ maxWidth: NARRATIVE_FIELD_MAX_WIDTH }}>
                <CompactTextField
                  label="Clause / Control Title"
                  required
                  fieldIdPrefix="audit-cl-item"
                  value={form.title}
                  onChange={(e) => setForm({ ...form, title: e.target.value })}
                  maxLength={512}
                  maxWidth="100%"
                />
              </Box>
              <Box sx={{ maxWidth: NARRATIVE_FIELD_MAX_WIDTH }}>
                <CompactTextField
                  label="What the Auditor Is Testing"
                  multiline
                  minRows={3}
                  maxRows={NARRATIVE_MAX_ROWS}
                  fieldIdPrefix="audit-cl-item"
                  value={form.testFocus}
                  onChange={(e) => setForm({ ...form, testFocus: e.target.value })}
                  maxLength={4000}
                  maxWidth="100%"
                />
              </Box>
              <Box sx={{ maxWidth: NARRATIVE_FIELD_MAX_WIDTH }}>
                <CompactTextField
                  label="Where the Evidence Lives"
                  fieldIdPrefix="audit-cl-item"
                  value={form.evidenceLives}
                  onChange={(e) => setForm({ ...form, evidenceLives: e.target.value })}
                  maxLength={300}
                  maxWidth="100%"
                />
              </Box>
            </Stack>
          </DialogContent>
          <DialogActions sx={{ px: 2.5, pb: 2 }}>
            <Button onClick={closeRowDialog} disabled={busy}>
              Cancel
            </Button>
            <BusyButton
              size="small"
              variant="contained"
              busy={busy}
              disabled={Boolean(rowWhy)}
              disabledReason={rowWhy}
              onClick={() => {
                void submitRow().catch(() => undefined)
              }}
            >
              {editingClientId ? 'Save Row' : 'Add Row'}
            </BusyButton>
          </DialogActions>
        </Dialog>

        <ConfirmActionDialog
          open={deleteTarget != null}
          title="Delete this row?"
          message="This removes the row from the checklist."
          confirmLabel={busy ? 'Deleting…' : 'Delete Row'}
          busy={busy}
          onCancel={() => setDeleteTarget(null)}
          onConfirm={() => {
            if (!deleteTarget) return
            const next = drafts.filter((d) => d.clientId !== deleteTarget.clientId)
            setDeleteTarget(null)
            if (editingClientId === deleteTarget.clientId) closeRowDialog()
            void persist(next)
          }}
        />
        <ConfirmActionDialog
          open={confirmUpload}
          title="Replace rows from Excel?"
          message="Upload replaces the row list on the server. Unsaved edits on this screen will be lost."
          confirmLabel="Upload and Replace"
          confirmColor="warning"
          busy={busy}
          onCancel={() => setConfirmUpload(false)}
          onConfirm={() => void runUpload()}
        />
      </Stack>
    )
  }

  return (
    <Stack spacing={1.5} sx={{ flex: 1, minHeight: 0, p: 1.5 }}>
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
            Saved checklists
          </Typography>
          <TextField
            size="small"
            placeholder="Name or standard"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            inputProps={{ 'aria-label': 'Search checklists' }}
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
          ready={listReady}
          count={paged.count}
          emptyMessage={
            rows.length === 0
              ? 'No checklists yet.'
              : 'No checklists match the current search.'
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
                  <TableCell sx={{ ...registerHeadCellSx, ...primaryTableCellSx, width: '44%' }}>
                    Name
                  </TableCell>
                  <TableCell sx={{ ...registerHeadCellSx, ...compactTableCellSx }}>Standard</TableCell>
                  <TableCell sx={{ ...registerHeadCellSx, ...compactTableCellSx }}>Rows</TableCell>
                  <TableCell sx={{ ...registerHeadCellSx, ...compactTableCellSx }}>Source</TableCell>
                  {canAdmin && (
                    <TableCell
                      sx={{
                        ...registerHeadCellSx,
                        ...lastTableCellSx,
                        ...compactTableCellSxWidth('10%'),
                        overflow: 'visible',
                      }}
                    >
                      Actions
                    </TableCell>
                  )}
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((q) => (
                  <TableRow
                    key={q.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() =>
                      requestNavigation(() => {
                        applyLoaded(q)
                        setSelectedId(q.id)
                      })
                    }
                  >
                    <TableCell sx={{ ...primaryRegisterNameSx, width: '44%' }}>{q.name}</TableCell>
                    <TableCell sx={compactTableCellSx}>
                      <StatusChip
                        label={templateLabel(q.templateKey)}
                        tone={q.templateKey === 'CUSTOM' ? 'neutral' : 'info'}
                      />
                    </TableCell>
                    <TableCell sx={compactTableCellSx}>{q.lineCount}</TableCell>
                    <TableCell sx={compactTableCellSx}>
                      <StatusChip
                        label={q.systemSeeded ? 'Starter template' : 'Custom'}
                        tone={q.systemSeeded ? 'info' : 'neutral'}
                      />
                    </TableCell>
                    {canAdmin && (
                      <TableCell
                        sx={{
                          ...lastTableCellSx,
                          ...compactTableCellSxWidth('10%'),
                          overflow: 'visible',
                        }}
                        onClick={(e) => e.stopPropagation()}
                      >
                        {q.systemSeeded ? (
                          <Typography sx={{ fontSize: '0.75rem', color: brand.mutedLight }}>-</Typography>
                        ) : (
                          <Tooltip title="Delete">
                            <IconButton
                              size="small"
                              aria-label="Delete"
                              onClick={() => setDeleteLibrary(q)}
                              sx={{
                                width: 32,
                                height: 32,
                                border: `1px solid ${brand.border}`,
                                borderRadius: 1,
                                color: brand.inkSoft,
                                '&:hover': {
                                  borderColor: '#DC2626',
                                  bgcolor: '#FEF2F2',
                                  color: '#B91C1C',
                                },
                              }}
                            >
                              <DeleteOutlineIcon sx={{ fontSize: 18 }} />
                            </IconButton>
                          </Tooltip>
                        )}
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </RegisterListFrame>
      </Card>
      <Dialog
        open={createOpen}
        onClose={() => {
          if (!busy) closeCreate()
        }}
        fullWidth
        maxWidth="xs"
      >
        <DialogTitle>Create Checklist</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 0.5 }}>
            <CompactTextField
              label="Checklist name"
              required
              fieldIdPrefix="audit-cl-new"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Site walkthrough"
              maxLength={INPUT_LIMIT.orgName}
              maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && name.trim() && !busy) {
                  e.preventDefault()
                  void create()
                }
              }}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button size="small" sx={controlActionSx} disabled={busy} onClick={closeCreate}>
            Cancel
          </Button>
          <BusyButton
            size="small"
            variant="contained"
            busy={busy}
            disabled={!name.trim()}
            disabledReason={!name.trim() ? 'Enter a checklist name' : undefined}
            onClick={() => void create()}
            sx={controlActionSx}
          >
            Create
          </BusyButton>
        </DialogActions>
      </Dialog>
      <ConfirmActionDialog
        open={deleteLibrary != null}
        title="Delete this checklist?"
        message="This removes the custom checklist from the library. Audits that already copied it keep their working paper."
        confirmLabel={busy ? 'Deleting…' : 'Delete Checklist'}
        busy={busy}
        onCancel={() => setDeleteLibrary(null)}
        onConfirm={() => void removeLibrary()}
      />
    </Stack>
  )
}
