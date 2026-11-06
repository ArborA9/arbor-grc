import { Box, Button, IconButton, Stack } from '@mui/material'
import AddRoundedIcon from '@mui/icons-material/AddRounded'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import CompactTextField from '../ui/CompactTextField'
import FormSelect from '../ui/FormSelect'
import FormFieldLabel from '../ui/FormFieldLabel'
import { controlActionSx, formFieldGridSx } from '../ui/formLayout'
import type { BiaAssetDependency, BiaAssetTypeLookup } from '../../api'

const FALLBACK_TYPES: BiaAssetTypeLookup[] = [
  { code: 'SOFTWARE', label: 'Software / Application', detailKind: 'STRING' },
  { code: 'HARDWARE', label: 'Hardware / Endpoint', detailKind: 'INTEGER' },
  { code: 'SERVER', label: 'Server / Compute', detailKind: 'INTEGER' },
  { code: 'NETWORK', label: 'Network / Communications', detailKind: 'INTEGER' },
  { code: 'FACILITY', label: 'Facility / Environment', detailKind: 'STRING' },
  { code: 'OT', label: 'OT / ICS / SCADA', detailKind: 'STRING' },
]

export function assetTypesOrFallback(types?: BiaAssetTypeLookup[] | null) {
  return types && types.length > 0 ? types : FALLBACK_TYPES
}

export function assetsComplete(rows: BiaAssetDependency[], types?: BiaAssetTypeLookup[] | null) {
  const lookup = assetTypesOrFallback(types)
  if (rows.length === 0) return false
  return rows.every((row) => {
    if (!row.type.trim() || !row.detail.trim()) return false
    const kind = lookup.find((t) => t.code === row.type)?.detailKind
    if (kind === 'INTEGER') return /^\d+$/.test(row.detail.trim())
    return true
  })
}

export default function BiaAssetTypeRows({
  rows,
  onChange,
  types,
  disabled,
  error,
  helperText,
}: {
  rows: BiaAssetDependency[]
  onChange: (next: BiaAssetDependency[]) => void
  types?: BiaAssetTypeLookup[] | null
  disabled?: boolean
  error?: boolean
  helperText?: string
}) {
  const lookup = assetTypesOrFallback(types)
  return (
    <Box>
      <FormFieldLabel required wrap>Dependency - Asset Type</FormFieldLabel>
      {helperText && (
        <Box sx={{ fontSize: '0.75rem', color: error ? 'error.main' : 'text.secondary', mb: 0.75 }}>{helperText}</Box>
      )}
      <Stack spacing={1}>
        {(rows.length === 0 ? [{ type: '', detail: '' }] : rows).map((row, idx) => {
          const kind = lookup.find((t) => t.code === row.type)?.detailKind
          const integer = kind === 'INTEGER'
          const setRow = (patch: Partial<BiaAssetDependency>) => {
            const base = rows.length === 0 ? [{ type: '', detail: '' }] : rows
            const next = base.map((r, i) => (i === idx ? { ...r, ...patch } : r))
            if (rows.length === 0 && next.every((r) => !r.type.trim() && !r.detail.trim())) return
            if (JSON.stringify(next) === JSON.stringify(rows)) return
            onChange(next)
          }
          return (
            <Box
              key={idx}
              sx={{
                ...formFieldGridSx,
                gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 1fr) minmax(0, 1fr) auto' },
                alignItems: 'end',
              }}
            >
              {disabled ? (
                <CompactTextField
                  wrapLabel
                  label="Asset type"
                  value={lookup.find((t) => t.code === row.type)?.label ?? row.type ?? '-'}
                  disabled
                />
              ) : (
                <FormSelect
                  wrapLabel
                  label="Asset type"
                  value={row.type}
                  options={[{ value: '', label: 'Select' }, ...lookup.map((t) => ({ value: t.code, label: t.label }))]}
                  onChange={(v) => {
                    if (v === row.type) return
                    setRow({ type: v, detail: '' })
                  }}
                  fullWidth
                />
              )}
              <CompactTextField
                wrapLabel
                label={integer ? 'Number of assets' : 'Asset name / details'}
                value={row.detail}
                onChange={(e) => {
                  const v = e.target.value
                  if (integer && v !== '' && !/^\d*$/.test(v)) return
                  setRow({ detail: v })
                }}
                disabled={disabled}
                error={error && (!row.type || !row.detail.trim() || (integer && row.detail.trim() !== '' && !/^\d+$/.test(row.detail.trim())))}
              />
              {!disabled && (
                <IconButton
                  size="small"
                  aria-label="Remove asset"
                  onClick={() => onChange(rows.filter((_, i) => i !== idx))}
                  sx={{
                    height: 32,
                    width: 32,
                    mb: 0.25,
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: 1,
                  }}
                >
                  <DeleteOutlineIcon fontSize="small" />
                </IconButton>
              )}
            </Box>
          )
        })}
        {!disabled && (
          <Box sx={{ pt: 0.25 }}>
            <Button
              size="small"
              variant="outlined"
              startIcon={<AddRoundedIcon sx={{ fontSize: 16 }} />}
              onClick={() => onChange([...rows, { type: '', detail: '' }])}
              sx={controlActionSx}
            >
              Add Asset Type
            </Button>
          </Box>
        )}
      </Stack>
    </Box>
  )
}
