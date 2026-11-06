import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import {
  Box,
  Button,
  Card,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
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
  type TprmAssessment,
  type TprmAssessmentItem,
  type TprmQuestionnaire,
  type TprmVendor,
  type TprmVerdict,
} from './api'
import { useAppShellPage } from './components/AppShellPageContext'
import StatusChip from './components/StatusChip'
import TprmReviewDrawer from './components/tprm/TprmReviewDrawer'
import {
  AttentionBanner,
  BusyButton,
  CompactTextField,
  ConfirmActionDialog,
  FilterPillRow,
  INPUT_LIMIT,
  KpiCard,
  KpiCardGrid,
  RegisterListFrame,
  RegisterPagination,
  RegisterToolbar,
  SettingsCard,
  complexFormContainerSx,
  controlActionSx,
  lastTableCellSx,
  compactTableCellSxWidth,
  mediumTableCellSx,
  primaryRegisterNameSx,
  primaryTableCellSx,
  registerTableSx,
  twoColFieldSx,
  NARRATIVE_FIELD_MAX_WIDTH,
  narrativeReadScrollSx,
  readOnlyCaptionSx,
} from './components/ui'
import FormSelect from './components/ui/FormSelect'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { useOptionalUnsavedChangesGuardApi, useRegisterUnsavedChanges } from './uiQuality/UnsavedChangesGuard'
import { brand, semantic } from './theme'

function answerLabel(value: string | null | undefined): string {
  switch (value) {
    case 'YES':
      return 'Yes'
    case 'PARTIAL':
      return 'Partial'
    case 'NO':
      return 'No'
    case 'NA':
      return 'N/A'
    default:
      return '-'
  }
}

const VERDICT_LABEL: Record<TprmVerdict, string> = {
  ACCEPTED: 'Accepted',
  ACCEPTED_WITH_OBSERVATION: 'Accepted with observation',
  DOWNGRADED: 'Downgraded',
  RETURNED: 'Returned',
}

const REVIEW_VERDICTS: TprmVerdict[] = ['ACCEPTED', 'ACCEPTED_WITH_OBSERVATION', 'RETURNED']

type StatusFilter = '' | 'DRAFT' | 'ACTIVE' | 'SUBMITTED' | 'COMPLETE'
type VerdictFilter = '' | 'PENDING' | TprmVerdict

const EMPTY_RESPONSE_ITEMS: TprmAssessmentItem[] = []

function pretty(s: string): string {
  return s.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (c: string) => c.toUpperCase())
}

function statusTone(s: string): 'success' | 'info' | 'warning' | 'danger' | 'neutral' {
  switch (s) {
    case 'COMPLETE':
      return 'success'
    case 'SUBMITTED':
    case 'IN_REVIEW':
    case 'ISSUED':
      return 'info'
    case 'DRAFT':
    case 'IN_PROGRESS':
      return 'warning'
    case 'RETURNED':
      return 'danger'
    default:
      return 'neutral'
  }
}

function tierTone(tier: string | null | undefined): 'success' | 'info' | 'warning' | 'danger' | 'neutral' {
  switch (tier) {
    case 'TIER_1':
      return 'danger'
    case 'TIER_2':
      return 'warning'
    case 'TIER_3':
      return 'info'
    case 'TIER_4':
      return 'success'
    default:
      return 'neutral'
  }
}

function dateOnly(iso?: string | null): string {
  return iso ? iso.slice(0, 10) : '-'
}

function dateTimeUtc(iso?: string | null): string {
  if (!iso) return '-'
  return `${iso.slice(0, 16).replace('T', ' ')} UTC`
}

type ReviewFieldSnap = { verdict: TprmVerdict | null; comment: string | null }

function buildReviewSnap(items: TprmAssessmentItem[]): Record<number, ReviewFieldSnap> {
  const out: Record<number, ReviewFieldSnap> = {}
  for (const it of items) {
    out[it.id] = {
      verdict: it.reviewVerdict ?? null,
      comment: it.reviewComment ?? null,
    }
  }
  return out
}

function isReviewDirty(
  items: TprmAssessmentItem[],
  snap: Record<number, ReviewFieldSnap> | null,
): boolean {
  if (!snap) return false
  return items.some((it) => {
    const s = snap[it.id]
    const verdict = it.reviewVerdict ?? null
    const comment = it.reviewComment ?? null
    if (!s) return verdict != null || (comment ?? '') !== ''
    return verdict !== s.verdict || (comment ?? '') !== (s.comment ?? '')
  })
}

function tierLabel(tier: string | null | undefined): string {
  if (!tier) return '-'
  return tier.replace('TIER_', 'Tier ')
}

function MetaPair({
  label,
  value,
  scroll,
  action,
}: {
  label: string
  value: ReactNode
  scroll?: boolean
  action?: ReactNode
}) {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: scroll ? 'flex-start' : 'baseline',
        gap: 0.75,
        minWidth: 0,
        py: 0.15,
      }}
    >
      <Typography
        sx={{
          ...readOnlyCaptionSx,
          mb: 0,
          flexShrink: 0,
          lineHeight: 1.3,
          minWidth: 72,
        }}
      >
        {label}
      </Typography>
      <Typography
        sx={{
          fontSize: '0.75rem',
          fontWeight: 550,
          color: brand.ink,
          lineHeight: 1.3,
          minWidth: 0,
          flex: 1,
          wordBreak: 'break-word',
          ...(scroll ? narrativeReadScrollSx : {}),
        }}
      >
        {value === '' || value == null ? '-' : value}
      </Typography>
      {action}
    </Box>
  )
}

function vendorSelectOptions(vendors: TprmVendor[]) {
  return [
    { value: '', label: 'Select a vendor' },
    ...vendors.map((v) => ({
      value: String(v.id),
      label: v.profileComplete ? v.name : `${v.name} (incomplete profile)`,
    })),
  ]
}

function primarySpocEmail(vendor: TprmVendor | null | undefined): string {
  const email = vendor?.contacts.find((c) => c.role === 'PRIMARY_SPOC')?.email?.trim()
  return email || '-'
}

function RecapTile({
  label,
  value,
  accent,
  fill,
  grow = 1,
  compact,
}: {
  label: string
  value: ReactNode
  accent: string
  fill: string
  grow?: number
  compact?: boolean
}) {
  return (
    <Box
      sx={{
        flex: compact ? '0 0 auto' : `${grow} 1 0`,
        minWidth: compact ? '4.75rem' : grow >= 2 ? '12rem' : '7rem',
        width: compact ? '4.75rem' : undefined,
        px: 1.25,
        py: 1,
        borderRadius: 1,
        bgcolor: fill,
        border: `1px solid ${brand.border}`,
        borderLeft: `3px solid ${accent}`,
      }}
    >
      <Typography sx={{ ...readOnlyCaptionSx, color: accent, mb: 0.35, letterSpacing: '0.04em' }}>
        {label}
      </Typography>
      <Typography
        sx={{
          fontSize: '0.8125rem',
          fontWeight: 700,
          color: brand.ink,
          lineHeight: 1.35,
          wordBreak: compact ? 'normal' : 'break-word',
          whiteSpace: compact ? 'nowrap' : 'normal',
        }}
      >
        {value === '' || value == null ? '-' : value}
      </Typography>
    </Box>
  )
}

