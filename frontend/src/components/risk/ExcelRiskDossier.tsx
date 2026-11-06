import { useEffect, useState, type ReactNode } from 'react'
import { Box, Stack, Typography } from '@mui/material'
import {
  api,
  type ControlAssessmentStatus,
  type EvidenceSummary,
  type RiskLevel,
  type RiskStatus,
  type RiskSummary,
  type TreatmentOption,
} from '../../api'
import {
  CONTROL_HEIGHT,
  DomainBadge,
  FormFieldLabel,
  RiskLevelBadge,
  SettingsCard,
  fourColFieldSx,
  sectionAnchorSx,
  MEDIUM_FORM_MAX_WIDTH,
} from '../ui'
import { apiRiskLevelToBadge } from '../ui/riskBadgeMappers'
import { controlEffectivenessTone } from '../ui/ratingTone'
import { fmtLevel } from '../../riskLevelLabels'
import EvidenceAttachedList from '../evidence/EvidenceAttachedList'
import { brand } from '../../theme'
import { excelRating, excelRatingToLevel, parseGridSize } from './riskMatrix'
import { riskStatusBadge } from './riskUi'
import {
  CE_EXCEL_LABEL,
  EXCEL_FIELD,
  EXCEL_TREAT_LABEL,
  LIKELIHOOD_EXCEL_LABEL,
  configStatusLabel,
  evaluationDecisionLabel,
  isExcelFinishedRow,
  reviewAlertLabel,
  treatmentProgressLabel,
  scaledExcelField,
  excelControlStatusLabel,
  isIdentificationEvidence,
} from './excelRiskFields'

export const EXCEL_DOSSIER_SECTIONS = [
  { id: 'risk-section-identification', label: 'Identification' },
  { id: 'risk-section-scoring', label: 'Analysis' },
  { id: 'risk-section-controls', label: 'Controls' },
  { id: 'risk-section-evaluation', label: 'Evaluation' },
  { id: 'risk-section-treatment', label: 'Treatment' },
] as const

function emptyVal(value: unknown): boolean {
  if (value == null || value === false) return true
  if (typeof value === 'number') return false
  if (typeof value === 'string') {
    const t = value.trim()
    return t === '' || t === '-' || t === '-' || t === '–'
  }
  return false
}

function sheetLabel(label: string): string {
  return label.replace(/\s*\(auto\)\s*/gi, '').replace(/\s+/g, ' ').trim()
}

const VALUE_SIZE = '0.78125rem'

const valueWellSx = {
  boxSizing: 'border-box' as const,
  height: CONTROL_HEIGHT,
  minHeight: CONTROL_HEIGHT,
  display: 'flex',
  alignItems: 'center',
  px: 1.25,
  border: `1px solid ${brand.border}`,
  borderRadius: '6px',
  bgcolor: brand.hover,
  fontSize: VALUE_SIZE,
  fontWeight: 600,
  color: brand.ink,
  lineHeight: 1.25,
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap' as const,
  minWidth: 0,
  width: '100%',
}

const valueWellWrapSx = {
  ...valueWellSx,
  height: 'auto',
  maxHeight: `calc(${VALUE_SIZE} * 1.4 * 6 + 12px)`,
  whiteSpace: 'pre-wrap' as const,
  wordBreak: 'break-word' as const,
  textOverflow: 'unset',
  overflowY: 'auto' as const,
  overflowX: 'hidden' as const,
  alignItems: 'flex-start',
  py: 0.75,
  lineHeight: 1.4,
}

const narrativeValueSx = {
  fontSize: VALUE_SIZE,
  fontWeight: 600,
  color: brand.ink,
  lineHeight: 1.45,
  whiteSpace: 'pre-wrap' as const,
  wordBreak: 'break-word' as const,
}

function ValueWell({ text, wrap }: { text: string; wrap?: boolean }) {
  return (
    <Box sx={wrap ? valueWellWrapSx : valueWellSx} title={text}>
      {text}
    </Box>
  )
}

