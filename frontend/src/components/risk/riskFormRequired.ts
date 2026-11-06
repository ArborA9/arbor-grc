import { fieldId } from '../ui/CompactTextField'
import { EXCEL_FIELD, ERM_RISK_CAUSE_OTHER } from './excelRiskFields'
import type { TreatmentOption } from '../../api'

export const requiredFieldMessage = (label: string) => `${label} is required`

export const RISK_INPUT_ID = {
  dateIdentified: fieldId(EXCEL_FIELD.dateIdentified),
  title: fieldId(EXCEL_FIELD.riskTitle),
  description: fieldId(EXCEL_FIELD.riskDescription),
  observedGap: fieldId(EXCEL_FIELD.gapObservation),
  category: fieldId(EXCEL_FIELD.riskCategory, 'fs'),
  categoryOther: fieldId('Specify category'),
  division: fieldId(EXCEL_FIELD.division, 'fs'),
  strategicObjective: fieldId(EXCEL_FIELD.strategicObjective),
  cause: fieldId(EXCEL_FIELD.riskCause, 'fs'),
  causeOther: fieldId('Specify cause'),
  associatedThreat: fieldId(EXCEL_FIELD.associatedThreat, 'fs'),
  riskOwner: fieldId(EXCEL_FIELD.riskOwner, 'fs'),
  residualScore: fieldId(EXCEL_FIELD.residualScore),
  autoCloseJustification: fieldId('Auto-close override justification'),
  treatmentJustification: fieldId(EXCEL_FIELD.treatmentJustification),
} as const

const FOCUS_ORDER: string[] = [
  RISK_INPUT_ID.dateIdentified,
  RISK_INPUT_ID.title,
  RISK_INPUT_ID.description,
  RISK_INPUT_ID.observedGap,
  RISK_INPUT_ID.category,
  RISK_INPUT_ID.categoryOther,
  RISK_INPUT_ID.division,
  RISK_INPUT_ID.strategicObjective,
  RISK_INPUT_ID.cause,
  RISK_INPUT_ID.causeOther,
  RISK_INPUT_ID.associatedThreat,
  RISK_INPUT_ID.riskOwner,
  RISK_INPUT_ID.residualScore,
  RISK_INPUT_ID.autoCloseJustification,
  RISK_INPUT_ID.treatmentJustification,
]

export function collectRiskRequiredErrors(input: {
  isErm: boolean
  dateIdentified: string
  title: string
  description: string
  observedGap: string
  category: string
  categoryOther: string
  orgUnitId: number | ''
  strategicObjective: string
  cause: string
  causeOther: string
  threats: string
  riskOwnerUserId: number | ''
  residualScoringMode: string
  manualResidualScore: string
  willAutoClose: boolean
  overrideAutoClose: boolean
  autoCloseJustification: string
  treatmentOption: TreatmentOption
  treatmentJustification: string
}): Record<string, string> {
  const errors: Record<string, string> = {}
  const add = (id: string, label: string) => {
    errors[id] = requiredFieldMessage(label)
  }
  if (!input.dateIdentified.trim()) add(RISK_INPUT_ID.dateIdentified, EXCEL_FIELD.dateIdentified)
  if (!input.title.trim()) add(RISK_INPUT_ID.title, EXCEL_FIELD.riskTitle)
  if (!input.description.trim()) add(RISK_INPUT_ID.description, EXCEL_FIELD.riskDescription)
  if (!input.observedGap.trim()) add(RISK_INPUT_ID.observedGap, EXCEL_FIELD.gapObservation)
  if (input.isErm && !input.category.trim()) add(RISK_INPUT_ID.category, EXCEL_FIELD.riskCategory)
  if (input.isErm && input.category === 'OTHER' && !input.categoryOther.trim()) {
    add(RISK_INPUT_ID.categoryOther, 'Specify category')
  }
  if (input.orgUnitId === '') add(RISK_INPUT_ID.division, EXCEL_FIELD.division)
  if (input.isErm && !input.strategicObjective.trim()) {
    add(RISK_INPUT_ID.strategicObjective, EXCEL_FIELD.strategicObjective)
  }
  if (input.isErm && !input.cause.trim()) add(RISK_INPUT_ID.cause, EXCEL_FIELD.riskCause)
  if (input.isErm && input.cause === ERM_RISK_CAUSE_OTHER && !input.causeOther.trim()) {
    add(RISK_INPUT_ID.causeOther, 'Specify cause')
  }
  if (!input.isErm && !input.threats.trim()) {
    add(RISK_INPUT_ID.associatedThreat, EXCEL_FIELD.associatedThreat)
  }
  if (input.riskOwnerUserId === '') add(RISK_INPUT_ID.riskOwner, EXCEL_FIELD.riskOwner)
  if (input.residualScoringMode === 'MANUAL_PROBABILITY_IMPACT' && !input.manualResidualScore.trim()) {
    add(RISK_INPUT_ID.residualScore, EXCEL_FIELD.residualScore)
  }
  if (input.willAutoClose && input.overrideAutoClose && !input.autoCloseJustification.trim()) {
    add(RISK_INPUT_ID.autoCloseJustification, 'Auto-close override justification')
  }
  if (!input.treatmentJustification.trim()) {
    add(RISK_INPUT_ID.treatmentJustification, EXCEL_FIELD.treatmentJustification)
  }
  return errors
}

export function focusFirstRiskError(errors: Record<string, string>): void {
  const id = FOCUS_ORDER.find((key) => errors[key])
  if (!id) return
  const el = document.getElementById(id)
  if (!el) return
  el.scrollIntoView({ block: 'center', behavior: 'smooth' })
  if (el instanceof HTMLElement) {
    el.focus()
  }
}
