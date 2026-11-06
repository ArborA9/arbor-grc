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
  ApiError,
  type Session,
  type TprmQuestionnaire,
  type TprmQuestionnaireItem,
} from './api'
import { useAppShellPage } from './components/AppShellPageContext'
import {
  BusyButton,
  CompactTextField,
  ConfirmActionDialog,
  EmptyRegister,
  FormSelect,
  INPUT_LIMIT,
  NARRATIVE_FIELD_MAX_WIDTH,
  PageLoading,
  RegisterListFrame,
  controlActionSx,
  formFieldGridSx,
  lastTableCellSx,
  compactTableCellSxWidth,
  mediumTableCellSx,
  primaryRegisterNameSx,
  primaryTableCellSx,
  registerTableSx,
} from './components/ui'
import StatusChip from './components/StatusChip'
import { formatTenantDate } from './uiQuality/tenantLocale'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { useOptionalUnsavedChangesGuardApi, useRegisterUnsavedChanges } from './uiQuality/UnsavedChangesGuard'

type DraftItem = {
  clientId: string
  id?: number
  controlCode: string
  domain: string
  requirement: string
  requiredEvidence: string
  evidenceMandatory: boolean
}

type QuestionForm = {
  controlCode: string
  domain: string
  requirement: string
  requiredEvidence: string
  evidenceMandatory: 'Yes' | 'No'
}

const EMPTY_FORM: QuestionForm = {
  controlCode: '',
  domain: '',
  requirement: '',
  requiredEvidence: '',
  evidenceMandatory: 'No',
}

function newClientId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function questionnaireListStats(q: TprmQuestionnaire) {
  const items = q.items ?? []
  const evidenceRequired = items.filter((it) => it.evidenceMandatory).length
  const domains = new Set(items.map((it) => it.domain.trim()).filter(Boolean)).size
  return { evidenceRequired, domains, ready: q.controlCount > 0 }
}

function toDraft(it: TprmQuestionnaireItem): DraftItem {
  return {
    clientId: `row-${it.id}`,
    id: it.id,
    controlCode: it.controlCode ?? '',
    domain: it.domain ?? '',
    requirement: it.requirement ?? '',
    requiredEvidence: it.requiredEvidence ?? '',
    evidenceMandatory: Boolean(it.evidenceMandatory),
  }
}

