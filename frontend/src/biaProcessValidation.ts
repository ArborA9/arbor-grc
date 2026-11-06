/** Client validation for BIA process Save / Save draft / MOR numerics. */

import { assetsComplete } from './components/bcms/BiaAssetTypeRows'
import type { BiaAssetDependency, BiaAssetTypeLookup, BiaPersonRef, BiaThirdPartyContact } from './api'

export const BIA_RECOVERY_OTHER = 'Other (describe in comments)'
export const BIA_SITE_OTHER = 'Other'

export type BiaProcessFieldKey =
  | 'processCode'
  | 'processName'
  | 'ownerUserId'
  | 'frequency'
  | 'description'
  | 'flowTrigger'
  | 'flowSteps'
  | 'flowOutput'
  | 'depStaff'
  | 'depPremises'
  | 'depIt'
  | 'depRecords'
  | 'thirdPartyActivities'
  | 'supplierName'
  | 'impact'
  | 'minStaff'
  | 'recoveryStrategy'
  | 'recoveryComments'
  | 'siteOther'
  | 'morResources'
  | 'thirdPartyContacts'

export type BiaFieldErrors = Partial<Record<BiaProcessFieldKey, string>>

const CATEGORIES = ['STRATEGIC', 'FINANCIAL', 'OPERATIONAL', 'COMPLIANCE'] as const
const BANDS = ['H0_4', 'H4_8', 'H8_24', 'H24_48', 'H48_72'] as const

export function parseWholeNumber(raw: string): number | null {
  const t = raw.trim()
  if (t === '') return null
  if (!/^-?\d+$/.test(t)) return NaN
  return Number(t)
}

export function laptopsError(raw: string): string | null {
  if (raw.trim() === '') return null
  const n = parseWholeNumber(raw)
  if (n == null || Number.isNaN(n)) return 'Enter a whole number 0 or greater'
  if (n < 0) return 'Enter a whole number 0 or greater'
  return null
}

export function impactAllPresent(ratings: Record<string, number | ''>): boolean {
  for (const cat of CATEGORIES) {
    for (const band of BANDS) {
      if (typeof ratings[`${cat}|${band}`] !== 'number') return false
    }
  }
  return true
}

function peopleOk(people: BiaPersonRef[] | undefined) {
  return (people ?? []).some((p) => p.displayName?.trim())
}

function ynOk(v: string) {
  return v === 'Yes' || v === 'No'
}

function siteOk(v: string) {
  return v === 'Primary Site' || v === 'Secondary Site' || v === 'Other'
}

function personKey(p: BiaPersonRef) {
  if (p.source === 'ENTRA' && p.directoryId) return `entra:${p.directoryId}`
  if (p.source === 'ARBOR' && p.userId != null) return `arbor:${p.userId}`
  return `email:${(p.email ?? p.displayName).toLowerCase()}`
}

function staffSubsetOfDependency(minStaff: BiaPersonRef[], depStaff: BiaPersonRef[]) {
  if (minStaff.length === 0) return true
  const allowed = new Set(depStaff.map(personKey))
  return minStaff.every((p) => allowed.has(personKey(p)))
}

function contactsOk(rows: BiaThirdPartyContact[]) {
  if (rows.length === 0) return false
  return rows.every((c) => c.name.trim() && c.email.trim())
}

function resourcesOk(mor: MorLike) {
  const laptop = laptopsError(mor.laptops)
  return (
    ynOk(mor.internetConnectivity)
    && ynOk(mor.intranetConnectivity)
    && ynOk(mor.vpnAccess)
    && siteOk(mor.siteType)
    && ynOk(mor.m365)
    && mor.softwareApplications.some((n) => n.trim())
    && mor.laptops.trim() !== ''
    && !laptop
  )
}

type FormLike = {
  processCode: string
  processName: string
  description?: string | null
  flowTrigger?: string | null
  flowSteps?: string | null
  flowOutput?: string | null
  ownerUserId?: number | null
  frequency?: string | null
  depStaffPeople?: BiaPersonRef[] | null
  depPremises?: string | null
  depAssets?: BiaAssetDependency[] | null
  depRecords?: string | null
  thirdPartyActivities?: string | null
  supplierName?: string | null
}

export type MorLike = {
  minStaffPeople: BiaPersonRef[]
  recoveryStrategies: string[]
  recoveryComments: string
  internetConnectivity: string
  intranetConnectivity: string
  vpnAccess: string
  siteType: string
  siteOther: string
  softwareApplications: string[]
  laptops: string
  m365: string
  thirdPartyContacts: BiaThirdPartyContact[]
}

