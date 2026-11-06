/** Client validation for Asset Register Save / Save draft (Excel Asset Register sheet). */

export type AssetFieldKey =
  | 'name'
  | 'description'
  | 'assetType'
  | 'assetSubType'
  | 'environment'
  | 'ownerUserId'
  | 'ownerDepartment'
  | 'custodian'
  | 'dataResidencyOther'
  | 'c'
  | 'i'
  | 'a'
  | 'cJust'
  | 'iJust'
  | 'aJust'

export type AssetFieldErrors = Partial<Record<AssetFieldKey, string>>

type FormLike = {
  name: string
  description: string
  assetType: string
  assetSubType: string
  environment: string
  ownerUserId: number | ''
  ownerDepartment: string
  custodianUserId: number | ''
  custodianName: string
  dataResidency: string
  dataResidencyOther: string
  c: number | ''
  i: number | ''
  a: number | ''
  cJust: string
  iJust: string
  aJust: string
}

export function isOtherDataResidency(value: string): boolean {
  return value.trim().toLowerCase() === 'other'
}

export function isCiaRating(value: number | '' | null | undefined): value is number {
  return value != null && value !== '' && value >= 1 && value <= 5
}

function firstError(
  errors: AssetFieldErrors,
  order: AssetFieldKey[],
  fallback: string,
): { ok: boolean; errors: AssetFieldErrors; firstKey: AssetFieldKey | null; reason: string } {
  const firstKey = order.find((k) => errors[k]) ?? null
  const reason = firstKey ? errors[firstKey] ?? fallback : ''
  return { ok: firstKey == null, errors, firstKey, reason }
}

/** Save draft: Asset Name only (software mid-journey persist). */
export function validateAssetDraft(form: FormLike): {
  ok: boolean
  errors: AssetFieldErrors
  firstKey: AssetFieldKey | null
  reason: string
} {
  const errors: AssetFieldErrors = {}
  if (!form.name.trim()) errors.name = 'Asset name is required to save a draft'
  return firstError(errors, ['name'], 'Fix the highlighted fields before Save draft')
}

/**
 * Full Save: identity + ownership + CIA ratings/justifications.
 * Matches Excel “start with name, type and owner, then the three CIA ratings”.
 */
export function validateAssetSave(form: FormLike): {
  ok: boolean
  errors: AssetFieldErrors
  firstKey: AssetFieldKey | null
  reason: string
} {
  const errors: AssetFieldErrors = {}
  if (!form.name.trim()) errors.name = 'Asset name is required'
  if (!form.assetType) errors.assetType = 'Asset type is required'
  if (form.assetType === 'OTHER' && !form.assetSubType.trim()) {
    errors.assetSubType = 'Asset sub-type is required when type is Other'
  }
  if (!form.environment.trim()) errors.environment = 'Environment is required'
  if (!form.description.trim()) errors.description = 'Asset description is required'
  if (form.ownerUserId === '') errors.ownerUserId = 'Asset owner is required'
  if (!form.ownerDepartment.trim()) errors.ownerDepartment = 'Owner department is required'
  if (form.custodianUserId === '' && !form.custodianName.trim()) {
    errors.custodian = 'Asset custodian is required'
  }
  if (isOtherDataResidency(form.dataResidency) && !form.dataResidencyOther.trim()) {
    errors.dataResidencyOther = 'Enter the data residency location when Other is selected'
  }
  if (!isCiaRating(form.c)) errors.c = 'Confidentiality rating is required'
  if (!isCiaRating(form.i)) errors.i = 'Integrity rating is required'
  if (!isCiaRating(form.a)) errors.a = 'Availability rating is required'
  if (isCiaRating(form.c) && !form.cJust.trim()) errors.cJust = 'Confidentiality justification is required'
  if (isCiaRating(form.i) && !form.iJust.trim()) errors.iJust = 'Integrity justification is required'
  if (isCiaRating(form.a) && !form.aJust.trim()) errors.aJust = 'Availability justification is required'

  return firstError(
    errors,
    [
      'name',
      'assetType',
      'assetSubType',
      'environment',
      'description',
      'ownerUserId',
      'ownerDepartment',
      'custodian',
      'dataResidencyOther',
      'c',
      'cJust',
      'i',
      'iJust',
      'a',
      'aJust',
    ],
    'Fix the highlighted fields before Save',
  )
}

export function focusAssetField(key: AssetFieldKey) {
  const byId = [
    `asset-field-${key}`,
    key === 'name' ? 'asset-asset-name' : null,
    key === 'description' ? 'asset-asset-description' : null,
    key === 'ownerUserId' ? 'fs-asset-owner' : null,
    key === 'environment' ? 'fs-environment' : null,
    key === 'assetType' ? 'fs-asset-type' : null,
    key === 'assetSubType' ? 'fs-asset-sub-type' : null,
    key === 'custodian' ? 'fs-asset-custodian' : null,
    key === 'dataResidencyOther' ? 'asset-residency-location' : null,
    key === 'dataResidencyOther' ? 'fs-data-residency' : null,
    key === 'c' ? 'fs-confidentiality-rating' : null,
    key === 'i' ? 'fs-integrity-rating' : null,
    key === 'a' ? 'fs-availability-rating' : null,
    key === 'cJust' ? 'asset-confidentiality-justification' : null,
    key === 'iJust' ? 'asset-integrity-justification' : null,
    key === 'aJust' ? 'asset-availability-justification' : null,
  ].filter(Boolean) as string[]
  for (const id of byId) {
    const el = document.getElementById(id)
    if (el instanceof HTMLElement) {
      el.focus()
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      return
    }
  }
}