function VendorGateDialog({
  open,
  title,
  message,
  vendorId,
  vendors,
  recap,
  showPicker = true,
  confirmLabel,
  confirmWhy,
  busy,
  onVendorId,
  onCancel,
  onConfirm,
}: {
  open: boolean
  title: string
  message: string
  vendorId: string
  vendors: TprmVendor[]
  recap?: { spoc: string; questionnaire?: string; controls?: string; vendorName?: string }
  showPicker?: boolean
  confirmLabel: string
  confirmWhy?: string
  busy: boolean
  onVendorId?: (id: string) => void
  onCancel: () => void
  onConfirm: () => void
}) {
  const vendor = vendors.find((v) => String(v.id) === vendorId) ?? null
  return (
    <Dialog
      open={open}
      onClose={busy ? undefined : onCancel}
      maxWidth="sm"
      fullWidth
      PaperProps={{
        sx: {
          overflow: 'hidden',
          border: `1px solid ${brand.border}`,
          boxShadow: brand.shadowDropdown,
        },
      }}
    >
      <Box
        sx={{
          position: 'relative',
          bgcolor: brand.sidebar,
          borderBottom: `1px solid ${brand.border}`,
          px: 2.5,
          pt: 2.25,
          pb: 1.5,
        }}
      >
        <Box
          aria-hidden
          sx={{
            position: 'absolute',
            left: 0,
            top: 0,
            right: 0,
            height: 4,
            bgcolor: brand.gold,
          }}
        />
        <Typography sx={{ fontSize: '1.0625rem', fontWeight: 700, color: brand.ink, letterSpacing: '-0.02em' }}>
          {title}
        </Typography>
        <Typography sx={{ mt: 0.5, fontSize: '0.8125rem', color: brand.goldDark, lineHeight: 1.45 }}>
          {message}
        </Typography>
      </Box>
      <DialogContent sx={{ px: 2.5, py: 2, bgcolor: brand.paper }}>
        <Stack spacing={1.5}>
          {showPicker && onVendorId && (
            <FormSelect
              label="Vendor"
              required
              fullWidth
              value={vendorId}
              options={vendorSelectOptions(vendors)}
              onChange={onVendorId}
            />
          )}
          {vendor && (
            <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
              {vendor.tier ? (
                <StatusChip label={tierLabel(vendor.tier)} tone={tierTone(vendor.tier)} />
              ) : null}
              <StatusChip
                label={vendor.profileComplete ? 'Profile complete' : 'Profile incomplete'}
                tone={vendor.profileComplete ? 'success' : 'warning'}
              />
              {vendor.vendorType ? (
                <StatusChip label={pretty(vendor.vendorType)} tone="neutral" />
              ) : null}
            </Stack>
          )}
          {recap && (
            <Box sx={{ display: 'flex', flexWrap: 'nowrap', gap: 1 }}>
              {recap.vendorName ? (
                <RecapTile label="Vendor" value={recap.vendorName} accent={brand.gold} fill={brand.sidebar} grow={1.2} />
              ) : null}
              <RecapTile
                label="SPOC email"
                value={recap.spoc}
                accent={brand.goldDark}
                fill={brand.goldLight}
                grow={2.4}
              />
              {recap.questionnaire != null && (
                <RecapTile
                  label="Questionnaire"
                  value={recap.questionnaire}
                  accent={brand.gold}
                  fill={brand.sidebar}
                  grow={1.2}
                />
              )}
              {recap.controls != null && (
                <RecapTile
                  label="Controls"
                  value={recap.controls}
                  accent={semantic.risk.low}
                  fill="#F0FDF4"
                  compact
                />
              )}
            </Box>
          )}
          {vendor && !vendor.profileComplete && (
            <AttentionBanner
              dense
              tone="warning"
              emphasis="Profile incomplete"
              message="Finish the vendor profile before you use this vendor."
            />
          )}
        </Stack>
      </DialogContent>
      <DialogActions
        sx={{
          px: 2.5,
          py: 1.5,
          bgcolor: brand.hover,
          borderTop: `1px solid ${brand.border}`,
        }}
      >
        <Button onClick={onCancel} disabled={busy}>
          Cancel
        </Button>
        <BusyButton
          size="small"
          variant="contained"
          busy={busy}
          disabled={Boolean(confirmWhy)}
          disabledReason={confirmWhy}
          onClick={onConfirm}
          sx={controlActionSx}
        >
          {confirmLabel}
        </BusyButton>
      </DialogActions>
    </Dialog>
  )
}

function MetaChip({
  label,
  chipLabel,
  tone,
}: {
  label: string
  chipLabel: string
  tone: 'success' | 'info' | 'warning' | 'danger' | 'neutral'
}) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, py: 0.15, minWidth: 0 }}>
      <Typography sx={{ ...readOnlyCaptionSx, mb: 0, flexShrink: 0, minWidth: 72, lineHeight: 1.3 }}>
        {label}
      </Typography>
      <StatusChip label={chipLabel} tone={tone} />
    </Box>
  )
}

