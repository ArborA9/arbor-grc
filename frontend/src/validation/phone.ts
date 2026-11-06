/** Country dial codes for SPOC mobile. Lengths come from libphonenumber; UAE is exactly 9. */
import {
  getExampleNumber,
  type CountryCode,
} from 'libphonenumber-js'
import examples from 'libphonenumber-js/mobile/examples'

export const PHONE_COUNTRY_OPTIONS = [
  { dial: '+971', label: 'UAE (+971)' },
  { dial: '+966', label: 'Saudi Arabia (+966)' },
  { dial: '+974', label: 'Qatar (+974)' },
  { dial: '+973', label: 'Bahrain (+973)' },
  { dial: '+968', label: 'Oman (+968)' },
  { dial: '+965', label: 'Kuwait (+965)' },
  { dial: '+880', label: 'Bangladesh (+880)' },
  { dial: '+91', label: 'India (+91)' },
  { dial: '+92', label: 'Pakistan (+92)' },
  { dial: '+63', label: 'Philippines (+63)' },
  { dial: '+65', label: 'Singapore (+65)' },
  { dial: '+61', label: 'Australia (+61)' },
  { dial: '+44', label: 'United Kingdom (+44)' },
  { dial: '+49', label: 'Germany (+49)' },
  { dial: '+33', label: 'France (+33)' },
  { dial: '+27', label: 'South Africa (+27)' },
  { dial: '+20', label: 'Egypt (+20)' },
  { dial: '+1', label: 'US / Canada (+1)' },
] as const

export type PhoneDial = (typeof PHONE_COUNTRY_OPTIONS)[number]['dial']

const DIAL_TO_REGION: Record<PhoneDial, CountryCode> = {
  '+971': 'AE',
  '+966': 'SA',
  '+974': 'QA',
  '+973': 'BH',
  '+968': 'OM',
  '+965': 'KW',
  '+880': 'BD',
  '+91': 'IN',
  '+92': 'PK',
  '+63': 'PH',
  '+65': 'SG',
  '+61': 'AU',
  '+44': 'GB',
  '+49': 'DE',
  '+33': 'FR',
  '+27': 'ZA',
  '+20': 'EG',
  '+1': 'US',
}

const DIALS_LONGEST_FIRST = [...PHONE_COUNTRY_OPTIONS]
  .map((o) => o.dial)
  .sort((a, b) => b.length - a.length)

export const DEFAULT_PHONE_DIAL: PhoneDial = '+971'

const UAE_NATIONAL = { min: 9, max: 9 }

function isPhoneDial(dial: string): dial is PhoneDial {
  return dial in DIAL_TO_REGION
}

function regionForDial(dial: string): CountryCode | undefined {
  return isPhoneDial(dial) ? DIAL_TO_REGION[dial] : undefined
}

function exampleNationalLength(dial: string): number {
  const region = regionForDial(dial)
  if (!region) return 10
  return getExampleNumber(region, examples)?.nationalNumber.length ?? 10
}

export function nationalRange(dial: string): { min: number; max: number } {
  if (dial === '+971') return UAE_NATIONAL
  const n = exampleNationalLength(dial)
  return { min: n, max: n }
}

/** Max digits the national field accepts (mobile example NSN; UAE is 9). */
export function nationalLength(dial: string): number {
  return nationalRange(dial).max
}

/** Example national digits for the selected country (placeholder). */
export function nationalPlaceholder(dial: string): string {
  if (dial === '+971') return '501234567'
  const region = regionForDial(dial)
  if (!region) return ''
  return getExampleNumber(region, examples)?.nationalNumber ?? ''
}

/** Split stored E.164-style value into dial + national digits. */
export function parseStoredMobile(value: string | null | undefined): { dial: PhoneDial; local: string } {
  const compact = (value ?? '').trim().replace(/\s+/g, '')
  if (!compact) return { dial: DEFAULT_PHONE_DIAL, local: '' }
  for (const dial of DIALS_LONGEST_FIRST) {
    if (compact === dial || compact.startsWith(dial)) {
      const local = compact.slice(dial.length).replace(/\D/g, '').slice(0, nationalLength(dial))
      return { dial, local }
    }
  }
  const digits = compact.replace(/\D/g, '')
  return { dial: DEFAULT_PHONE_DIAL, local: digits.slice(0, nationalLength(DEFAULT_PHONE_DIAL)) }
}

/** Persist as dial + national digits (no spaces). Keeps the dial when the number is still empty. */
export function formatMobile(dial: string, local: string): string {
  const digits = local.replace(/\D/g, '').slice(0, nationalLength(dial))
  return `${dial}${digits}`
}

/** API payload: omit a country code with no national digits. */
export function storedMobileForApi(value: string | null | undefined): string | null {
  const { dial, local } = parseStoredMobile(value)
  if (!local) return null
  return formatMobile(dial, local)
}

export function isValidPhone(value: string): boolean {
  const trimmed = (value ?? '').trim().replace(/\s+/g, '')
  if (!trimmed) return false
  let dial: PhoneDial | undefined
  for (const candidate of DIALS_LONGEST_FIRST) {
    if (trimmed.startsWith(candidate)) {
      dial = candidate
      break
    }
  }
  if (!dial) return false
  const local = trimmed.slice(dial.length)
  if (!/^\d+$/.test(local)) return false
  return local.length === nationalLength(dial)
}

export function phoneDigitHint(dial: string): string {
  const { min, max } = nationalRange(dial)
  return min === max ? `Exactly ${min} digits` : `${min} to ${max} digits`
}

/** Returns an error message when invalid; empty when valid or blank (optional). */
export function phoneError(value: string | null | undefined, required = false): string {
  const { dial, local } = parseStoredMobile(value)
  if (!local) return required ? 'Mobile is required' : ''
  const { min, max } = nationalRange(dial)
  if (local.length < min || local.length > max) {
    return `Enter ${phoneDigitHint(dial).toLowerCase()} for the selected country`
  }
  return isValidPhone(formatMobile(dial, local))
    ? ''
    : 'Enter a valid mobile number for the selected country'
}
