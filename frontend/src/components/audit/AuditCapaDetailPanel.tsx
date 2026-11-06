import { useEffect, useState, type ReactNode } from 'react'
import { Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, Typography } from '@mui/material'
import {
  api,
  EVIDENCE_CLASSIFICATIONS,
  type CapaRecord,
  type EvidenceClassification,
  type EvidenceSummary,
  type UserSummary,
} from '../../api'
import StatusChip from '../StatusChip'
import {
  AttentionBanner,
  BusyButton,
  CompactTextField,
  ConfirmActionDialog,
  DomainBadge,
  FormSelect,
  FormSectionShell,
  SettingsCard,
  SettingsCardGrid,
  complexFormContainerSx,
  controlActionSx,
  formFieldGridSx,
  sectionAnchorSx,
  twoColFieldSx,
  DATE_FIELD_WIDTH,
  NARRATIVE_FIELD_MAX_WIDTH,
} from '../ui'
import { brand } from '../../theme'
import { findingSeverityRiskLevel } from './auditKit'
import { capaRef, capaSourceLabel } from './capaRegisterUtils'
import { CapaReadOnlyField } from './capaFormKit'
import { CAPA_EXCEL } from './capaExcelFields'

const capaCard = {
  accentColor: brand.gold,
  headerBg: brand.tableHeader,
} as const

function CapaHeaderValue({ value }: { value: string }) {
  return (
    <Typography
      title={value}
      noWrap
      sx={{
        fontSize: '0.875rem',
        fontWeight: 700,
        color: brand.ink,
        lineHeight: 1.2,
        textAlign: 'right',
        maxWidth: '11rem',
      }}
    >
      {value}
    </Typography>
  )
}
import {
  CAPA_VERIFICATION_RESULTS,
  capaLineStatus,
  capaLineStatusTone,
  classificationLabel,
  isVerificationEffective,
  verificationResultLabel,
} from './capaJourney'
import { useRegisterUnsavedChanges } from '../../uiQuality/UnsavedChangesGuard'
import { formatTenantDate } from '../../uiQuality/tenantLocale'
import ContributorEvidencePanel from '../contributor/ContributorEvidencePanel'
import { EVIDENCE_ACCEPT, EVIDENCE_MAX_BYTES } from '../evidence/evidenceFileAccept'

type CapaWorkTab = 'details' | 'action' | 'verify'
type ActionFieldKey = 'response' | 'reviewRemarks' | 'verificationDate' | 'verificationResult'

function fmtDate(iso: string | null | undefined): string {
  if (!iso) return '-'
  return formatTenantDate(iso)
}


