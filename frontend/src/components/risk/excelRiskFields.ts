import { createElement, type CSSProperties, type ReactNode } from 'react'
import type { RiskLevel, RiskStatus, TreatmentOption } from '../../api'

/** Who fills the Excel Risk Register column. Custodian in the sheet = Contributor here. */
export type FieldFiller = 'Admin' | 'Contributor' | 'Risk Owner' | 'Auto'

const FILLER_TONE: Record<FieldFiller, { bg: string; fg: string; border: string }> = {
  Admin: { bg: 'rgba(31,56,100,0.08)', fg: '#1F3864', border: 'rgba(31,56,100,0.18)' },
  Contributor: { bg: 'rgba(13,148,136,0.12)', fg: '#0F766E', border: 'rgba(13,148,136,0.22)' },
  'Risk Owner': { bg: 'rgba(183,121,31,0.12)', fg: '#8A5A12', border: 'rgba(183,121,31,0.28)' },
  Auto: { bg: 'rgba(110,123,139,0.12)', fg: '#4B5563', border: 'rgba(110,123,139,0.22)' },
}

export function FieldFillerChip({ filler }: { filler: FieldFiller }) {
  const t = FILLER_TONE[filler]
  const style: CSSProperties = {
    display: 'inline-flex',
    alignItems: 'center',
    flexShrink: 0,
    padding: '1px 5px',
    borderRadius: 4,
    fontSize: '0.5625rem',
    fontWeight: 700,
    letterSpacing: '0.04em',
    textTransform: 'uppercase',
    lineHeight: 1.3,
    background: t.bg,
    color: t.fg,
    border: `1px solid ${t.border}`,
  }
  return createElement('span', { style }, filler)
}

export function fillerHint(filler: FieldFiller): ReactNode {
  return createElement(FieldFillerChip, { filler })
}

/** Config sheet: ERM Risk Cause / Source. */
export const ERM_RISK_CAUSES = [
  'Economic Conditions',
  'Regulatory Change',
  'Technology Failure',
  'Supplier Failure',
  'Talent Shortage',
  'Process Breakdown',
  'Project Delivery',
  'Fraud / Corruption',
  'Natural Hazard',
  'Public Perception',
  'Contractual Dispute',
  'Capacity Constraint',
] as const

export const ERM_RISK_CAUSE_OTHER = 'Other'

export function isListedErmCause(value: string | null | undefined): boolean {
  return !!value && (ERM_RISK_CAUSES as readonly string[]).includes(value)
}

export function ermCauseToForm(stored: string | null | undefined): {
  cause: string
  causeOther: string
} {
  const v = stored?.trim() ?? ''
  if (!v) return { cause: '', causeOther: '' }
  if (isListedErmCause(v)) return { cause: v, causeOther: '' }
  if (v.toLowerCase() === ERM_RISK_CAUSE_OTHER.toLowerCase()) {
    return { cause: ERM_RISK_CAUSE_OTHER, causeOther: '' }
  }
  return { cause: ERM_RISK_CAUSE_OTHER, causeOther: v }
}

export function ermCauseFromForm(cause: string, causeOther: string): string | null {
  if (cause === ERM_RISK_CAUSE_OTHER) {
    const other = causeOther.trim()
    return other || null
  }
  const listed = cause.trim()
  return listed || null
}

/** Config sheet: IS Associated Threat. */
export const IS_ASSOCIATED_THREATS = [
  'External Attack',
  'Ransomware',
  'Phishing',
  'Credential Theft',
  'Insider Misuse',
  'Data Leakage',
  'Supply Chain',
  'Physical Intrusion',
  'Environmental Failure',
  'Operational Failure',
  'Regulatory Non-Compliance',
  'Device Loss',
] as const

export const EXCEL_TREAT_LABEL: Record<TreatmentOption, string> = {
  TREAT: 'Treat (Mitigate)',
  TOLERATE: 'Tolerate (Accept)',
  TERMINATE: 'Terminate (Avoid)',
  TRANSFER: 'Transfer',
}

/** Control effectiveness names: no 1–5 rank (rank is the tenant axis slot). */
export const CE_EXCEL_LABEL: Record<RiskLevel, string> = {
  VERY_LOW: 'Ineffective',
  LOW: 'Weak',
  MEDIUM: 'Moderate',
  HIGH: 'Strong',
  VERY_HIGH: 'Very strong',
}

