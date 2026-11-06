import { useEffect, useMemo, useState } from 'react'
import { Autocomplete, Box, TextField, Typography } from '@mui/material'
import FormFieldLabel, { compactInputSx, CONTROL_HEIGHT } from '../ui/FormFieldLabel'
import { api, type BiaPersonRef } from '../../api'
import CompactOverflowChips from './CompactOverflowChips'

export function biaPersonKey(p: BiaPersonRef) {
  if (p.source === 'ENTRA' && p.directoryId) return `entra:${p.directoryId}`
  if (p.source === 'ARBOR' && p.userId != null) return `arbor:${p.userId}`
  return `email:${(p.email ?? p.displayName).toLowerCase()}`
}

function personLabel(p: BiaPersonRef) {
  return p.jobTitle ? `${p.displayName} · ${p.jobTitle}` : p.displayName
}

export default function BiaPersonPicker({
  id,
  label,
  required,
  value,
  onChange,
  disabled,
  error,
  helperText,
  /** When set, only these people can be chosen (no full directory / Entra search). */
  restrictTo,
}: {
  id?: string
  label: string
  required?: boolean
  value: BiaPersonRef[]
  onChange: (next: BiaPersonRef[]) => void
  disabled?: boolean
  error?: boolean
  helperText?: string
  restrictTo?: BiaPersonRef[]
}) {
  const [query, setQuery] = useState('')
  const [options, setOptions] = useState<BiaPersonRef[]>([])
  const [arbor, setArbor] = useState<BiaPersonRef[]>([])
  const selectedKeys = useMemo(() => new Set(value.map(biaPersonKey)), [value])
  const restricted = restrictTo != null
  const pool = useMemo(() => restrictTo ?? [], [restrictTo])

  useEffect(() => {
    if (restricted) return
    let cancelled = false
    void api.listUserDirectory().then((rows) => {
      if (cancelled) return
      setArbor(
        rows.map((u) => ({
          source: 'ARBOR' as const,
          userId: u.id,
          email: u.email,
          displayName: u.fullName,
          jobTitle: null,
        })),
      )
    }).catch(() => {
      if (!cancelled) setArbor([])
    })
    return () => {
      cancelled = true
    }
  }, [restricted])

  useEffect(() => {
    const q = query.trim()
    const lower = q.toLowerCase()
    if (restricted) {
      setOptions(
        pool
          .filter((p) =>
            !q
              ? true
              : p.displayName.toLowerCase().includes(lower)
                || (p.email ?? '').toLowerCase().includes(lower)
                || (p.jobTitle ?? '').toLowerCase().includes(lower),
          )
          .slice(0, 40),
      )
      return
    }
    if (q.length < 2) {
      setOptions(
        arbor.filter((p) =>
          !q
            ? true
            : p.displayName.toLowerCase().includes(lower) || (p.email ?? '').toLowerCase().includes(lower),
        ).slice(0, 20),
      )
      return
    }
    let cancelled = false
    const t = window.setTimeout(() => {
      void api.searchEmployees(q).then((res) => {
        if (cancelled) return
        if (res.available) {
          setOptions(
            res.results.map((e) => ({
              source: 'ENTRA' as const,
              directoryId: e.directoryId,
              email: e.email,
              displayName: e.displayName,
              jobTitle: e.jobTitle,
            })),
          )
          return
        }
        setOptions(
          arbor.filter(
            (p) => p.displayName.toLowerCase().includes(lower) || (p.email ?? '').toLowerCase().includes(lower),
          ).slice(0, 20),
        )
      }).catch(() => {
        if (cancelled) return
        setOptions(
          arbor.filter(
            (p) => p.displayName.toLowerCase().includes(lower) || (p.email ?? '').toLowerCase().includes(lower),
          ).slice(0, 20),
        )
      })
    }, 250)
    return () => {
      cancelled = true
      window.clearTimeout(t)
    }
  }, [query, arbor, restricted, pool])

  const chips = (
    <CompactOverflowChips
      items={value.map((p) => ({
        key: biaPersonKey(p),
        label: p.displayName,
        secondary: p.jobTitle || p.email,
      }))}
      disabled={disabled}
      noun={value.length === 1 ? 'person' : 'people'}
      onRemove={disabled ? undefined : (key) => onChange(value.filter((p) => biaPersonKey(p) !== key))}
    />
  )

  const placeholder = restricted
    ? pool.length === 0
      ? 'Add Dependency Minimum Staff on Identity first'
      : value.length
        ? `${value.length} selected. Search Dependency staff`
        : 'Search Dependency staff'
    : value.length
      ? `${value.length} selected. Search to add`
      : 'Search name'

  return (
    <Box sx={{ width: '100%', minWidth: 0 }}>
      <FormFieldLabel htmlFor={id} required={required} wrap>
        {label}
      </FormFieldLabel>
      {disabled ? (
        value.length === 0 ? (
          <Typography variant="body2" sx={{ minHeight: CONTROL_HEIGHT, display: 'flex', alignItems: 'center' }}>
            -
          </Typography>
        ) : (
          chips
        )
      ) : (
        <>
          <Autocomplete
            multiple
            options={[
              ...value.filter((v) => !options.some((o) => biaPersonKey(o) === biaPersonKey(v))),
              ...options.filter((o) => !selectedKeys.has(biaPersonKey(o))),
            ]}
            value={value}
            getOptionLabel={personLabel}
            isOptionEqualToValue={(a, b) => biaPersonKey(a) === biaPersonKey(b)}
            filterOptions={(x) => x}
            inputValue={query}
            onInputChange={(_, v) => setQuery(v)}
            onChange={(_, next) => onChange(next)}
            disableCloseOnSelect
            disabled={restricted && pool.length === 0}
            renderTags={() => null}
            renderInput={(params) => (
              <TextField
                {...params}
                id={id}
                placeholder={placeholder}
                error={error}
                helperText={helperText}
                size="small"
                sx={{
                  ...compactInputSx,
                  '& .MuiOutlinedInput-root': {
                    flexWrap: 'nowrap',
                    height: CONTROL_HEIGHT,
                    minHeight: CONTROL_HEIGHT,
                    maxHeight: CONTROL_HEIGHT,
                  },
                }}
              />
            )}
          />
          {chips}
        </>
      )}
    </Box>
  )
}