export default function AuditCapaDetailPanel({
  capa,
  users,
  canAdmin,
  currentUserId,
  onChrome,
  onAssign,
  onClose,
  onReject,
  onSaved,
  onError,
}: {
  capa: CapaRecord
  users: UserSummary[]
  canAdmin: boolean
  currentUserId?: number
  onChrome?: (actions: ReactNode | null) => void
  onAssign: () => void
  onClose: () => void | Promise<void>
  onReject: (reason: string) => void | Promise<void>
  onSaved?: (c: CapaRecord, notice?: string) => void
  onError?: (msg: string) => void
}) {
  const ownerLabel = (id: number | null) =>
    id == null ? 'Unassigned' : users.find((u) => u.id === id)?.fullName ?? `#${id}`
  const editable = capa.status !== 'CLOSED'
  const delivered = capa.status === 'PENDING_VERIFICATION'
  const isAssignee = currentUserId != null && capa.ownerUserId === currentUserId
  const contributorCanEdit = isAssignee && !canAdmin && editable && !delivered
  const lineStatus = capaLineStatus(capa)

  const [reviewRemarks, setReviewRemarks] = useState(capa.reviewRemarks ?? '')
  const [verificationDate, setVerificationDate] = useState(capa.verificationDate ?? '')
  const [verificationResult, setVerificationResult] = useState(
    verificationResultLabel(capa.verificationResult) === '-'
      ? ''
      : verificationResultLabel(capa.verificationResult),
  )
  const [rejectOpen, setRejectOpen] = useState(false)
  const [rejectReason, setRejectReason] = useState('')
  const [confirmClose, setConfirmClose] = useState(false)
  const [responseDescription, setResponseDescription] = useState(capa.responseDescription ?? '')
  const [contributorEvidenceNote, setContributorEvidenceNote] = useState(
    capa.contributorEvidenceNote ?? '',
  )
  const [busy, setBusy] = useState(false)
  const [closing, setClosing] = useState(false)
  const [rejecting, setRejecting] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [linkedEvidence, setLinkedEvidence] = useState<EvidenceSummary[]>([])
  const [evidenceFile, setEvidenceFile] = useState<File | null>(null)
  const [evidenceClass, setEvidenceClass] = useState<EvidenceClassification | ''>('')
  const [removeEvidence, setRemoveEvidence] = useState<EvidenceSummary | null>(null)
  const [workTab, setWorkTab] = useState<CapaWorkTab>(() =>
    isAssignee && !canAdmin && capa.status !== 'CLOSED' && capa.status !== 'PENDING_VERIFICATION'
      ? 'action'
      : canAdmin && capa.status === 'PENDING_VERIFICATION'
        ? 'verify'
        : 'details',
  )
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<ActionFieldKey, string>>>({})
  const [editingPlan, setEditingPlan] = useState(false)
  const [editClause, setEditClause] = useState(capa.controlRef ?? '')
  const [editCorrection, setEditCorrection] = useState(capa.correction || capa.recommendedAction || '')
  const [editRootCause, setEditRootCause] = useState(capa.rootCause ?? '')
  const [editPlan, setEditPlan] = useState(capa.correctiveAction ?? '')
  const [editEvidenceRequired, setEditEvidenceRequired] = useState(capa.evidenceRequired ?? '')
  const [editTargetDate, setEditTargetDate] = useState(capa.targetDate ?? '')
  const [editOwnerUserId, setEditOwnerUserId] = useState<number | ''>(capa.ownerUserId ?? '')
  const [editDepartment, setEditDepartment] = useState(capa.contributorDepartment ?? '')
  const [planFieldErrors, setPlanFieldErrors] = useState<Partial<Record<string, string>>>({})

  const contributors = users.filter((u) => u.roles.includes('CONTRIBUTOR'))
  const reopened = Boolean(capa.rejectionReason) && !delivered && capa.status !== 'CLOSED'
  const adminCanEditPlan = canAdmin && editable && !delivered && !reopened

  const resetPlanDraft = () => {
    setEditClause(capa.controlRef ?? '')
    setEditCorrection(capa.correction || capa.recommendedAction || '')
    setEditRootCause(capa.rootCause ?? '')
    setEditPlan(capa.correctiveAction ?? '')
    setEditEvidenceRequired(capa.evidenceRequired ?? '')
    setEditTargetDate(capa.targetDate ?? '')
    setEditOwnerUserId(capa.ownerUserId ?? '')
    setEditDepartment(capa.contributorDepartment ?? '')
    setPlanFieldErrors({})
  }

  useEffect(() => {
    setReviewRemarks(capa.reviewRemarks ?? '')
    setVerificationDate(capa.verificationDate ?? '')
    setVerificationResult(
      verificationResultLabel(capa.verificationResult) === '-'
        ? ''
        : verificationResultLabel(capa.verificationResult),
    )
    setResponseDescription(capa.responseDescription ?? '')
    setContributorEvidenceNote(capa.contributorEvidenceNote ?? '')
    setEvidenceFile(null)
    setEditingPlan(false)
    setEditClause(capa.controlRef ?? '')
    setEditCorrection(capa.correction || capa.recommendedAction || '')
    setEditRootCause(capa.rootCause ?? '')
    setEditPlan(capa.correctiveAction ?? '')
    setEditEvidenceRequired(capa.evidenceRequired ?? '')
    setEditTargetDate(capa.targetDate ?? '')
    setEditOwnerUserId(capa.ownerUserId ?? '')
    setEditDepartment(capa.contributorDepartment ?? '')
    setPlanFieldErrors({})
  }, [capa.id, capa.updatedAt])

  useEffect(() => {
    setWorkTab(
      isAssignee && !canAdmin && capa.status !== 'CLOSED' && capa.status !== 'PENDING_VERIFICATION'
        ? 'action'
        : canAdmin && capa.status === 'PENDING_VERIFICATION'
          ? 'verify'
          : 'details',
    )
  }, [capa.id, canAdmin, isAssignee, capa.status])

  useEffect(() => {
    let cancelled = false
    void api
      .listEvidenceLinked('CAPA', 'CAPA', String(capa.id))
      .then((list) => {
        if (!cancelled) setLinkedEvidence(list)
      })
      .catch((e) => {
        if (!cancelled) onError?.(String(e))
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [capa.id])

  const verificationWhy = !reviewRemarks.trim()
    ? `Enter ${CAPA_EXCEL.reviewRemarks}`
    : !verificationDate
      ? `Enter ${CAPA_EXCEL.verificationDate}`
      : !verificationResult
        ? `Select ${CAPA_EXCEL.verificationResult}`
        : undefined

  const contributorWhy = !responseDescription.trim()
    ? `Enter ${CAPA_EXCEL.responseDescription}`
    : linkedEvidence.length === 0
      ? 'Upload at least one evidence file'
      : undefined

  const focusFirst = (errors: Partial<Record<ActionFieldKey, string>>) => {
    const first = (Object.keys(errors) as ActionFieldKey[])[0]
    if (first) document.getElementById(`capa-action-${first}`)?.focus()
  }

  const savePlan = async (): Promise<boolean> => {
    const errors: Partial<Record<string, string>> = {}
    if (!editCorrection.trim()) errors.correction = `Enter ${CAPA_EXCEL.correction}`
    if (!editRootCause.trim()) errors.rootCause = `Enter ${CAPA_EXCEL.rootCause}`
    if (!editPlan.trim()) errors.plan = `Enter ${CAPA_EXCEL.correctiveActionPlan}`
    if (!editTargetDate) errors.targetDate = `Enter ${CAPA_EXCEL.agreedTargetDate}`
    if (editOwnerUserId === '') errors.owner = `Select ${CAPA_EXCEL.assignedContributor}`
    if (!editDepartment.trim()) errors.department = `Enter ${CAPA_EXCEL.contributorDepartment}`
    setPlanFieldErrors(errors)
    if (Object.keys(errors).length) {
      setWorkTab('details')
      setEditingPlan(true)
      const first = Object.keys(errors)[0]
      document.getElementById(`capa-plan-${first}`)?.focus()
      onError?.(errors[first] ?? 'Complete the required fields')
      return false
    }
    setBusy(true)
    try {
      const next = await api.updateCapaTracker(capa.id, {
        clauseRef: editClause.trim(),
        correction: editCorrection.trim(),
        rootCause: editRootCause.trim(),
        correctiveActionPlan: editPlan.trim(),
        evidenceRequired: editEvidenceRequired.trim(),
        agreedTargetDate: editTargetDate || null,
        assignedContributorUserId: Number(editOwnerUserId),
        contributorDepartment: editDepartment.trim(),
      })
      setEditingPlan(false)
      onSaved?.(next)
      return true
    } catch (e) {
      onError?.(String(e))
      return false
    } finally {
      setBusy(false)
    }
  }

  const saveVerification = async (quiet = false): Promise<boolean> => {
    const errors: Partial<Record<ActionFieldKey, string>> = {}
    if (!reviewRemarks.trim()) errors.reviewRemarks = `Enter ${CAPA_EXCEL.reviewRemarks}`
    if (!verificationDate) errors.verificationDate = `Enter ${CAPA_EXCEL.verificationDate}`
    if (!verificationResult) errors.verificationResult = `Select ${CAPA_EXCEL.verificationResult}`
    setFieldErrors(errors)
    if (Object.keys(errors).length) {
      setWorkTab('action')
      focusFirst(errors)
      onError?.(errors[Object.keys(errors)[0] as ActionFieldKey] ?? 'Complete the required fields')
      return false
    }
    setBusy(true)
    try {
      const next = await api.updateCapaTracker(capa.id, {
        reviewRemarks,
        verificationDate: verificationDate || null,
        verificationResult,
      })
      if (!quiet) onSaved?.(next, 'Verification saved')
      return true
    } catch (e) {
      onError?.(String(e))
      return false
    } finally {
      setBusy(false)
    }
  }

  const saveContributorResponse = async (): Promise<boolean> => {
    setBusy(true)
    try {
      const next = await api.submitCapaResponse(capa.id, {
        responseDescription,
        contributorEvidenceNote,
      })
      onSaved?.(next)
      return true
    } catch (e) {
      onError?.(String(e))
      return false
    } finally {
      setBusy(false)
    }
  }

  const submitContributorResponse = async (): Promise<boolean> => {
    const errors: Partial<Record<ActionFieldKey, string>> = {}
    if (!responseDescription.trim()) errors.response = `Enter ${CAPA_EXCEL.responseDescription}`
    setFieldErrors(errors)
    if (errors.response || linkedEvidence.length === 0) {
      setWorkTab('action')
      if (errors.response) focusFirst(errors)
      onError?.(errors.response ?? 'Upload at least one evidence file')
      return false
    }
    setBusy(true)
    try {
      await api.submitCapaResponse(capa.id, {
        responseDescription,
        contributorEvidenceNote,
      })
      const next = await api.submitCapa(capa.id)
      onSaved?.(next, 'CAPA submitted for verification')
      return true
    } catch (e) {
      onError?.(String(e))
      return false
    } finally {
      setBusy(false)
    }
  }

  const uploadEvidence = async () => {
    if (!evidenceFile) return
    if (!evidenceClass) {
      onError?.('Select a classification')
      return
    }
    if (evidenceFile.size > EVIDENCE_MAX_BYTES) {
      onError?.('Evidence file must be 25MB or less')
      return
    }
    setUploading(true)
    try {
      await api.uploadEvidence({
        file: evidenceFile,
        classification: evidenceClass,
        title: evidenceFile.name,
        moduleCode: 'CAPA',
        recordType: 'CAPA',
        recordId: String(capa.id),
        workflowStage: 'IN_PROGRESS',
      })
      setEvidenceFile(null)
      setEvidenceClass('')
      setLinkedEvidence(await api.listEvidenceLinked('CAPA', 'CAPA', String(capa.id)))
    } catch (e) {
      onError?.(String(e))
    } finally {
      setUploading(false)
    }
  }

  const unlinkEvidence = async (ev: EvidenceSummary) => {
    setBusy(true)
    try {
      await api.unlinkEvidenceFromRecord(ev.id, {
        moduleCode: 'CAPA',
        recordType: 'CAPA',
        recordId: String(capa.id),
      })
      setLinkedEvidence(await api.listEvidenceLinked('CAPA', 'CAPA', String(capa.id)))
    } catch (e) {
      onError?.(String(e))
    } finally {
      setBusy(false)
      setRemoveEvidence(null)
    }
  }

  const storedVerification =
    verificationResultLabel(capa.verificationResult) === '-'
      ? ''
      : verificationResultLabel(capa.verificationResult)
  const verificationDirty =
    reviewRemarks !== (capa.reviewRemarks ?? '') ||
    verificationDate !== (capa.verificationDate ?? '') ||
    verificationResult !== storedVerification
  const planDirty =
    editingPlan &&
    (editClause !== (capa.controlRef ?? '') ||
      editCorrection !== (capa.correction || capa.recommendedAction || '') ||
      editRootCause !== (capa.rootCause ?? '') ||
      editPlan !== (capa.correctiveAction ?? '') ||
      editEvidenceRequired !== (capa.evidenceRequired ?? '') ||
      editTargetDate !== (capa.targetDate ?? '') ||
      editOwnerUserId !== (capa.ownerUserId ?? '') ||
      editDepartment !== (capa.contributorDepartment ?? ''))
  const capaDirty =
    editable &&
    (verificationDirty ||
      responseDescription !== (capa.responseDescription ?? '') ||
      contributorEvidenceNote !== (capa.contributorEvidenceNote ?? '') ||
      evidenceFile != null)

  useRegisterUnsavedChanges(
    planDirty || capaDirty
      ? {
          dirty: true,
          title: 'Leave without saving this CAPA?',
          message: 'You have unsaved CAPA edits. Save to keep them, or discard to leave.',
          saveDraftLabel: 'Save',
          onSaveDraft: async () => {
            const ok = planDirty
              ? await savePlan()
              : canAdmin
                ? await saveVerification()
                : await saveContributorResponse()
            if (!ok) throw new Error('not saved')
          },
        }
      : null,
  )

  const verificationEffective = isVerificationEffective(verificationResult)
  const saveVerificationWhy = verificationWhy ?? (!verificationDirty ? 'No changes to save' : undefined)
  const closeVerificationWhy =
    verificationWhy ??
    (!verificationEffective ? 'Verification Result must be Effective to close' : undefined)
  const rejectVerificationWhy = verificationEffective
    ? 'Use Close CAPA when verification is Effective'
    : undefined
  const canSaveVerification = canAdmin && delivered && !saveVerificationWhy
  const canClose = canAdmin && delivered && !closeVerificationWhy
  const canReject = canAdmin && delivered && !rejectVerificationWhy

  const showAssign = canAdmin && editable && capa.ownerUserId == null
  const showEditPlan = adminCanEditPlan && !editingPlan
  const showSavePlan = adminCanEditPlan && editingPlan
  const showContributorSubmit = contributorCanEdit
  const showAdminVerify = canAdmin && delivered && editable
  const hasChrome = showAssign || showEditPlan || showSavePlan || showContributorSubmit || showAdminVerify

  useEffect(() => {
    if (!hasChrome) {
      onChrome?.(null)
      return () => onChrome?.(null)
    }
    onChrome?.(
      <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
        {showAssign && (
          <Button size="small" variant="outlined" sx={controlActionSx} onClick={onAssign} disabled={busy}>
            Assign Contributor
          </Button>
        )}
        {showEditPlan && (
          <Button
            size="small"
            variant="outlined"
            sx={controlActionSx}
            disabled={busy}
            onClick={() => {
              resetPlanDraft()
              setEditingPlan(true)
              setWorkTab('details')
            }}
          >
            Edit
          </Button>
        )}
        {showSavePlan && (
          <>
            <Button
              size="small"
              variant="outlined"
              sx={controlActionSx}
              disabled={busy}
              onClick={() => {
                resetPlanDraft()
                setEditingPlan(false)
              }}
            >
              Cancel
            </Button>
            <BusyButton
              size="small"
              variant="contained"
              sx={controlActionSx}
              busy={busy}
              busyLabel="Saving..."
              onClick={() => void savePlan()}
            >
              Save
            </BusyButton>
          </>
        )}
        {showContributorSubmit && (
          <BusyButton
            size="small"
            variant="contained"
            sx={controlActionSx}
            busy={busy}
            busyLabel="Submitting..."
            disabled={Boolean(contributorWhy) || uploading}
            disabledReason={uploading ? 'Wait for the file upload to finish' : contributorWhy}
            onClick={() => void submitContributorResponse()}
          >
            Submit Response
          </BusyButton>
        )}
        {showAdminVerify && (
          <>
            <BusyButton
              size="small"
              variant="outlined"
              sx={controlActionSx}
              busy={busy}
              busyLabel="Saving..."
              disabled={!canSaveVerification}
              disabledReason={saveVerificationWhy}
              onClick={() => void saveVerification()}
            >
              Save Verification
            </BusyButton>
            <BusyButton
              size="small"
              variant="contained"
              sx={controlActionSx}
              busy={busy}
              disabled={!canClose}
              disabledReason={closeVerificationWhy}
              onClick={() => setConfirmClose(true)}
            >
              Close CAPA
            </BusyButton>
            <BusyButton
              size="small"
              color="error"
              variant="outlined"
              sx={controlActionSx}
              busy={busy}
              disabled={!canReject}
              disabledReason={rejectVerificationWhy}
              onClick={() => {
                setRejectReason('')
                setRejectOpen(true)
              }}
            >
              Reject
            </BusyButton>
          </>
        )}
      </Stack>,
    )
    return () => onChrome?.(null)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    hasChrome,
    showAssign,
    showEditPlan,
    showSavePlan,
    showContributorSubmit,
    showAdminVerify,
    busy,
    contributorWhy,
    saveVerificationWhy,
    closeVerificationWhy,
    rejectVerificationWhy,
    canSaveVerification,
    canClose,
    canReject,
    verificationResult,
    reviewRemarks,
    verificationDate,
    verificationDirty,
    editingPlan,
    editClause,
    editCorrection,
    editRootCause,
    editPlan,
    editEvidenceRequired,
    editTargetDate,
    editOwnerUserId,
    editDepartment,
  ])

  return (
    <Stack spacing={1} sx={{ flex: 1, minHeight: 0, height: '100%', overflow: 'hidden', ...complexFormContainerSx }}>
      {capa.rejectionReason ? (
        <AttentionBanner tone="error" emphasis="Returned for more work" message={capa.rejectionReason} />
      ) : null}

      <FormSectionShell
        fill
        mode="panels"
        activeSectionId={
          workTab === 'action' ? 'capa-action' : workTab === 'verify' ? 'capa-verify' : 'capa-details'
        }
        onActiveSectionChange={(id) =>
          setWorkTab(id === 'capa-action' ? 'action' : id === 'capa-verify' ? 'verify' : 'details')
        }
        sections={[
          { id: 'capa-details', label: 'Details' },
          ...(contributorCanEdit ? [{ id: 'capa-action', label: 'Action' }] : []),
          ...(canAdmin && (delivered || capa.status === 'CLOSED')
            ? [{ id: 'capa-verify', label: 'Verification' }]
            : []),
        ]}
      >
        {workTab === 'details' && (
          <Box id="capa-details" sx={sectionAnchorSx}>
            <SettingsCardGrid columns={3}>
              <SettingsCard
                compact
                autoHeight
                title="Identity"
                action={<CapaHeaderValue value={capaRef(capa.id)} />}
                {...capaCard}
              >
                <Box sx={formFieldGridSx}>
                  <CapaReadOnlyField label={CAPA_EXCEL.findingId} value={capa.sourceFindingDisplayCode ?? '-'} emphasis />
                  <CapaReadOnlyField label="Source" value={capaSourceLabel(capa)} />
                  <CapaReadOnlyField label={CAPA_EXCEL.classification}>
                    {capa.severity ? (
                      <DomainBadge
                        variant="risk"
                        level={findingSeverityRiskLevel(capa.severity)}
                        label={classificationLabel(capa.severity)}
                      />
                    ) : (
                      <Typography variant="body2" color="text.secondary">
                        -
                      </Typography>
                    )}
                  </CapaReadOnlyField>
                  <CapaReadOnlyField label={CAPA_EXCEL.clauseRef} value={capa.controlRef || '-'} />
                  <CapaReadOnlyField label={CAPA_EXCEL.findingTitle} value={capa.title || '-'} />
                </Box>
              </SettingsCard>
              <SettingsCard
                compact
                autoHeight
                title="Assignment"
                action={<CapaHeaderValue value={ownerLabel(capa.ownerUserId)} />}
                {...capaCard}
              >
                <Box sx={formFieldGridSx}>
                  <CapaReadOnlyField label={CAPA_EXCEL.dateRaised} value={fmtDate(capa.createdAt)} />
                  <CapaReadOnlyField label={CAPA_EXCEL.dateAssigned} value={fmtDate(capa.assignedAt)} />
                  <CapaReadOnlyField label={CAPA_EXCEL.agreedTargetDate} value={fmtDate(capa.targetDate)} />
                  <CapaReadOnlyField
                    label={CAPA_EXCEL.contributorDepartment}
                    value={capa.contributorDepartment || '-'}
                  />
                </Box>
              </SettingsCard>
              <SettingsCard
                compact
                autoHeight
                title="Status"
                action={<StatusChip label={lineStatus} tone={capaLineStatusTone(lineStatus)} />}
                {...capaCard}
              >
                <Stack spacing={1} direction="row" flexWrap="wrap" useFlexGap alignItems="center">
                  {capa.severity ? (
                    <DomainBadge
                      variant="risk"
                      level={findingSeverityRiskLevel(capa.severity)}
                      label={classificationLabel(capa.severity)}
                    />
                  ) : null}
                </Stack>
                <Box sx={{ ...formFieldGridSx, mt: 1 }}>
                  <CapaReadOnlyField
                    label={CAPA_EXCEL.verificationResult}
                    value={verificationResultLabel(capa.verificationResult)}
                  />
                </Box>
              </SettingsCard>
            </SettingsCardGrid>

            <Box sx={{ mt: 1.5 }}>
              <SettingsCard
                autoHeight
                {...capaCard}
                title="CAPA plan"
                subtitle={
                  editingPlan
                    ? 'Edit tracker fields. Save is in the page header.'
                    : adminCanEditPlan
                      ? 'Use Edit in the page header to change the plan before the contributor submits.'
                      : delivered
                        ? 'Plan is locked after the contributor submitted a response.'
                        : reopened
                          ? 'Plan is locked while the contributor updates their response.'
                          : undefined
                }
              >
              {editingPlan ? (
                <Stack spacing={1.5}>
                  <Box sx={formFieldGridSx}>
                    <CompactTextField
                      label={CAPA_EXCEL.clauseRef}
                      wrapLabel
                      value={editClause}
                      onChange={(e) => setEditClause(e.target.value)}
                      maxLength={80}
                    />
                    <Box sx={{ width: DATE_FIELD_WIDTH }}>
                      <CompactTextField
                        id="capa-plan-targetDate"
                        label={CAPA_EXCEL.agreedTargetDate}
                        wrapLabel
                        required
                        type="date"
                        value={editTargetDate}
                        onChange={(e) => {
                          setEditTargetDate(e.target.value)
                          setPlanFieldErrors((err) => ({ ...err, targetDate: undefined }))
                        }}
                        error={Boolean(planFieldErrors.targetDate)}
                        helperText={planFieldErrors.targetDate}
                      />
                    </Box>
                    <FormSelect
                      label={CAPA_EXCEL.assignedContributor}
                      wrapLabel
                      required
                      value={editOwnerUserId === '' ? '' : String(editOwnerUserId)}
                      options={[
                        { value: '', label: 'Select' },
                        ...contributors.map((u) => ({ value: String(u.id), label: u.fullName })),
                      ]}
                      onChange={(v) => {
                        setEditOwnerUserId(v ? Number(v) : '')
                        setPlanFieldErrors((err) => ({ ...err, owner: undefined }))
                      }}
                      fullWidth
                      error={Boolean(planFieldErrors.owner)}
                      helperText={planFieldErrors.owner}
                    />
                    <CompactTextField
                      id="capa-plan-department"
                      label={CAPA_EXCEL.contributorDepartment}
                      wrapLabel
                      required
                      value={editDepartment}
                      onChange={(e) => {
                        setEditDepartment(e.target.value)
                        setPlanFieldErrors((err) => ({ ...err, department: undefined }))
                      }}
                      maxLength={120}
                      error={Boolean(planFieldErrors.department)}
                      helperText={planFieldErrors.department}
                    />
                  </Box>
                  <Box sx={twoColFieldSx}>
                    <CompactTextField
                      id="capa-plan-correction"
                      label={CAPA_EXCEL.correction}
                      wrapLabel
                      required
                      multiline
                      minRows={3}
                      fullWidth
                      value={editCorrection}
                      onChange={(e) => {
                        setEditCorrection(e.target.value)
                        setPlanFieldErrors((err) => ({ ...err, correction: undefined }))
                      }}
                      maxLength={4000}
                      error={Boolean(planFieldErrors.correction)}
                      helperText={planFieldErrors.correction}
                    />
                    <CompactTextField
                      id="capa-plan-rootCause"
                      label={CAPA_EXCEL.rootCause}
                      wrapLabel
                      required
                      multiline
                      minRows={3}
                      fullWidth
                      value={editRootCause}
                      onChange={(e) => {
                        setEditRootCause(e.target.value)
                        setPlanFieldErrors((err) => ({ ...err, rootCause: undefined }))
                      }}
                      maxLength={4000}
                      error={Boolean(planFieldErrors.rootCause)}
                      helperText={planFieldErrors.rootCause}
                    />
                    <CompactTextField
                      id="capa-plan-plan"
                      label={CAPA_EXCEL.correctiveActionPlan}
                      wrapLabel
                      required
                      multiline
                      minRows={3}
                      fullWidth
                      value={editPlan}
                      onChange={(e) => {
                        setEditPlan(e.target.value)
                        setPlanFieldErrors((err) => ({ ...err, plan: undefined }))
                      }}
                      maxLength={4000}
                      error={Boolean(planFieldErrors.plan)}
                      helperText={planFieldErrors.plan}
                    />
                    <CompactTextField
                      label={CAPA_EXCEL.evidenceRequired}
                      wrapLabel
                      multiline
                      minRows={3}
                      fullWidth
                      value={editEvidenceRequired}
                      onChange={(e) => setEditEvidenceRequired(e.target.value)}
                      maxLength={4000}
                    />
                  </Box>
                </Stack>
              ) : (
                <Box sx={twoColFieldSx}>
                  <CapaReadOnlyField
                    label={CAPA_EXCEL.correction}
                    value={capa.correction || capa.recommendedAction || '-'}
                    well
                  />
                  <CapaReadOnlyField label={CAPA_EXCEL.rootCause} value={capa.rootCause || '-'} well />
                  <CapaReadOnlyField
                    label={CAPA_EXCEL.correctiveActionPlan}
                    value={capa.correctiveAction || '-'}
                    well
                  />
                  <CapaReadOnlyField
                    label={CAPA_EXCEL.evidenceRequired}
                    value={capa.evidenceRequired || '-'}
                    well
                  />
                </Box>
              )}
              </SettingsCard>
            </Box>
            {canAdmin && !delivered && capa.status !== 'CLOSED' ? (
              <Box sx={{ mt: 1.5 }}>
                <AttentionBanner
                  tone="info"
                  emphasis="Verification not open"
                  message="Verification opens after the contributor submits a response."
                />
              </Box>
            ) : null}
            {!contributorCanEdit &&
            (delivered ||
              capa.status === 'CLOSED' ||
              Boolean(capa.responseDescription?.trim()) ||
              Boolean(capa.contributorEvidenceNote?.trim()) ||
              linkedEvidence.length > 0) ? (
              <Box sx={{ mt: 1.5 }}>
                <SettingsCard
                  compact
                  autoHeight
                  {...capaCard}
                  title="Contributor response"
                  subtitle="Submitted by the assigned contributor."
                >
                  <Box sx={twoColFieldSx}>
                    <CapaReadOnlyField
                      label={CAPA_EXCEL.responseDescription}
                      value={capa.responseDescription || '-'}
                      well
                    />
                    <Box sx={{ minWidth: 0, width: '100%' }}>
                      <ContributorEvidencePanel
                        heading={CAPA_EXCEL.uploadEvidence}
                        linkedEvidence={linkedEvidence}
                        evidenceFile={null}
                        evidenceClass=""
                        evidenceClasses={EVIDENCE_CLASSIFICATIONS}
                        evidenceAccept={EVIDENCE_ACCEPT}
                        busy={false}
                        disabled
                        onSelectFile={() => undefined}
                        onEvidenceClass={() => undefined}
                        onUpload={() => undefined}
                        onDownload={(id) => void api.downloadEvidence(id)}
                      />
                    </Box>
                  </Box>
                </SettingsCard>
              </Box>
            ) : null}
          </Box>
        )}

        {workTab === 'action' && contributorCanEdit && (
          <Box id="capa-action" sx={sectionAnchorSx}>
            <SettingsCard
              compact
              autoHeight
              {...capaCard}
              title="Your response"
              subtitle="Submit from the page header when both are ready."
            >
              <Box sx={twoColFieldSx}>
                <CompactTextField
                  id="capa-action-response"
                  label={CAPA_EXCEL.responseDescription}
                  wrapLabel
                  required
                  value={responseDescription}
                  onChange={(e) => {
                    setResponseDescription(e.target.value)
                    setFieldErrors((err) => ({ ...err, response: undefined }))
                  }}
                  multiline
                  minRows={5}
                  fullWidth
                  error={Boolean(fieldErrors.response)}
                  helperText={fieldErrors.response}
                />
                <Box sx={{ minWidth: 0, width: '100%' }}>
                  <ContributorEvidencePanel
                    heading={CAPA_EXCEL.uploadEvidence}
                    linkedEvidence={linkedEvidence}
                    evidenceFile={evidenceFile}
                    evidenceClass={evidenceClass}
                    evidenceClasses={EVIDENCE_CLASSIFICATIONS}
                    evidenceAccept={EVIDENCE_ACCEPT}
                    busy={uploading}
                    disabled={busy}
                    onSelectFile={setEvidenceFile}
                    onEvidenceClass={setEvidenceClass}
                    onUpload={() => void uploadEvidence()}
                    onDownload={(id) => void api.downloadEvidence(id)}
                    onRemove={(ev) => setRemoveEvidence(ev)}
                  />
                </Box>
              </Box>
            </SettingsCard>
          </Box>
        )}

        {workTab === 'verify' && canAdmin && delivered && editable && (
          <Stack spacing={1.5}>
            <SettingsCard
              compact
              autoHeight
              {...capaCard}
              title="Contributor response"
              subtitle="Submitted by the assigned contributor."
            >
              <Box sx={twoColFieldSx}>
                <CapaReadOnlyField
                  label={CAPA_EXCEL.responseDescription}
                  value={capa.responseDescription || '-'}
                  well
                />
                <Box sx={{ minWidth: 0, width: '100%' }}>
                  <ContributorEvidencePanel
                    heading={CAPA_EXCEL.uploadEvidence}
                    linkedEvidence={linkedEvidence}
                    evidenceFile={null}
                    evidenceClass=""
                    evidenceClasses={EVIDENCE_CLASSIFICATIONS}
                    evidenceAccept={EVIDENCE_ACCEPT}
                    busy={false}
                    disabled
                    onSelectFile={() => undefined}
                    onEvidenceClass={() => undefined}
                    onUpload={() => undefined}
                    onDownload={(id) => void api.downloadEvidence(id)}
                  />
                </Box>
              </Box>
            </SettingsCard>
                <SettingsCard
                  autoHeight
                  {...capaCard}
                  title="Verification"
                  subtitle="Close requires an Effective result. Reject returns the line to the contributor."
                >
                  <Stack spacing={1.5} alignItems="stretch" sx={{ width: '100%' }}>
                      <CompactTextField
                        id="capa-action-reviewRemarks"
                        label={CAPA_EXCEL.reviewRemarks}
                        wrapLabel
                        required
                        value={reviewRemarks}
                        onChange={(e) => {
                          setReviewRemarks(e.target.value)
                          setFieldErrors((err) => ({ ...err, reviewRemarks: undefined }))
                        }}
                        multiline
                        minRows={3}
                        fullWidth
                        maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                        error={Boolean(fieldErrors.reviewRemarks)}
                        helperText={fieldErrors.reviewRemarks}
                      />
                    <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap alignItems="flex-start">
                      <Box sx={{ width: DATE_FIELD_WIDTH }}>
                        <CompactTextField
                          id="capa-action-verificationDate"
                          label={CAPA_EXCEL.verificationDate}
                          wrapLabel
                          required
                          type="date"
                          value={verificationDate}
                          onChange={(e) => {
                            setVerificationDate(e.target.value)
                            setFieldErrors((err) => ({ ...err, verificationDate: undefined }))
                          }}
                          error={Boolean(fieldErrors.verificationDate)}
                          helperText={fieldErrors.verificationDate}
                        />
                      </Box>
                      <FormSelect
                        label={CAPA_EXCEL.verificationResult}
                        wrapLabel
                        required
                        value={verificationResult}
                        options={[
                          { value: '', label: 'Select result' },
                          ...CAPA_VERIFICATION_RESULTS.map((o) => ({ value: o.value, label: o.label })),
                          ...(verificationResult === 'Not yet verified'
                            ? [{ value: 'Not yet verified', label: 'Not yet verified' }]
                            : []),
                        ]}
                        onChange={(v) => {
                          setVerificationResult(v)
                          setFieldErrors((err) => ({ ...err, verificationResult: undefined }))
                        }}
                        error={Boolean(fieldErrors.verificationResult)}
                        helperText={fieldErrors.verificationResult}
                        hint={
                          verificationResult === 'Not yet verified'
                            ? 'Choose Effective or Not effective.'
                            : undefined
                        }
                      />
                    </Stack>
                  </Stack>
                </SettingsCard>
          </Stack>
              )}

        {workTab === 'verify' && canAdmin && capa.status === 'CLOSED' && (
                <SettingsCard autoHeight title="Verification" {...capaCard}>
                  <Box sx={formFieldGridSx}>
                    <CapaReadOnlyField
                      label={CAPA_EXCEL.verificationResult}
                      value={verificationResultLabel(capa.verificationResult)}
                    />
                    <CapaReadOnlyField
                      label={CAPA_EXCEL.verificationDate}
                      value={fmtDate(capa.verificationDate)}
                    />
                  </Box>
                  <Box sx={{ width: '100%', mt: 1.25 }}>
                    <CapaReadOnlyField label={CAPA_EXCEL.reviewRemarks} value={capa.reviewRemarks || '-'} well />
                  </Box>
                </SettingsCard>
              )}
      </FormSectionShell>

      <ConfirmActionDialog
        open={confirmClose}
        title="Close this CAPA?"
        message="This CAPA will be closed as Effective. This cannot be undone from the tracker."
        confirmLabel="Close CAPA"
        confirmColor="error"
        busy={closing}
        busyLabel="Closing..."
        onCancel={() => {
          if (!closing) setConfirmClose(false)
        }}
        onConfirm={() => {
          void (async () => {
            setClosing(true)
            try {
              const ok = verificationDirty ? await saveVerification(true) : true
              if (!ok) return
              await onClose()
              setConfirmClose(false)
            } catch (e) {
              onError?.(String(e))
            } finally {
              setClosing(false)
            }
          })()
        }}
      />
      <ConfirmActionDialog
        open={removeEvidence != null}
        title="Remove evidence?"
        message="Unlink this file from the CAPA. The file stays in the Evidence Library."
        confirmLabel="Remove"
        busy={busy}
        onCancel={() => setRemoveEvidence(null)}
        onConfirm={() => {
          if (removeEvidence) void unlinkEvidence(removeEvidence)
        }}
      />
      <Dialog open={rejectOpen} onClose={() => { if (!rejecting) setRejectOpen(false) }} fullWidth maxWidth="xs">
        <DialogTitle>Reject CAPA</DialogTitle>
        <DialogContent>
          <CompactTextField
            label="Reject reason"
            required
            fieldIdPrefix="capa-reject"
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            multiline
            minRows={2}
            maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
          />
        </DialogContent>
        <DialogActions>
          <Button size="small" sx={controlActionSx} onClick={() => setRejectOpen(false)} disabled={rejecting}>
            Cancel
          </Button>
          <BusyButton
            size="small"
            color="error"
            variant="contained"
            sx={controlActionSx}
            busy={rejecting}
            busyLabel="Rejecting..."
            disabled={!rejectReason.trim()}
            onClick={() => {
              const reason = rejectReason.trim()
              if (!reason || rejecting) return
              setRejecting(true)
              void Promise.resolve(onReject(reason))
                .then(() => setRejectOpen(false))
                .catch((e) => onError?.(String(e)))
                .finally(() => setRejecting(false))
            }}
          >
            Reject
          </BusyButton>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