/** Likelihood / Impact names: no 1–5 rank (rank is the tenant axis slot). */
export const LIKELIHOOD_EXCEL_LABEL: Record<RiskLevel, string> = {
  VERY_LOW: 'Very Low',
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  VERY_HIGH: 'Very High',
}

export function evaluationDecisionLabel(decision: string | null | undefined): string {
  if (decision === 'AUTO_CLOSE' || decision === 'AUTO_CLOSED') return 'Auto-closed - no action'
  if (decision === 'MONITOR') return 'Within appetite - monitor only'
  if (decision === 'TREATMENT_REQUIRED') return 'TREATMENT REQUIRED'
  return decision ? decision.replace(/_/g, ' ') : '-'
}

export function withinAppetiteLabel(within: boolean | null | undefined): string {
  if (within == null) return '-'
  return within ? 'Within Appetite' : 'EXCEEDS APPETITE'
}

export function acceptanceDecisionMark(
  decision: string | null | undefined,
  waiting: boolean,
  inferredApproved = false,
): string {
  const d = (decision ?? '').toUpperCase()
  if (d === 'REJECTED') return 'Rejected'
  if (d === 'SENT_BACK') return 'Send Back'
  if (d === 'APPROVED' || d === 'ACCEPTED' || inferredApproved) return 'Approved'
  if (waiting) return 'Waiting'
  return '-'
}

/** Excel Risk Acceptance O/P/Q: person + current mark (empty / Waiting / Approved / Rejected / Send Back). */
export function acceptanceApproverLine(
  name: string | null | undefined,
  decision: string | null | undefined,
  waiting: boolean,
  inferredApproved = false,
): string {
  const who = name?.trim() ? name.trim() : '-'
  return `${who} · ${acceptanceDecisionMark(decision, waiting, inferredApproved)}`
}

export function acceptanceApproverDisplay(
  name: string | null | undefined,
  decision: string | null | undefined,
  waiting: boolean,
  inferredApproved = false,
  comments?: string | null,
): string {
  const line = acceptanceApproverLine(name, decision, waiting, inferredApproved)
  const extra = comments?.trim()
  return extra ? `${line}: ${extra}` : line
}

function isAcceptanceAdminTurn(status: string): boolean {
  return status === 'PENDING_ADMIN' || status === 'PENDING_ADMIN_REVIEW'
}

export function acceptanceChainState(status: string | null | undefined, hasLevel3 = true) {
  const st = status ?? ''
  const adminWaiting = isAcceptanceAdminTurn(st)
  return {
    ownerWaiting: st === 'PENDING_RISK_OWNER',
    cisoWaiting: st === 'PENDING_APPROVER_2',
    chairWaiting: hasLevel3 && st === 'PENDING_APPROVER_3',
    adminWaiting,
    ownerDone:
      st === 'PENDING_APPROVER_2' || st === 'PENDING_APPROVER_3' || adminWaiting,
    cisoDone: st === 'PENDING_APPROVER_3' || adminWaiting,
    chairDone: hasLevel3 && adminWaiting,
  }
}

export function acceptanceLevel2Label(riskType?: string | null): string {
  if (riskType === 'ERM') return 'ERM Head'
  if (riskType === 'PRIVACY_RISK') return 'Data Privacy Officer'
  return 'CISO'
}

export function acceptanceLevel3Label(): string {
  return 'Level 3 approver'
}

export function acceptanceTurnLabel(
  status: string | null | undefined,
  riskType?: string | null,
): string {
  switch (status) {
    case 'PENDING_RISK_OWNER':
      return 'Waiting on Risk Owner'
    case 'PENDING_APPROVER_2':
      return `Waiting on ${acceptanceLevel2Label(riskType)}`
    case 'PENDING_APPROVER_3':
      return 'Waiting on Level 3 approver'
    case 'PENDING_ADMIN':
    case 'PENDING_ADMIN_REVIEW':
      return 'Waiting on Admin'
    case 'ACCEPTED':
      return 'Accepted'
    case 'REJECTED':
      return 'Returned to Contributor'
    default:
      return status ? status.replace(/_/g, ' ') : 'Risk Acceptance'
  }
}

export function acceptanceRequiredLabel(
  treatment: TreatmentOption | null | undefined,
  targetResidual: number | null,
  appetiteMax: number,
): string {
  if (treatment === 'TERMINATE' || treatment === 'TRANSFER') return 'No'
  if (treatment === 'TOLERATE') return targetResidual == null ? '-' : 'YES - Tolerate path'
  if (targetResidual == null) return '-'
  if (targetResidual > appetiteMax) return 'YES - plan leaves it above appetite'
  return 'No - plan reaches appetite'
}