function Field({
  label,
  value,
  span,
  wide,
  tone,
  keep,
  well,
  wrap,
  children,
}: {
  label: string
  value?: unknown
  span?: boolean
  wide?: boolean
  tone?: 'critical' | 'high' | 'medium' | 'low' | 'veryLow'
  /** Show '-' when empty (create-style cards that should not collapse). */
  keep?: boolean
  /** Filled 32px box so a chosen dropdown/date/score reads as a value, not a second label. */
  well?: boolean
  /** Grow the well and wrap long text instead of ellipsis. */
  wrap?: boolean
  children?: ReactNode
}) {
  if (!children && emptyVal(value) && !keep) return null
  const display = emptyVal(value) ? '-' : String(value)
  return (
    <Box
      sx={{
        minWidth: 0,
        gridColumn: span ? { xs: '1 / -1' } : wide ? { sm: 'span 2' } : undefined,
      }}
    >
      <FormFieldLabel wrap>{sheetLabel(label)}</FormFieldLabel>
      {children ? (
        children
      ) : tone && !emptyVal(value) ? (
        <Box sx={{ minHeight: CONTROL_HEIGHT, display: 'flex', alignItems: 'center' }}>
          <RiskLevelBadge level={tone} label={display} wrap={wrap} />
        </Box>
      ) : well ? (
        <ValueWell text={display} wrap={wrap} />
      ) : (
        <Typography sx={narrativeValueSx}>{display}</Typography>
      )}
    </Box>
  )
}

function LevelField({
  label,
  level,
  keep,
}: {
  label: string
  level: RiskLevel | null | undefined
  keep?: boolean
}) {
  if (!level && !keep) return null
  return (
    <Box sx={{ minWidth: 0 }}>
      <FormFieldLabel wrap>{sheetLabel(label)}</FormFieldLabel>
      <ValueWell text={level ? levelCaption(label, level) : '-'} />
    </Box>
  )
}

function levelCaption(label: string, level: RiskLevel): string {
  if (label.includes('Control Effectiveness')) return CE_EXCEL_LABEL[level] ?? fmtLevel(level)
  if (label.includes('Likelihood') || label.includes('Impact')) {
    return LIKELIHOOD_EXCEL_LABEL[level] ?? fmtLevel(level)
  }
  return fmtLevel(level)
}

function Grid({ children }: { children: ReactNode }) {
  return <Box sx={fourColFieldSx}>{children}</Box>
}

function reductionPct(raw: number | null | undefined): string | null {
  if (raw == null) return null
  const n = Number(raw)
  if (!Number.isFinite(n)) return null
  return `${Math.round(n * (n <= 1 ? 100 : 1))}%`
}

function scoreTone(
  score: number | null | undefined,
  gridSize?: number | null,
): 'critical' | 'high' | 'medium' | 'low' | 'veryLow' | undefined {
  if (score == null || !Number.isFinite(Number(score))) return undefined
  return apiRiskLevelToBadge(excelRatingToLevel(excelRating(parseGridSize(gridSize), Number(score))))
}

function bandTone(
  value: string | null | undefined,
): 'critical' | 'high' | 'medium' | 'low' | 'veryLow' | undefined {
  if (!value || emptyVal(value)) return undefined
  return apiRiskLevelToBadge(excelRatingToLevel(value))
}

function treatedScoreTone(
  value: string | number | null | undefined,
  gridSize?: number | null,
): 'critical' | 'high' | 'medium' | 'low' | 'veryLow' | undefined {
  if (value == null || emptyVal(value)) return undefined
  const n = Number(value)
  if (Number.isFinite(n) && String(n) === String(value).trim()) return scoreTone(n, gridSize)
  return bandTone(String(value))
}

function acceptanceRequiredTone(
  label: string | null | undefined,
): 'critical' | 'high' | 'medium' | 'low' | 'veryLow' | undefined {
  if (!label || emptyVal(label)) return undefined
  const t = label.trim().toUpperCase()
  if (t.startsWith('YES')) return 'critical'
  if (t.startsWith('NO')) return 'low'
  return undefined
}

