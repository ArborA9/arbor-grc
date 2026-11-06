import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { Box, Button, Stack } from '@mui/material'
import { api, ApiError, type CapaRegisterRecord } from '../../api'
import {
  BusyButton,
  CompactTextField,
  FormSelect,
  SettingsCard,
  complexFormContainerSx,
  controlActionSx,
  twoColFieldSx,
} from '../ui'
import { useRegisterUnsavedChanges } from '../../uiQuality/UnsavedChangesGuard'

function yearOptions(existingYears: number[]): { value: string; label: string }[] {
  const now = new Date().getFullYear()
  const years = new Set<number>()
  for (let y = now - 3; y <= now + 1; y += 1) years.add(y)
  for (const y of existingYears) years.add(y)
  return [...years]
    .sort((a, b) => b - a)
    .map((y) => ({
      value: String(y),
      label: existingYears.includes(y) ? `${y} (already exists)` : String(y),
    }))
}

export default function AuditCapaRegisterCreateView({
  existingYears,
  onChrome,
  onCancel,
  onSaved,
  onError,
}: {
  existingYears: number[]
  onChrome?: (actions: ReactNode | null) => void
  onCancel: () => void
  onSaved: (r: CapaRegisterRecord) => void
  onError: (msg: string) => void
}) {
  const thisYear = String(new Date().getFullYear())
  const [year, setYear] = useState(thisYear)
  const [name, setName] = useState('')
  const [busy, setBusy] = useState(false)
  const [nameError, setNameError] = useState<string>()

  const years = useMemo(() => yearOptions(existingYears), [existingYears])
  const yearTaken = existingYears.includes(Number(year))

  const why = !year
    ? 'Select a year'
    : yearTaken
      ? `A CAPA register for ${year} already exists`
      : !name.trim()
        ? 'Enter a name'
        : undefined

  const dirty = name.trim() !== '' || year !== thisYear

  useRegisterUnsavedChanges(
    dirty && !busy
      ? { dirty: true, title: 'Leave Add CAPA Register?', message: 'You have unsaved changes.' }
      : null,
  )

  const submit = async () => {
    if (!name.trim()) {
      setNameError('Enter a name')
      document.getElementById('capa-register-name')?.focus()
    }
    if (why) {
      onError(why)
      return
    }
    setBusy(true)
    try {
      const created = await api.createCapaRegister({
        year: Number(year),
        name: name.trim(),
      })
      onSaved(created)
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => {
    onChrome?.(
      <Stack direction="row" spacing={0.75}>
        <Button size="small" variant="outlined" sx={controlActionSx} onClick={onCancel} disabled={busy}>
          Cancel
        </Button>
        <BusyButton
          size="small"
          variant="contained"
          sx={controlActionSx}
          busy={busy}
          busyLabel="Creating..."
          disabled={Boolean(why)}
          disabledReason={why}
          onClick={() => void submit()}
        >
          Create Register
        </BusyButton>
      </Stack>,
    )
    return () => onChrome?.(null)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [why, busy, name, year])

  return (
    <Stack spacing={1.5} sx={{ flex: 1, minHeight: 0, overflow: 'auto', p: 1.5, ...complexFormContainerSx }}>
      <SettingsCard
        heading
        autoHeight
        title="CAPA register"
        subtitle="One register per year. Open it to add CAPAs."
      >
        <Box sx={twoColFieldSx}>
          <FormSelect
            label="Year"
            required
            value={year}
            options={years}
            onChange={setYear}
          />
          <CompactTextField
            id="capa-register-name"
            label="Name"
            required
            value={name}
            onChange={(e) => {
              setName(e.target.value)
              setNameError(undefined)
            }}
            maxLength={200}
            placeholder="CAPA Register 2026"
            error={Boolean(nameError)}
            helperText={nameError}
          />
        </Box>
      </SettingsCard>
    </Stack>
  )
}
