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
import { excelRatingToLevel } from './riskMatrix'
import {
  CE_EXCEL_LABEL,
  ERM_RISK_CAUSES,
  ERM_RISK_CAUSE_OTHER,
  EXCEL_DATE_FIELD_WIDTH,
  EXCEL_FIELD,
  EXCEL_TREAT_LABEL,
  IS_ASSOCIATED_THREATS,
  LIKELIHOOD_EXCEL_LABEL,
  scaledExcelField,
  excelControlStatusLabel,
} from './excelRiskFields'
import ReferenceAttachmentField from './ReferenceAttachmentField'
import RiskRecordCreateProgress from './RiskRecordCreateProgress'
import { RISK_INPUT_ID } from './riskFormRequired'

const TREAT: TreatmentOption[] = ['TREAT', 'TOLERATE', 'TERMINATE', 'TRANSFER']

const fourColFieldSx = {
  display: 'grid',
  gridTemplateColumns: {
    xs: '1fr',
    sm: 'repeat(2, minmax(0, 1fr))',
    md: 'repeat(4, minmax(0, 1fr))',
  },
  gap: 1,
  alignItems: 'end',
} as const

const twoColFieldSx = {
  display: 'grid',
  gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
  gap: 1,
  alignItems: 'start',
} as const

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

  const levelOptions = scoreLevels.map((l) => ({
    value: l,
    label: LIKELIHOOD_EXCEL_LABEL[l] ?? fmtLevel(l),
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
      label: c === 'OTHER' ? 'Other' : c.replace(/_/g, ' ').replace(/\b\w/g, (ch) => ch.toUpperCase()),
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
    <Box sx={{ ...complexFormContainerSx, position: 'relative' }}>
      {!editMode && props.formBusy ? <RiskRecordCreateProgress isErm={isErm} isPrivacy={isPrivacy} /> : null}
      <Stack spacing={1.25} sx={{ pointerEvents: !editMode && props.formBusy ? 'none' : 'auto' }}>
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
          <SettingsCard
            compact
            autoHeight
            titleCaps
            title="Risk identification"
            subtitle="What the risk is, and whose it is"
          >
            <Stack spacing={1}>
              <Box sx={fourColFieldSx}>
                {!editMode && (
                  <CompactTextField
                    label={EXCEL_FIELD.riskId}
                    wrapLabel
                    placeholder={isErm ? 'ERM-2026-001' : isPrivacy ? 'DPR-2026-001' : 'ISR-2026-001'}
                    value={props.displayIdOverride}
                    onChange={(e) => props.setDisplayIdOverride(e.target.value)}
                    maxWidth="100%"
                  />
                )}
                <CompactTextField
                  label={EXCEL_FIELD.dateIdentified}
                  wrapLabel
                  type="date"
                  required
                  value={props.dateIdentified}
                  onChange={(e) => {
                    clear(RISK_INPUT_ID.dateIdentified)
                    props.setDateIdentified(e.target.value)
                  }}
                  error={Boolean(err(RISK_INPUT_ID.dateIdentified))}
                  helperText={err(RISK_INPUT_ID.dateIdentified)}
                  inputProps={{ max: dateMin }}
                  maxWidth={EXCEL_DATE_FIELD_WIDTH}
                />
                <Box sx={{ gridColumn: { xs: '1', sm: '1 / -1', md: editMode ? 'span 3' : 'span 2' } }}>
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
              </Box>
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))' },
                  gap: 1,
                  alignItems: 'start',
                }}
              >
                <CompactTextField
                  label={EXCEL_FIELD.riskDescription}
                  wrapLabel
                  required
                  multiline
                  minRows={3}
                  maxRows={NARRATIVE_MAX_ROWS}
                  value={props.description}
                  onChange={(e) => {
                    clear(RISK_INPUT_ID.description)
                    props.setDescription(e.target.value)
                  }}
                  error={Boolean(err(RISK_INPUT_ID.description))}
                  helperText={err(RISK_INPUT_ID.description)}
                />
                <CompactTextField
                  label={EXCEL_FIELD.gapObservation}
                  wrapLabel
                  required
                  multiline
                  minRows={3}
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
              {isErm ? (
                <>
                  <Box sx={fourColFieldSx}>
                    <FormSelect
                      label={EXCEL_FIELD.riskCategory}
                      wrapLabel
                      required
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
                    {props.category === 'OTHER' && (
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
                    )}
                    <FormSelect
                      label={EXCEL_FIELD.division}
                      wrapLabel
                      required
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
                  <Box sx={fourColFieldSx}>
                    <FormSelect
                      label={EXCEL_FIELD.riskCause}
                      wrapLabel
                      required
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
                    {props.cause === ERM_RISK_CAUSE_OTHER && (
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
                    )}
                    <FormSelect
                      label={EXCEL_FIELD.riskOwner}
                      wrapLabel
                      required
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
                    {!editMode && (
                      <FormSelect
                        label={EXCEL_FIELD.riskCustodian}
                        wrapLabel
                        value={props.assignUserId === '' ? '' : String(props.assignUserId)}
                        options={[
                          {
                            value: '',
                            label: custodianPlaceholder,
                          },
                          ...props.eligibleContributors.map((u) => ({
                            value: String(u.id),
                            label: `${u.fullName} (${u.email})`,
                          })),
                        ]}
                        onChange={(v) => props.setAssignUserId(v === '' ? '' : Number(v))}
                        disabled={divisionHasNoCustodian}
                        helperText={custodianEmptyHint}
                      />
                    )}
                  </Box>
                </>
              ) : (
                <>
                  <Box sx={fourColFieldSx}>
                    <FormSelect
                      label={EXCEL_FIELD.division}
                      wrapLabel
                      required
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
                    {!isPrivacy && (
                      <>
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
                        <Box>
                          <FormFieldLabel wrap>{EXCEL_FIELD.controlStatus}</FormFieldLabel>
                          <Box sx={{ minHeight: 32, display: 'flex', alignItems: 'center' }}>
                            <Typography variant="body2">
                              {excelControlStatusLabel(frameworkNodeId, props.controlAssessmentStatus) || '-'}
                            </Typography>
                          </Box>
                        </Box>
                      </>
                    )}
                    <FormSelect
                      label={EXCEL_FIELD.associatedThreat}
                      wrapLabel
                      required
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
                  <Box sx={twoColFieldSx}>
                    <FormSelect
                      label={EXCEL_FIELD.riskOwner}
                      wrapLabel
                      required
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
                    {!editMode && (
                      <FormSelect
                        label={EXCEL_FIELD.riskCustodian}
                        wrapLabel
                        value={props.assignUserId === '' ? '' : String(props.assignUserId)}
                        options={[
                          {
                            value: '',
                            label: custodianPlaceholder,
                          },
                          ...props.eligibleContributors.map((u) => ({
                            value: String(u.id),
                            label: `${u.fullName} (${u.email})`,
                          })),
                        ]}
                        onChange={(v) => props.setAssignUserId(v === '' ? '' : Number(v))}
                        disabled={divisionHasNoCustodian}
                        helperText={custodianEmptyHint}
                      />
                    )}
                  </Box>
                </>
              )}
            </Stack>
          </SettingsCard>
        </Box>

        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: {
              xs: '1fr',
              md: 'minmax(0, 0.88fr) minmax(0, 0.88fr) minmax(0, 1.24fr)',
            },
            gap: 1.25,
            alignItems: 'stretch',
          }}
        >
          <Box id="risk-form-inherent" sx={sectionAnchorSx}>
            <SettingsCard
              compact
              titleCaps
              title="Risk analysis"
              subtitle="Likelihood × Impact"
              action={
                <RiskLevelBadge
                  level={apiRiskLevelToBadge(excelRatingToLevel(previewInherentBand))}
                  label={fmtLevel(previewInherentBand)}
                />
              }
            >
              <Stack spacing={1}>
                <Box
                  sx={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(2, minmax(0, max-content))',
                    columnGap: 1.25,
                    alignItems: 'start',
                  }}
                >
                  <FormSelect
                    label={scaledExcelField('likelihood', matrixGridSize)}
                    wrapLabel
                    minWidth={128}
                    maxWidth={140}
                    value={props.probability}
                    options={levelOptions}
                    onChange={(v) => props.setProbability(v as RiskLevel)}
                  />
                  <FormSelect
                    label={scaledExcelField('impact', matrixGridSize)}
                    wrapLabel
                    minWidth={128}
                    maxWidth={140}
                    value={props.impact}
                    options={levelOptions}
                    onChange={(v) => props.setImpact(v as RiskLevel)}
                  />
                </Box>
                <Box>
                  <FormFieldLabel wrap>{EXCEL_FIELD.inherentScore}</FormFieldLabel>
                  <Box sx={{ minHeight: 32, display: 'flex', alignItems: 'center' }}>
                    <Typography variant="body2">
                      {`${props.previewInherentScore} · ${fmtLevel(previewInherentBand)}`}
                    </Typography>
                  </Box>
                </Box>
              </Stack>
            </SettingsCard>
          </Box>

          <Box id="risk-form-controls" sx={sectionAnchorSx}>
            <SettingsCard compact titleCaps title="Existing controls" subtitle="What is already in place">
              <Stack spacing={1.5}>
                <CompactTextField
                  label={EXCEL_FIELD.existingControls}
                  wrapLabel
                  multiline
                  minRows={2}
                  maxRows={NARRATIVE_MAX_ROWS}
                  value={props.existingControls}
                  onChange={(e) => props.setExistingControls(e.target.value)}
                />
                <Box
                  sx={{
                    display: 'grid',
                    gridTemplateColumns: { xs: '1fr', sm: 'minmax(0, 1.2fr) minmax(0, 1fr)' },
                    gap: 1.5,
                    alignItems: 'start',
                  }}
                >
                  <FormSelect
                    label={scaledExcelField('controlEffectiveness', matrixGridSize)}
                    wrapLabel
                    value={props.controlEff}
                    options={ceOptions}
                    onChange={(v) => props.setControlEff(v as RiskLevel)}
                  />
                  <Box>
                    <FormFieldLabel wrap>{EXCEL_FIELD.riskReduction}</FormFieldLabel>
                    <Box sx={{ minHeight: 32, display: 'flex', alignItems: 'center' }}>
                      <Typography variant="body2">{props.previewReductionPct || '-'}</Typography>
                    </Box>
                  </Box>
                </Box>
              </Stack>
            </SettingsCard>
          </Box>

          <Box id="risk-form-residual" sx={sectionAnchorSx}>
            <SettingsCard
              compact
              titleCaps
              title="Residual & evaluation"
              action={
                <RiskLevelBadge
                  level={apiRiskLevelToBadge(excelRatingToLevel(previewResidualBand))}
                  label={fmtLevel(previewResidualBand)}
                />
              }
            >
              <Stack spacing={1}>
                <FormControlLabel
                  sx={{ m: 0, alignItems: 'center' }}
                  control={
                    <Checkbox
                      checked={props.residualScoringMode === 'MANUAL_PROBABILITY_IMPACT'}
                      onChange={(e) =>
                        props.setResidualScoringMode(
                          e.target.checked ? 'MANUAL_PROBABILITY_IMPACT' : 'CONTROL_EFFECTIVENESS',
                        )
                      }
                      size="small"
                      sx={{ py: 0 }}
                    />
                  }
                  label={
                    <Typography sx={{ fontSize: '0.6875rem', lineHeight: 1.35 }}>
                      Enter residual score manually
                    </Typography>
                  }
                />
                {props.residualScoringMode === 'MANUAL_PROBABILITY_IMPACT' ? (
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
                ) : (
                  <Box>
                    <FormFieldLabel wrap>{EXCEL_FIELD.residualScore}</FormFieldLabel>
                    <Box sx={{ minHeight: 32, display: 'flex', alignItems: 'center' }}>
                      <Typography variant="body2">{String(props.previewResidualScore ?? '-')}</Typography>
                    </Box>
                  </Box>
                )}
                <Box sx={twoColFieldSx}>
                  <Box>
                    <FormFieldLabel wrap>{EXCEL_FIELD.residualRating}</FormFieldLabel>
                    <Box sx={{ height: 32, display: 'flex', alignItems: 'center' }}>
                      <RiskLevelBadge
                        level={apiRiskLevelToBadge(excelRatingToLevel(previewResidualBand))}
                        label={fmtLevel(previewResidualBand)}
                      />
                    </Box>
                  </Box>
                  <Box>
                    <FormFieldLabel wrap>{EXCEL_FIELD.withinAppetite}</FormFieldLabel>
                    <Box sx={{ height: 32, display: 'flex', alignItems: 'center' }}>
                      <RiskLevelBadge
                        level={props.previewWithinAppetite.startsWith('EXCEEDS') ? 'critical' : 'low'}
                        label={
                          props.previewWithinAppetite.startsWith('EXCEEDS')
                            ? 'Exceeds'
                            : 'Within appetite'
                        }
                      />
                    </Box>
                  </Box>
                  <Box sx={{ minWidth: 0, gridColumn: '1 / -1' }}>
                    <FormFieldLabel wrap>{EXCEL_FIELD.evaluationDecision}</FormFieldLabel>
                    <Box sx={{ height: 32, display: 'flex', alignItems: 'center', minWidth: 0 }}>
                      <RiskLevelBadge
                        level={evalDecisionChip(willAutoClose, props.previewEvalDecision).level}
                        label={evalDecisionChip(willAutoClose, props.previewEvalDecision).label}
                      />
                    </Box>
                  </Box>
                </Box>
                <Typography sx={{ fontSize: '0.6875rem', color: 'text.secondary', lineHeight: 1.35 }}>
                  {willAutoClose
                    ? 'Evaluation is Auto-closed - no action unless Admin keeps it open.'
                    : previewResidualAction}
                </Typography>
                {willAutoClose && (
                  <Stack spacing={0.75}>
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
                        <Typography sx={{ fontSize: '0.6875rem', lineHeight: 1.35 }}>
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
                        minRows={1}
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
                )}
              </Stack>
            </SettingsCard>
          </Box>
        </Box>

        {/* STAGE 4: create: 4T only. Action plan, evidence, target CE wait for later stages. */}
        <Box id="risk-form-treatment" sx={sectionAnchorSx}>
          <SettingsCard compact titleCaps title="Risk treatment">
            <Stack spacing={1}>
              <FormSelect
                label={EXCEL_FIELD.treatmentOption}
                wrapLabel
                value={props.treatmentOption}
                options={TREAT.map((t) => ({ value: t, label: EXCEL_TREAT_LABEL[t] }))}
                onChange={(v) => props.setTreatmentOption(v as TreatmentOption)}
                maxWidth={240}
              />
              <CompactTextField
                label={EXCEL_FIELD.treatmentJustification}
                wrapLabel
                required
                multiline
                minRows={2}
                maxRows={NARRATIVE_MAX_ROWS}
                value={props.treatmentJustification}
                onChange={(e) => {
                  clear(RISK_INPUT_ID.treatmentJustification)
                  props.setTreatmentJustification(e.target.value)
                }}
                error={Boolean(err(RISK_INPUT_ID.treatmentJustification))}
                helperText={err(RISK_INPUT_ID.treatmentJustification)}
              />
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: {
                    xs: '1fr',
                    sm: 'minmax(0, 1fr) max-content',
                  },
                  gap: 1,
                  alignItems: 'start',
                }}
              >
                <CompactTextField
                  label={EXCEL_FIELD.evidenceRequired}
                  wrapLabel
                  value={props.planEvidenceRequired}
                  onChange={(e) => props.setPlanEvidenceRequired(e.target.value)}
                />
                <CompactTextField
                  label={EXCEL_FIELD.targetDate}
                  wrapLabel
                  type="date"
                  value={props.planTargetDate}
                  onChange={(e) => props.setPlanTargetDate(e.target.value)}
                  error={!!treatPlanDateError}
                  helperText={treatPlanDateError || undefined}
                  inputProps={{ min: dateMin }}
                  maxWidth={EXCEL_DATE_FIELD_WIDTH}
                />
              </Box>
            </Stack>
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
