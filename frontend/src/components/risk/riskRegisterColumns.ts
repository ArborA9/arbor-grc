import type { ColumnDefinition } from '../../hooks/columnVisibility'
import { EXCEL_FIELD, scaledExcelField } from './excelRiskFields'

/** Client register: default tracks plus Risk Custodian; picker lists every Excel field. */
export const RISK_REGISTER_MAX_COLUMNS = 9

type Scope = 'both' | 'erm' | 'is'

type CatalogColumn = ColumnDefinition & { scope: Scope }

const W = {
  id: '7.5rem',
  title: 'minmax(6.5rem, 1fr)',
  cat: '7.25rem',
  score: '6.5rem',
  treat: '7.25rem',
  status: '9.5rem',
  owner: '7.5rem',
  date: '6.75rem',
  text: '7.5rem',
  long: '8.5rem',
  num: '5.75rem',
} as const

/**
 * Default-visible tracks first (ID through Risk Custodian), then the rest of the
 * Excel Risk Register row-5 fields. Labels drop “(auto)”; “(1-5)” only when the
 * tenant matrix is 5×5 (`scaledExcelField`). Keep “(Manual)”.
 * Not included: IS Field Dictionary-only (Framework, UAE IA Priority, Third Party,
 * Action Owner, Start Date, % Complete) and Acceptance-sheet people (CISO / Chairman).
 * AK Action is the Config status Admin picks on approve: same as Risk Status on the list.
 */