function evalText(decision: string | null | undefined): string | null {
  if (!decision) return null
  if (decision === 'TREATMENT_REQUIRED') return 'TREATMENT REQUIRED'
  if (decision === 'MONITOR') return 'Within appetite - monitor only'
  if (decision === 'AUTO_CLOSE' || decision === 'AUTO_CLOSED') return 'Auto-closed - no action'
  const labelled = evaluationDecisionLabel(decision)
  return emptyVal(labelled) ? null : labelled
}

export type ExcelRiskView = Pick<
  RiskSummary,
  | 'displayId'
  | 'title'
  | 'description'
  | 'observedGap'
  | 'frameworkId'
  | 'frameworkNodeId'
  | 'category'
  | 'categoryOther'
  | 'dateIdentified'
  | 'strategicObjective'
  | 'cause'
  | 'nodeCode'
  | 'threats'
  | 'orgUnitId'
  | 'orgUnitName'
  | 'riskOwnerUserId'
  | 'riskOwnerName'
  | 'assignedContributorId'
  | 'assignedContributorName'
  | 'probability'
  | 'impact'
  | 'inherentScore'
  | 'inherentRating'
  | 'inherentRisk'
  | 'existingControls'
  | 'controlEffectiveness'
  | 'riskReductionPct'
  | 'residualScore'
  | 'residualRating'
  | 'residualRisk'
  | 'withinAppetite'
  | 'evaluationDecision'
  | 'treatmentOption'
  | 'treatmentChangeJustification'
  | 'activeTreatmentPlan'
  | 'treatmentPlan'
  | 'targetControlEffectiveness'
  | 'targetResidualScore'
  | 'treatedRiskScore'
  | 'acceptanceRequiredLabel'
  | 'finalRiskStatus'
  | 'riskType'
  | 'status'
  | 'nextReviewDate'
>