export default function TprmProjectsPage({
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
  const [projects, setProjects] = useState<TprmAssessment[]>([])
  const [vendors, setVendors] = useState<TprmVendor[]>([])
  const [questionnaires, setQuestionnaires] = useState<TprmQuestionnaire[]>([])
  const [creating, setCreating] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [vendorId, setVendorId] = useState('')
  const [questionnaireId, setQuestionnaireId] = useState('')
  const [draftVendorId, setDraftVendorId] = useState('')
  const [draftQuestionnaireId, setDraftQuestionnaireId] = useState('')
  const [selected, setSelected] = useState<TprmAssessment | null>(null)
  const [busy, setBusy] = useState(false)
  const [collabEmail, setCollabEmail] = useState('')
  const [collabOpen, setCollabOpen] = useState(false)
  const [sessionCollabs, setSessionCollabs] = useState<string[]>([])
  const [confirmIssue, setConfirmIssue] = useState(false)
  const [confirmChangeVendor, setConfirmChangeVendor] = useState(false)
  const [changeVendorId, setChangeVendorId] = useState('')
  const [assignVendorId, setAssignVendorId] = useState('')
  const [confirmClose, setConfirmClose] = useState(false)
  const [confirmReturn, setConfirmReturn] = useState(false)
  const [confirmAcceptAll, setConfirmAcceptAll] = useState(false)
  const [reviewItemId, setReviewItemId] = useState<number | null>(null)
  const [reviewSnap, setReviewSnap] = useState<Record<number, ReviewFieldSnap> | null>(null)
  const [verdictFilter, setVerdictFilter] = useState<VerdictFilter>('')
  const [acceptAllNotice, setAcceptAllNotice] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('')
  const { listReady, markReady } = useListReady()
  const dirtyCreate = creating && (name.trim() !== '' || description.trim() !== '' || vendorId !== '' || questionnaireId !== '')
  const reviewEditable = canAdmin && selected != null
    && (selected.status === 'SUBMITTED' || selected.status === 'IN_REVIEW')
  const reviewDirty = Boolean(reviewEditable && selected && isReviewDirty(selected.items, reviewSnap))
  const draftDirty = Boolean(
    selected?.status === 'DRAFT'
      && (String(selected.vendorId) !== draftVendorId
        || String(selected.questionnaireId ?? '') !== draftQuestionnaireId),
  )
  useRegisterUnsavedChanges(
    creating && dirtyCreate && !busy
      ? {
          dirty: true,
          onSaveDraft: () => create(),
          saveDraftLabel: 'Create Project',
        }
      : selected?.status === 'DRAFT' && draftDirty && !busy
        ? {
            dirty: true,
            title: 'Leave without saving?',
            message: 'You have unsaved vendor or questionnaire changes. Save them, discard them, or keep editing.',
            onSaveDraft: () => saveDraftLinks(),
            saveDraftLabel: 'Save Changes',
          }
      : reviewDirty && !busy
        ? {
            dirty: true,
            title: 'Leave without saving?',
            message: 'You have unsaved review changes. Save them, discard them, or keep editing.',
            onSaveDraft: async () => {
              const ok = await saveReview(false)
              if (!ok) throw new Error('Review save blocked')
            },
            saveDraftLabel: 'Save Review',
          }
        : null,
  )

  const applySelected = useCallback((p: TprmAssessment | null) => {
    setSelected(p)
    setReviewSnap(p ? buildReviewSnap(p.items) : null)
    setDraftVendorId(p?.vendorId != null ? String(p.vendorId) : '')
    setDraftQuestionnaireId(p?.questionnaireId != null ? String(p.questionnaireId) : '')
  }, [])

  const load = useCallback(async () => {
    try {
      const [p, v, q] = await Promise.all([
        api.listTprmProjects(),
        api.listTprmVendors(),
        api.listTprmQuestionnaires(),
      ])
      setProjects(p)
      setVendors(v)
      setQuestionnaires(q)
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
      setProjects([])
    } finally {
      markReady()
    }
  }, [onError, markReady])

  useEffect(() => { void load() }, [load])

  useEffect(() => {
    setSessionCollabs([])
  }, [selected?.id])

  const selectedVendor = useMemo(
    () => vendors.find((v) => v.id === selected?.vendorId) ?? null,
    [vendors, selected?.vendorId],
  )
  const selectedQuestionnaire = useMemo(
    () => questionnaires.find((q) => q.id === selected?.questionnaireId) ?? null,
    [questionnaires, selected?.questionnaireId],
  )
  const createVendor = useMemo(
    () => vendors.find((v) => String(v.id) === vendorId) ?? null,
    [vendors, vendorId],
  )
  const createQuestionnaire = useMemo(
    () => questionnaires.find((q) => String(q.id) === questionnaireId) ?? null,
    [questionnaires, questionnaireId],
  )
  const draftVendor = useMemo(
    () => vendors.find((v) => String(v.id) === draftVendorId) ?? null,
    [vendors, draftVendorId],
  )
  const draftQuestionnaire = useMemo(
    () => questionnaires.find((q) => String(q.id) === draftQuestionnaireId) ?? null,
    [questionnaires, draftQuestionnaireId],
  )
  const changeVendor = useMemo(
    () => vendors.find((v) => String(v.id) === changeVendorId) ?? null,
    [vendors, changeVendorId],
  )
  const assignVendor = useMemo(
    () => vendors.find((v) => String(v.id) === assignVendorId) ?? null,
    [vendors, assignVendorId],
  )

  const createWhy = !name.trim()
    ? 'Enter a project name'
    : !vendorId
      ? 'Select a vendor'
      : !questionnaireId
        ? 'Select a questionnaire'
        : createQuestionnaire && createQuestionnaire.controlCount === 0
          ? 'This questionnaire has no questions yet'
          : undefined

  const assignWhy = selected?.status !== 'DRAFT'
    ? undefined
    : draftDirty
      ? 'Save vendor and questionnaire changes first'
      : !selectedVendor
        ? 'Vendor record is missing'
        : !selectedVendor.profileComplete
          ? 'Complete the vendor profile before assigning'
          : (selectedQuestionnaire?.controlCount ?? selected.items.length) === 0
            ? 'This questionnaire has no questions'
            : undefined

  const draftSaveWhy = !draftVendorId
    ? 'Select a vendor'
    : !draftQuestionnaireId
      ? 'Select a questionnaire'
      : draftQuestionnaire && draftQuestionnaire.controlCount === 0
        ? 'This questionnaire has no questions yet'
        : !draftDirty
          ? 'No vendor or questionnaire changes'
          : undefined

  const changeVendorWhy = !changeVendorId
    ? 'Select a vendor'
    : changeVendorId === (selected ? String(selected.vendorId) : '')
      ? 'Vendor is unchanged'
      : changeVendor && !changeVendor.profileComplete
        ? 'Complete the vendor profile before using this vendor'
        : undefined

  const assignDialogWhy = !assignVendorId
    ? 'Select a vendor'
    : assignVendor && !assignVendor.profileComplete
      ? 'Complete the vendor profile before assigning'
      : undefined

  const create = async () => {
    if (createWhy) {
      onError(createWhy)
      return
    }
    setBusy(true)
    try {
      const p = await api.createTprmProject({
        name: name.trim(),
        description: description.trim() || null,
        vendorId: Number(vendorId),
        questionnaireId: Number(questionnaireId),
      })
      onOk('Project created')
      setCreating(false)
      setName('')
      setDescription('')
      setVendorId('')
      setQuestionnaireId('')
      await load()
      applySelected(p)
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const saveDraftLinks = async () => {
    if (!selected || selected.status !== 'DRAFT' || draftSaveWhy) {
      if (draftSaveWhy && draftDirty) onError(draftSaveWhy)
      return
    }
    setBusy(true)
    try {
      const p = await api.updateTprmProject(selected.id, {
        vendorId: Number(draftVendorId),
        questionnaireId: Number(draftQuestionnaireId),
      })
      applySelected(p)
      onOk('Project updated')
      await load()
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
      throw e instanceof Error ? e : new Error(String(e))
    } finally {
      setBusy(false)
    }
  }

  const refreshSelected = async (id: number) => {
    const p = await api.getTprmAssessment(id)
    applySelected(p)
    setProjects((prev) => prev.map((row) => (row.id === p.id ? p : row)))
  }

  const saveChangeVendor = async () => {
    if (!selected || selected.status !== 'DRAFT' || changeVendorWhy) {
      if (changeVendorWhy) onError(changeVendorWhy)
      return
    }
    setBusy(true)
    try {
      const p = await api.updateTprmProject(selected.id, {
        vendorId: Number(changeVendorId),
        questionnaireId: Number(draftQuestionnaireId || selected.questionnaireId),
      })
      applySelected(p)
      setConfirmChangeVendor(false)
      onOk('Vendor updated')
      await load()
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const issue = async () => {
    if (!selected || assignDialogWhy) {
      if (assignDialogWhy) onError(assignDialogWhy)
      return
    }
    setBusy(true)
    try {
      const p = await api.issueTprmProject(selected.id, {
        vendorId: Number(assignVendorId),
        expiryDays: 14,
      })
      applySelected(p)
      onOk('Questionnaire assigned to the vendor')
      setConfirmIssue(false)
      await load()
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const addCollab = async () => {
    if (!selected || !collabEmail.trim()) return
    setBusy(true)
    try {
      const invited = collabEmail.trim()
      const p = await api.addTprmCollaborator(selected.id, { email: invited })
      applySelected(p)
      setSessionCollabs((prev) => (prev.some((e) => e.toLowerCase() === invited.toLowerCase()) ? prev : [...prev, invited]))
      setCollabEmail('')
      setCollabOpen(false)
      onOk('Collaborator invited')
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const setItem = (id: number, patch: Partial<TprmAssessmentItem>) => {
    if (!selected) return
    setSelected({
      ...selected,
      items: selected.items.map((it) => (it.id === id ? { ...it, ...patch } : it)),
    })
  }

  const discardReviewEdits = () => {
    if (!selected || !reviewSnap) return
    setSelected({
      ...selected,
      items: selected.items.map((it) => {
        const s = reviewSnap[it.id]
        if (!s) return it
        return { ...it, reviewVerdict: s.verdict, reviewComment: s.comment }
      }),
    })
    setAcceptAllNotice(null)
  }

  const saveReview = async (complete: boolean, returnToVendor = false): Promise<boolean> => {
    if (!selected) return false
    if (complete && returnToVendor) {
      onError('Choose either Return to vendor or Close project, not both')
      return false
    }
    const items = selected.items
      .filter((it) => it.reviewVerdict != null)
      .map((it) => ({ itemId: it.id, verdict: it.reviewVerdict as TprmVerdict, comment: it.reviewComment }))
    if (items.length === 0) {
      onError('Set a verdict on at least one control before saving')
      return false
    }
    if (items.some((it) => it.verdict === 'DOWNGRADED')) {
      onError('Downgraded is no longer a review verdict')
      return false
    }
    if (items.some((it) => it.verdict === 'ACCEPTED_WITH_OBSERVATION' && !(it.comment ?? '').trim())) {
      onError('Review comment is required for Accepted with observation')
      return false
    }
    if (returnToVendor) {
      const returnedCount = selected.items.filter((it) => it.reviewVerdict === 'RETURNED').length
      if (returnedCount === 0) {
        onError('Mark at least one control as Returned before returning to the vendor')
        return false
      }
    }
    if (complete) {
      const missing = selected.items.find((it) => it.reviewVerdict == null)
      if (missing) {
        onError('Set a verdict on every control before closing')
        return false
      }
      if (selected.items.some((it) => it.reviewVerdict === 'RETURNED')) {
        onError('Return returned questions to the vendor first. Close only when every question is accepted.')
        return false
      }
    }
    setBusy(true)
    try {
      const p = await api.reviewTprmAssessment(selected.id, { complete, returnToVendor, items })
      applySelected(p)
      setConfirmClose(false)
      setConfirmReturn(false)
      setAcceptAllNotice(null)
      onOk(
        complete
          ? 'Project closed'
          : returnToVendor
            ? 'Returned controls sent to the vendor'
            : 'Review saved',
      )
      await load()
      return true
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
      return false
    } finally {
      setBusy(false)
    }
  }

  const returnedMarkedCount = selected?.items.filter((it) => it.reviewVerdict === 'RETURNED').length ?? 0
  const pendingVerdictCount = selected?.items.filter((it) => it.reviewVerdict == null).length ?? 0
  const acceptedCount = selected?.items.filter((it) => it.reviewVerdict === 'ACCEPTED').length ?? 0
  const acceptAllChecked = Boolean(selected && selected.items.length > 0 && acceptedCount === selected.items.length)
  const acceptAllIndeterminate = acceptedCount > 0 && !acceptAllChecked
  const closeWhy = !reviewEditable
    ? undefined
    : returnedMarkedCount > 0
      ? 'Return returned questions to the vendor first'
      : pendingVerdictCount > 0
        ? `Set a verdict on ${pendingVerdictCount} remaining control${pendingVerdictCount === 1 ? '' : 's'}`
        : selected.items.some(
            (it) => it.reviewVerdict === 'ACCEPTED_WITH_OBSERVATION' && !(it.reviewComment ?? '').trim(),
          )
          ? 'Add a review comment on every Accepted with observation control'
        : undefined

  const applyAcceptAll = () => {
    if (!selected || !reviewEditable) return
    const pending = selected.items.filter((it) => it.reviewVerdict == null).length
    const skipped = selected.items.filter(
      (it) => it.reviewVerdict != null && it.reviewVerdict !== 'ACCEPTED',
    ).length
    setSelected({
      ...selected,
      items: selected.items.map((it) =>
        it.reviewVerdict == null || it.reviewVerdict === 'ACCEPTED'
          ? { ...it, reviewVerdict: 'ACCEPTED' as TprmVerdict }
          : it,
      ),
    })
    setConfirmAcceptAll(false)
    if (pending === 0 && skipped === 0) {
      setAcceptAllNotice('All controls are already Accepted. Use Save review if you still have other unsaved changes.')
    } else if (pending === 0) {
      setAcceptAllNotice(
        `No Pending controls to accept. ${skipped} control${skipped === 1 ? '' : 's'} already have another verdict and were left unchanged.`,
      )
    } else {
      setAcceptAllNotice(
        `${pending} Pending control${pending === 1 ? '' : 's'} marked Accepted. Use Save review to keep these verdicts.`
        + (skipped
          ? ` ${skipped} with Returned or another verdict ${skipped === 1 ? 'was' : 'were'} left unchanged.`
          : ''),
      )
    }
  }

  const clearAcceptedVerdicts = () => {
    if (!selected || !reviewEditable) return
    const cleared = selected.items.filter((it) => it.reviewVerdict === 'ACCEPTED').length
    setSelected({
      ...selected,
      items: selected.items.map((it) =>
        it.reviewVerdict === 'ACCEPTED' ? { ...it, reviewVerdict: null } : it,
      ),
    })
    setAcceptAllNotice(
      cleared > 0
        ? `Cleared Accepted on ${cleared} control${cleared === 1 ? '' : 's'}. Use Save review to keep. Open a row to set a different verdict.`
        : null,
    )
  }

  const onAcceptAllCheckbox = (checked: boolean) => {
    if (!selected || !reviewEditable) return
    if (checked) {
      setConfirmAcceptAll(true)
      return
    }
    clearAcceptedVerdicts()
  }
  const reviewItem = selected?.items.find((it) => it.id === reviewItemId) ?? null
  const reviewIndex = reviewItem && selected ? selected.items.findIndex((it) => it.id === reviewItem.id) : -1
  const kpis = useMemo(() => ({
    total: projects.length,
    draft: projects.filter((p) => p.status === 'DRAFT').length,
    active: projects.filter((p) => p.status === 'ISSUED' || p.status === 'IN_PROGRESS' || p.status === 'RETURNED').length,
    submitted: projects.filter((p) => p.status === 'SUBMITTED' || p.status === 'IN_REVIEW').length,
    complete: projects.filter((p) => p.status === 'COMPLETE').length,
  }), [projects])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    return projects.filter((p) => {
      if (statusFilter === 'DRAFT' && p.status !== 'DRAFT') return false
      if (statusFilter === 'ACTIVE' && !(p.status === 'ISSUED' || p.status === 'IN_PROGRESS' || p.status === 'RETURNED')) return false
      if (statusFilter === 'SUBMITTED' && !(p.status === 'SUBMITTED' || p.status === 'IN_REVIEW')) return false
      if (statusFilter === 'COMPLETE' && p.status !== 'COMPLETE') return false
      if (!q) return true
      const hay = `${p.title ?? ''} ${p.description ?? ''} ${p.displayId} ${p.vendorName ?? ''} ${p.questionnaireName ?? ''}`.toLowerCase()
      return hay.includes(q)
    })
  }, [projects, search, statusFilter])
  const paged = usePagedRows(filtered)

  const responseItems = useMemo(() => {
    if (!selected) return EMPTY_RESPONSE_ITEMS
    const canFilterVerdict = selected.status === 'SUBMITTED' || selected.status === 'IN_REVIEW'
      || selected.status === 'COMPLETE' || selected.status === 'RETURNED'
    if (!canFilterVerdict || !verdictFilter) return selected.items
    if (verdictFilter === 'PENDING') return selected.items.filter((it) => it.reviewVerdict == null)
    return selected.items.filter((it) => it.reviewVerdict === verdictFilter)
  }, [selected, verdictFilter])
  const responsePaged = usePagedRows(responseItems)

  const closeCreate = () => {
    requestNavigation(() => {
      setCreating(false)
      setName('')
      setDescription('')
      setVendorId('')
      setQuestionnaireId('')
    })
  }

  const closeSelected = () => {
    requestNavigation(() => {
      applySelected(null)
      setCollabEmail('')
      setCollabOpen(false)
      setSessionCollabs([])
      setReviewItemId(null)
      setVerdictFilter('')
      setAcceptAllNotice(null)
      void load()
    })
  }

  useAppShellPage({
    title: selected ? (selected.title || selected.displayId) : creating ? 'New Project' : 'Projects',
    breadcrumbTrail: selected || creating ? undefined : 'TPRM',
    breadcrumbRoot:
      selected || creating
        ? {
            label: 'Third-Party Risk / Projects',
            onClick: selected ? closeSelected : closeCreate,
          }
        : undefined,
    hideContextLabel: true,
    hideRoleChips: true,
    // Project workspace: fill height so Responses table grows; list page keeps normal scroll.
    flushContent: Boolean(selected),
    actions: creating ? (
      <Stack direction="row" spacing={0.75}>
        <Button size="small" variant="outlined" sx={controlActionSx} onClick={closeCreate}>Cancel</Button>
        <BusyButton size="small" variant="contained" busy={busy} disabled={Boolean(createWhy)}
          disabledReason={createWhy} onClick={() => void create()} sx={controlActionSx}>
          Create Project
        </BusyButton>
      </Stack>
    ) : selected ? (
      <Stack direction="row" spacing={0.75}>
        <Button size="small" variant="outlined" sx={controlActionSx} onClick={closeSelected}>Back</Button>
        {canAdmin && selected.status === 'DRAFT' && (
          <BusyButton
            size="small"
            variant="outlined"
            busy={busy}
            disabled={Boolean(draftSaveWhy)}
            disabledReason={draftSaveWhy}
            onClick={() => { void saveDraftLinks().catch(() => undefined) }}
            sx={controlActionSx}
          >
            Save Changes
          </BusyButton>
        )}
        {canAdmin && selected.status === 'DRAFT' && (
          <BusyButton
            size="small"
            variant="contained"
            busy={busy}
            disabled={Boolean(assignWhy)}
            disabledReason={assignWhy}
            onClick={() => {
              setAssignVendorId(String(selected.vendorId))
              setConfirmIssue(true)
            }}
            sx={controlActionSx}
          >
            Assign to Vendor
          </BusyButton>
        )}
        {canAdmin
          && (selected.status === 'ISSUED'
            || selected.status === 'IN_PROGRESS'
            || selected.status === 'RETURNED') && (
          <BusyButton
            size="small"
            variant="outlined"
            busy={busy}
            onClick={() => setCollabOpen(true)}
            sx={controlActionSx}
          >
            Add Collaborator
          </BusyButton>
        )}
        {canAdmin && reviewEditable && (
          <BusyButton
            size="small"
            variant="outlined"
            busy={busy}
            disabled={!reviewDirty}
            disabledReason={!reviewDirty ? 'No review changes to save' : undefined}
            onClick={() => void saveReview(false)}
            sx={controlActionSx}
          >
            Save Review
          </BusyButton>
        )}
        {canAdmin && reviewEditable && returnedMarkedCount > 0 && (
          <BusyButton
            size="small"
            variant="outlined"
            color="warning"
            busy={busy}
            onClick={() => setConfirmReturn(true)}
            sx={controlActionSx}
          >
            Return to Vendor
          </BusyButton>
        )}
        {canAdmin && reviewEditable && (
          <BusyButton
            size="small"
            variant="contained"
            busy={busy}
            disabled={Boolean(closeWhy)}
            disabledReason={closeWhy}
            onClick={() => setConfirmClose(true)}
            sx={controlActionSx}
          >
            Close Project
          </BusyButton>
        )}
        {selected.status !== 'DRAFT' && (
          <>
            <BusyButton size="small" variant="outlined" busy={busy} onClick={() => void api.downloadTprmProjectExport(selected.id, 'xlsx')} sx={controlActionSx}>
              Excel
            </BusyButton>
            <BusyButton size="small" variant="outlined" busy={busy} onClick={() => void api.downloadTprmProjectExport(selected.id, 'pdf')} sx={controlActionSx}>
              PDF
            </BusyButton>
          </>
        )}
      </Stack>
    ) : canAdmin ? (
      <BusyButton size="small" variant="contained" busy={false} onClick={() => setCreating(true)} sx={controlActionSx}>
        New Project
      </BusyButton>
    ) : undefined,
  }, [creating, selected, busy, canAdmin, createWhy, assignWhy, draftSaveWhy, reviewEditable, reviewDirty, returnedMarkedCount, closeWhy, collabEmail])

  if (creating) {
    return (
      <Stack spacing={1.5} sx={{ flex: 1, minHeight: 0, overflow: 'auto', p: 1.5, ...complexFormContainerSx }}>
        <SettingsCard
          heading
          autoHeight
          title="Project details"
          subtitle="Create the Third-Party Risk Management Project, and select the appropriate Vendor and Questionnaire to initiate the project."
        >
          <Box sx={twoColFieldSx}>
            <CompactTextField
              label="Project name"
              required
              fieldIdPrefix="tprm-proj"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. 2026 annual review"
              maxLength={INPUT_LIMIT.orgName}
            />
            <CompactTextField
              label="Project description"
              fieldIdPrefix="tprm-proj"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g. Annual security review"
              maxLength={INPUT_LIMIT.reason}
            />
          </Box>
          <Box sx={{ ...twoColFieldSx, mt: 1.5 }}>
            <FormSelect
              label="Vendor"
              required
              fullWidth
              value={vendorId}
                  options={vendorSelectOptions(vendors)}
              onChange={setVendorId}
            />
            <FormSelect
              label="Questionnaire"
              required
              fullWidth
              value={questionnaireId}
              options={[
                { value: '', label: 'Select a questionnaire' },
                ...questionnaires.map((q) => ({
                  value: String(q.id),
                  label: `${q.name} (${q.controlCount} question${q.controlCount === 1 ? '' : 's'})`,
                })),
              ]}
              onChange={setQuestionnaireId}
            />
          </Box>
        </SettingsCard>

        {(createVendor || createQuestionnaire) && (
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' },
              gap: 1.5,
            }}
          >
            {createVendor && (
              <SettingsCard compact heading autoHeight title="Vendor" subtitle="Inventory record that will receive this questionnaire.">
                <Box sx={{ ...twoColFieldSx }}>
                  <MetaPair label="Legal name" value={createVendor.name} />
                  {createVendor.tier ? (
                    <MetaChip label="Tier" chipLabel={tierLabel(createVendor.tier)} tone={tierTone(createVendor.tier)} />
                  ) : (
                    <MetaPair label="Tier" value="-" />
                  )}
                  <MetaChip
                    label="Profile"
                    chipLabel={createVendor.profileComplete ? 'Complete' : 'Incomplete'}
                    tone={createVendor.profileComplete ? 'success' : 'warning'}
                  />
                  <MetaPair label="Type" value={pretty(createVendor.vendorType ?? '')} />
                </Box>
                {!createVendor.profileComplete && (
                  <Box sx={{ mt: 1.5 }}>
                    <AttentionBanner
                      tone="warning"
                      emphasis="Profile incomplete"
                      message="You can still create the draft. Finish the vendor profile before Assign to vendor."
                    />
                  </Box>
                )}
              </SettingsCard>
            )}
            {createQuestionnaire && (
              <SettingsCard compact heading autoHeight title="Questionnaire" subtitle="Questions copied onto the project when you create it.">
                <Box sx={{ ...twoColFieldSx }}>
                  <MetaPair label="Name" value={createQuestionnaire.name} />
                  <MetaPair label="Questions" value={String(createQuestionnaire.controlCount)} />
                </Box>
                {createQuestionnaire.controlCount === 0 && (
                  <Box sx={{ mt: 1.5 }}>
                    <AttentionBanner
                      tone="warning"
                      emphasis="No questions"
                      message="Add or upload questions on the Questionnaires page before creating this project."
                    />
                  </Box>
                )}
              </SettingsCard>
            )}
          </Box>
        )}
      </Stack>
    )
  }

  if (selected) {
    const reviewable = selected.status === 'SUBMITTED' || selected.status === 'IN_REVIEW'
      || selected.status === 'COMPLETE' || selected.status === 'RETURNED'
    const postureTargetRaw = selected.status === 'DRAFT'
      ? (draftVendor?.postureTarget ?? selected.snapshotTarget)
      : (selected.snapshotTarget ?? selectedVendor?.postureTarget)
    const postureTargetLabel = postureTargetRaw == null
      ? '-'
      : `${Math.round(postureTargetRaw * 1000) / 10}%`
    const vendorProgressVisible = reviewable || selected.status === 'IN_PROGRESS'
    const inFlight = selected.status === 'ISSUED' || selected.status === 'IN_PROGRESS' || selected.status === 'RETURNED'
    const spocEmail = selectedVendor?.contacts.find((c) => c.role === 'PRIMARY_SPOC')?.email?.trim().toLowerCase() ?? ''
    const collabInvites = (selected.portalInvites ?? []).filter((inv) => {
      if (!inv.accessToken) return false
      if (!spocEmail) return true
      return inv.email.trim().toLowerCase() !== spocEmail
    })
    const listedEmails = new Set(collabInvites.map((inv) => inv.email.trim().toLowerCase()))
    const collabEmailsOnly = sessionCollabs.filter((email) => {
      const key = email.trim().toLowerCase()
      if (listedEmails.has(key)) return false
      if (spocEmail && key === spocEmail) return false
      return true
    })
    const hasCollabs = collabInvites.length > 0 || collabEmailsOnly.length > 0

    const verdictFilterOptions = reviewable
      ? ([
          { value: '' as VerdictFilter, label: 'All', count: selected.items.length },
          {
            value: 'PENDING' as VerdictFilter,
            label: 'Pending',
            count: selected.items.filter((it) => it.reviewVerdict == null).length,
          },
          ...REVIEW_VERDICTS.map((v) => ({
            value: v as VerdictFilter,
            label: VERDICT_LABEL[v],
            count: selected.items.filter((it) => it.reviewVerdict === v).length,
          })),
        ].map((opt) => ({
          ...opt,
          disabled: opt.value !== '' && (opt.count ?? 0) === 0,
        })))
      : []
    const collabWhy = !collabEmail.trim()
      ? 'Enter an email'
      : !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(collabEmail.trim())
        ? 'Enter a valid email'
        : undefined
    return (
      <Stack
        spacing={1}
        sx={{
          flex: 1,
          minHeight: 0,
          // Keep Responses filling the viewport; scroll inside the table, not the whole page.
          overflow: 'hidden',
          pt: 1,
          px: 1.5,
          pb: 1,
          ...complexFormContainerSx,
        }}
      >
        {selected.status === 'COMPLETE' && (
          <AttentionBanner
            dense
            tone={selected.compliant ? 'success' : 'warning'}
            emphasis={selected.compliant ? 'Compliant' : 'Not compliant'}
            message={selected.posturePercent != null
              ? `Posture ${Math.round(selected.posturePercent * 1000) / 10}%. Closure email was sent to the vendor SPOC.`
              : 'Project is closed.'}
          />
        )}
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', md: 'repeat(3, minmax(0, 1fr))' },
            gap: 1,
            flexShrink: 0,
          }}
        >
          <SettingsCard
            compact
            heading
            autoHeight
            title="Project"
            subtitle={selected.displayId}
            accentColor={brand.gold}
            headerBg={brand.sidebarSelectedFill}
          >
            <Stack spacing={0.35}>
              <MetaPair label="Name" value={selected.title || selected.displayId} />
              <MetaPair label="Description" value={selected.description?.trim() || '-'} scroll />
              <MetaChip label="Status" chipLabel={pretty(selected.status)} tone={statusTone(selected.status)} />
              <MetaPair label="Created" value={dateOnly(selected.createdAt)} />
            </Stack>
          </SettingsCard>
          <SettingsCard
            compact
            heading
            autoHeight
            title="Vendor"
            subtitle={selected.status === 'DRAFT' ? (draftVendor?.name || 'Select a vendor') : (selected.vendorName || '-')}
            accentColor={brand.gold}
            headerBg={brand.sidebar}
            action={
              <Box sx={{ textAlign: 'right', flexShrink: 0 }}>
                <Typography
                  sx={{
                    fontSize: '0.625rem',
                    fontWeight: 700,
                    letterSpacing: '0.06em',
                    textTransform: 'uppercase',
                    color: brand.goldDark,
                    lineHeight: 1.2,
                  }}
                >
                  Posture target
                </Typography>
                <Typography sx={{ fontSize: '0.875rem', fontWeight: 700, color: brand.goldDark, lineHeight: 1.25 }}>
                  {postureTargetLabel}
                </Typography>
              </Box>
            }
          >
            <Stack spacing={0.75}>
              {selected.status === 'DRAFT' && canAdmin ? (
                <FormSelect
                  label="Vendor"
                  required
                  fullWidth
                  value={draftVendorId}
                  options={vendorSelectOptions(vendors)}
                  onChange={(id) => {
                    setDraftVendorId(id)
                    if (id && id !== String(selected.vendorId)) {
                      setChangeVendorId(id)
                      setConfirmChangeVendor(true)
                    }
                  }}
                />
              ) : (
                <MetaPair label="Legal name" value={selected.vendorName} />
              )}
              {((selected.status === 'DRAFT' ? draftVendor?.tier : (selected.snapshotTier || selectedVendor?.tier))) ? (
                <MetaChip
                  label="Tier"
                  chipLabel={tierLabel(selected.status === 'DRAFT' ? draftVendor?.tier : (selected.snapshotTier || selectedVendor?.tier))}
                  tone={tierTone(selected.status === 'DRAFT' ? draftVendor?.tier : (selected.snapshotTier || selectedVendor?.tier))}
                />
              ) : (
                <MetaPair label="Tier" value="-" />
              )}
              {(selected.status === 'DRAFT' ? draftVendor : selectedVendor) ? (
                <MetaChip
                  label="Profile"
                  chipLabel={(selected.status === 'DRAFT' ? draftVendor : selectedVendor)!.profileComplete ? 'Complete' : 'Incomplete'}
                  tone={(selected.status === 'DRAFT' ? draftVendor : selectedVendor)!.profileComplete ? 'success' : 'warning'}
                />
              ) : (
                <MetaPair label="Profile" value="-" />
              )}
              <MetaPair
                label="Type"
                value={pretty(
                  (selected.status === 'DRAFT' ? draftVendor?.vendorType : (selected.snapshotVendorType || selectedVendor?.vendorType)) || '',
                )}
              />
            </Stack>
          </SettingsCard>
          <SettingsCard
            compact
            heading
            autoHeight
            title="Questionnaire"
            subtitle={
              selected.status === 'DRAFT'
                ? `${draftQuestionnaire?.controlCount ?? selected.items.length} question${(draftQuestionnaire?.controlCount ?? selected.items.length) === 1 ? '' : 's'}`
                : `${selected.items.length} question${selected.items.length === 1 ? '' : 's'}`
            }
            accentColor={brand.gold}
            headerBg={brand.sidebarSelectedFill}
          >
            <Stack spacing={0.75}>
              {selected.status === 'DRAFT' && canAdmin ? (
                <FormSelect
                  label="Questionnaire"
                  required
                  fullWidth
                  value={draftQuestionnaireId}
                  options={[
                    { value: '', label: 'Select a questionnaire' },
                    ...questionnaires.map((q) => ({
                      value: String(q.id),
                      label: `${q.name} (${q.controlCount} question${q.controlCount === 1 ? '' : 's'})`,
                    })),
                  ]}
                  onChange={setDraftQuestionnaireId}
                />
              ) : (
                <MetaPair label="Name" value={selected.questionnaireName || selectedQuestionnaire?.name || '-'} />
              )}
              <MetaPair label="Assigned" value={dateOnly(selected.sentAt)} />
              <MetaPair label="Vendor started" value={selected.startedAt ? dateTimeUtc(selected.startedAt) : 'Not started'} />
              <MetaPair label="Submitted" value={dateOnly(selected.submittedAt)} />
            </Stack>
          </SettingsCard>
        </Box>

        {selected.status === 'DRAFT' && assignWhy && (
          <AttentionBanner
            dense
            tone="warning"
            emphasis="Not ready to assign"
            message={`${assignWhy}. ${selectedVendor && !selectedVendor.profileComplete && selectedVendor.missingProfileFields.length
              ? `Still needed: ${selectedVendor.missingProfileFields.slice(0, 4).join(', ')}${selectedVendor.missingProfileFields.length > 4 ? ' and more.' : '.'}`
              : ''}`}
          />
        )}
        {selected.status === 'RETURNED' && (
          <AttentionBanner
            dense
            tone="warning"
            emphasis="Returned"
            message="Controls marked Returned were sent back to the vendor. You can view responses here. Review unlocks again when they resubmit."
          />
        )}
        {reviewEditable && returnedMarkedCount > 0 && (
          <AttentionBanner
            dense
            tone="warning"
            emphasis={`${returnedMarkedCount} marked Returned`}
            message="Keep reviewing other controls. When ready, Return to vendor sends only those controls back (same portal link). Close stays blocked until every control is accepted."
          />
        )}
        <Card
          sx={{
            flex: 1,
            minHeight: 0,
            overflow: 'hidden',
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          <Box sx={{ px: 1.25, pt: 1, pb: 0.35, flexShrink: 0 }}>
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              spacing={1}
              alignItems={{ sm: 'flex-start' }}
              justifyContent="space-between"
            >
              <Box sx={{ minWidth: 0, flex: 1 }}>
                <Typography variant="subtitle2">
                  {vendorProgressVisible ? 'Responses' : 'Controls Defined for This Project'}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {selected.items.length === 0
                    ? 'No questions on this project.'
                    : reviewable
                      ? 'Open a row to review the vendor answer, evidence, and verdict.'
                      : selected.status === 'IN_PROGRESS'
                        ? 'Open a row to see saved vendor answers. Verdicts wait until they submit.'
                        : 'These controls will be assigned to the vendor upon selection of "Assign to Vendor."'}
                </Typography>
              </Box>
              {reviewEditable && selected.items.length > 0 && (
                <FormControlLabel
                  sx={{
                    m: 0,
                    flexShrink: 0,
                    alignItems: 'center',
                    '& .MuiFormControlLabel-label': {
                      fontSize: '0.75rem',
                      fontWeight: 550,
                      color: brand.inkSoft,
                      userSelect: 'none',
                    },
                  }}
                  control={
                    <Checkbox
                      size="small"
                      checked={acceptAllChecked}
                      indeterminate={acceptAllIndeterminate}
                      disabled={busy}
                      onChange={(e) => onAcceptAllCheckbox(e.target.checked)}
                      sx={{ py: 0.25 }}
                    />
                  }
                  label="Accept all"
                  title="Mark every Pending control as Accepted (confirmation first). Save review to keep."
                />
              )}
            </Stack>
            {reviewable && selected.items.length > 0 && (
              <Box sx={{ mt: 1, mb: 0.5 }}>
                <FilterPillRow
                  value={verdictFilter}
                  options={[...verdictFilterOptions]}
                  onChange={setVerdictFilter}
                />
              </Box>
            )}
            {acceptAllNotice && (
              <Box sx={{ mt: 1 }}>
                <AttentionBanner
                  dense
                  tone="info"
                  emphasis="Accept all"
                  message={acceptAllNotice}
                  onDismiss={() => setAcceptAllNotice(null)}
                />
              </Box>
            )}
          </Box>
          {selected.items.length === 0 ? null : responseItems.length === 0 ? (
            <Box sx={{ px: 1.5, py: 2 }}>
              <Typography variant="body2" color="text.secondary">
                No controls match this verdict filter.
              </Typography>
              <Button size="small" sx={{ mt: 1, ...controlActionSx }} onClick={() => setVerdictFilter('')}>
                Show All
              </Button>
            </Box>
          ) : (
            <>
              <TableContainer sx={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
                <Table
                  size="small"
                  stickyHeader
                  sx={{
                    ...registerTableSx,
                    '& .MuiTableCell-root': { py: 0.45 },
                    '& .MuiTableCell-head': { py: 0.55 },
                  }}
                >
                  <TableHead>
                    <TableRow>
                      <TableCell sx={compactTableCellSxWidth('12%')}>Control ID</TableCell>
                      <TableCell sx={mediumTableCellSx}>Domain</TableCell>
                      <TableCell sx={primaryTableCellSx}>Requirement</TableCell>
                      {vendorProgressVisible ? (
                        <>
                          <TableCell sx={compactTableCellSxWidth('10%')}>Answer</TableCell>
                          <TableCell sx={compactTableCellSxWidth('12%')}>Evidence</TableCell>
                          <TableCell sx={lastTableCellSx}>{reviewable ? 'Verdict' : 'Progress'}</TableCell>
                        </>
                      ) : (
                        <TableCell sx={lastTableCellSx}>Evidence</TableCell>
                      )}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {responsePaged.paged.map((it) => (
                      <TableRow
                        key={it.id}
                        hover={vendorProgressVisible}
                        sx={{ cursor: vendorProgressVisible ? 'pointer' : 'default' }}
                        onClick={vendorProgressVisible ? () => setReviewItemId(it.id) : undefined}
                      >
                        <TableCell sx={compactTableCellSxWidth('12%')}>{it.controlCode}</TableCell>
                        <TableCell sx={{ ...mediumTableCellSx, whiteSpace: 'normal' }}>{it.domain || '-'}</TableCell>
                        <TableCell sx={primaryRegisterNameSx}>{it.requirement || '-'}</TableCell>
                        {vendorProgressVisible ? (
                          <>
                            <TableCell sx={compactTableCellSxWidth('10%')}>{answerLabel(it.vendorAnswer)}</TableCell>
                            <TableCell sx={compactTableCellSxWidth('12%')}>
                              {it.evidenceFiles?.length ? `${it.evidenceFiles.length} file(s)` : (it.evidenceRef?.trim() ? 'Reference' : (it.evidenceMandatory ? 'Required' : '-'))}
                            </TableCell>
                            <TableCell sx={lastTableCellSx}>
                              {reviewable
                                ? (it.reviewVerdict ? VERDICT_LABEL[it.reviewVerdict] : 'Pending')
                                : (it.vendorAnswer ? 'Saved' : 'Not started')}
                            </TableCell>
                          </>
                        ) : (
                          <TableCell sx={lastTableCellSx}>{it.evidenceMandatory ? 'Mandatory' : 'Optional'}</TableCell>
                        )}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              <RegisterPagination
                dense
                count={responsePaged.count}
                page={responsePaged.page}
                rowsPerPage={responsePaged.rowsPerPage}
                onPageChange={responsePaged.setPage}
                onRowsPerPageChange={responsePaged.setRowsPerPage}
                rowsPerPageOptions={[10, 25, 50, 100]}
              />
            </>
          )}
        </Card>

        {/* Portal links only while the vendor can still open the questionnaire - keeps Responses tall during review. */}
        {inFlight && (
          <Box sx={{ flexShrink: 0 }}>
            {lastIssuedBanner(selected, session.tenantId)}
          </Box>
        )}
        {canAdmin && inFlight && (
          <Box sx={{ flexShrink: 0 }}>
            <SettingsCard
              compact
              heading
              autoHeight
              title="Collaborators"
              subtitle="Invite another email after the questionnaire is assigned. They receive their own portal link."
            >
              {hasCollabs ? (
                <Stack spacing={1}>
                  <PortalInviteList tenantId={session.tenantId} invites={collabInvites} />
                  {collabEmailsOnly.map((email) => (
                    <Typography key={email} sx={{ fontSize: '0.75rem', fontWeight: 600 }}>{email}</Typography>
                  ))}
                </Stack>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  No collaborators yet.
                </Typography>
              )}
            </SettingsCard>
          </Box>
        )}

        {reviewItem && (
          <TprmReviewDrawer
            open
            item={reviewItem}
            index={reviewIndex}
            total={selected.items.length}
            reviewable={reviewable}
            reviewEditable={reviewEditable}
            progressHint={
              selected.status === 'IN_PROGRESS'
                ? 'This is saved progress. Assign a verdict after the vendor submits.'
                : null
            }
            busy={busy}
            dirty={reviewDirty}
            onClose={() => setReviewItemId(null)}
            onPrev={() => {
              const prev = selected.items[reviewIndex - 1]
              if (prev) setReviewItemId(prev.id)
            }}
            onNext={() => {
              const next = selected.items[reviewIndex + 1]
              if (next) setReviewItemId(next.id)
            }}
            onPatch={(patch) => setItem(reviewItem.id, patch)}
            onSave={() => saveReview(false)}
            onDiscard={discardReviewEdits}
            onCloseProject={
              reviewEditable && !closeWhy
                ? () => setConfirmClose(true)
                : undefined
            }
            onReturnToVendor={
              reviewEditable && returnedMarkedCount > 0
                ? () => setConfirmReturn(true)
                : undefined
            }
            onError={onError}
          />
        )}
        <Dialog
          open={collabOpen}
          onClose={busy ? undefined : () => {
            setCollabOpen(false)
            setCollabEmail('')
          }}
          fullWidth
          maxWidth="xs"
        >
          <DialogTitle sx={{ pb: 1 }}>Add Collaborator</DialogTitle>
          <DialogContent>
            <Box sx={{ mt: 0.5, maxWidth: NARRATIVE_FIELD_MAX_WIDTH }}>
              <CompactTextField
                label="Collaborator email"
                required
                type="email"
                fieldIdPrefix="tprm-collab"
                value={collabEmail}
                autoFocus
                onChange={(e) => setCollabEmail(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !collabWhy && !busy) void addCollab()
                }}
              />
            </Box>
          </DialogContent>
          <DialogActions sx={{ px: 2.5, pb: 2 }}>
            <Button
              onClick={() => {
                setCollabOpen(false)
                setCollabEmail('')
              }}
              disabled={busy}
            >
              Cancel
            </Button>
            <BusyButton
              size="small"
              variant="contained"
              busy={busy}
              disabled={Boolean(collabWhy)}
              disabledReason={collabWhy}
              onClick={() => void addCollab()}
              sx={controlActionSx}
            >
              Add
            </BusyButton>
          </DialogActions>
        </Dialog>
        <VendorGateDialog
          open={confirmChangeVendor}
          title="Change vendor?"
          message="Confirm the vendor and SPOC before you save."
          vendorId={changeVendorId}
          vendors={vendors}
          showPicker={false}
          recap={{
            vendorName: changeVendor?.name || '-',
            spoc: primarySpocEmail(changeVendor),
          }}
          confirmLabel={busy ? 'Saving…' : 'Save Vendor'}
          confirmWhy={changeVendorWhy}
          busy={busy}
          onCancel={() => {
            if (selected) setDraftVendorId(String(selected.vendorId))
            setChangeVendorId(selected ? String(selected.vendorId) : '')
            setConfirmChangeVendor(false)
          }}
          onConfirm={() => void saveChangeVendor()}
        />
        <VendorGateDialog
          open={confirmIssue}
          title="Assign to vendor?"
          message="This sends the questionnaire to the vendor you select. Check the SPOC email before you assign."
          vendorId={assignVendorId}
          vendors={vendors}
          recap={{
            spoc: primarySpocEmail(assignVendor),
            questionnaire: selectedQuestionnaire?.name || selected?.questionnaireName || '-',
            controls: String(selectedQuestionnaire?.controlCount ?? selected?.items.length ?? 0),
          }}
          confirmLabel={busy ? 'Assigning…' : 'Assign'}
          confirmWhy={assignDialogWhy}
          busy={busy}
          onVendorId={setAssignVendorId}
          onCancel={() => setConfirmIssue(false)}
          onConfirm={() => void issue()}
        />
        <ConfirmActionDialog
          open={confirmAcceptAll}
          title="Accept all pending controls?"
          message={
            pendingVerdictCount > 0
              ? `${pendingVerdictCount} Pending control${pendingVerdictCount === 1 ? '' : 's'} will be marked Accepted. Controls already Returned or Accepted with observation stay as they are. Use Save review afterward to keep these verdicts.`
              : returnedMarkedCount > 0 || acceptedCount < (selected?.items.length ?? 0)
                ? 'There are no Pending controls. Accept all will not change Returned or other non-Accepted verdicts.'
                : 'All controls are already Accepted.'
          }
          confirmLabel="Accept All"
          confirmColor="primary"
          busy={busy}
          onCancel={() => setConfirmAcceptAll(false)}
          onConfirm={applyAcceptAll}
        />
        <ConfirmActionDialog
          open={confirmClose}
          title="Close project?"
          message="Close only when every question is accepted. The vendor will receive a closure email."
          confirmLabel={busy ? 'Closing…' : 'Close Project'}
          confirmColor="primary"
          busy={busy}
          onCancel={() => setConfirmClose(false)}
          onConfirm={() => void saveReview(true)}
        />
        <ConfirmActionDialog
          open={confirmReturn}
          title="Return controls to vendor?"
          message={
            pendingVerdictCount > 0
              ? `${returnedMarkedCount} control${returnedMarkedCount === 1 ? '' : 's'} will go back to the vendor (same portal link). You still have ${pendingVerdictCount} with no verdict. If you return now, you cannot review those until the vendor resubmits. Set their verdicts, Save, then return.`
              : `${returnedMarkedCount} control${returnedMarkedCount === 1 ? '' : 's'} marked Returned will go back to the vendor on the same portal link. Accepted controls stay locked for the vendor.`
          }
          confirmLabel={busy ? 'Returning…' : 'Return to Vendor'}
          confirmColor="warning"
          busy={busy}
          onCancel={() => setConfirmReturn(false)}
          onConfirm={() => void saveReview(false, true)}
        />
      </Stack>
    )
  }

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, p: 1.5 }}>
      <KpiCardGrid columns={5} dense>
        <KpiCard
          compact
          label="Total"
          value={String(kpis.total)}
          accentColor={brand.gold}
          onClick={() => setStatusFilter('')}
        />
        <KpiCard
          compact
          label="Draft"
          value={String(kpis.draft)}
          accentColor={brand.muted}
          onClick={() => setStatusFilter(statusFilter === 'DRAFT' ? '' : 'DRAFT')}
        />
        <KpiCard
          compact
          label="In progress"
          value={String(kpis.active)}
          accentColor={semantic.risk.high}
          onClick={() => setStatusFilter(statusFilter === 'ACTIVE' ? '' : 'ACTIVE')}
        />
        <KpiCard
          compact
          label="Submitted"
          value={String(kpis.submitted)}
          accentColor={brand.goldDark}
          onClick={() => setStatusFilter(statusFilter === 'SUBMITTED' ? '' : 'SUBMITTED')}
        />
        <KpiCard
          compact
          label="Complete"
          value={String(kpis.complete)}
          accentColor={semantic.risk.low}
          onClick={() => setStatusFilter(statusFilter === 'COMPLETE' ? '' : 'COMPLETE')}
        />
      </KpiCardGrid>
      <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        <RegisterToolbar
          search={search}
          searchPlaceholder="Search projects"
          onSearchChange={setSearch}
          onClearFilters={search || statusFilter ? () => { setSearch(''); setStatusFilter('') } : undefined}
          filters={[
            {
              id: 'status',
              label: 'Status',
              value: statusFilter,
              options: [
                { value: '', label: `All (${kpis.total})` },
                { value: 'DRAFT', label: `Draft (${kpis.draft})` },
                { value: 'ACTIVE', label: `In progress (${kpis.active})` },
                { value: 'SUBMITTED', label: `Submitted (${kpis.submitted})` },
                { value: 'COMPLETE', label: `Complete (${kpis.complete})` },
              ],
              onChange: (v) => setStatusFilter(v as StatusFilter),
              minWidth: 140,
            },
          ]}
        />
        <RegisterListFrame
          ready={listReady}
          count={paged.count}
          emptyMessage={
            projects.length === 0
              ? 'No projects yet. Create a project with a vendor and a questionnaire.'
              : 'No projects match these filters.'
          }
          emptyAction={projects.length === 0 && canAdmin ? (
            <Button size="small" variant="contained" onClick={() => setCreating(true)}>New Project</Button>
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
                  <TableCell sx={{ ...primaryTableCellSx, width: '22%' }}>Project</TableCell>
                  <TableCell sx={{ ...mediumTableCellSx, width: '18%' }}>Vendor</TableCell>
                  <TableCell sx={{ ...mediumTableCellSx, width: '18%' }}>Questionnaire</TableCell>
                  <TableCell sx={compactTableCellSxWidth('12%')}>Status</TableCell>
                  <TableCell sx={compactTableCellSxWidth('12%')}>Assigned</TableCell>
                  <TableCell sx={{ ...lastTableCellSx, ...compactTableCellSxWidth('18%'), whiteSpace: 'normal', overflow: 'visible' }}>
                    Vendor started
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((p) => (
                  <TableRow key={p.id} hover sx={{ cursor: 'pointer' }} onClick={() => { void refreshSelected(p.id) }}>
                    <TableCell sx={{ ...primaryTableCellSx, width: '22%', whiteSpace: 'normal' }}>
                      <Box sx={primaryRegisterNameSx}>{p.title || p.displayId}</Box>
                      <Typography sx={{ fontSize: '0.6875rem', color: brand.muted, mt: 0.15 }}>{p.displayId}</Typography>
                    </TableCell>
                    <TableCell sx={{ ...mediumTableCellSx, width: '18%', whiteSpace: 'normal' }}>{p.vendorName}</TableCell>
                    <TableCell sx={{ ...mediumTableCellSx, width: '18%', whiteSpace: 'normal' }}>{p.questionnaireName || '-'}</TableCell>
                    <TableCell sx={compactTableCellSxWidth('12%')}>
                      <StatusChip label={pretty(p.status)} tone={statusTone(p.status)} />
                    </TableCell>
                    <TableCell sx={compactTableCellSxWidth('12%')}>{dateOnly(p.sentAt)}</TableCell>
                    <TableCell sx={{ ...lastTableCellSx, ...compactTableCellSxWidth('18%') }}>
                      {p.startedAt ? dateOnly(p.startedAt) : 'Not started'}
                    </TableCell>
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

function vendorPortalUrl(tenantId: string, token: string) {
  return `${window.location.origin}/vendor-portal?tenant=${encodeURIComponent(tenantId)}&token=${encodeURIComponent(token)}`
}

function PortalInviteList({
  tenantId,
  invites,
}: {
  tenantId: string | null
  invites: { email: string; accessToken: string }[]
}) {
  if (!tenantId) return null
  return (
    <Stack spacing={1}>
      {invites.map((inv) => {
        const href = vendorPortalUrl(tenantId, inv.accessToken)
        return (
          <Box key={`${inv.email}-${inv.accessToken}`}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600 }}>{inv.email}</Typography>
            <Box
              component="a"
              href={href}
              target="_blank"
              rel="noreferrer"
              sx={{
                display: 'block',
                fontSize: '0.75rem',
                color: brand.goldDark,
                wordBreak: 'break-all',
                textDecoration: 'underline',
              }}
            >
              {href}
            </Box>
          </Box>
        )
      })}
    </Stack>
  )
}

function lastIssuedBanner(p: TprmAssessment, tenantId: string | null) {
  if (!tenantId) return null
  const invites =
    p.portalInvites && p.portalInvites.length > 0
      ? p.portalInvites.filter((inv) => inv.accessToken)
      : p.accessToken
        ? [{ email: 'Vendor', accessToken: p.accessToken }]
        : []
  if (invites.length === 0) return null
  return (
    <SettingsCard
      compact
      heading
      autoHeight
      title="Vendor portal (local)"
      subtitle="Email is not sending on this app. Open the link as the vendor. They enter the invite email, then a one-time code."
    >
      <PortalInviteList tenantId={tenantId} invites={invites} />
    </SettingsCard>
  )
}
