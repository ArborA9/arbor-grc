import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { Box, Button, Stack } from '@mui/material'
import {
  api,
  ApiError,
  type CapaAvailableFinding,
  type CapaRecord,
  type CapaRegisterRecord,
  type UserSummary,
} from '../../api'
import {
  BusyButton,
  CompactTextField,
  FormSelect,
  FormSectionShell,
  SettingsCard,
  complexFormContainerSx,
  controlActionSx,
  formFieldGridSx,
  sectionAnchorSx,
  twoColFieldSx,
  DATE_FIELD_WIDTH,
} from '../ui'
import { useRegisterUnsavedChanges } from '../../uiQuality/UnsavedChangesGuard'
import { formatTenantDate } from '../../uiQuality/tenantLocale'
import { todayIso } from '../../validation/dates'
import { CAPA_EXCEL } from './capaExcelFields'
import { classificationLabel } from './capaJourney'
import { CapaReadOnlyField } from './capaFormKit'

type FieldKey =
  | 'title'
  | 'correction'
  | 'rootCause'
  | 'plan'
  | 'targetDate'
  | 'ownerUserId'
  | 'department'
  | 'externalFinding'
  | 'source'
  | 'sourceOther'

type ManualSource = '' | 'INTERNAL_AUDIT' | 'EXTERNAL_AUDIT' | 'OTHER'

/** Match FormFieldLabel so a read-only value sits on the same line as a select. */
const readOnlyAlignSx = {
  '& > label': { minHeight: 16, mb: 0.4 },
}

function departmentFromContributor(
  user: UserSummary | undefined,
  orgUnits: { id: number; name: string }[],
): string {
  if (!user?.orgUnitIds?.length) return ''
  const names = user.orgUnitIds
    .map((id) => orgUnits.find((u) => u.id === id)?.name?.trim())
    .filter((n): n is string => Boolean(n))
  return names.join(', ')
}

