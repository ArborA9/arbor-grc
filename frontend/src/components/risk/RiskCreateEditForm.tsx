import { type ReactNode } from 'react'
import {
  Box,
  Checkbox,
  FormControlLabel,
  Stack,
  Typography,
} from '@mui/material'
import {
  type AssetSummary,
  type EvidenceClassification,
  type FrameworkDetail,
  type FrameworkSummary,
  type OrgUnitNode,
  type RiskCategory,
  type RiskLevel,
  type ResidualScoringMode,
  type TreatmentOption,
  type ControlAssessmentStatus,
  type EvidenceSummary,
  type UserSummary,
} from '../../api'
import {
  SettingsCard,
  RiskLevelBadge,
  CompactTextField,
  FormFieldLabel,
  AttentionBanner,
  sectionAnchorSx,
  complexFormContainerSx,
  INPUT_LIMIT,
  NARRATIVE_MAX_ROWS,
} from '../ui'
import { apiRiskLevelToBadge } from '../ui/riskBadgeMappers'
import { apiLevelTone, controlEffectivenessTone } from '../ui/ratingTone'
import FormSelect from '../ui/FormSelect'
import { fmtLevel } from '../../riskLevelLabels'
import { colRank, excelRatingToLevel, rowRank, type MatrixAxes } from './riskMatrix'
import {
  CE_EXCEL_LABEL,
  ERM_RISK_CAUSES,
  ERM_RISK_CAUSE_OTHER,
  EXCEL_FIELD,
  EXCEL_TREAT_LABEL,
  IS_ASSOCIATED_THREATS,
  LIKELIHOOD_EXCEL_LABEL,
  scaledExcelField,
  excelControlStatusLabel,
} from './excelRiskFields'
import {
  ERM_IMPACT_LABEL,
  ERM_LIKELIHOOD_LABEL,
  fmtErmRating,
} from './ermRiskLabels'
import ReferenceAttachmentField from './ReferenceAttachmentField'
import RiskRecordCreateProgress from './RiskRecordCreateProgress'
import { RISK_INPUT_ID } from './riskFormRequired'
import { formGrid12Sx, span, spanReadoutSx } from '../ui/formLayout'
import { brand } from '../../theme'

const TREAT: TreatmentOption[] = ['TREAT', 'TOLERATE', 'TERMINATE', 'TRANSFER']

/** Rating readout beside the inputs: label / value rows on a warm panel. */
function Readout({ rows, note }: { rows: { label: string; value: ReactNode }[]; note?: string }) {
  return (
    <Box
      sx={{
        bgcolor: brand.tableHeader,
        border: `1px solid ${brand.border}`,
        borderRadius: 2,
        px: 1.75,
        py: 1.5,
        display: 'flex',
        flexDirection: 'column',
        gap: 1,
        mt: { lg: 2.75 },
      }}
    >
      {rows.map((r) => (
        <Box key={r.label} sx={{ display: 'flex', alignItems: 'center', gap: 1.5, minHeight: 24 }}>
          <Typography sx={{ fontSize: '0.75rem', color: brand.muted, width: 132, flexShrink: 0 }}>{r.label}</Typography>
          <Box sx={{ minWidth: 0, fontSize: '0.875rem', fontWeight: 600 }}>{r.value}</Box>
        </Box>
      ))}
      {note ? <Typography sx={{ fontSize: '0.75rem', color: brand.muted, pt: 0.25 }}>{note}</Typography> : null}
    </Box>
  )
}

function evalDecisionChip(willAutoClose: boolean, decision: string): {
  level: 'critical' | 'medium' | 'low'
  label: string
} {
  const d = decision.toLowerCase()
  if (willAutoClose || d.includes('auto-closed') || d.includes('auto-close')) {
    return { level: 'low', label: 'Auto-closed' }
  }
  if (d.includes('treatment')) return { level: 'critical', label: 'Treatment required' }
  if (d.includes('monitor') || d.includes('within')) {
    return { level: 'low', label: 'Within appetite - monitor only' }
  }
  return { level: 'medium', label: decision }
}

