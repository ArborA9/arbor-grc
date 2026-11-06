import { Box, Stack, Typography } from '@mui/material'

/** Excel Audit Journey: Admin role, Projects, Checklist, CAPA. */
export type AuditJourneyStep = 'projects' | 'checklists' | 'capa'

const LINES = [
  'STEP 1 · Admin assigns the Auditor role.',
  'STEP 2 (Audit Projects) · Admin creates the audit, selects scope and checklist, and assigns the auditor.',
  'STEP 3 (Audit Checklist) · Auditor fills the checklist.',
  'STEP 4 (CAPA) · Admin adds one CAPA register per year, then adds CAPAs from findings.',
]

const STEP_LINE: Record<AuditJourneyStep, number> = {
  projects: 1,
  checklists: 2,
  capa: 3,
}

/** Excel journey strip (screenshot navy bar) on Audit Projects / Checklist / CAPA. */
export default function AuditJourneyBanner({ step }: { step: AuditJourneyStep }) {
  const activeLine = STEP_LINE[step]
  return (
    <Box
      sx={{
        bgcolor: '#1B4F72',
        color: '#fff',
        px: 1.75,
        py: 1.25,
        borderRadius: 1,
      }}
    >
      <Stack spacing={0.35}>
        {LINES.map((line, i) => (
          <Typography
            key={line}
            variant="body2"
            sx={{
              fontSize: '0.8125rem',
              lineHeight: 1.45,
              fontWeight: i === activeLine ? 700 : 500,
              opacity: i === activeLine ? 1 : 0.88,
            }}
          >
            {line}
          </Typography>
        ))}
      </Stack>
    </Box>
  )
}