/** Admin Actions / live preview: name the score in full (UAT 20-Sep). */
export function acceptanceRequiredDisplay(
  label: string,
  treatedResidual: number | null | undefined,
): string {
  if (!label || label === '-') return label || '-'
  if (treatedResidual == null || !Number.isFinite(treatedResidual)) return label
  return `${label} · treated residual risk score - ${treatedResidual}`
}

/** Excel: Terminate / Transfer never open the Risk Acceptance sheet. */
export function acceptanceSheetRequired(
  treatment: TreatmentOption | null | undefined,
  targetResidual: number | null,
  appetiteMax: number,
): boolean {
  if (treatment === 'TERMINATE' || treatment === 'TRANSFER' || treatment == null) return false
  if (treatment === 'TOLERATE') return targetResidual != null
  if (targetResidual == null) return false
  return targetResidual > appetiteMax
}

/** Visible Risk Register headers only: blank spacer columns are ignored. */
export const ERM_CREATE_HEADERS = [
  'Risk ID',
  'Date Identified',
  'Risk Title',
  'Risk Description',
  'Gap / Observation',
  'Input field to upload the reference attachment',
  'Risk Category',
  'Division / Department',
  'Strategic Objective (Manual)',
  'Risk Cause / Source',
  'Risk Owner',
  'Risk Custodian',
  'Likelihood (1-5)',
  'Impact (1-5)',
  'Inherent Risk Score (auto)',
  'Existing Controls',
  'Control Effectiveness (1-5)',
  'Risk Reduction % (auto)',
  'Residual Risk Score (auto)',
  'Residual Rating (auto)',
  'Within Appetite? (auto)',
  'Evaluation Decision (auto)',
  'Treatment Option (4T)',
  'Treatment Action Plan',
  'Evidence Required',
  'Target Date',
] as const

export const IS_CREATE_HEADERS = [
  'Risk ID',
  'Date Identified',
  'Risk Title',
  'Risk Description',
  'Gap / Observation',
  'Input field to upload the reference attachment',
  'Division / Department',
  'Control Ref',
  'Control Status',
  'Associated Threat',
  'Risk Owner',
  'Risk Custodian',
  'Likelihood (1-5)',
  'Impact (1-5)',
  'Inherent Risk Score (auto)',
  'Existing Controls',
  'Control Effectiveness (1-5)',
  'Risk Reduction % (auto)',
  'Residual Risk Score (auto)',
  'Residual Rating (auto)',
  'Within Appetite? (auto)',
  'Evaluation Decision (auto)',
  'Treatment Option (4T)',
  'Treatment Action Plan',
  'Evidence Required',
  'Target Date',
] as const

export const ERM_CONTRIBUTOR_HEADERS = ['Treatment Action Plan Detail', 'Attached Evidence'] as const
export const IS_CONTRIBUTOR_HEADERS = ['Treatment Action Plan Detail', 'Evidence Attached'] as const

export const POST_SUBMIT_HEADERS = [
  'Target Control Effectiveness (1-5)',
  'Target Residual Score (auto)',
  'Treated Risk Score',
  'Acceptance Required? (auto)',
  'Final Risk Status',
] as const

export const POST_SUBMIT_STATUSES = ['SUBMITTED', 'UNDER_VALIDATION', 'PENDING_ACCEPTANCE', 'MITIGATING'] as const

/** AF–AJ (and after Approve, AK/AL): not on DRAFT / ASSIGNED / RE-OPENED. */
export function showPostSubmitFields(
  status: string | null | undefined,
  finalRiskStatus?: string | null,
): boolean {
  if (status == null || ['DRAFT', 'ASSIGNED', 'RE_OPENED', 'CANCELLED'].includes(status)) return false
  // Create-time AUTO CLOSED never submitted: no AF–AJ / AK / AL yet.
  if (status === 'AUTO_CLOSED' && finalRiskStatus !== 'APPROVE') return false
  return true
}

/** After Admin Approve (or Accept). No second treat-until-close workflow. */
export function isExcelPostApproveStatus(status: string | null | undefined): boolean {
  return (
    status != null &&
    [
      'MITIGATING',
      'OVERDUE',
      'AUTO_CLOSED',
      'AVOIDED',
      'TRANSFERRED',
      'CLOSED',
      'ACCEPTED',
      'TOLERATED',
    ].includes(status)
  )
}