export type RiskCreateEditFormProps = {
  isErm: boolean
  isPrivacy?: boolean
  editMode: boolean
  copyFromLabel?: string | null
  canAdmin: boolean
  cats: RiskCategory[]
  flatOrgs: OrgUnitNode[]
  users: UserSummary[]
  assets: AssetSummary[]
  frameworks: FrameworkSummary[]
  frameworkDetail: FrameworkDetail | null
  frameworkId: number | ''
  frameworkNodeId: number | ''
  controlAssessmentStatus: ControlAssessmentStatus | null
  previewUaeIa: string | null
  scoreLevels: RiskLevel[]
  matrixGridSize: 3 | 4 | 5
  previewInherent: RiskLevel
  previewResidual: RiskLevel
  previewInherentScore: number
  previewResidualScore: number | null
  previewInherentBand: string
  previewResidualBand: string
  previewInherentAction: string
  previewResidualAction: string
  previewPriority: string
  willAutoClose: boolean
  treatPlanDateError: string
  dateMin: string
  displayIdOverride: string
  setDisplayIdOverride: (v: string) => void
  title: string
  setTitle: (v: string) => void
  strategicObjective: string
  setStrategicObjective: (v: string) => void
  category: RiskCategory | ''
  setCategory: (v: RiskCategory | '') => void
  categoryOther: string
  setCategoryOther: (v: string) => void
  orgUnitId: number | ''
  setOrgUnitId: (v: number | '') => void
  riskOwnerUserId: number | ''
  setRiskOwnerUserId: (v: number | '') => void
  financialImpactAed: string
  setFinancialImpactAed: (v: string) => void
  assetId: number | ''
  setAssetId: (v: number | '') => void
  setFrameworkId: (v: number | '') => void
  setFrameworkNodeId: (v: number | '') => void
  description: string
  setDescription: (v: string) => void
  observedGap: string
  setObservedGap: (v: string) => void
  processingActivity?: string
  setProcessingActivity?: (v: string) => void
  threats: string
  setThreats: (v: string) => void
  probability: RiskLevel
  setProbability: (v: RiskLevel) => void
  impact: RiskLevel
  setImpact: (v: RiskLevel) => void
  controlEff: RiskLevel
  setControlEff: (v: RiskLevel) => void
  residualScoringMode: ResidualScoringMode
  setResidualScoringMode: (v: ResidualScoringMode) => void
  residualProbability: RiskLevel
  setResidualProbability: (v: RiskLevel) => void
  residualImpact: RiskLevel
  setResidualImpact: (v: RiskLevel) => void
  /** Active matrix axes - used to cap ERM residual L/I by Risk Analysis. */
  matrixAxes: MatrixAxes
  overrideAutoClose: boolean
  setOverrideAutoClose: (v: boolean) => void
  autoCloseJustification: string
  setAutoCloseJustification: (v: string) => void
  existingControls: string
  setExistingControls: (v: string) => void
  infosecStatusInitial: string
  setInfosecStatusInitial: (v: string) => void
  treatmentOption: TreatmentOption
  setTreatmentOption: (v: TreatmentOption) => void
  treatmentPlan: string
  setTreatmentPlan: (v: string) => void
  planActionOwnerId: number | ''
  setPlanActionOwnerId: (v: number | '') => void
  planCustodianUserId: number | ''
  setPlanCustodianUserId: (v: number | '') => void
  planStartDate: string
  setPlanStartDate: (v: string) => void
  planTargetDate: string
  setPlanTargetDate: (v: string) => void
  planEvidenceRequired: string
  setPlanEvidenceRequired: (v: string) => void
  adminNotes: string
  setAdminNotes: (v: string) => void
  createEvidenceFiles: File[]
  setCreateEvidenceFiles: (v: File[]) => void
  createEvidenceClass: EvidenceClassification | ''
  setCreateEvidenceClass: (v: EvidenceClassification | '') => void
  identificationEvidence: EvidenceSummary[]
  evidenceQuotaUsed?: number
  onUploadReference: () => void
  onRemoveReference?: (item: EvidenceSummary) => void
  onEvidenceError?: (message: string) => void
  formBusy?: boolean
  /** True only while a reference file is uploading or being removed. */
  referenceBusy?: boolean
  cause: string
  setCause: (v: string) => void
  causeOther: string
  setCauseOther: (v: string) => void
  manualResidualScore: string
  setManualResidualScore: (v: string) => void
  residualScoreError: string
  assignUserId: number | ''
  setAssignUserId: (v: number | '') => void
  eligibleContributors: { id: number; email: string; fullName: string }[]
  dateIdentified: string
  setDateIdentified: (v: string) => void
  nextReviewDate: string
  setNextReviewDate: (v: string) => void
  thirdPartyName: string
  setThirdPartyName: (v: string) => void
  treatmentJustification: string
  setTreatmentJustification: (v: string) => void
  targetControlEff: RiskLevel
  setTargetControlEff: (v: RiskLevel) => void
  compensatingControls: string
  setCompensatingControls: (v: string) => void
  previewReductionPct: string
  previewWithinAppetite: string
  previewEvalDecision: string
  previewAcceptanceRequired: string
  previewTargetResidualScore: number | null
  previewTargetResidualBand: string
  fieldErrors: Record<string, string>
  clearFieldError: (id: string) => void
}

