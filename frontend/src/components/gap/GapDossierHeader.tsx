import { type ReactNode } from 'react'
import { Box, Stack, Typography } from '@mui/material'
import StatusChip from '../StatusChip'
import SettingsCard from '../ui/SettingsCard'
import { brand } from '../../theme'
import { readOnlyCaptionSx, TYPE_CONTROL } from '../ui/FormFieldLabel'

function Fact({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 0.75, minWidth: 0, py: 0.15 }}>
      <Typography sx={{ ...readOnlyCaptionSx, mb: 0, flexShrink: 0, minWidth: 72, lineHeight: 1.3 }}>
        {label}
      </Typography>
      <Box
        sx={{
          fontSize: TYPE_CONTROL,
          fontWeight: 600,
          color: brand.ink,
          lineHeight: 1.3,
          minWidth: 0,
          flex: 1,
          wordBreak: 'break-word',
        }}
      >
        {children}
      </Box>
    </Box>
  )
}

export default function GapDossierHeader({
  name,
  typeLabel,
  statusLabel,
  statusTone,
  due,
  projectName,
  frameworkName,
  soaLabel,
  soaLinked,
  onOpenSoa,
}: {
  name: string
  typeLabel: string
  statusLabel: string
  statusTone: 'success' | 'info' | 'warning' | 'neutral'
  due: string
  projectName: string
  frameworkName: string
  soaLabel: string
  soaLinked: boolean
  onOpenSoa?: () => void
}) {
  return (
    <SettingsCard
      compact
      heading
      title={name}
      subtitle={`${typeLabel} · Due ${due}`}
      accentColor={brand.gold}
      action={<StatusChip label={statusLabel} tone={statusTone} />}
    >
      <Stack spacing={0.45} sx={{ minWidth: 0 }}>
        <Fact label="Project">{projectName || '-'}</Fact>
        <Fact label="Framework">{frameworkName || '-'}</Fact>
        <Fact label="SoA">
          {soaLinked && onOpenSoa ? (
            <Box
              component="button"
              type="button"
              onClick={onOpenSoa}
              sx={{
                border: 0,
                p: 0,
                m: 0,
                bgcolor: 'transparent',
                color: brand.accentHover,
                font: 'inherit',
                fontSize: TYPE_CONTROL,
                fontWeight: 650,
                textDecoration: 'underline',
                cursor: 'pointer',
              }}
            >
              {soaLabel}
            </Box>
          ) : (
            soaLabel
          )}
        </Fact>
      </Stack>
    </SettingsCard>
  )
}