export default function AuditCapaCreateView({
  register,
  findings,
  users,
  orgUnits = [],
  onChrome,
  onCancel,
  onSaved,
  onError,
}: {
  register: CapaRegisterRecord
  findings: CapaAvailableFinding[]
  users: UserSummary[]
  orgUnits?: { id: number; name: string }[]
  onChrome?: (actions: ReactNode | null) => void
  onCancel: () => void
  onSaved: (c: CapaRecord) => void
  onError: (msg: string) => void
}) {
  const [findingId, setFindingId] = useState<number | ''>('')
  const [findingOther, setFindingOther] = useState(false)
  const [externalFindingCode, setExternalFindingCode] = useState('')
  const [findingSource, setFindingSource] = useState<ManualSource>('')
  const [sourceOther, setSourceOther] = useState('')
  const [title, setTitle] = useState('')
  const [classification, setClassification] = useState('')
  const [controlRef, setControlRef] = useState('')
  const [correction, setCorrection] = useState('')
  const [rootCause, setRootCause] = useState('')
  const [plan, setPlan] = useState('')
  const [evidenceRequired, setEvidenceRequired] = useState('')
  const [department, setDepartment] = useState('')
  const [ownerUserId, setOwnerUserId] = useState<number | ''>('')
  const [targetDate, setTargetDate] = useState('')
  const [busy, setBusy] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<FieldKey, string>>>({})
  const raisedOn = formatTenantDate(todayIso())

  const contributors = users.filter((u) => u.roles.includes('CONTRIBUTOR'))
  const unusedFindings = findings
  const selectedFinding = unusedFindings.find((f) => f.id === findingId)

  const why = findingOther && !externalFindingCode.trim()
    ? `Enter ${CAPA_EXCEL.findingId}`
    : findingOther && !findingSource
      ? 'Select a source'
      : findingOther && findingSource === 'OTHER' && !sourceOther.trim()
        ? 'Enter a source'
        : !title.trim()
    ? `Enter ${CAPA_EXCEL.findingTitle}`
    : !correction.trim()
      ? `Enter ${CAPA_EXCEL.correction}`
      : !rootCause.trim()
        ? `Enter ${CAPA_EXCEL.rootCause}`
        : !plan.trim()
          ? `Enter ${CAPA_EXCEL.correctiveActionPlan}`
          : !targetDate
            ? `Enter ${CAPA_EXCEL.agreedTargetDate}`
            : ownerUserId === ''
              ? `Select ${CAPA_EXCEL.assignedContributor}`
              : !department.trim()
                ? `Enter ${CAPA_EXCEL.contributorDepartment}`
                : undefined

  const dirty = useMemo(
    () =>
      findingId !== '' ||
      findingOther ||
      externalFindingCode.trim() !== '' ||
      findingSource !== '' ||
      sourceOther.trim() !== '' ||
      title.trim() !== '' ||
      classification.trim() !== '' ||
      controlRef.trim() !== '' ||
      correction.trim() !== '' ||
      rootCause.trim() !== '' ||
      plan.trim() !== '' ||
      evidenceRequired.trim() !== '' ||
      department.trim() !== '' ||
      ownerUserId !== '' ||
      targetDate !== '',
    [
      findingId,
      findingOther,
      externalFindingCode,
      findingSource,
      sourceOther,
      title,
      classification,
      controlRef,
      correction,
      rootCause,
      plan,
      evidenceRequired,
      department,
      ownerUserId,
      targetDate,
    ],
  )

  useRegisterUnsavedChanges(
    dirty && !busy ? { dirty: true, title: 'Leave Add CAPA?', message: 'You have unsaved changes.' } : null,
  )

  const onFindingChange = (v: string) => {
    if (v === 'OTHER') {
      setFindingOther(true)
      setFindingId('')
      setTitle('')
      setClassification('')
      setControlRef('')
      setFieldErrors((err) => ({ ...err, title: undefined, externalFinding: undefined }))
      window.setTimeout(() => document.getElementById('capa-field-externalFinding')?.focus(), 0)
      return
    }
    setFindingOther(false)
    setExternalFindingCode('')
    setFindingSource('')
    setSourceOther('')
    const id = v ? Number(v) : ''
    setFindingId(id)
    const f = unusedFindings.find((row) => row.id === id)
    setTitle(f?.title ?? '')
    setClassification(f?.severity ?? '')
    setControlRef(f?.controlRef ?? '')
    setFieldErrors((err) => ({ ...err, title: undefined, externalFinding: undefined }))
  }

  const collectErrors = (): Partial<Record<FieldKey, string>> => {
    const next: Partial<Record<FieldKey, string>> = {}
    if (findingOther && !externalFindingCode.trim()) {
      next.externalFinding = `Enter ${CAPA_EXCEL.findingId}`
    }
    if (findingOther && !findingSource) {
      next.source = 'Select a source'
    }
    if (findingOther && findingSource === 'OTHER' && !sourceOther.trim()) {
      next.sourceOther = 'Enter a source'
    }
    if (!title.trim()) next.title = `Enter ${CAPA_EXCEL.findingTitle}`
    if (!correction.trim()) next.correction = `Enter ${CAPA_EXCEL.correction}`
    if (!rootCause.trim()) next.rootCause = `Enter ${CAPA_EXCEL.rootCause}`
    if (!plan.trim()) next.plan = `Enter ${CAPA_EXCEL.correctiveActionPlan}`
    if (!targetDate) next.targetDate = `Enter ${CAPA_EXCEL.agreedTargetDate}`
    if (ownerUserId === '') next.ownerUserId = `Select ${CAPA_EXCEL.assignedContributor}`
    if (!department.trim()) next.department = `Enter ${CAPA_EXCEL.contributorDepartment}`
    return next
  }

  const submit = async () => {
    const errors = collectErrors()
    setFieldErrors(errors)
    const first = (Object.keys(errors) as FieldKey[])[0]
    if (first) {
      const focusId = first === 'source' ? 'fs-source' : `capa-field-${first}`
      document.getElementById(focusId)?.focus()
      onError(errors[first] ?? 'Complete the required fields')
      return
    }
    setBusy(true)
    try {
      const created = await api.createCapa({
        registerCode: findingOther && findingSource ? findingSource : 'INTERNAL_AUDIT',
        title: title.trim(),
        priority: 'MEDIUM',
        capaRegisterId: register.id,
        findingId: findingOther || findingId === '' ? undefined : Number(findingId),
        externalFindingCode: findingOther ? externalFindingCode.trim() : undefined,
        sourceOther: findingOther && findingSource === 'OTHER' ? sourceOther.trim() : undefined,
        severity: selectedFinding ? undefined : classification || undefined,
        controlRef: controlRef.trim() || undefined,
        correction: correction.trim(),
        rootCause: rootCause.trim(),
        correctiveActionPlan: plan.trim(),
        evidenceRequired: evidenceRequired.trim() || undefined,
        contributorDepartment: department.trim(),
        assignedContributorUserId: Number(ownerUserId),
        targetDate,
      })
      onSaved(created)
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => {
    onChrome?.(
      <Stack direction="row" spacing={0.75}>
        <Button size="small" variant="outlined" sx={controlActionSx} onClick={onCancel} disabled={busy}>
          Cancel
        </Button>
        <BusyButton
          size="small"
          variant="contained"
          sx={controlActionSx}
          busy={busy}
          busyLabel="Creating..."
          disabled={Boolean(why)}
          disabledReason={why}
          onClick={() => void submit()}
        >
          Create CAPA
        </BusyButton>
      </Stack>,
    )
    return () => onChrome?.(null)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [why, busy, dirty, title, correction, rootCause, plan, targetDate, ownerUserId, department, findingOther, externalFindingCode, findingSource, sourceOther])

  return (
    <Stack spacing={0} sx={{ flex: 1, minHeight: 0, overflow: 'hidden', p: 1.5, ...complexFormContainerSx }}>
      <FormSectionShell
        fill
        sections={[
          { id: 'capa-finding', label: 'Finding' },
          { id: 'capa-plan', label: 'Plan' },
        ]}
      >
        <Box id="capa-finding" sx={sectionAnchorSx}>
          <SettingsCard autoHeight title="Finding">
            <Box sx={formFieldGridSx}>
              <Box sx={{ gridColumn: { md: '1' }, gridRow: { md: '1' } }}>
                <FormSelect
                  label={CAPA_EXCEL.findingId}
                  wrapLabel
                  value={findingOther ? 'OTHER' : findingId === '' ? '' : String(findingId)}
                  options={[
                    { value: '', label: 'Select' },
                    ...unusedFindings.map((f) => ({
                      value: String(f.id),
                      label: `${f.displayCode} ${f.title}${f.auditName ? ` (${f.auditName})` : ''}`,
                    })),
                    { value: 'OTHER', label: 'Other' },
                  ]}
                  onChange={onFindingChange}
                  fullWidth
                  helperText={
                    unusedFindings.length
                      ? undefined
                      : 'No unused findings. Choose Other to type a finding ID.'
                  }
                />
              </Box>
              {findingOther ? (
                <Box sx={{ gridColumn: { md: '2' }, gridRow: { md: '1' } }}>
                  <CompactTextField
                    id="capa-field-externalFinding"
                    label={CAPA_EXCEL.findingId}
                    wrapLabel
                    required
                    value={externalFindingCode}
                    onChange={(e) => {
                      setExternalFindingCode(e.target.value)
                      setFieldErrors((err) => ({ ...err, externalFinding: undefined }))
                    }}
                    maxLength={80}
                    placeholder="EXT-2024-014"
                    error={Boolean(fieldErrors.externalFinding)}
                    helperText={
                      fieldErrors.externalFinding ?? 'From an audit that was not done in Arbor'
                    }
                  />
                </Box>
              ) : null}
              <Box
                sx={{
                  gridColumn: { md: findingOther ? '1 / -1' : '2 / span 2' },
                  gridRow: { md: findingOther ? '2' : '1' },
                }}
              >
                <CompactTextField
                  id="capa-field-title"
                  label={CAPA_EXCEL.findingTitle}
                  wrapLabel
                  required
                  value={title}
                  onChange={(e) => {
                    setTitle(e.target.value)
                    setFieldErrors((err) => ({ ...err, title: undefined }))
                  }}
                  maxLength={512}
                  error={Boolean(fieldErrors.title)}
                  helperText={fieldErrors.title}
                />
              </Box>
              {findingOther ? (
                <Box sx={{ gridColumn: { md: '1' }, gridRow: { md: '3' } }}>
                  <FormSelect
                    label="Source"
                    wrapLabel
                    required
                    fullWidth
                    value={findingSource}
                    options={[
                      { value: '', label: 'Select' },
                      { value: 'INTERNAL_AUDIT', label: 'Internal' },
                      { value: 'EXTERNAL_AUDIT', label: 'External' },
                      { value: 'OTHER', label: 'Other' },
                    ]}
                    onChange={(v) => {
                      const next = v as ManualSource
                      setFindingSource(next)
                      if (next !== 'OTHER') setSourceOther('')
                      setFieldErrors((err) => ({ ...err, source: undefined, sourceOther: undefined }))
                      if (next === 'OTHER') {
                        window.setTimeout(() => document.getElementById('capa-field-sourceOther')?.focus(), 0)
                      }
                    }}
                    error={Boolean(fieldErrors.source)}
                    helperText={fieldErrors.source}
                  />
                </Box>
              ) : null}
              {findingOther && findingSource === 'OTHER' ? (
                <Box sx={{ gridColumn: { md: '2' }, gridRow: { md: '3' } }}>
                  <CompactTextField
                    id="capa-field-sourceOther"
                    label="Other source"
                    wrapLabel
                    required
                    value={sourceOther}
                    onChange={(e) => {
                      setSourceOther(e.target.value)
                      setFieldErrors((err) => ({ ...err, sourceOther: undefined }))
                    }}
                    maxLength={120}
                    placeholder="Regulator review"
                    error={Boolean(fieldErrors.sourceOther)}
                    helperText={fieldErrors.sourceOther}
                  />
                </Box>
              ) : null}
              <Box
                sx={{
                  gridColumn: { md: findingOther ? (findingSource === 'OTHER' ? '3' : '2') : '1' },
                  gridRow: { md: findingOther ? '3' : '2' },
                  ...(selectedFinding ? readOnlyAlignSx : {}),
                }}
              >
                {selectedFinding ? (
                  <CapaReadOnlyField
                    label={CAPA_EXCEL.classification}
                    value={classificationLabel(selectedFinding.severity)}
                  />
                ) : (
                  <FormSelect
                    label={CAPA_EXCEL.classification}
                    wrapLabel
                    fullWidth
                    value={classification}
                    options={[
                      { value: '', label: 'Select' },
                      { value: 'MAJOR_NC', label: 'Major NC' },
                      { value: 'MINOR_NC', label: 'Minor NC' },
                      { value: 'OBSERVATION', label: 'Observation' },
                      { value: 'OFI', label: 'OFI' },
                    ]}
                    onChange={setClassification}
                  />
                )}
              </Box>
              <Box
                sx={{
                  gridColumn: { md: '1' },
                  gridRow: { md: findingOther ? '4' : '3' },
                  ...readOnlyAlignSx,
                }}
              >
                <CapaReadOnlyField label={CAPA_EXCEL.capaId} value="Assigned on save" />
              </Box>
              <Box
                sx={{
                  gridColumn: { md: '2' },
                  gridRow: { md: findingOther ? '4' : '3' },
                  ...readOnlyAlignSx,
                }}
              >
                <CapaReadOnlyField label={CAPA_EXCEL.dateRaised} value={raisedOn} />
              </Box>
              <Box sx={{ gridColumn: { md: findingOther ? '3' : '2' }, gridRow: { md: findingOther ? '4' : '2' } }}>
                <CompactTextField
                  label={CAPA_EXCEL.clauseRef}
                  wrapLabel
                  value={controlRef}
                  onChange={(e) => setControlRef(e.target.value)}
                  maxLength={80}
                />
              </Box>
            </Box>
          </SettingsCard>
        </Box>

        <Box id="capa-plan" sx={{ ...sectionAnchorSx, mt: 1.5 }}>
          <SettingsCard autoHeight title="CAPA plan">
            <Box sx={formFieldGridSx}>
              <CapaReadOnlyField label={CAPA_EXCEL.dateAssigned} value="Set on create" />
              <Box sx={{ width: DATE_FIELD_WIDTH }}>
                <CompactTextField
                  id="capa-field-targetDate"
                  label={CAPA_EXCEL.agreedTargetDate}
                  wrapLabel
                  required
                  type="date"
                  value={targetDate}
                  onChange={(e) => {
                    setTargetDate(e.target.value)
                    setFieldErrors((err) => ({ ...err, targetDate: undefined }))
                  }}
                  error={Boolean(fieldErrors.targetDate)}
                  helperText={fieldErrors.targetDate}
                />
              </Box>
              <FormSelect
                label={CAPA_EXCEL.assignedContributor}
                wrapLabel
                required
                value={ownerUserId === '' ? '' : String(ownerUserId)}
                options={[
                  { value: '', label: 'Select' },
                  ...contributors.map((u) => ({ value: String(u.id), label: u.fullName })),
                ]}
                onChange={(v) => {
                  const id = v ? Number(v) : ''
                  setOwnerUserId(id)
                  setFieldErrors((err) => ({ ...err, ownerUserId: undefined, department: undefined }))
                  const nextDept = departmentFromContributor(
                    contributors.find((u) => u.id === id),
                    orgUnits,
                  )
                  if (nextDept) setDepartment(nextDept)
                }}
                fullWidth
                error={Boolean(fieldErrors.ownerUserId)}
                helperText={fieldErrors.ownerUserId}
              />
              <CompactTextField
                id="capa-field-department"
                label={CAPA_EXCEL.contributorDepartment}
                wrapLabel
                required
                value={department}
                onChange={(e) => {
                  setDepartment(e.target.value)
                  setFieldErrors((err) => ({ ...err, department: undefined }))
                }}
                maxLength={120}
                error={Boolean(fieldErrors.department)}
                helperText={
                  fieldErrors.department ??
                  (department
                    ? 'Filled from the contributor organisation unit. You can edit.'
                    : 'Select a contributor to fill from their organisation unit')
                }
              />
            </Box>
            <Box sx={{ ...twoColFieldSx, mt: 1.5 }}>
              <CompactTextField
                id="capa-field-correction"
                label={CAPA_EXCEL.correction}
                wrapLabel
                required
                multiline
                minRows={3}
                fullWidth
                value={correction}
                onChange={(e) => {
                  setCorrection(e.target.value)
                  setFieldErrors((err) => ({ ...err, correction: undefined }))
                }}
                maxLength={4000}
                error={Boolean(fieldErrors.correction)}
                helperText={fieldErrors.correction}
              />
              <CompactTextField
                id="capa-field-rootCause"
                label={CAPA_EXCEL.rootCause}
                wrapLabel
                required
                multiline
                minRows={3}
                fullWidth
                value={rootCause}
                onChange={(e) => {
                  setRootCause(e.target.value)
                  setFieldErrors((err) => ({ ...err, rootCause: undefined }))
                }}
                maxLength={4000}
                error={Boolean(fieldErrors.rootCause)}
                helperText={fieldErrors.rootCause}
              />
              <CompactTextField
                id="capa-field-plan"
                label={CAPA_EXCEL.correctiveActionPlan}
                wrapLabel
                required
                multiline
                minRows={3}
                fullWidth
                value={plan}
                onChange={(e) => {
                  setPlan(e.target.value)
                  setFieldErrors((err) => ({ ...err, plan: undefined }))
                }}
                maxLength={4000}
                error={Boolean(fieldErrors.plan)}
                helperText={fieldErrors.plan}
              />
              <CompactTextField
                label={CAPA_EXCEL.evidenceRequired}
                wrapLabel
                multiline
                minRows={3}
                fullWidth
                value={evidenceRequired}
                onChange={(e) => setEvidenceRequired(e.target.value)}
                maxLength={4000}
              />
            </Box>
          </SettingsCard>
        </Box>
      </FormSectionShell>
    </Stack>
  )
}