export default function RiskCreateEditForm(props: RiskCreateEditFormProps) {
  const {
    isErm,
    isPrivacy = false,
    editMode,
    cats,
    flatOrgs,
    users,
    frameworkDetail,
    frameworkId,
    frameworkNodeId,
    scoreLevels,
    matrixGridSize,
    previewInherentBand,
    previewResidualBand,
    previewResidualAction,
    willAutoClose,
    treatPlanDateError,
    dateMin,
  } = props
  const err = (id: string) => props.fieldErrors[id] || undefined
  const clear = (id: string) => props.clearFieldError(id)

  const likelihoodLabel = (l: RiskLevel) =>
    isErm ? ERM_LIKELIHOOD_LABEL[l] : (LIKELIHOOD_EXCEL_LABEL[l] ?? fmtLevel(l))
  const impactLabel = (l: RiskLevel) =>
    isErm ? ERM_IMPACT_LABEL[l] : (LIKELIHOOD_EXCEL_LABEL[l] ?? fmtLevel(l))
  const ratingLabel = (band: string) => (isErm ? fmtErmRating(band) : fmtLevel(band))
  const levelOptions = scoreLevels.map((l) => ({
    value: l,
    label: likelihoodLabel(l),
    tone: apiLevelTone(l),
  }))
  const impactOptions = scoreLevels.map((l) => ({
    value: l,
    label: impactLabel(l),
    tone: apiLevelTone(l),
  }))
  const maxResidualLikelihoodRank = rowRank(props.matrixAxes, props.probability)
  const maxResidualImpactRank = colRank(props.matrixAxes, props.impact)
  const residualLikelihoodOptions = scoreLevels
    .filter((l) => rowRank(props.matrixAxes, l) <= maxResidualLikelihoodRank)
    .map((l) => ({
      value: l,
      label: likelihoodLabel(l),
      tone: apiLevelTone(l),
    }))
  const residualImpactOptions = scoreLevels
    .filter((l) => colRank(props.matrixAxes, l) <= maxResidualImpactRank)
    .map((l) => ({
      value: l,
      label: impactLabel(l),
      tone: apiLevelTone(l),
    }))
  const ceOptions = scoreLevels.map((l) => ({
    value: l,
    label: CE_EXCEL_LABEL[l] ?? fmtLevel(l),
    tone: controlEffectivenessTone(l),
  }))
  const categoryOptions = [
    { value: '', label: 'Select category…' },
    ...cats.map((c) => ({
      value: c,
      label: c === 'OTHER' ? 'Other' : c.charAt(0) + c.slice(1).toLowerCase().replace(/_/g, ' '),
    })),
    ...(props.category !== '' && !cats.includes(props.category)
      ? [
          {
            value: props.category,
            label:
              props.category === 'OTHER'
                ? 'Other'
                : props.category.replace(/_/g, ' ').replace(/\b\w/g, (ch) => ch.toUpperCase()),
          },
        ]
      : []),
  ]
  const orgOptions = [
    { value: '' as const, label: 'Select division…' },
    ...flatOrgs.map((o) => ({ value: o.id, label: o.name })),
  ]
  const userOptions = [
    { value: '' as const, label: 'Select owner…' },
    ...users.map((u) => ({ value: u.id, label: u.fullName })),
  ]
  const causeOptions = [
    { value: '', label: 'Select…' },
    ...ERM_RISK_CAUSES.map((c) => ({ value: c, label: c })),
    { value: ERM_RISK_CAUSE_OTHER, label: ERM_RISK_CAUSE_OTHER },
  ]
  const threatOptions = [
    { value: '', label: 'Select…' },
    ...IS_ASSOCIATED_THREATS.map((c) => ({ value: c, label: c })),
    ...(props.threats && !(IS_ASSOCIATED_THREATS as readonly string[]).includes(props.threats)
      ? [{ value: props.threats, label: props.threats }]
      : []),
  ]
  const divisionHasNoCustodian = props.orgUnitId !== '' && props.eligibleContributors.length === 0
  const custodianPlaceholder =
    props.orgUnitId === ''
      ? 'Select division first'
      : divisionHasNoCustodian
        ? 'No users in this division'
        : 'Select risk custodian…'
  const custodianEmptyHint = divisionHasNoCustodian
    ? 'No Contributor is linked to this division. Link a Contributor to this division in Settings → Users.'
    : undefined

  return (
    <Box sx={{ ...complexFormContainerSx, position: 'relative', maxWidth: 1180, mx: 'auto' }}>
      {!editMode && props.formBusy ? <RiskRecordCreateProgress isErm={isErm} isPrivacy={isPrivacy} /> : null}
      <Stack spacing={2} sx={{ pointerEvents: !editMode && props.formBusy ? 'none' : 'auto' }}>
        {!editMode && props.copyFromLabel ? (
          <AttentionBanner
            dense
            tone="info"
            emphasis={`Copied from ${props.copyFromLabel}`}
            message="Review the fields, then create. Leave Risk Custodian filled to assign now, or clear it to keep a Draft."
          />
        ) : null}
        {/* STAGE 1 */}
        <Box id="risk-form-identification" sx={sectionAnchorSx}>
          <SettingsCard section autoHeight title="Risk identification" subtitle="What the risk is, and whose it is">
            <Box sx={formGrid12Sx}>
              {!editMode && (
                <Box sx={span(3)}>
                  <CompactTextField
                    label={EXCEL_FIELD.riskId}
                    wrapLabel
                    placeholder={isErm ? 'ERM-2026-001' : isPrivacy ? 'DPR-2026-001' : 'UR-2026-001'}
                    value={props.displayIdOverride}
                    onChange={(e) => props.setDisplayIdOverride(e.target.value)}
                    maxWidth="100%"
                  />
                </Box>
              )}
              <Box sx={span(3)}>
                <CompactTextField
                  label={EXCEL_FIELD.dateIdentified}
                  wrapLabel
                  type="date"
                  fill
                  required
                  value={props.dateIdentified}
                  onChange={(e) => {
                    clear(RISK_INPUT_ID.dateIdentified)
                    props.setDateIdentified(e.target.value)
                  }}
                  error={Boolean(err(RISK_INPUT_ID.dateIdentified))}
                  helperText={err(RISK_INPUT_ID.dateIdentified)}
                  inputProps={{ max: dateMin }}
                />
              </Box>
              <Box sx={span(3)}>
                <FormSelect
                  label={EXCEL_FIELD.riskOwner}
                  wrapLabel
                  required
                  fullWidth
                  value={props.riskOwnerUserId === '' ? '' : String(props.riskOwnerUserId)}
                  options={userOptions.map((o) => ({
                    value: o.value === '' ? '' : String(o.value),
                    label: o.label,
                  }))}
                  onChange={(v) => {
                    clear(RISK_INPUT_ID.riskOwner)
                    props.setRiskOwnerUserId(v === '' ? '' : Number(v))
                  }}
                  error={Boolean(err(RISK_INPUT_ID.riskOwner))}
                  helperText={err(RISK_INPUT_ID.riskOwner)}
                />
              </Box>
              {!editMode && (
                <Box sx={span(3)}>
                  <FormSelect
                    label={EXCEL_FIELD.riskCustodian}
                    wrapLabel
                    fullWidth
                    value={props.assignUserId === '' ? '' : String(props.assignUserId)}
                    options={[
                      { value: '', label: custodianPlaceholder },
                      ...props.eligibleContributors.map((u) => ({
                        value: String(u.id),
                        label: `${u.fullName} (${u.email})`,
                      })),
                    ]}
                    onChange={(v) => props.setAssignUserId(v === '' ? '' : Number(v))}
                    disabled={divisionHasNoCustodian}
                    helperText={custodianEmptyHint}
                  />
                </Box>
              )}
              {isPrivacy ? (
                <Box sx={span(6)}>
                  <CompactTextField
                    label="Processing Activity"
                    wrapLabel
                    value={props.processingActivity ?? ''}
                    onChange={(e) => props.setProcessingActivity?.(e.target.value)}
                    maxWidth="100%"
                  />
                </Box>
              ) : null}
              <Box sx={span(12)}>
                <CompactTextField
                  label={EXCEL_FIELD.riskTitle}
                  wrapLabel
                  required
                  maxLength={INPUT_LIMIT.riskTitle}
                  value={props.title}
                  onChange={(e) => {
                    clear(RISK_INPUT_ID.title)
                    props.setTitle(e.target.value)
                  }}
                  error={Boolean(err(RISK_INPUT_ID.title))}
                  helperText={err(RISK_INPUT_ID.title)}
                />
              </Box>
              <Box sx={span(6)}>
                <CompactTextField
                  label={EXCEL_FIELD.riskDescription}
                  wrapLabel
                  required
                  multiline
                  minRows={4}
                  maxRows={NARRATIVE_MAX_ROWS}
                  value={props.description}
                  onChange={(e) => {
                    clear(RISK_INPUT_ID.description)
                    props.setDescription(e.target.value)
                  }}
                  error={Boolean(err(RISK_INPUT_ID.description))}
                  helperText={err(RISK_INPUT_ID.description)}
                />
              </Box>
              <Box sx={span(6)}>
                <CompactTextField
                  label={EXCEL_FIELD.gapObservation}
                  wrapLabel
                  required
                  multiline
                  minRows={4}
                  maxRows={NARRATIVE_MAX_ROWS}
                  value={props.observedGap}
                  onChange={(e) => {
                    clear(RISK_INPUT_ID.observedGap)
                    props.setObservedGap(e.target.value)
                  }}
                  error={Boolean(err(RISK_INPUT_ID.observedGap))}
                  helperText={err(RISK_INPUT_ID.observedGap)}
                />
              </Box>
              {isErm ? (
                <>
                  <Box sx={span(4)}>
                    <FormSelect
                      label={EXCEL_FIELD.riskCategory}
                      wrapLabel
                      required
                      fullWidth
                      value={props.category}
                      options={categoryOptions}
                      onChange={(v) => {
                        clear(RISK_INPUT_ID.category)
                        props.setCategory(v as RiskCategory | '')
                        if (v !== 'OTHER') {
                          clear(RISK_INPUT_ID.categoryOther)
                          props.setCategoryOther('')
                        }
                      }}
                      error={Boolean(err(RISK_INPUT_ID.category))}
                      helperText={err(RISK_INPUT_ID.category)}
                    />
                  </Box>
                  {props.category === 'OTHER' && (
                    <Box sx={span(4)}>
                      <CompactTextField
                        label="Specify category"
                        wrapLabel
                        required
                        value={props.categoryOther}
                        onChange={(e) => {
                          clear(RISK_INPUT_ID.categoryOther)
                          props.setCategoryOther(e.target.value)
                        }}
                        error={Boolean(err(RISK_INPUT_ID.categoryOther))}
                        helperText={err(RISK_INPUT_ID.categoryOther)}
                        maxWidth="100%"
                        maxLength={128}
                      />
                    </Box>
                  )}
                  <Box sx={span(4)}>
                    <FormSelect
                      label={EXCEL_FIELD.riskCause}
                      wrapLabel
                      required
                      fullWidth
                      value={props.cause}
                      options={causeOptions}
                      onChange={(v) => {
                        clear(RISK_INPUT_ID.cause)
                        clear(RISK_INPUT_ID.causeOther)
                        props.setCause(v)
                        if (v !== ERM_RISK_CAUSE_OTHER) props.setCauseOther('')
                      }}
                      error={Boolean(err(RISK_INPUT_ID.cause))}
                      helperText={err(RISK_INPUT_ID.cause)}
                    />
                  </Box>
                  {props.cause === ERM_RISK_CAUSE_OTHER && (
                    <Box sx={span(4)}>
                      <CompactTextField
                        label="Specify cause"
                        wrapLabel
                        required
                        value={props.causeOther}
                        onChange={(e) => {
                          clear(RISK_INPUT_ID.causeOther)
                          props.setCauseOther(e.target.value)
                        }}
                        error={Boolean(err(RISK_INPUT_ID.causeOther))}
                        helperText={err(RISK_INPUT_ID.causeOther)}
                        maxWidth="100%"
                        maxLength={128}
                      />
                    </Box>
                  )}
                  <Box sx={span(4)}>
                    <FormSelect
                      label={EXCEL_FIELD.division}
                      wrapLabel
                      required
                      fullWidth
                      value={props.orgUnitId === '' ? '' : String(props.orgUnitId)}
                      options={orgOptions.map((o) => ({
                        value: o.value === '' ? '' : String(o.value),
                        label: o.label,
                      }))}
                      onChange={(v) => {
                        clear(RISK_INPUT_ID.division)
                        props.setOrgUnitId(v === '' ? '' : Number(v))
                      }}
                      error={Boolean(err(RISK_INPUT_ID.division))}
                      helperText={err(RISK_INPUT_ID.division)}
                    />
                  </Box>
                  <Box sx={span(12)}>
                    <CompactTextField
                      label={EXCEL_FIELD.strategicObjective}
                      wrapLabel
                      required
                      value={props.strategicObjective}
                      onChange={(e) => {
                        clear(RISK_INPUT_ID.strategicObjective)
                        props.setStrategicObjective(e.target.value)
                      }}
                      error={Boolean(err(RISK_INPUT_ID.strategicObjective))}
                      helperText={err(RISK_INPUT_ID.strategicObjective)}
                      maxWidth="100%"
                    />
                  </Box>
                </>
              ) : (
                <>
                  <Box sx={span(4)}>
                    <FormSelect
                      label={EXCEL_FIELD.division}
                      wrapLabel
                      required
                      fullWidth
                      value={props.orgUnitId === '' ? '' : String(props.orgUnitId)}
                      options={orgOptions.map((o) => ({
                        value: o.value === '' ? '' : String(o.value),
                        label: o.label,
                      }))}
                      onChange={(v) => {
                        clear(RISK_INPUT_ID.division)
                        props.setOrgUnitId(v === '' ? '' : Number(v))
                      }}
                      error={Boolean(err(RISK_INPUT_ID.division))}
                      helperText={err(RISK_INPUT_ID.division)}
                    />
                  </Box>
                  <Box sx={span(4)}>
                    <FormSelect
                      label={EXCEL_FIELD.associatedThreat}
                      wrapLabel
                      required
                      fullWidth
                      value={props.threats}
                      options={threatOptions}
                      onChange={(v) => {
                        clear(RISK_INPUT_ID.associatedThreat)
                        props.setThreats(v)
                      }}
                      error={Boolean(err(RISK_INPUT_ID.associatedThreat))}
                      helperText={err(RISK_INPUT_ID.associatedThreat)}
                    />
                  </Box>
                  {!isPrivacy && (
                    <>
                      <Box sx={span(8)}>
                        <FormSelect
                          label={EXCEL_FIELD.controlRef}
                          wrapLabel
                          fullWidth
                          value={frameworkNodeId === '' ? '' : String(frameworkNodeId)}
                          options={[
                            { value: '', label: 'Select control or clause…' },
                            ...(frameworkDetail?.nodes ?? [])
                              .filter((n) => !n.retired)
                              .map((n) => ({
                                value: String(n.id),
                                label: `${n.code}: ${n.title}${n.priority ? ` (${n.priority})` : ''}`,
                              })),
                          ]}
                          onChange={(v) => props.setFrameworkNodeId(v === '' ? '' : Number(v))}
                          disabled={frameworkId === ''}
                          hint={
                            frameworkId === ''
                              ? 'This IS project has no framework, so the control list is empty.'
                              : undefined
                          }
                        />
                      </Box>
                      <Box sx={span(4)}>
                        <FormFieldLabel wrap>{EXCEL_FIELD.controlStatus}</FormFieldLabel>
                        <Box sx={{ minHeight: 32, display: 'flex', alignItems: 'center' }}>
                          <Typography variant="body2">
                            {excelControlStatusLabel(frameworkNodeId, props.controlAssessmentStatus) || '-'}
                          </Typography>
                        </Box>
                      </Box>
                    </>
                  )}
                </>
              )}
              <Box sx={span(12)}>
                <ReferenceAttachmentField
                  pendingFiles={props.createEvidenceFiles}
                  classification={props.createEvidenceClass}
                  saved={props.identificationEvidence}
                  busy={props.referenceBusy}
                  disabled={props.formBusy}
                  hideInlineProgress={!editMode}
                  onPendingFilesChange={props.setCreateEvidenceFiles}
                  onClassification={props.setCreateEvidenceClass}
                  onUploadPending={props.onUploadReference}
                  onRemoveSaved={props.onRemoveReference}
                  onError={props.onEvidenceError}
                  quotaUsed={props.evidenceQuotaUsed}
                />
              </Box>
            </Box>
          </SettingsCard>
        </Box>

        {/* STAGE 2: before controls */}
        <Box id="risk-form-inherent" sx={sectionAnchorSx}>
          <SettingsCard section autoHeight title="Risk analysis" subtitle="Before controls: likelihood × impact">
            <Box sx={formGrid12Sx}>
              <Box sx={span(4)}>
                <FormSelect
                  label={scaledExcelField('likelihood', matrixGridSize)}
                  wrapLabel
                  fullWidth
                  value={props.probability}
                  options={levelOptions}
                  onChange={(v) => props.setProbability(v as RiskLevel)}
                />
              </Box>
              <Box sx={span(4)}>
                <FormSelect
                  label={scaledExcelField('impact', matrixGridSize)}
                  wrapLabel
                  fullWidth
                  value={props.impact}
                  options={impactOptions}
                  onChange={(v) => props.setImpact(v as RiskLevel)}
                />
              </Box>
              <Box sx={spanReadoutSx}>
                <Readout
                  rows={[
                    {
                      label: 'Inherent rating',
                      value: (
                        <RiskLevelBadge
                          level={apiRiskLevelToBadge(excelRatingToLevel(previewInherentBand))}
                          label={ratingLabel(previewInherentBand)}
                        />
                      ),
                    },
                    { label: EXCEL_FIELD.inherentScore, value: String(props.previewInherentScore) },
                  ]}
                />
              </Box>
            </Box>
          </SettingsCard>
        </Box>

        {/* STAGE 3 */}
        <Box id="risk-form-controls" sx={sectionAnchorSx}>
          <SettingsCard section autoHeight title="Existing controls" subtitle="What is already in place">
            <Box sx={formGrid12Sx}>
              <Box sx={span(12)}>
                <CompactTextField
                  label={EXCEL_FIELD.existingControls}
                  wrapLabel
                  multiline
                  minRows={3}
                  maxRows={NARRATIVE_MAX_ROWS}
                  value={props.existingControls}
                  onChange={(e) => props.setExistingControls(e.target.value)}
                />
              </Box>
              <Box sx={span(4)}>
                <FormSelect
                  label={scaledExcelField('controlEffectiveness', matrixGridSize)}
                  wrapLabel
                  fullWidth
                  value={props.controlEff}
                  options={ceOptions}
                  onChange={(v) => props.setControlEff(v as RiskLevel)}
                />
              </Box>
              <Box sx={span(4)}>
                <FormFieldLabel wrap>{EXCEL_FIELD.riskReduction}</FormFieldLabel>
                <Box sx={{ minHeight: 32, display: 'flex', alignItems: 'center' }}>
                  <Typography variant="body2">{props.previewReductionPct || '-'}</Typography>
                </Box>
              </Box>
            </Box>
          </SettingsCard>
        </Box>

        {/* STAGE 4: after controls */}
        <Box id="risk-form-residual" sx={sectionAnchorSx}>
          <SettingsCard section autoHeight title="Residual & evaluation" subtitle="After controls">
            <Box sx={formGrid12Sx}>
              <Box sx={span(12)}>
                <FormControlLabel
                  sx={{ m: 0, alignItems: 'center' }}
                  control={
                    <Checkbox
                      checked={props.residualScoringMode === 'MANUAL_PROBABILITY_IMPACT'}
                      onChange={(e) => {
                        const manual = e.target.checked
                        props.setResidualScoringMode(
                          manual ? 'MANUAL_PROBABILITY_IMPACT' : 'CONTROL_EFFECTIVENESS',
                        )
                        if (manual && isErm) {
                          props.setResidualProbability(props.probability)
                          props.setResidualImpact(props.impact)
                        }
                      }}
                      size="small"
                      sx={{ py: 0 }}
                    />
                  }
                  label={<Typography sx={{ fontSize: '0.8125rem' }}>Enter residual score manually</Typography>}
                />
              </Box>
              {props.residualScoringMode === 'MANUAL_PROBABILITY_IMPACT' ? (
                isErm ? (
                  <>
                    <Box sx={span(4)}>
                      <FormSelect
                        label={scaledExcelField('likelihood', matrixGridSize)}
                        wrapLabel
                        fullWidth
                        value={props.residualProbability}
                        options={residualLikelihoodOptions}
                        onChange={(v) => props.setResidualProbability(v as RiskLevel)}
                      />
                    </Box>
                    <Box sx={span(4)}>
                      <FormSelect
                        label={scaledExcelField('impact', matrixGridSize)}
                        wrapLabel
                        fullWidth
                        value={props.residualImpact}
                        options={residualImpactOptions}
                        onChange={(v) => props.setResidualImpact(v as RiskLevel)}
                      />
                    </Box>
                  </>
                ) : (
                  <Box sx={span(8)}>
                    <CompactTextField
                      label={EXCEL_FIELD.residualScore}
                      wrapLabel
                      required
                      type="number"
                      value={props.manualResidualScore}
                      onChange={(e) => {
                        clear(RISK_INPUT_ID.residualScore)
                        props.setManualResidualScore(e.target.value)
                      }}
                      error={!!props.residualScoreError || Boolean(err(RISK_INPUT_ID.residualScore))}
                      helperText={
                        err(RISK_INPUT_ID.residualScore) ||
                        props.residualScoreError ||
                        `Whole number from 1 to ${props.previewInherentScore}`
                      }
                      slotProps={{
                        htmlInput: { min: 1, max: props.previewInherentScore, step: 1 },
                      }}
                    />
                  </Box>
                )
              ) : null}
              <Box
                sx={
                  props.residualScoringMode === 'MANUAL_PROBABILITY_IMPACT'
                    ? spanReadoutSx
                    : { gridColumn: { xs: 'span 12', lg: 'span 6' }, minWidth: 0 }
                }
              >
                <Readout
                  rows={[
                    { label: EXCEL_FIELD.residualScore, value: String(props.previewResidualScore ?? '-') },
                    {
                      label: EXCEL_FIELD.residualRating,
                      value: (
                        <RiskLevelBadge
                          level={apiRiskLevelToBadge(excelRatingToLevel(previewResidualBand))}
                          label={ratingLabel(previewResidualBand)}
                        />
                      ),
                    },
                    {
                      label: EXCEL_FIELD.withinAppetite,
                      value: (
                        <RiskLevelBadge
                          level={props.previewWithinAppetite.startsWith('EXCEEDS') ? 'critical' : 'low'}
                          label={props.previewWithinAppetite.startsWith('EXCEEDS') ? 'Exceeds' : 'Within appetite'}
                        />
                      ),
                    },
                    {
                      label: EXCEL_FIELD.evaluationDecision,
                      value: (
                        <RiskLevelBadge
                          level={evalDecisionChip(willAutoClose, props.previewEvalDecision).level}
                          label={evalDecisionChip(willAutoClose, props.previewEvalDecision).label}
                        />
                      ),
                    },
                  ]}
                  note={
                    willAutoClose
                      ? 'Evaluation is Auto-closed - no action unless Admin keeps it open.'
                      : previewResidualAction
                  }
                />
              </Box>
              {willAutoClose && (
                <Box sx={span(12)}>
                  <Stack spacing={1}>
                    <FormControlLabel
                      sx={{ m: 0, alignItems: 'flex-start' }}
                      control={
                        <Checkbox
                          checked={props.overrideAutoClose}
                          onChange={(e) => props.setOverrideAutoClose(e.target.checked)}
                          size="small"
                          sx={{ py: 0.25 }}
                        />
                      }
                      label={
                        <Typography sx={{ fontSize: '0.8125rem' }}>
                          Keep open (override auto-closure). Justification required.
                        </Typography>
                      }
                    />
                    {props.overrideAutoClose && (
                      <CompactTextField
                        label="Auto-close override justification"
                        wrapLabel
                        required
                        multiline
                        minRows={2}
                        value={props.autoCloseJustification}
                        onChange={(e) => {
                          clear(RISK_INPUT_ID.autoCloseJustification)
                          props.setAutoCloseJustification(e.target.value)
                        }}
                        error={Boolean(err(RISK_INPUT_ID.autoCloseJustification))}
                        helperText={err(RISK_INPUT_ID.autoCloseJustification)}
                      />
                    )}
                  </Stack>
                </Box>
              )}
            </Box>
          </SettingsCard>
        </Box>

        {/* STAGE 5: create: 4T only. Action plan, evidence, target CE wait for later stages. */}
        <Box id="risk-form-treatment" sx={sectionAnchorSx}>
          <SettingsCard section autoHeight title="Risk treatment" subtitle="How the risk will be handled">
            <Box sx={formGrid12Sx}>
              <Box sx={span(4)}>
                <FormSelect
                  label={EXCEL_FIELD.treatmentOption}
                  wrapLabel
                  fullWidth
                  value={props.treatmentOption}
                  options={TREAT.map((t) => ({ value: t, label: EXCEL_TREAT_LABEL[t] }))}
                  onChange={(v) => props.setTreatmentOption(v as TreatmentOption)}
                />
              </Box>
              <Box sx={span(12)}>
                <CompactTextField
                  label={EXCEL_FIELD.treatmentJustification}
                  wrapLabel
                  required
                  multiline
                  minRows={3}
                  maxRows={NARRATIVE_MAX_ROWS}
                  value={props.treatmentJustification}
                  onChange={(e) => {
                    clear(RISK_INPUT_ID.treatmentJustification)
                    props.setTreatmentJustification(e.target.value)
                  }}
                  error={Boolean(err(RISK_INPUT_ID.treatmentJustification))}
                  helperText={err(RISK_INPUT_ID.treatmentJustification)}
                />
              </Box>
              <Box sx={span(8)}>
                <CompactTextField
                  label={EXCEL_FIELD.evidenceRequired}
                  wrapLabel
                  value={props.planEvidenceRequired}
                  onChange={(e) => props.setPlanEvidenceRequired(e.target.value)}
                />
              </Box>
              <Box sx={span(4)}>
                <CompactTextField
                  label={EXCEL_FIELD.targetDate}
                  wrapLabel
                  type="date"
                  fill
                  value={props.planTargetDate}
                  onChange={(e) => props.setPlanTargetDate(e.target.value)}
                  error={!!treatPlanDateError}
                  helperText={treatPlanDateError || undefined}
                  inputProps={{ min: dateMin }}
                />
              </Box>
            </Box>
          </SettingsCard>
        </Box>
      </Stack>
    </Box>
  )
}

export const RISK_FORM_SECTIONS = [
  { id: 'risk-form-identification', label: 'Identification' },
  { id: 'risk-form-inherent', label: 'Analysis' },
  { id: 'risk-form-controls', label: 'Controls' },
  { id: 'risk-form-residual', label: 'Evaluation' },
  { id: 'risk-form-treatment', label: 'Treatment' },
  { id: 'risk-form-admin', label: 'Admin' },
] as const

export function visibleFormSections(_editMode: boolean, _canAdmin: boolean) {
  return RISK_FORM_SECTIONS.filter((s) => s.id !== 'risk-form-admin')
}