export default function ExcelRiskDossier({
  risk,
  orgLabel,
  userLabel,
  showContributor = false,
  showPostSubmit = false,
  linkedEvidence = [],
  onEvidenceError,
  gridSize = 5,
  evidenceSlot,
  headerSlot,
}: {
  risk: ExcelRiskView
  orgLabel: (id: number | null | undefined) => string
  userLabel: (id: number | null | undefined) => string
  showContributor?: boolean
  showPostSubmit?: boolean
  linkedEvidence?: EvidenceSummary[]
  onEvidenceError?: (message: string) => void
  gridSize?: number
  evidenceSlot?: ReactNode
  headerSlot?: ReactNode
}) {
  const isErm = risk.riskType === 'ERM'
  const category =
    risk.category === 'OTHER' && risk.categoryOther?.trim()
      ? `Other: ${risk.categoryOther.trim()}`
      : risk.category.replace(/_/g, ' ')
  const inherentBand = fmtLevel(risk.inherentRating ?? risk.inherentRisk)
  const residualBand = fmtLevel(risk.residualRating ?? risk.residualRisk)
  const treat = risk.treatmentOption ? EXCEL_TREAT_LABEL[risk.treatmentOption as TreatmentOption] : null
  const planText = risk.activeTreatmentPlan?.actionDescription || risk.treatmentPlan
  const showPlan = showContributor && !emptyVal(planText)
  const contributorFiles = linkedEvidence.filter((e) => !isIdentificationEvidence(e.workflowStage))
  const showFiles = showContributor && contributorFiles.length > 0
  const referenceFiles = linkedEvidence.filter((e) => isIdentificationEvidence(e.workflowStage))

  const [controlStatus, setControlStatus] = useState<ControlAssessmentStatus | null>(null)
  useEffect(() => {
    if (isErm || risk.frameworkId == null || risk.frameworkNodeId == null) {
      setControlStatus(null)
      return
    }
    let cancelled = false
    void api
      .getControlAssessmentStatus(risk.frameworkId, risk.frameworkNodeId)
      .then((s) => {
        if (!cancelled) setControlStatus(s)
      })
      .catch(() => {
        if (!cancelled) setControlStatus(null)
      })
    return () => {
      cancelled = true
    }
  }, [isErm, risk.frameworkId, risk.frameworkNodeId])
  const showAkAl = isExcelFinishedRow(risk.status, risk.finalRiskStatus)
  const finishTitle = showAkAl ? 'Action and review' : 'After submission'
  const finishSubtitle = showAkAl
    ? 'Target scoring, Action, and Next Review Date'
    : 'Target scoring and Final Risk Status'
  const actionBadge = riskStatusBadge(risk.status as RiskStatus)
  const progressText = treatmentProgressLabel({
    startDate: risk.activeTreatmentPlan?.startDate,
    targetDate: risk.activeTreatmentPlan?.targetDate,
    status: risk.activeTreatmentPlan?.status,
  })
  const alertText = reviewAlertLabel(risk.nextReviewDate)

  const isoDate = (value: string | null | undefined) =>
    !value ? null : value.length >= 10 ? value.slice(0, 10) : value

  return (
    <Stack spacing={1.25}>
      <Box id="risk-section-identification" sx={sectionAnchorSx}>
        {headerSlot}
        <SettingsCard compact autoHeight titleCaps title="Risk identification" subtitle="What the risk is, and whose it is">
          <Grid>
            <Field well label={EXCEL_FIELD.riskId} value={risk.displayId} />
            <Field well label={EXCEL_FIELD.dateIdentified} value={risk.dateIdentified} />
            <Field wide well wrap label={EXCEL_FIELD.riskTitle} value={risk.title} />
            <Box
              sx={{
                gridColumn: '1 / -1',
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))' },
                gap: 1,
                alignItems: 'start',
              }}
            >
              <Field keep well wrap label={EXCEL_FIELD.riskDescription} value={risk.description} />
              <Field keep well wrap label={EXCEL_FIELD.gapObservation} value={risk.observedGap} />
            </Box>
            <Box sx={{ gridColumn: '1 / -1', my: 0.5, maxWidth: MEDIUM_FORM_MAX_WIDTH }}>
              <SettingsCard
                compact
                autoHeight
                titleCaps
                title={EXCEL_FIELD.referenceAttachment}
                subtitle="Optional supporting files"
              >
                <EvidenceAttachedList
                  items={referenceFiles}
                  numbered={false}
                  flush
                  emptyLabel="No reference attachments yet"
                  onError={onEvidenceError}
                />
              </SettingsCard>
            </Box>
            {isErm && <Field well label={EXCEL_FIELD.riskCategory} value={category} />}
            <Field well label={EXCEL_FIELD.division} value={risk.orgUnitName || orgLabel(risk.orgUnitId)} />
            {isErm ? (
              <Field well wrap label={EXCEL_FIELD.strategicObjective} value={risk.strategicObjective} />
            ) : (
              <>
                <Field keep well label={EXCEL_FIELD.controlRef} value={risk.nodeCode} />
                <Field
                  keep
                  well
                  label={EXCEL_FIELD.controlStatus}
                  value={excelControlStatusLabel(risk.frameworkNodeId, controlStatus)}
                />
              </>
            )}
            <Field
              span
              well
              wrap
              label={isErm ? EXCEL_FIELD.riskCause : EXCEL_FIELD.associatedThreat}
              value={isErm ? risk.cause : risk.threats}
            />
            <Field well label={EXCEL_FIELD.riskOwner} value={risk.riskOwnerName || userLabel(risk.riskOwnerUserId)} />
            <Field
              well
              label={EXCEL_FIELD.riskCustodian}
              value={risk.assignedContributorName || userLabel(risk.assignedContributorId)}
            />
          </Grid>
        </SettingsCard>
      </Box>

      {evidenceSlot}

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
        <Box id="risk-section-scoring" sx={sectionAnchorSx}>
          <SettingsCard
            compact
            autoHeight
            titleCaps
            title="Risk analysis"
            subtitle="Likelihood × Impact"
            action={
              <RiskLevelBadge
                level={apiRiskLevelToBadge(excelRatingToLevel(risk.inherentRating ?? risk.inherentRisk))}
                label={inherentBand}
              />
            }
          >
            <Stack spacing={1}>
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
                  gap: 1,
                }}
              >
                <LevelField keep label={scaledExcelField('likelihood', gridSize)} level={risk.probability} />
                <LevelField keep label={scaledExcelField('impact', gridSize)} level={risk.impact} />
              </Box>
              <Field
                keep
                well
                label={EXCEL_FIELD.inherentScore}
                value={
                  risk.inherentScore != null
                    ? `${risk.inherentScore} · ${risk.inherentRating || inherentBand}`
                    : risk.inherentRating || inherentBand
                }
              />
            </Stack>
          </SettingsCard>
        </Box>

        <Box id="risk-section-controls" sx={sectionAnchorSx}>
          <SettingsCard compact autoHeight titleCaps title="Existing controls" subtitle="What is already in place">
            <Stack spacing={1}>
              <Field keep span well wrap label={EXCEL_FIELD.existingControls} value={risk.existingControls} />
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr', sm: 'minmax(0, 1.2fr) minmax(0, 1fr)' },
                  gap: 1,
                }}
              >
                <LevelField keep label={scaledExcelField('controlEffectiveness', gridSize)} level={risk.controlEffectiveness} />
                <Field keep well label={EXCEL_FIELD.riskReduction} value={reductionPct(risk.riskReductionPct)} />
              </Box>
            </Stack>
          </SettingsCard>
        </Box>

        <Box id="risk-section-evaluation" sx={sectionAnchorSx}>
          <SettingsCard
            compact
            autoHeight
            titleCaps
            title="Residual & evaluation"
            action={
              <RiskLevelBadge
                level={apiRiskLevelToBadge(excelRatingToLevel(risk.residualRating ?? risk.residualRisk))}
                label={residualBand}
              />
            }
          >
            <Stack spacing={1}>
              <Field
                keep
                well
                label={EXCEL_FIELD.residualScore}
                value={
                  risk.residualScore != null
                    ? `${risk.residualScore} · ${risk.residualRating || residualBand}`
                    : risk.residualRating || residualBand
                }
              />
              <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 1 }}>
                <Field
                  keep
                  label={EXCEL_FIELD.residualRating}
                  value={risk.residualRating || residualBand}
                  tone={apiRiskLevelToBadge(excelRatingToLevel(risk.residualRating ?? risk.residualRisk))}
                />
                <Field
                  keep
                  label={EXCEL_FIELD.withinAppetite}
                  value={
                    risk.withinAppetite === false
                      ? 'EXCEEDS APPETITE'
                      : risk.withinAppetite
                        ? 'Within Appetite'
                        : null
                  }
                  tone={risk.withinAppetite === false ? 'critical' : risk.withinAppetite ? 'low' : undefined}
                />
              </Box>
              <Field
                keep
                label={EXCEL_FIELD.evaluationDecision}
                value={evalText(risk.evaluationDecision)}
                tone={risk.evaluationDecision === 'TREATMENT_REQUIRED' ? 'critical' : 'low'}
              />
            </Stack>
          </SettingsCard>
        </Box>
      </Box>

      <Box id="risk-section-treatment" sx={sectionAnchorSx}>
        <SettingsCard compact autoHeight titleCaps title="Risk treatment" subtitle="What will be done, by whom, by when">
          <Grid>
            <Field well label={EXCEL_FIELD.treatmentOption} value={treat} />
            <Field well wrap label={EXCEL_FIELD.evidenceRequired} value={risk.activeTreatmentPlan?.evidenceRequired} />
            <Field well label={EXCEL_FIELD.targetDate} value={risk.activeTreatmentPlan?.targetDate} />
            <Field span well wrap label={EXCEL_FIELD.treatmentJustification} value={risk.treatmentChangeJustification} />
          </Grid>
        </SettingsCard>
      </Box>

      {(showPlan || showFiles) && (
        <Box id="risk-section-contributor" sx={sectionAnchorSx}>
          <SettingsCard compact autoHeight titleCaps title="Contributor" subtitle="Treatment Action Plan Detail and Attached Evidence">
            <Grid>
              <Field span well wrap label={EXCEL_FIELD.treatmentActionPlan} value={planText} />
              {showFiles && (
                <Field span label={EXCEL_FIELD.attachedEvidence}>
                  <EvidenceAttachedList
                    items={contributorFiles}
                    numbered={false}
                    emptyLabel="No attached evidence."
                    onError={onEvidenceError}
                  />
                </Field>
              )}
            </Grid>
          </SettingsCard>
        </Box>
      )}

      {showPostSubmit && (
        <Box id="risk-section-post-submit" sx={sectionAnchorSx}>
          <SettingsCard compact autoHeight titleCaps title={finishTitle} subtitle={finishSubtitle}>
            <Grid>
              <Field
                label={scaledExcelField(isErm ? 'targetCe' : 'treatmentCe', gridSize)}
                value={
                  risk.targetControlEffectiveness
                    ? (CE_EXCEL_LABEL[risk.targetControlEffectiveness] ??
                      fmtLevel(risk.targetControlEffectiveness))
                    : null
                }
                tone={controlEffectivenessTone(risk.targetControlEffectiveness)}
              />
              <Field
                label={isErm ? EXCEL_FIELD.targetResidual : EXCEL_FIELD.treatedTargetResidual}
                value={risk.targetResidualScore}
                tone={scoreTone(risk.targetResidualScore, gridSize)}
              />
              <Field
                label={EXCEL_FIELD.treatedRiskScore}
                value={risk.treatedRiskScore}
                tone={treatedScoreTone(risk.treatedRiskScore, gridSize)}
              />
              <Field
                wrap
                label={EXCEL_FIELD.acceptanceRequired}
                value={risk.targetControlEffectiveness ? risk.acceptanceRequiredLabel : null}
                tone={
                  risk.targetControlEffectiveness
                    ? acceptanceRequiredTone(risk.acceptanceRequiredLabel)
                    : undefined
                }
              />
              <Field
                label={EXCEL_FIELD.finalRiskStatus}
                value={risk.finalRiskStatus === 'APPROVE' ? 'Approve' : null}
                tone={risk.finalRiskStatus === 'APPROVE' ? 'low' : undefined}
              />
            </Grid>
            {showAkAl && (
              <Box
                sx={{
                  mt: 1.25,
                  pt: 1.25,
                  borderTop: `1px solid ${brand.border}`,
                  display: 'grid',
                  gridTemplateColumns: {
                    xs: '1fr',
                    sm: 'repeat(2, minmax(0, 1fr))',
                    md: 'repeat(4, minmax(0, 1fr))',
                  },
                  gap: 1,
                  alignItems: 'start',
                }}
              >
                <Field keep label={EXCEL_FIELD.action}>
                  <Box sx={{ minHeight: 32, display: 'flex', alignItems: 'center' }}>
                    <DomainBadge
                      variant="lifecycle"
                      status={actionBadge.status}
                      label={configStatusLabel(risk.status)}
                    />
                  </Box>
                </Field>
                <Field keep well label={EXCEL_FIELD.nextReviewDate} value={isoDate(risk.nextReviewDate)} />
                <Field keep well wrap label={EXCEL_FIELD.treatmentProgress} value={progressText} />
                <Field keep well wrap label={EXCEL_FIELD.reviewAlert} value={alertText} />
              </Box>
            )}
          </SettingsCard>
        </Box>
      )}
    </Stack>
  )
}