/** True after Approve (AJ) or after the Accept path lands on a finish status. Not create-time AUTO CLOSED. */
export function isExcelFinishedRow(
  status: string | null | undefined,
  finalRiskStatus?: string | null,
): boolean {
  if (finalRiskStatus === 'APPROVE') return true
  if (status === 'AUTO_CLOSED') return false
  return isExcelPostApproveStatus(status)
}

export function configStatusLabel(status: string | null | undefined): string {
  if (!status) return '-'
  return EXCEL_CONFIG_RISK_STATUSES.find((s) => s.value === status)?.label ?? status.replace(/_/g, ' ')
}

export function treatmentProgressLabel(opts: {
  startDate?: string | null
  targetDate?: string | null
  status?: string | null
  today?: string
}): string {
  const today = opts.today ?? new Date().toISOString().slice(0, 10)
  if (opts.status === 'COMPLETE' || opts.status === 'COMPLETED') return 'Complete'
  if (!opts.targetDate) return '-'
  if (opts.targetDate < today) return 'OVERDUE'
  if (opts.startDate && opts.startDate > today) return 'Not Started'
  return 'On Track'
}

/** Config sheet col F: Risk Status (system-set). Same order on ERM and IS. */
export const EXCEL_CONFIG_RISK_STATUSES: { value: RiskStatus; label: string }[] = [
  { value: 'DRAFT', label: 'DRAFT' },
  { value: 'ASSIGNED', label: 'ASSIGNED' },
  { value: 'SUBMITTED', label: 'SUBMITTED' },
  { value: 'UNDER_VALIDATION', label: 'UNDER VALIDATION' },
  { value: 'AUTO_CLOSED', label: 'AUTO CLOSED' },
  { value: 'MITIGATING', label: 'MITIGATING' },
  { value: 'PENDING_ACCEPTANCE', label: 'PENDING ACCEPTANCE' },
  { value: 'PENDING_ADMIN_REVIEW', label: 'PENDING ADMIN REVIEW' },
  { value: 'ACCEPTED', label: 'ACCEPTED' },
  { value: 'AVOIDED', label: 'AVOIDED' },
  { value: 'TRANSFERRED', label: 'TRANSFERRED' },
  { value: 'CLOSED', label: 'CLOSED' },
  { value: 'OVERDUE', label: 'OVERDUE' },
  { value: 'RE_OPENED', label: 'RE-OPENED' },
]

/** Admin finish picker when Acceptance Required is No. Send Back is RE_OPENED. Risk Accepted is only after the acceptance sheet. */
export const ADMIN_FINISH_ACTIONS: { value: RiskStatus; label: string }[] = [
  { value: 'CLOSED', label: 'Risk Treated & Close' },
  { value: 'AVOIDED', label: 'Risk Avoided' },
  { value: 'TRANSFERRED', label: 'Risk Transferred' },
  { value: 'RE_OPENED', label: 'Send Back' },
]

/** Suggested Action when Admin Approves and acceptance is not required. */
export function defaultApproveAction(option: TreatmentOption | null | undefined): RiskStatus {
  if (option === 'TERMINATE') return 'AVOIDED'
  if (option === 'TRANSFER') return 'TRANSFERRED'
  return 'CLOSED'
}

export function reviewAlertLabel(nextReviewDate?: string | null, today?: string): string {
  if (!nextReviewDate) return '-'
  const now = today ?? new Date().toISOString().slice(0, 10)
  if (nextReviewDate < now) return 'REVIEW OVERDUE'
  const due = new Date(nextReviewDate)
  const t = new Date(now)
  const days = Math.round((due.getTime() - t.getTime()) / 86400000)
  if (days <= 30) return 'Due within 30 days'
  return 'OK'
}

/** Date picker: calendar date only: do not stretch to the grid cell. */
export const EXCEL_DATE_FIELD_WIDTH = 148