function snapshot(name: string, items: DraftItem[]) {
  return JSON.stringify({
    name: name.trim(),
    items: items.map((i) => ({
      id: i.id ?? null,
      controlCode: i.controlCode.trim(),
      domain: i.domain.trim(),
      requirement: i.requirement.trim(),
      requiredEvidence: i.requiredEvidence.trim(),
      evidenceMandatory: i.evidenceMandatory,
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
    input.addEventListener('change', () => {
      const file = input.files?.[0] ?? null
      input.remove()
      resolve(file)
    }, { once: true })
    input.click()
  })
}

function duplicateControlId(items: DraftItem[], code: string, exceptClientId: string | null) {
  const key = code.trim().toLowerCase()
  if (!key) return null
  const clash = items.find((d) => d.clientId !== exceptClientId && d.controlCode.trim().toLowerCase() === key)
  return clash ? clash.controlCode.trim() : null
}

export default function TprmQuestionnairesPage({
  session,
  onError,
  onOk,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const canAdmin = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const { requestNavigation } = useOptionalUnsavedChangesGuardApi()
  const [rows, setRows] = useState<TprmQuestionnaire[]>([])
  const [name, setName] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [rename, setRename] = useState('')
  const [drafts, setDrafts] = useState<DraftItem[]>([])
  const [baseline, setBaseline] = useState('')
  const [form, setForm] = useState<QuestionForm>(EMPTY_FORM)
  const [formBaseline, setFormBaseline] = useState('')
  const [editingClientId, setEditingClientId] = useState<string | null>(null)
  const [questionOpen, setQuestionOpen] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<DraftItem | null>(null)
  const [confirmUpload, setConfirmUpload] = useState(false)
  const { listReady, markReady } = useListReady()
  const paged = usePagedRows(rows)
  const pagedDrafts = usePagedRows(drafts)
  const createWhy = !name.trim() ? 'Enter a questionnaire name' : undefined
  const creatingDirty = createOpen && name.trim().length > 0

  const selected = useMemo(() => rows.find((r) => r.id === selectedId) ?? null, [rows, selectedId])
  const currentSnap = snapshot(rename, drafts)
  const dirtyEditor = selected != null && currentSnap !== baseline
  const formDirty = questionOpen && JSON.stringify(form) !== formBaseline
  const questionWhy = !form.controlCode.trim()
    ? 'Enter a Control ID'
    : !form.domain.trim()
      ? 'Enter a Domain'
      : !form.requirement.trim()
        ? 'Enter a Control requirement'
        : duplicateControlId(drafts, form.controlCode, editingClientId)
          ? `Control ID "${form.controlCode.trim()}" already exists on this questionnaire`
          : undefined

  useRegisterUnsavedChanges(creatingDirty && !busy
    ? {
        dirty: true,
        onSaveDraft: () => create(),
        saveDraftLabel: 'Create Questionnaire',
      }
    : selected && formDirty && !busy
      ? {
          dirty: true,
          onSaveDraft: () => submitQuestion(),
          saveDraftLabel: editingClientId ? 'Save Question' : 'Add Question',
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
        : null)

  const applyLoaded = (q: TprmQuestionnaire) => {
    const next = (q.items ?? []).map(toDraft)
    setRename(q.name)
    setDrafts(next)
    setBaseline(snapshot(q.name, next))
  }

  const load = useCallback(async () => {
    try {
      const list = await api.listTprmQuestionnaires()
      setRows(list)
      return list
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
      setRows([])
      return [] as TprmQuestionnaire[]
    } finally {
      markReady()
    }
  }, [onError, markReady])

  useEffect(() => { void load() }, [load])

  useEffect(() => {
    if (!selected) {
      setRename('')
      setDrafts([])
      setBaseline('')
      setQuestionOpen(false)
      setForm(EMPTY_FORM)
      setEditingClientId(null)
      return
    }
    applyLoaded(selected)
    // selected.id + updatedAt is the server version we hydrate from
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected?.id, selected?.updatedAt])

  const create = async () => {
    if (createWhy) {
      onError(createWhy)
      return
    }
    const n = name.trim()
    setBusy(true)
    try {
      const q = await api.createTprmQuestionnaire(n)
      setName('')
      setCreateOpen(false)
      onOk('Questionnaire created')
      applyLoaded({ ...q, items: [] })
      setSelectedId(q.id)
      const list = await load()
      const created = list.find((r) => r.id === q.id) ?? q
      applyLoaded({ ...created, items: created.items ?? [] })
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const closeCreate = () => {
    requestNavigation(() => {
      setCreateOpen(false)
      setName('')
    })
  }

  const persist = async (next: DraftItem[]) => {
    if (!selected) return false
    const n = (rename.trim() || selected.name).trim()
    if (!n) {
      onError('Questionnaire name is required')
      return false
    }
    const dup = (() => {
      const seen = new Set<string>()
      for (const d of next) {
        const key = d.controlCode.trim().toLowerCase()
        if (!key) continue
        if (seen.has(key)) return d.controlCode.trim()
        seen.add(key)
      }
      return null
    })()
    if (dup) {
      onError(`Control ID "${dup}" is duplicated`)
      return false
    }
    if (next.some((d) => !d.controlCode.trim())) {
      onError('Each question needs a Control ID')
      return false
    }
    const asDraft = next.some((d) => !d.domain.trim() || !d.requirement.trim())
    setBusy(true)
    try {
      const q = await api.saveTprmQuestionnaire(selected.id, {
        name: n,
        draft: asDraft,
        items: next.map((d) => ({
          id: d.id,
          controlCode: d.controlCode.trim(),
          domain: d.domain.trim(),
          requirement: d.requirement.trim(),
          requiredEvidence: d.requiredEvidence.trim(),
          evidenceMandatory: d.evidenceMandatory,
        })),
      })
      const list = await load()
      const latest = list.find((r) => r.id === q.id) ?? q
      applyLoaded(latest)
      onOk(asDraft ? 'Draft saved' : 'Questionnaire saved')
      return true
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
      return false
    } finally {
      setBusy(false)
    }
  }

  const closeQuestionDialog = () => {
    setQuestionOpen(false)
    setEditingClientId(null)
    setForm(EMPTY_FORM)
    setFormBaseline(JSON.stringify(EMPTY_FORM))
  }

  const openAddQuestion = () => {
    setEditingClientId(null)
    setForm(EMPTY_FORM)
    setFormBaseline(JSON.stringify(EMPTY_FORM))
    setQuestionOpen(true)
  }

  const openEditQuestion = (item: DraftItem) => {
    const next: QuestionForm = {
      controlCode: item.controlCode,
      domain: item.domain,
      requirement: item.requirement,
      requiredEvidence: item.requiredEvidence,
      evidenceMandatory: item.evidenceMandatory ? 'Yes' : 'No',
    }
    setEditingClientId(item.clientId)
    setForm(next)
    setFormBaseline(JSON.stringify(next))
    setQuestionOpen(true)
  }

  const submitQuestion = async () => {
    if (questionWhy) {
      onError(questionWhy)
      throw new Error(questionWhy)
    }
    const code = form.controlCode.trim()
    const nextItem: DraftItem = {
      clientId: editingClientId ?? newClientId(),
      id: editingClientId ? drafts.find((d) => d.clientId === editingClientId)?.id : undefined,
      controlCode: code,
      domain: form.domain.trim(),
      requirement: form.requirement.trim(),
      requiredEvidence: form.requiredEvidence.trim(),
      evidenceMandatory: form.evidenceMandatory === 'Yes',
    }
    const next = editingClientId
      ? drafts.map((d) => (d.clientId === editingClientId ? nextItem : d))
      : [...drafts, nextItem]
    const ok = await persist(next)
    if (!ok) throw new Error('save failed')
    closeQuestionDialog()
  }

  const runUpload = async () => {
    if (!selected) return
    const file = await pickXlsx()
    if (!file) return
    setBusy(true)
    try {
      const q = await api.uploadTprmQuestionnaireLibrary(selected.id, file)
      onOk(q.controlCount > 0
        ? `Uploaded. Questionnaire now has ${q.controlCount} question${q.controlCount === 1 ? '' : 's'}`
        : 'Template uploaded')
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
      closeQuestionDialog()
    })
  }

  useAppShellPage({
    title: selected ? selected.name : 'Questionnaires',
    breadcrumbTrail: selected ? undefined : 'TPRM',
    breadcrumbRoot: selected
      ? { label: 'Third-Party Risk / Questionnaires', onClick: closeEditor }
      : undefined,
    hideContextLabel: true,
    hideRoleChips: true,
    actions: canAdmin && selected ? (
      <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
        <Button size="small" variant="outlined" sx={controlActionSx} onClick={closeEditor}>Back</Button>
        <BusyButton size="small" variant="contained" busy={busy} onClick={openAddQuestion} sx={controlActionSx}>
          Add a Question
        </BusyButton>
        <BusyButton size="small" variant="outlined" busy={busy} onClick={() => void api.downloadTprmQuestionnaireTemplate(selected.id)} sx={controlActionSx}>
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
        onClick={() => setCreateOpen(true)}
        sx={controlActionSx}
      >
        Create Questionnaire
      </BusyButton>
    ) : undefined,
  }, [canAdmin, selected, busy, dirtyEditor, formDirty, createOpen])

  if (selected) {
    return (
      <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, p: 1.5 }}>
        <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          {selected.controlCount > 0 && drafts.length === 0 ? (
            <PageLoading />
          ) : drafts.length === 0 ? (
            <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <EmptyRegister
                message="No questions"
                action={canAdmin ? (
                  <Button size="small" variant="contained" onClick={openAddQuestion}>Add a Question</Button>
                ) : undefined}
              />
            </Box>
          ) : (
            <RegisterListFrame
              ready
              count={pagedDrafts.count}
              emptyMessage="No questions"
              page={pagedDrafts.page}
              rowsPerPage={pagedDrafts.rowsPerPage}
              onPageChange={pagedDrafts.setPage}
              onRowsPerPageChange={pagedDrafts.setRowsPerPage}
            >
              <TableContainer sx={{ flex: 1 }}>
                <Table size="small" sx={registerTableSx}>
                  <TableHead>
                    <TableRow>
                      <TableCell sx={compactTableCellSxWidth('12%')}>Control ID</TableCell>
                      <TableCell sx={compactTableCellSxWidth('12%')}>Domain</TableCell>
                      <TableCell sx={{ ...primaryTableCellSx, width: '24%' }}>Control requirement</TableCell>
                      <TableCell sx={{ ...mediumTableCellSx, width: '18%', whiteSpace: 'normal', overflow: 'visible' }}>
                        Required evidence
                      </TableCell>
                      <TableCell sx={{ ...compactTableCellSxWidth('18%'), whiteSpace: 'normal', overflow: 'visible' }}>
                        Evidence Mandatory
                      </TableCell>
                      {canAdmin && (
                        <TableCell sx={{ ...lastTableCellSx, ...compactTableCellSxWidth('16%'), overflow: 'visible' }}>
                          Actions
                        </TableCell>
                      )}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {pagedDrafts.paged.map((it) => (
                      <TableRow key={it.clientId} hover>
                        <TableCell sx={compactTableCellSxWidth('12%')}>{it.controlCode || '-'}</TableCell>
                        <TableCell sx={{ ...compactTableCellSxWidth('12%'), whiteSpace: 'normal' }}>{it.domain || '-'}</TableCell>
                        <TableCell sx={{ ...primaryRegisterNameSx, whiteSpace: 'normal' }}>{it.requirement || '-'}</TableCell>
                        <TableCell sx={{ ...mediumTableCellSx, width: '18%', whiteSpace: 'normal' }}>{it.requiredEvidence || '-'}</TableCell>
                        <TableCell sx={compactTableCellSxWidth('18%')}>{it.evidenceMandatory ? 'Yes' : 'No'}</TableCell>
                        {canAdmin && (
                          <TableCell sx={{ ...lastTableCellSx, ...compactTableCellSxWidth('16%') }}>
                            <Stack direction="row" spacing={0.5}>
                              <Button size="small" variant="outlined" sx={controlActionSx} onClick={() => openEditQuestion(it)}>
                                Edit
                              </Button>
                              <Button size="small" color="error" variant="outlined" sx={controlActionSx} onClick={() => setDeleteTarget(it)}>
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
          open={questionOpen}
          onClose={busy ? undefined : () => {
            if (formDirty) return
            closeQuestionDialog()
          }}
          fullWidth
          maxWidth="sm"
        >
          <DialogTitle sx={{ pb: 1 }}>
            {editingClientId ? 'Edit Question' : 'Add a Question'}
          </DialogTitle>
          <DialogContent>
            <Stack spacing={1.25} sx={{ mt: 0.5 }}>
              <Box sx={formFieldGridSx}>
                <CompactTextField
                  label="Control ID"
                  required
                  fieldIdPrefix="tprm-q-item"
                  value={form.controlCode}
                  onChange={(e) => setForm({ ...form, controlCode: e.target.value })}
                  maxLength={40}
                />
                <CompactTextField
                  label="Domain"
                  required
                  fieldIdPrefix="tprm-q-item"
                  value={form.domain}
                  onChange={(e) => setForm({ ...form, domain: e.target.value })}
                  maxLength={80}
                />
                <FormSelect
                  label="Evidence Mandatory"
                  required
                  value={form.evidenceMandatory}
                  options={[
                    { value: 'Yes', label: 'Yes' },
                    { value: 'No', label: 'No' },
                  ]}
                  onChange={(v) => setForm({ ...form, evidenceMandatory: v === 'Yes' ? 'Yes' : 'No' })}
                />
              </Box>
              <Box sx={{ maxWidth: NARRATIVE_FIELD_MAX_WIDTH }}>
                <CompactTextField
                  label="Control requirement"
                  required
                  multiline
                  minRows={3}
                  fieldIdPrefix="tprm-q-item"
                  value={form.requirement}
                  onChange={(e) => setForm({ ...form, requirement: e.target.value })}
                  maxLength={2000}
                  maxWidth="100%"
                />
              </Box>
              <Box sx={{ maxWidth: NARRATIVE_FIELD_MAX_WIDTH }}>
                <CompactTextField
                  label="Required evidence"
                  multiline
                  minRows={2}
                  fieldIdPrefix="tprm-q-item"
                  value={form.requiredEvidence}
                  onChange={(e) => setForm({ ...form, requiredEvidence: e.target.value })}
                  maxLength={2000}
                  maxWidth="100%"
                />
              </Box>
            </Stack>
          </DialogContent>
          <DialogActions sx={{ px: 2.5, pb: 2 }}>
            <Button onClick={closeQuestionDialog} disabled={busy}>Cancel</Button>
            <BusyButton
              size="small"
              variant="contained"
              busy={busy}
              disabled={Boolean(questionWhy)}
              disabledReason={questionWhy}
              onClick={() => { void submitQuestion().catch(() => undefined) }}
            >
              {editingClientId ? 'Save Question' : 'Add Question'}
            </BusyButton>
          </DialogActions>
        </Dialog>

        <ConfirmActionDialog
          open={deleteTarget != null}
          title="Delete this question?"
          message="This removes the question from the questionnaire."
          confirmLabel={busy ? 'Deleting…' : 'Delete Question'}
          busy={busy}
          onCancel={() => setDeleteTarget(null)}
          onConfirm={() => {
            if (!deleteTarget) return
            const next = drafts.filter((d) => d.clientId !== deleteTarget.clientId)
            setDeleteTarget(null)
            if (editingClientId === deleteTarget.clientId) closeQuestionDialog()
            void persist(next)
          }}
        />
        <ConfirmActionDialog
          open={confirmUpload}
          title="Upload Excel questions?"
          message="Upload adds the Excel rows to this questionnaire. Unsaved edits on this screen will be lost."
          confirmLabel="Upload and Add"
          confirmColor="warning"
          busy={busy}
          onCancel={() => setConfirmUpload(false)}
          onConfirm={() => void runUpload()}
        />
      </Stack>
    )
  }

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, p: 1.5 }}>
      <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        <Box sx={{ px: 1.5, pt: 1.25, pb: 0.5 }}>
          <Typography variant="subtitle2">Saved questionnaires</Typography>
          <Typography variant="body2" color="text.secondary">
            Already created questionnaires. Open a row to add, edit, or delete questions.
          </Typography>
        </Box>
        <RegisterListFrame
          ready={listReady}
          count={paged.count}
          emptyMessage={
            canAdmin
              ? 'No questionnaires yet. Use Create questionnaire to add one.'
              : 'No questionnaires yet.'
          }
          emptyAction={canAdmin ? (
            <Button size="small" variant="contained" onClick={() => setCreateOpen(true)}>
              Create Questionnaire
            </Button>
          ) : undefined}
          page={paged.page}
          rowsPerPage={paged.rowsPerPage}
          onPageChange={paged.setPage}
          onRowsPerPageChange={paged.setRowsPerPage}
        >
          <TableContainer>
            <Table size="small" sx={registerTableSx}>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ ...primaryTableCellSx, width: '28%' }}>Name</TableCell>
                  <TableCell sx={compactTableCellSxWidth('10%')}>Questions</TableCell>
                  <TableCell sx={compactTableCellSxWidth('14%')}>Evidence required</TableCell>
                  <TableCell sx={compactTableCellSxWidth('10%')}>Domains</TableCell>
                  <TableCell sx={compactTableCellSxWidth('12%')}>Status</TableCell>
                  <TableCell sx={compactTableCellSxWidth('13%')}>Created</TableCell>
                  <TableCell sx={{ ...lastTableCellSx, width: '13%' }}>Updated</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((q) => {
                  const stats = questionnaireListStats(q)
                  return (
                  <TableRow key={q.id} hover sx={{ cursor: 'pointer' }} onClick={() => requestNavigation(() => {
                    applyLoaded(q)
                    setSelectedId(q.id)
                  })}>
                    <TableCell sx={primaryRegisterNameSx}>{q.name}</TableCell>
                    <TableCell sx={compactTableCellSxWidth('10%')}>{q.controlCount}</TableCell>
                    <TableCell sx={compactTableCellSxWidth('14%')}>{stats.evidenceRequired}</TableCell>
                    <TableCell sx={compactTableCellSxWidth('10%')}>{stats.domains}</TableCell>
                    <TableCell sx={compactTableCellSxWidth('12%')}>
                      <StatusChip label={stats.ready ? 'Ready' : 'Empty'} tone={stats.ready ? 'success' : 'neutral'} />
                    </TableCell>
                    <TableCell sx={compactTableCellSxWidth('13%')}>{formatTenantDate(q.createdAt)}</TableCell>
                    <TableCell sx={{ ...lastTableCellSx, width: '13%' }}>{formatTenantDate(q.updatedAt)}</TableCell>
                  </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </RegisterListFrame>
      </Card>

      <Dialog
        open={createOpen}
        onClose={busy ? undefined : closeCreate}
        fullWidth
        maxWidth="xs"
      >
        <DialogTitle sx={{ pb: 1 }}>Create Questionnaire</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 0.5, maxWidth: NARRATIVE_FIELD_MAX_WIDTH }}>
            <CompactTextField
              label="Questionnaire name"
              required
              fieldIdPrefix="tprm-q-new"
              value={name}
              autoFocus
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Netflix"
              maxLength={INPUT_LIMIT.orgName}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !createWhy && !busy) {
                  e.preventDefault()
                  void create()
                }
              }}
            />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 2.5, pb: 2 }}>
          <Button size="small" onClick={closeCreate} disabled={busy}>Cancel</Button>
          <BusyButton
            size="small"
            variant="contained"
            busy={busy}
            disabled={Boolean(createWhy)}
            disabledReason={createWhy}
            onClick={() => void create()}
          >
            Create
          </BusyButton>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