/** Save draft: code + name; reject garbage laptop counts if typed. */
export function validateBiaDraft(
  form: FormLike,
  mor: MorLike,
): { ok: boolean; errors: BiaFieldErrors; firstKey: BiaProcessFieldKey | null; reason: string } {
  const errors: BiaFieldErrors = {}
  if (!form.processCode.trim()) errors.processCode = 'Process code is required'
  if (!form.processName.trim()) errors.processName = 'Business process name is required'
  const lap = laptopsError(mor.laptops)
  if (lap) errors.morResources = lap
  const keys = Object.keys(errors) as BiaProcessFieldKey[]
  const firstKey = keys[0] ?? null
  let reason = ''
  if (errors.processCode || errors.processName) reason = 'Process code and name are required for a draft'
  else if (errors.morResources) reason = 'Fix invalid MOR numbers before saving a draft'
  return { ok: keys.length === 0, errors, firstKey, reason }
}

export function validateBiaFullSave(
  form: FormLike,
  ratings: Record<string, number | ''>,
  mor: MorLike,
  morRequired: boolean,
  tpInvolved: boolean,
  assetTypes?: BiaAssetTypeLookup[] | null,
): { ok: boolean; errors: BiaFieldErrors; firstKey: BiaProcessFieldKey | null; reason: string } {
  const errors: BiaFieldErrors = {}
  if (!form.processCode.trim()) errors.processCode = 'Process code is required'
  if (!form.processName.trim()) errors.processName = 'Business process name is required'
  if (form.ownerUserId == null) errors.ownerUserId = 'Process owner is required'
  if (!form.frequency?.trim()) errors.frequency = 'Process frequency is required'
  if (!form.description?.trim()) errors.description = 'Process description is required'
  if (!form.flowTrigger?.trim()) errors.flowTrigger = 'Trigger / inflow is required'
  if (!form.flowSteps?.trim()) errors.flowSteps = 'Key steps are required'
  if (!form.flowOutput?.trim()) errors.flowOutput = 'Output / outflow is required'
  if (!peopleOk(form.depStaffPeople ?? [])) errors.depStaff = 'Select at least one person'
  if (!form.depPremises?.trim()) errors.depPremises = 'Premises / facilities is required'
  if (!assetsComplete(form.depAssets ?? [], assetTypes)) errors.depIt = 'Add at least one asset type with details'
  if (!form.depRecords?.trim()) errors.depRecords = 'Vital records is required'
  if (tpInvolved) {
    if (!form.thirdPartyActivities?.trim()) {
      errors.thirdPartyActivities = 'Third party activities are required when third party is involved'
    }
    if (!form.supplierName?.trim()) errors.supplierName = 'Supplier name is required when third party is involved'
  }
  if (!impactAllPresent(ratings)) {
    errors.impact = 'Rate all 20 impact cells (1-5)'
  }
  if (morRequired) {
    if (!peopleOk(mor.minStaffPeople)) errors.minStaff = 'Select at least one person'
    else if (!staffSubsetOfDependency(mor.minStaffPeople, form.depStaffPeople ?? [])) {
      errors.minStaff = 'Min staff must be chosen from Dependency Minimum Staff on Identity'
    }
    if (mor.recoveryStrategies.length === 0) errors.recoveryStrategy = 'Select at least one recovery strategy'
    if (mor.recoveryStrategies.includes(BIA_RECOVERY_OTHER) && !mor.recoveryComments.trim()) {
      errors.recoveryComments = 'Comments are required when Other is selected'
    }
    if (mor.siteType === BIA_SITE_OTHER && !mor.siteOther.trim()) {
      errors.siteOther = 'Other site is required when Other is selected'
    }
    if (!resourcesOk(mor)) errors.morResources = 'Complete all MOR resource fields'
    if (tpInvolved && !contactsOk(mor.thirdPartyContacts)) {
      errors.thirdPartyContacts = 'Add name and email for each third party contact'
    }
  } else {
    const lap = laptopsError(mor.laptops)
    if (lap) errors.morResources = lap
  }

  const order: BiaProcessFieldKey[] = [
    'processCode',
    'processName',
    'ownerUserId',
    'frequency',
    'description',
    'flowTrigger',
    'flowSteps',
    'flowOutput',
    'depStaff',
    'depPremises',
    'depIt',
    'depRecords',
    'thirdPartyActivities',
    'supplierName',
    'impact',
    'minStaff',
    'recoveryStrategy',
    'recoveryComments',
    'siteOther',
    'morResources',
    'thirdPartyContacts',
  ]
  const firstKey = order.find((k) => errors[k]) ?? null
  const reason = firstKey
    ? errors[firstKey] ?? 'Fix the highlighted fields before Save'
    : ''
  return { ok: firstKey == null, errors, firstKey, reason }
}

export function focusBiaField(key: BiaProcessFieldKey) {
  const el = document.getElementById(`bia-field-${key}`)
  if (el instanceof HTMLElement) {
    el.focus()
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
  if (key === 'impact') {
    document.getElementById('bia-impact')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
  if (key === 'morResources' || key === 'minStaff' || key === 'recoveryStrategy' || key === 'recoveryComments' || key === 'siteOther' || key === 'thirdPartyContacts') {
    document.getElementById('bia-mor')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
  if (key === 'depStaff' || key === 'depIt') {
    document.getElementById('bia-identity')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}