export const EXCEL_FIELD = {
  riskId: 'Risk ID',
  dateIdentified: 'Date Identified',
  riskTitle: 'Risk Title',
  riskDescription: 'Risk Description',
  gapObservation: 'Gap / Observation',
  referenceAttachment: 'Reference attachments',
  riskCategory: 'Risk Category',
  division: 'Division / Department',
  strategicObjective: 'Strategic Objective',
  riskCause: 'Risk Cause / Source',
  framework: 'Framework',
  controlRef: 'Control Ref',
  controlStatus: 'Control Status',
  uaeIaPriority: 'UAE IA Priority',
  associatedThreat: 'Associated Threat',
  thirdParty: 'Third Party (if any)',
  riskOwner: 'Risk Owner',
  ciso: 'CISO',
  chairman: 'Chairman',
  ermHead: 'ERM Head',
  level3Approver: 'Level 3 approver',
  riskCustodian: 'Risk Custodian',
  likelihood: 'Likelihood',
  impact: 'Impact',
  inherentScore: 'Inherent Risk Score',
  existingControls: 'Existing Controls',
  controlEffectiveness: 'Control Effectiveness',
  riskReduction: 'Risk Reduction %',
  residualScore: 'Residual Risk Score',
  residualRating: 'Residual Rating',
  withinAppetite: 'Within Appetite?',
  evaluationDecision: 'Evaluation Decision',
  treatmentOption: 'Treatment Option',
  treatmentJustification: 'Treatment Action Plan',
  evidenceRequired: 'Evidence Required',
  actionOwner: 'Action Owner',
  startDate: 'Start Date',
  targetDate: 'Target Date',
  treatmentActionPlan: 'Treatment Action Plan Detail',
  attachedEvidence: 'Attached Evidence',
  compensatingControls: 'Compensating Controls',
  compensatingControlsMandatory: 'Compensating Controls',
  acceptanceId: 'Acceptance ID',
  cycle: 'Cycle',
  riskTitleRo: 'Risk Title (read only)',
  riskCategoryRo: 'Risk Category (read only)',
  riskValue: 'Risk Value',
  withinAppetiteRo: 'Within Appetite? (read only)',
  justificationForAccepting: 'Justification for Accepting',
  businessRationale: 'Other Details',
  acceptanceReviewDate: 'Acceptance Review Date',
  targetCe: 'Target Control Effectiveness',
  treatmentCe: 'Treatment Control Effectiveness',
  targetResidual: 'Target Residual Score',
  treatedRiskScore: 'Treated Risk Score',
  treatedTargetResidual: 'Treated Target Residual Score',
  acceptanceRequired: 'Acceptance Required?',
  finalRiskStatus: 'Final Risk Status',
  action: 'Action',
  treatmentProgress: 'Treatment Progress',
  riskStatus: 'Risk Status',
  nextReviewDate: 'Next Review Date',
  reviewAlert: 'Review Alert',
  evidenceAttached: 'Evidence Attached',
} as const

/** Excel 5×5 headers keep “(1-5)”. 3×3 / 4×4 omit it so the title matches the dropdown. */
export function excelScaleHeader(base: string, gridSize: number | null | undefined): string {
  return Number(gridSize) === 5 ? `${base} (1-5)` : base
}

export type ScaledScoreField =
  | 'likelihood'
  | 'impact'
  | 'controlEffectiveness'
  | 'targetCe'
  | 'treatmentCe'

export function scaledExcelField(key: ScaledScoreField, gridSize?: number | null): string {
  return excelScaleHeader(EXCEL_FIELD[key], gridSize)
}

export const REFERENCE_ATTACHMENT_RULES = 'PDF, Word, Excel, images · max 10MB each'

export function gapImplementationLabel(status: string | null | undefined): string {
  switch (status) {
    case 'FULLY_IMPLEMENTED':
      return 'Fully implemented'
    case 'PARTIALLY_IMPLEMENTED':
      return 'Partially implemented'
    case 'PLANNED':
      return 'Planned'
    case 'NOT_IMPLEMENTED':
      return 'Not implemented'
    default:
      return ''
  }
}

/** IS Control Status: Gap effective implementation for the selected control. */
export function excelControlStatusLabel(
  nodeId: number | '' | null | undefined,
  status: {
    found: boolean
    effectiveStatus?: string | null
    finalImplementationStatus?: string | null
    implementationStatus?: string | null
  } | null,
): string {
  if (nodeId == null || nodeId === '') return '-'
  if (!status?.found) return 'No assessment'
  return (
    gapImplementationLabel(
      status.effectiveStatus ?? status.finalImplementationStatus ?? status.implementationStatus,
    ) || '-'
  )
}

export function isIdentificationEvidence(stage: string | null | undefined): boolean {
  return (stage ?? '').toUpperCase() === 'IDENTIFICATION'
}