const CATALOG: CatalogColumn[] = [
  { id: 'id', label: EXCEL_FIELD.riskId, width: W.id, required: true, scope: 'both' },
  { id: 'title', label: EXCEL_FIELD.riskTitle, width: W.title, scope: 'both' },
  { id: 'category', label: EXCEL_FIELD.riskCategory, width: W.cat, scope: 'erm' },
  { id: 'inherent', label: EXCEL_FIELD.inherentScore, width: W.score, scope: 'both' },
  { id: 'residual', label: EXCEL_FIELD.residualScore, width: W.score, scope: 'both' },
  { id: 'treatment', label: EXCEL_FIELD.treatmentOption, width: W.treat, scope: 'both' },
  { id: 'status', label: EXCEL_FIELD.riskStatus, width: W.status, scope: 'both' },
  { id: 'owner', label: EXCEL_FIELD.riskOwner, width: W.owner, scope: 'both' },
  { id: 'custodian', label: EXCEL_FIELD.riskCustodian, width: W.owner, scope: 'both' },

  { id: 'dateIdentified', label: EXCEL_FIELD.dateIdentified, width: W.date, defaultVisible: false, scope: 'both' },
  { id: 'description', label: EXCEL_FIELD.riskDescription, width: W.long, defaultVisible: false, scope: 'both' },
  { id: 'observedGap', label: EXCEL_FIELD.gapObservation, width: W.long, defaultVisible: false, scope: 'both' },
  {
    id: 'referenceAttachment',
    label: EXCEL_FIELD.referenceAttachment,
    width: W.num,
    defaultVisible: false,
    scope: 'both',
  },
  { id: 'orgUnit', label: EXCEL_FIELD.division, width: W.text, defaultVisible: false, scope: 'both' },
  {
    id: 'strategicObjective',
    label: EXCEL_FIELD.strategicObjective,
    width: W.long,
    defaultVisible: false,
    scope: 'erm',
  },
  { id: 'cause', label: EXCEL_FIELD.riskCause, width: W.text, defaultVisible: false, scope: 'erm' },
  { id: 'nodeCode', label: EXCEL_FIELD.controlRef, width: W.text, defaultVisible: false, scope: 'is' },
  { id: 'controlStatus', label: EXCEL_FIELD.controlStatus, width: W.text, defaultVisible: false, scope: 'is' },
  { id: 'threats', label: EXCEL_FIELD.associatedThreat, width: W.text, defaultVisible: false, scope: 'is' },
  { id: 'likelihood', label: EXCEL_FIELD.likelihood, width: W.text, defaultVisible: false, scope: 'both' },
  { id: 'impact', label: EXCEL_FIELD.impact, width: W.text, defaultVisible: false, scope: 'both' },
  { id: 'existingControls', label: EXCEL_FIELD.existingControls, width: W.long, defaultVisible: false, scope: 'both' },
  {
    id: 'controlEffectiveness',
    label: EXCEL_FIELD.controlEffectiveness,
    width: W.text,
    defaultVisible: false,
    scope: 'both',
  },
  { id: 'riskReduction', label: EXCEL_FIELD.riskReduction, width: W.num, defaultVisible: false, scope: 'both' },
  { id: 'residualRating', label: EXCEL_FIELD.residualRating, width: W.score, defaultVisible: false, scope: 'both' },
  { id: 'withinAppetite', label: EXCEL_FIELD.withinAppetite, width: W.text, defaultVisible: false, scope: 'both' },
  {
    id: 'evaluationDecision',
    label: EXCEL_FIELD.evaluationDecision,
    width: W.long,
    defaultVisible: false,
    scope: 'both',
  },
  {
    id: 'treatmentDescription',
    label: EXCEL_FIELD.treatmentJustification,
    width: W.long,
    defaultVisible: false,
    scope: 'both',
  },
  { id: 'evidenceRequired', label: EXCEL_FIELD.evidenceRequired, width: W.long, defaultVisible: false, scope: 'both' },
  { id: 'targetDate', label: EXCEL_FIELD.targetDate, width: W.date, defaultVisible: false, scope: 'both' },
  {
    id: 'treatmentPlan',
    label: EXCEL_FIELD.treatmentActionPlan,
    width: W.long,
    defaultVisible: false,
    scope: 'both',
  },
  {
    id: 'attachedEvidence',
    label: EXCEL_FIELD.attachedEvidence,
    width: W.text,
    defaultVisible: false,
    scope: 'both',
  },
  { id: 'targetCe', label: EXCEL_FIELD.targetCe, width: W.text, defaultVisible: false, scope: 'both' },
  { id: 'targetResidual', label: EXCEL_FIELD.targetResidual, width: W.score, defaultVisible: false, scope: 'both' },
  { id: 'treatedRiskScore', label: EXCEL_FIELD.treatedRiskScore, width: W.score, defaultVisible: false, scope: 'both' },
  {
    id: 'acceptanceRequired',
    label: EXCEL_FIELD.acceptanceRequired,
    width: W.text,
    defaultVisible: false,
    scope: 'both',
  },
  { id: 'finalRiskStatus', label: EXCEL_FIELD.finalRiskStatus, width: W.score, defaultVisible: false, scope: 'both' },
  {
    id: 'treatmentProgress',
    label: EXCEL_FIELD.treatmentProgress,
    width: W.text,
    defaultVisible: false,
    scope: 'both',
  },
  { id: 'nextReviewDate', label: EXCEL_FIELD.nextReviewDate, width: W.date, defaultVisible: false, scope: 'both' },
]

export function riskRegisterColumnsForType(isErm: boolean, gridSize: number = 5): ColumnDefinition[] {
  return CATALOG.filter((c) => c.scope === 'both' || c.scope === (isErm ? 'erm' : 'is')).map((c) => {
    const { scope: _scope, ...def } = c
    if (c.id === 'attachedEvidence') {
      return { ...def, label: isErm ? EXCEL_FIELD.attachedEvidence : EXCEL_FIELD.evidenceAttached }
    }
    if (c.id === 'likelihood') return { ...def, label: scaledExcelField('likelihood', gridSize) }
    if (c.id === 'impact') return { ...def, label: scaledExcelField('impact', gridSize) }
    if (c.id === 'controlEffectiveness') {
      return { ...def, label: scaledExcelField('controlEffectiveness', gridSize) }
    }
    if (c.id === 'targetCe') {
      return {
        ...def,
        label: scaledExcelField(isErm ? 'targetCe' : 'treatmentCe', gridSize),
      }
    }
    if (c.id === 'targetResidual') {
      return { ...def, label: isErm ? EXCEL_FIELD.targetResidual : EXCEL_FIELD.treatedTargetResidual }
    }
    return def
  })
}

/** @deprecated Use `riskRegisterColumnsForType`: full Excel catalog, not the default 8. */
export const RISK_REGISTER_COLUMNS = riskRegisterColumnsForType(true)
