import { useEffect, useMemo, useRef, useState } from 'react'
import {
  Box,
  Button,
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
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import FilterListRoundedIcon from '@mui/icons-material/FilterListRounded'
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import {
  api,
  type AuditChecklistItem,
  type AuditChecklistSheet,
  type ChecklistTemplateKey,
  type GrcAuditDetail,
} from '../../api'
import {
  BusyButton,
  CompactTextField,
  compactInputSx,
  FilterDropdown,
  FilterPill,
  RegisterPagination,
} from '../ui'
import ConfirmActionDialog from '../ui/ConfirmActionDialog'
import StatusChip from '../StatusChip'
import { useRegisterUnsavedChanges } from '../../uiQuality/UnsavedChangesGuard'
import { useRegisterColumnPicker } from '../../hooks/useRegisterColumnPicker'
import type { ColumnDefinition } from '../../hooks/columnVisibility'
import AuditChecklistLineDrawer, { CHECKLIST_CONCLUSIONS } from './AuditChecklistLineDrawer'
import { auditControlRefSx, auditTableContainerSx } from './auditKit'
import { brand } from '../../theme'

const iconBtnSx = {
  width: 32,
  height: 32,
  border: `1px solid ${brand.border}`,
  borderRadius: 1,
  color: brand.inkSoft,
  bgcolor: brand.paper,
  '&:hover': { borderColor: brand.gold, bgcolor: brand.hover, color: brand.goldDark },
}

const removeBtnSx = {
  ...iconBtnSx,
  '&:hover': { borderColor: '#DC2626', bgcolor: '#FEF2F2', color: '#B91C1C' },
}

type LineFilter = 'all' | 'open' | 'finding'

const CHECKLIST_PAPER_COLUMNS: ColumnDefinition[] = [
  { id: 'clause', label: 'Clause / Ref', required: true },
  { id: 'control', label: 'Control', required: true },
  { id: 'testFocus', label: 'Test focus' },
  { id: 'soa', label: 'SoA / type' },
  { id: 'inScope', label: 'In scope' },
  { id: 'conclusion', label: 'Conclusion' },
  { id: 'finding', label: 'Finding' },
  { id: 'notes', label: 'Auditor note' },
  { id: 'evidenceExamined', label: 'Evidence examined' },
  { id: 'persons', label: 'Persons / date' },
  { id: 'actions', label: 'Actions', required: true },
]

const PAPER_COL_PX: Record<string, number> = {
  clause: 88,
  control: 240,
  testFocus: 260,
  soa: 110,
  inScope: 80,
  conclusion: 140,
  finding: 168,
  notes: 180,
  evidenceExamined: 160,
  persons: 140,
  actions: 104,
}

const LINE_FILTER_OPTIONS: { value: LineFilter; label: string }[] = [
  { value: 'all', label: 'All' },
  { value: 'open', label: 'Not concluded' },
  { value: 'finding', label: 'Has finding' },
]

const paperHeadSx = {
  textTransform: 'none' as const,
  letterSpacing: '0.01em',
  fontSize: '0.75rem',
  fontWeight: 650,
  color: brand.muted,
  bgcolor: brand.canvas,
  borderBottom: `1px solid ${brand.border}`,
  whiteSpace: 'nowrap' as const,
  lineHeight: 1.3,
  py: 1,
  px: 1.25,
}

const paperCellSx = {
  fontSize: '0.8125rem',
  verticalAlign: 'top' as const,
  py: 1.25,
  px: 1.25,
  color: brand.ink,
  whiteSpace: 'normal' as const,
  overflowWrap: 'anywhere' as const,
  lineHeight: 1.4,
}

function checklistPillLabel(name: string): string {
  return name.replace(/^Checklist\s+/i, '').trim() || name
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

function conclusionTone(
  value: string | null | undefined,
): 'success' | 'warning' | 'info' | 'danger' | 'neutral' {
  if (value === 'CONFORMS') return 'success'
  if (value === 'MAJOR_NC') return 'danger'
  if (value === 'MINOR_NC') return 'warning'
  if (value === 'OBSERVATION' || value === 'OFI') return 'info'
  return 'neutral'
}

const LIBRARY: { key: ChecklistTemplateKey; label: string }[] = [
  { key: 'ISO_27001', label: 'Checklist ISO 27001' },
  { key: 'ISO_20000', label: 'Checklist ISO 20000' },
  { key: 'UAE_IA', label: 'Checklist UAE IA' },
]

export default function AuditWorkingPaperPanel({
  detail,
  canEdit,
  onRefresh,
  onError,
  onOk,
  onAddFinding,
  onDirtyChange,
}: {
  detail: GrcAuditDetail
  canEdit: boolean
  onRefresh: () => Promise<void>
  onError: (msg: string) => void
  onOk: (msg: string) => void
  onAddFinding?: () => void
  onDirtyChange?: (dirty: boolean) => void
}) {
  const sheets = detail.checklists?.length
    ? detail.checklists
    : [
        {
          id: 0,
          templateKey: 'CUSTOM' as const,
          name: 'Checklist',
          sortOrder: 0,
          items: detail.checklist,
        },
      ]
  const [tab, setTab] = useState(0)
  const sheetCountRef = useRef(sheets.length)
  const [openId, setOpenId] = useState<number | null>(null)
  const [patches, setPatches] = useState<Record<number, Partial<AuditChecklistItem>>>({})
  const [page, setPage] = useState(0)
  const [rowsPerPage, setRowsPerPage] = useState(25)
  const [query, setQuery] = useState('')
  const [lineFilter, setLineFilter] = useState<LineFilter>('all')
  const paperColumns = useRegisterColumnPicker(CHECKLIST_PAPER_COLUMNS)
  const showCol = paperColumns.isVisible

  useEffect(() => {
    setPatches({})
    setOpenId(null)
    setPage(0)
    setTab(0)
    sheetCountRef.current = sheets.length
  }, [detail.audit.id])

  useEffect(() => {
    if (sheets.length > sheetCountRef.current) {
      setTab(sheets.length - 1)
    } else if (tab > sheets.length - 1) {
      setTab(Math.max(0, sheets.length - 1))
    }
    sheetCountRef.current = sheets.length
  }, [sheets.length, tab])
  const sheet: AuditChecklistSheet | undefined = sheets[Math.min(tab, Math.max(sheets.length - 1, 0))]
  const used = useMemo(() => new Set(sheets.map((s) => s.templateKey)), [sheets])
  const addable = LIBRARY.filter((t) => !used.has(t.key))

  const [busy, setBusy] = useState(false)
  const [addSheetOpen, setAddSheetOpen] = useState(false)
  const [customName, setCustomName] = useState('')
  const [addRowOpen, setAddRowOpen] = useState(false)
  const [clauseRef, setClauseRef] = useState('')
  const [section, setSection] = useState('')
  const [title, setTitle] = useState('')
  const [testFocus, setTestFocus] = useState('')
  const [removeSheet, setRemoveSheet] = useState<AuditChecklistSheet | null>(null)
  const [removeItemId, setRemoveItemId] = useState<number | null>(null)
  const [replaceAsk, setReplaceAsk] = useState(false)
  const [pendingReplace, setPendingReplace] = useState<File | null>(null)

  const run = async (fn: () => Promise<unknown>, ok?: string) => {
    setBusy(true)
    try {
      await fn()
      await onRefresh()
      if (ok) onOk(ok)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const patchLocal = (itemId: number, body: Partial<AuditChecklistItem>) => {
    setPatches((prev) => ({ ...prev, [itemId]: { ...prev[itemId], ...body } }))
  }

  const dirtyIds = useMemo(() => {
    const ids: number[] = []
    for (const sheetRow of sheets) {
      for (const raw of sheetRow.items) {
        const patch = patches[raw.id]
        if (!patch) continue
        const keys = Object.keys(patch) as (keyof AuditChecklistItem)[]
        const changed = keys.some((k) => {
          const next = patch[k]
          const prev = raw[k]
          const a = next == null || next === '' ? '' : String(next)
          const b = prev == null || prev === '' ? '' : String(prev)
          return a !== b
        })
        if (changed) ids.push(raw.id)
      }
    }
    return ids
  }, [patches, sheets])

  const dirty = dirtyIds.length > 0

  useEffect(() => {
    onDirtyChange?.(dirty)
  }, [dirty, onDirtyChange])

  useRegisterUnsavedChanges(
    dirty && canEdit && !busy
      ? { dirty: true, title: 'Leave checklist?', message: 'You have unsaved checklist changes.' }
      : null,
  )

  useEffect(() => {
    setPage(0)
  }, [sheet?.id, rowsPerPage, query, lineFilter])

  const filteredItems = useMemo(() => {
    if (!sheet) return []
    const q = query.trim().toLowerCase()
    return sheet.items.filter((raw) => {
      const row = { ...raw, ...patches[raw.id] }
      if (lineFilter === 'open' && row.conclusion && row.conclusion !== 'NOT_TESTED') return false
      if (lineFilter === 'finding' && !row.findingDisplayCode && !row.findingId) return false
      if (!q) return true
      const hay = [
        row.nodeCode,
        row.section,
        row.title,
        row.testFocus,
        row.findingDisplayCode,
        row.findingDescription,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
      return hay.includes(q)
    })
  }, [sheet, patches, query, lineFilter])

  const pagedItems = useMemo(() => {
    return filteredItems.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage)
  }, [filteredItems, page, rowsPerPage])

  useEffect(() => {
    const maxPage = Math.max(0, Math.ceil(filteredItems.length / rowsPerPage) - 1)
    if (page > maxPage) setPage(maxPage)
  }, [filteredItems.length, page, rowsPerPage])

  const saveProgress = async () => {
    if (!dirtyIds.length) return
    setBusy(true)
    try {
      let minted = false
      for (const itemId of dirtyIds) {
        const body = patches[itemId]
        if (!body) continue
        const updated = await api.updateAuditChecklist(detail.audit.id, itemId, {
          notes: body.notes,
          inScope: body.inScope,
          evidenceExamined: body.evidenceExamined,
          personsInterviewed: body.personsInterviewed,
          conclusion: body.conclusion,
          findingDescription: body.findingDescription,
        })
        if (updated.findingId) minted = true
        setPatches((prev) => ({ ...prev, [itemId]: { ...prev[itemId], ...updated } }))
      }
      await onRefresh()
      setPatches({})
      onOk(minted ? 'Progress saved. Finding ID minted from the checklist.' : 'Progress saved')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const addLibrary = (key: ChecklistTemplateKey) => {
    void run(() => api.addAuditChecklist(detail.audit.id, { templateKey: key }), 'Checklist added')
    setAddSheetOpen(false)
  }

  const addBlank = () => {
    if (!customName.trim()) {
      onError('Checklist name is required')
      return
    }
    void run(
      () => api.addAuditChecklist(detail.audit.id, { templateKey: 'CUSTOM', name: customName.trim() }),
      'Checklist created',
    )
    setCustomName('')
    setAddSheetOpen(false)
  }

  const downloadTemplate = () => {
    void api
      .downloadAuditChecklistTemplate()
      .then(() => onOk('Template downloaded'))
      .catch((e) => onError(String(e)))
  }

  const uploadNewFromDialog = async () => {
    if (!customName.trim()) {
      onError('Checklist name is required')
      return
    }
    const file = await pickXlsx()
    if (!file) return
    setAddSheetOpen(false)
    await run(
      () => api.uploadNewAuditChecklist(detail.audit.id, customName.trim(), file),
      'Checklist uploaded',
    )
    setCustomName('')
  }

  const uploadOntoSheet = async (file?: File | null) => {
    if (!sheet || sheet.id === 0) return
    const chosen = file ?? (await pickXlsx())
    if (!chosen) return
    await run(
      () => api.uploadAuditChecklist(detail.audit.id, sheet.id, chosen),
      sheet.items.length ? 'Checklist replaced from Excel' : 'Checklist uploaded',
    )
    setPendingReplace(null)
    setReplaceAsk(false)
  }

  const requestSheetUpload = async () => {
    if (!sheet || sheet.id === 0) return
    const file = await pickXlsx()
    if (!file) return
    if (sheet.items.length > 0) {
      setPendingReplace(file)
      setReplaceAsk(true)
      return
    }
    await uploadOntoSheet(file)
  }

  const addRow = () => {
    if (!sheet || !clauseRef.trim() || !title.trim()) {
      onError('Clause ref and title are required')
      return
    }
    void run(
      () =>
        api.addAuditChecklistItem(detail.audit.id, sheet.id, {
          clauseRef: clauseRef.trim(),
          section: section.trim() || undefined,
          title: title.trim(),
          testFocus: testFocus.trim() || undefined,
        }),
      'Row added',
    )
    setClauseRef('')
    setSection('')
    setTitle('')
    setTestFocus('')
    setAddRowOpen(false)
  }

  if (!sheet) {
    return (
      <Stack spacing={1.5}>
        <Typography variant="body2" color="text.secondary">
          No checklists on this audit yet.
        </Typography>
        {canEdit && (
          <Button size="small" variant="contained" onClick={() => setAddSheetOpen(true)}>
            Add Checklist
          </Button>
        )}
      </Stack>
    )
  }

  const colCount = paperColumns.visibleIds.length
  const tableMinWidth = paperColumns.visibleIds.reduce((sum, id) => sum + (PAPER_COL_PX[id] ?? 140), 0)
  const openIndex = openId == null ? -1 : filteredItems.findIndex((r) => r.id === openId)
  const openRow = openIndex >= 0 ? { ...filteredItems[openIndex], ...patches[filteredItems[openIndex].id] } : null

  return (
    <Stack spacing={0.75} sx={{ flex: 1, minHeight: 0 }}>
      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="center"
        flexWrap="wrap"
        useFlexGap
        gap={1}
      >
        {sheets.length > 1 ? (
          <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap sx={{ minWidth: 0 }}>
            {sheets.map((s, i) => (
              <FilterPill
                key={s.id}
                dense
                label={checklistPillLabel(s.name)}
                count={s.items.length}
                active={i === Math.min(tab, sheets.length - 1)}
                onClick={() => setTab(i)}
              />
            ))}
          </Stack>
        ) : (
          <Typography variant="caption" color="text.secondary" sx={{ lineHeight: '32px' }}>
            {filteredItems.length === sheet.items.length
              ? `${sheet.items.length} lines`
              : `${filteredItems.length} of ${sheet.items.length} lines`}
          </Typography>
        )}
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap alignItems="center" sx={{ ml: 'auto' }}>
          <TextField
            size="small"
            placeholder="Clause, title, or finding ID"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            inputProps={{ 'aria-label': 'Find' }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchRoundedIcon sx={{ fontSize: 16, color: brand.mutedLight }} />
                </InputAdornment>
              ),
            }}
            sx={{
              ...compactInputSx,
              width: 240,
              maxWidth: 240,
              flex: '0 0 240px',
              '& .MuiOutlinedInput-root': { bgcolor: brand.paper },
            }}
          />
          <FilterDropdown
            label="Filter"
            hideLabel
            startIcon={<FilterListRoundedIcon sx={{ fontSize: 16, color: brand.mutedLight }} />}
            value={lineFilter}
            options={LINE_FILTER_OPTIONS}
            onChange={(v) => setLineFilter(v)}
            minWidth={156}
          />
          {paperColumns.columnPicker}
          {!canEdit && (
            <Typography variant="caption" color="text.secondary" sx={{ lineHeight: '32px', whiteSpace: 'nowrap' }}>
              Only the assigned auditor can edit rows
            </Typography>
          )}
          {canEdit && (
            <>
              {onAddFinding && (
                <Button size="small" variant="outlined" disabled={busy} onClick={onAddFinding}>
                  Add Finding
                </Button>
              )}
              <BusyButton
                size="small"
                variant="contained"
                busy={busy}
                busyLabel="Saving..."
                disabled={!dirty}
                disabledReason={!dirty ? 'No checklist changes to save' : undefined}
                onClick={() => void saveProgress()}
              >
                Save Progress
              </BusyButton>
              <Button size="small" variant="outlined" disabled={busy} onClick={() => setAddSheetOpen(true)}>
                Add Checklist
              </Button>
              {sheet.templateKey === 'CUSTOM' && sheet.id !== 0 && (
                <>
                  <Button size="small" variant="outlined" disabled={busy} onClick={downloadTemplate}>
                    Download Template
                  </Button>
                  <Button size="small" variant="outlined" disabled={busy} onClick={() => void requestSheetUpload()}>
                    Upload
                  </Button>
                </>
              )}
              <Button size="small" variant="outlined" disabled={busy} onClick={() => setAddRowOpen(true)}>
                Add Row
              </Button>
              <BusyButton
                size="small"
                color="error"
                variant="outlined"
                busy={busy}
                disabled={sheet.items.some((i) => i.findingId)}
                disabledReason={
                  sheet.items.some((i) => i.findingId)
                    ? 'A line on this checklist has a finding. Keep or close that finding first.'
                    : undefined
                }
                onClick={() => setRemoveSheet(sheet)}
              >
                Remove Checklist
              </BusyButton>
            </>
          )}
        </Stack>
      </Stack>
      <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
      <TableContainer
        sx={{
          ...auditTableContainerSx,
          flex: 1,
          minHeight: 240,
          overflow: 'auto',
        }}
      >
        <Table size="small" stickyHeader sx={{ minWidth: tableMinWidth, tableLayout: 'fixed' }}>
          <TableHead>
            <TableRow>
              <TableCell
                sx={{
                  ...paperHeadSx,
                  width: 88,
                  position: 'sticky',
                  left: 0,
                  zIndex: 4,
                  boxShadow: `1px 0 0 ${brand.border}`,
                }}
              >
                Clause / Ref
              </TableCell>
              <TableCell
                sx={{
                  ...paperHeadSx,
                  width: 240,
                  position: 'sticky',
                  left: 88,
                  zIndex: 4,
                  boxShadow: `1px 0 0 ${brand.border}`,
                }}
              >
                Control
              </TableCell>
              {showCol('testFocus') && <TableCell sx={{ ...paperHeadSx, width: 260 }}>Test focus</TableCell>}
              {showCol('soa') && <TableCell sx={{ ...paperHeadSx, width: 110 }}>SoA / type</TableCell>}
              {showCol('inScope') && (
                <TableCell sx={{ ...paperHeadSx, width: 80, textAlign: 'center' }}>In scope</TableCell>
              )}
              {showCol('conclusion') && <TableCell sx={{ ...paperHeadSx, width: 140 }}>Conclusion</TableCell>}
              {showCol('finding') && <TableCell sx={{ ...paperHeadSx, width: 168 }}>Finding</TableCell>}
              {showCol('notes') && <TableCell sx={{ ...paperHeadSx, width: 180 }}>Auditor note</TableCell>}
              {showCol('evidenceExamined') && (
                <TableCell sx={{ ...paperHeadSx, width: 160 }}>Evidence examined</TableCell>
              )}
              {showCol('persons') && <TableCell sx={{ ...paperHeadSx, width: 140 }}>Persons / date</TableCell>}
              <TableCell
                sx={{
                  ...paperHeadSx,
                  width: 104,
                  position: 'sticky',
                  right: 0,
                  zIndex: 4,
                  boxShadow: `-1px 0 0 ${brand.border}`,
                }}
              >
                Actions
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {pagedItems.map((raw) => {
              const row = { ...raw, ...patches[raw.id] }
              const conclusionLabel =
                CHECKLIST_CONCLUSIONS.find((c) => c.value === row.conclusion)?.label ?? '-'
              return (
              <TableRow
                key={row.id}
                hover
                onClick={() => setOpenId(row.id)}
                sx={{ cursor: 'pointer' }}
              >
                <TableCell
                  sx={{
                    ...paperCellSx,
                    ...auditControlRefSx,
                    whiteSpace: 'nowrap',
                    overflowWrap: 'normal',
                    position: 'sticky',
                    left: 0,
                    zIndex: 2,
                    bgcolor: brand.paper,
                    boxShadow: `1px 0 0 ${brand.border}`,
                  }}
                >
                  {row.nodeCode}
                </TableCell>
                <TableCell
                  sx={{
                    ...paperCellSx,
                    position: 'sticky',
                    left: 88,
                    zIndex: 2,
                    bgcolor: brand.paper,
                    boxShadow: `1px 0 0 ${brand.border}`,
                  }}
                >
                  <Typography sx={{ fontSize: '0.8125rem', fontWeight: 600, lineHeight: 1.35 }}>
                    {row.title}
                  </Typography>
                  {row.section ? (
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.25 }}>
                      {row.section}
                    </Typography>
                  ) : null}
                </TableCell>
                {showCol('testFocus') && (
                  <TableCell sx={{ ...paperCellSx, color: brand.muted }}>{row.testFocus || '-'}</TableCell>
                )}
                {showCol('soa') && <TableCell sx={paperCellSx}>{row.soaApplicability || '-'}</TableCell>}
                {showCol('inScope') && (
                  <TableCell sx={{ ...paperCellSx, textAlign: 'center' }}>
                    {row.inScope === false ? 'No' : 'Yes'}
                  </TableCell>
                )}
                {showCol('conclusion') && (
                  <TableCell sx={paperCellSx}>
                    {row.conclusion ? (
                      <StatusChip label={conclusionLabel} tone={conclusionTone(row.conclusion)} />
                    ) : (
                      '-'
                    )}
                  </TableCell>
                )}
                {showCol('finding') && (
                  <TableCell sx={paperCellSx}>
                    <Typography sx={{ fontSize: '0.8125rem', fontWeight: 650, whiteSpace: 'nowrap' }}>
                      {row.findingDisplayCode || '-'}
                    </Typography>
                    {row.findingDescription ? (
                      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.25 }}>
                        {row.findingDescription}
                      </Typography>
                    ) : null}
                  </TableCell>
                )}
                {showCol('notes') && <TableCell sx={paperCellSx}>{row.notes || '-'}</TableCell>}
                {showCol('evidenceExamined') && (
                  <TableCell sx={paperCellSx}>{row.evidenceExamined || '-'}</TableCell>
                )}
                {showCol('persons') && (
                  <TableCell sx={paperCellSx}>
                    <Typography sx={{ fontSize: '0.8125rem' }}>{row.personsInterviewed || '-'}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {row.reviewedDate || '-'}
                    </Typography>
                  </TableCell>
                )}
                <TableCell
                  sx={{
                    ...paperCellSx,
                    position: 'sticky',
                    right: 0,
                    bgcolor: brand.paper,
                    zIndex: 2,
                    whiteSpace: 'nowrap',
                    boxShadow: `-1px 0 0 ${brand.border}`,
                  }}
                  onClick={(e) => e.stopPropagation()}
                >
                  <Stack direction="row" spacing={0.5}>
                    <Tooltip title={canEdit ? 'Edit line' : 'Open line'}>
                      <span>
                        <IconButton
                          size="small"
                          aria-label={canEdit ? 'Edit' : 'Open'}
                          disabled={busy}
                          onClick={() => setOpenId(row.id)}
                          sx={iconBtnSx}
                        >
                          {canEdit ? (
                            <EditOutlinedIcon sx={{ fontSize: 18 }} />
                          ) : (
                            <OpenInNewOutlinedIcon sx={{ fontSize: 18 }} />
                          )}
                        </IconButton>
                      </span>
                    </Tooltip>
                    {canEdit && (
                      <Tooltip
                        title={
                          row.findingId
                            ? 'This line has a finding. Keep or close that finding first.'
                            : 'Remove line'
                        }
                      >
                        <span>
                          <IconButton
                            size="small"
                            aria-label="Remove"
                            disabled={busy || Boolean(row.findingId)}
                            onClick={() => setRemoveItemId(row.id)}
                            sx={removeBtnSx}
                          >
                            <DeleteOutlineIcon sx={{ fontSize: 18 }} />
                          </IconButton>
                        </span>
                      </Tooltip>
                    )}
                  </Stack>
                </TableCell>
              </TableRow>
              )
            })}
            {sheet.items.length === 0 && (
              <TableRow>
                <TableCell colSpan={colCount}>
                  <Typography variant="body2" color="text.secondary" sx={{ py: 2, textAlign: 'center' }}>
                    No rows. Download the template, upload the filled file, or add a row.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
            {sheet.items.length > 0 && filteredItems.length === 0 && (
              <TableRow>
                <TableCell colSpan={colCount}>
                  <Typography variant="body2" color="text.secondary" sx={{ py: 2, textAlign: 'center' }}>
                    No lines match this search.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
      <RegisterPagination
        count={filteredItems.length}
        page={page}
        rowsPerPage={rowsPerPage}
        onPageChange={setPage}
        onRowsPerPageChange={setRowsPerPage}
        dense
        rowsPerPageOptions={[10, 25, 50, 100]}
      />
      </Box>

      {openRow && (
        <AuditChecklistLineDrawer
          detail={detail}
          row={openRow}
          canEdit={canEdit}
          busy={busy}
          onBusy={setBusy}
          onClose={() => setOpenId(null)}
          onSaved={(updated) => {
            setPatches((prev) => {
              const next = { ...prev }
              delete next[updated.id]
              return next
            })
            onOk(updated.findingId ? 'Line saved. Finding ID minted from the checklist.' : 'Line saved')
            void onRefresh()
          }}
          onError={onError}
          hasPrev={openIndex > 0}
          hasNext={openIndex >= 0 && openIndex < filteredItems.length - 1}
          onPrev={() => {
            if (openIndex > 0) setOpenId(filteredItems[openIndex - 1].id)
          }}
          onNext={() => {
            if (openIndex >= 0 && openIndex < filteredItems.length - 1) {
              setOpenId(filteredItems[openIndex + 1].id)
            }
          }}
        />
      )}

      <Dialog open={addSheetOpen} onClose={() => !busy && setAddSheetOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Add Checklist</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ mt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Add a starter template, or a custom checklist. Download the template, fill the rows, then upload.
            </Typography>
            {addable.map((t) => (
              <Button key={t.key} variant="outlined" onClick={() => addLibrary(t.key)} disabled={busy}>
                {t.label}
              </Button>
            ))}
            {addable.length === 0 && (
              <Typography variant="caption" color="text.secondary">
                ISO 27001, ISO 20000, and UAE IA are already on this audit.
              </Typography>
            )}
            <CompactTextField
              label="New custom checklist name"
              fieldIdPrefix="new-cl"
              value={customName}
              onChange={(e) => setCustomName(e.target.value)}
              required
              fullWidth
            />
            <Button size="small" variant="outlined" disabled={busy} onClick={downloadTemplate}>
              Download Template
            </Button>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddSheetOpen(false)} disabled={busy}>
            Cancel
          </Button>
          <BusyButton
            variant="outlined"
            busy={busy}
            disabled={!customName.trim()}
            disabledReason={!customName.trim() ? 'Enter a checklist name' : undefined}
            onClick={() => void uploadNewFromDialog()}
          >
            Upload
          </BusyButton>
          <BusyButton
            variant="contained"
            busy={busy}
            disabled={!customName.trim()}
            disabledReason={!customName.trim() ? 'Enter a checklist name' : undefined}
            onClick={addBlank}
          >
            Create Blank
          </BusyButton>
        </DialogActions>
      </Dialog>

      <Dialog open={addRowOpen} onClose={() => !busy && setAddRowOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Add Checklist Row</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ mt: 1 }}>
            <CompactTextField
              label="Clause / Ref"
              fieldIdPrefix="row-ref"
              value={clauseRef}
              onChange={(e) => setClauseRef(e.target.value)}
              required
              fullWidth
            />
            <CompactTextField
              label="Section"
              fieldIdPrefix="row-sec"
              value={section}
              onChange={(e) => setSection(e.target.value)}
              fullWidth
            />
            <CompactTextField
              label="Title"
              fieldIdPrefix="row-title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              fullWidth
            />
            <CompactTextField
              label="What the auditor is testing"
              fieldIdPrefix="row-test"
              value={testFocus}
              onChange={(e) => setTestFocus(e.target.value)}
              multiline
              minRows={2}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddRowOpen(false)} disabled={busy}>
            Cancel
          </Button>
          <Button variant="contained" onClick={addRow} disabled={busy || !clauseRef.trim() || !title.trim()}>
            Add Row
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmActionDialog
        open={removeSheet != null}
        title="Remove Checklist"
        message={`Remove ${removeSheet?.name ?? 'this checklist'} and all of its rows? This cannot be undone.`}
        confirmLabel="Remove"
        busy={busy}
        onCancel={() => setRemoveSheet(null)}
        onConfirm={() => {
          if (!removeSheet) return
          const id = removeSheet.id
          setRemoveSheet(null)
          setTab(0)
          void run(() => api.deleteAuditChecklist(detail.audit.id, id), 'Checklist removed')
        }}
      />
      <ConfirmActionDialog
        open={removeItemId != null}
        title="Remove Row"
        message="Remove this checklist row? This cannot be undone."
        confirmLabel="Remove"
        busy={busy}
        onCancel={() => setRemoveItemId(null)}
        onConfirm={() => {
          if (removeItemId == null) return
          const id = removeItemId
          setRemoveItemId(null)
          void run(() => api.deleteAuditChecklistItem(detail.audit.id, id), 'Row removed')
        }}
      />
      <ConfirmActionDialog
        open={replaceAsk}
        title="Replace Checklist"
        message="Upload will replace the current rows on this custom checklist. Lines with findings must be removed first."
        confirmLabel="Replace"
        confirmColor="primary"
        busy={busy}
        onCancel={() => {
          setReplaceAsk(false)
          setPendingReplace(null)
        }}
        onConfirm={() => void uploadOntoSheet(pendingReplace)}
      />
    </Stack>
  )
}
