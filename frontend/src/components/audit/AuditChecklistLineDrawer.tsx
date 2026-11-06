import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { Box, Button, Drawer, IconButton, Stack, Tooltip, Typography } from '@mui/material'
import ChevronLeftRoundedIcon from '@mui/icons-material/ChevronLeftRounded'
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded'
import CloseRoundedIcon from '@mui/icons-material/CloseRounded'
import {
  api,
  type AuditChecklistItem,
  type ChecklistConclusion,
  type GrcAuditDetail,
} from '../../api'
import { BusyButton, CompactTextField, FormSelect } from '../ui'
import ConfirmActionDialog from '../ui/ConfirmActionDialog'
import { formFieldGridSx, NARRATIVE_FIELD_MAX_WIDTH } from '../ui/formLayout'
import { brand } from '../../theme'
import StatusChip from '../StatusChip'
import { formatTenantDate } from '../../uiQuality/tenantLocale'

export const CHECKLIST_CONCLUSIONS: { value: ChecklistConclusion; label: string }[] = [
  { value: 'CONFORMS', label: 'Conforms' },
  { value: 'MAJOR_NC', label: 'Major NC' },
  { value: 'MINOR_NC', label: 'Minor NC' },
  { value: 'OBSERVATION', label: 'Observation' },
  { value: 'OFI', label: 'OFI' },
  { value: 'NOT_APPLICABLE', label: 'Not Applicable' },
  { value: 'NOT_TESTED', label: 'Not Tested' },
]

type Draft = {
  clauseRef: string
  section: string
  title: string
  testFocus: string
  soaApplicability: string
  inScope: boolean
  notes: string
  evidenceExamined: string
  personsInterviewed: string
  conclusion: ChecklistConclusion | ''
  findingDescription: string
}

function toDraft(row: AuditChecklistItem): Draft {
  return {
    clauseRef: row.nodeCode ?? '',
    section: row.section ?? '',
    title: row.title ?? '',
    testFocus: row.testFocus ?? '',
    soaApplicability: row.soaApplicability ?? '',
    inScope: row.inScope !== false,
    notes: row.notes ?? '',
    evidenceExamined: row.evidenceExamined ?? '',
    personsInterviewed: row.personsInterviewed ?? '',
    conclusion: row.conclusion ?? '',
    findingDescription: row.findingDescription ?? '',
  }
}

function conclusionTone(
  value: string | null | undefined,
): 'success' | 'warning' | 'info' | 'danger' | 'neutral' {
  if (value === 'CONFORMS') return 'success'
  if (value === 'MAJOR_NC') return 'danger'
  if (value === 'MINOR_NC') return 'warning'
  if (value === 'OBSERVATION' || value === 'OFI') return 'info'
  return 'neutral'
}

function conclusionLabel(value: string | null | undefined): string {
  return CHECKLIST_CONCLUSIONS.find((c) => c.value === value)?.label ?? ''
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Box>
      <Typography
        sx={{
          fontSize: '0.8125rem',
          fontWeight: 650,
          color: brand.ink,
          letterSpacing: '-0.01em',
          mb: 1,
        }}
      >
        {title}
      </Typography>
      {children}
    </Box>
  )
}

function Dossier({ children }: { children: ReactNode }) {
  return (
    <Box
      sx={{
        border: `1px solid ${brand.border}`,
        borderRadius: 1.25,
        bgcolor: brand.paper,
        overflow: 'hidden',
      }}
    >
      {children}
    </Box>
  )
}

function DossierRow({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', sm: '132px minmax(0, 1fr)' },
        gap: { xs: 0.35, sm: 1.5 },
        px: 1.5,
        py: 1.1,
        borderBottom: `1px solid ${brand.borderLight}`,
        '&:last-child': { borderBottom: 0 },
      }}
    >
      <Typography sx={{ fontSize: '0.75rem', fontWeight: 550, color: brand.muted, pt: { sm: 0.15 } }}>
        {label}
      </Typography>
      <Box
        sx={{
          fontSize: '0.875rem',
          color: brand.ink,
          lineHeight: 1.5,
          wordBreak: 'break-word',
          whiteSpace: 'pre-wrap',
        }}
      >
        {children}
      </Box>
    </Box>
  )
}

function EmptyValue() {
  return (
    <Typography component="span" sx={{ color: brand.mutedLight }}>
      -
    </Typography>
  )
}

function displayOrEmpty(value: string | null | undefined) {
  const t = value?.trim()
  return t ? t : <EmptyValue />
}

export default function AuditChecklistLineDrawer({
  detail,
  row,
  canEdit,
  busy,
  onBusy,
  onClose,
  onSaved,
  onError,
  onPrev,
  onNext,
  hasPrev,
  hasNext,
}: {
  detail: GrcAuditDetail
  row: AuditChecklistItem
  canEdit: boolean
  busy: boolean
  onBusy: (v: boolean) => void
  onClose: () => void
  onSaved: (updated: AuditChecklistItem) => void
  onError: (msg: string) => void
  onPrev?: () => void
  onNext?: () => void
  hasPrev?: boolean
  hasNext?: boolean
}) {
  const [draft, setDraft] = useState<Draft>(() => toDraft(row))
  const [leaveAsk, setLeaveAsk] = useState(false)
  const locked = Boolean(row.libraryLocked)
  const initial = useMemo(() => toDraft(row), [row])

  useEffect(() => {
    setDraft(toDraft(row))
  }, [row.id, row.updatedAt])

  const dirty = useMemo(() => JSON.stringify(draft) !== JSON.stringify(initial), [draft, initial])
  const findingNeeded = ['MAJOR_NC', 'MINOR_NC', 'OBSERVATION', 'OFI'].includes(draft.conclusion)
  const findingWhy =
    findingNeeded && !draft.findingDescription.trim() ? 'Enter the finding description' : undefined
  const saveWhy = !canEdit
    ? 'This checklist is read-only'
    : findingWhy
      ? findingWhy
      : !dirty
        ? 'No changes to save'
        : undefined
  const shownConclusion = canEdit ? draft.conclusion : row.conclusion
  const shownInScope = canEdit ? draft.inScope : row.inScope !== false
  const navWhy = dirty ? 'Save progress before leaving this line' : undefined

  const patch = (part: Partial<Draft>) => setDraft((prev) => ({ ...prev, ...part }))

  const requestClose = () => {
    if (canEdit && dirty) {
      setLeaveAsk(true)
      return
    }
    onClose()
  }

  const save = async () => {
    if (!canEdit || !dirty) return
    onBusy(true)
    try {
      const updated = await api.updateAuditChecklist(detail.audit.id, row.id, {
        notes: draft.notes,
        inScope: draft.inScope,
        evidenceExamined: draft.evidenceExamined,
        personsInterviewed: draft.personsInterviewed,
        conclusion: (draft.conclusion || null) as ChecklistConclusion | null,
        findingDescription: draft.findingDescription,
        clauseRef: draft.clauseRef,
        section: draft.section,
        title: draft.title,
        testFocus: draft.testFocus,
        soaApplicability: draft.soaApplicability,
      })
      onSaved(updated)
    } catch (e) {
      onError(String(e))
    } finally {
      onBusy(false)
    }
  }

  return (
    <Drawer
      anchor="right"
      open
      onClose={requestClose}
      PaperProps={{
        sx: {
          width: { xs: '100%', sm: 500, md: 580 },
          bgcolor: brand.canvas,
        },
      }}
    >
      <Stack sx={{ height: '100%' }}>
        <Box
          sx={{
            px: 2.25,
            pt: 1.75,
            pb: 1.5,
            bgcolor: brand.paper,
            borderBottom: `1px solid ${brand.border}`,
            boxShadow: '0 1px 0 rgba(28, 25, 23, 0.04)',
            position: 'relative',
            '&::before': {
              content: '""',
              position: 'absolute',
              left: 0,
              top: 0,
              bottom: 0,
              width: 3,
              bgcolor: brand.gold,
            },
          }}
        >
          <Stack direction="row" alignItems="flex-start" justifyContent="space-between" gap={1}>
            <Box sx={{ minWidth: 0, flex: 1 }}>
              <Stack direction="row" spacing={0.75} alignItems="center" flexWrap="wrap" useFlexGap>
                <Typography
                  sx={{
                    fontFamily: 'ui-monospace, Menlo, monospace',
                    fontWeight: 700,
                    fontSize: '0.75rem',
                    color: brand.goldDark,
                    letterSpacing: '0.02em',
                  }}
                >
                  {row.nodeCode}
                </Typography>
                {row.section ? (
                  <Typography sx={{ fontSize: '0.75rem', color: brand.mutedLight }}>
                    {row.section}
                  </Typography>
                ) : null}
              </Stack>
              <Typography
                sx={{
                  fontWeight: 650,
                  fontSize: '1.0625rem',
                  letterSpacing: '-0.02em',
                  lineHeight: 1.35,
                  mt: 0.5,
                  color: brand.ink,
                }}
              >
                {row.title}
              </Typography>
              <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap sx={{ mt: 1 }}>
                {row.findingDisplayCode ? (
                  <StatusChip label={row.findingDisplayCode} tone="danger" />
                ) : null}
                {canEdit ? <StatusChip label="Editable" tone="info" /> : <StatusChip label="View only" tone="neutral" />}
              </Stack>
            </Box>
            <IconButton size="small" onClick={requestClose} aria-label="Close" sx={{ mt: -0.25 }}>
              <CloseRoundedIcon fontSize="small" />
            </IconButton>
          </Stack>
        </Box>

        <Box sx={{ flex: 1, overflow: 'auto', px: 2.25, py: 2 }}>
          <Stack spacing={2.25}>
            {canEdit && !locked && (
              <Section title="Control">
                <Box sx={formFieldGridSx}>
                  <CompactTextField
                    label="Clause / Ref"
                    fieldIdPrefix="line-ref"
                    value={draft.clauseRef}
                    onChange={(e) => patch({ clauseRef: e.target.value })}
                    required
                  />
                  <CompactTextField
                    label="Section"
                    fieldIdPrefix="line-sec"
                    value={draft.section}
                    onChange={(e) => patch({ section: e.target.value })}
                  />
                  <CompactTextField
                    label="Title"
                    fieldIdPrefix="line-title"
                    value={draft.title}
                    onChange={(e) => patch({ title: e.target.value })}
                    required
                    fullWidth
                    sx={{ gridColumn: { md: '1 / -1' } }}
                  />
                </Box>
              </Section>
            )}

            <Section title="What is being tested">
              {canEdit && !locked ? (
                <Stack spacing={1.25}>
                  <CompactTextField
                    label="Test focus"
                    fieldIdPrefix="line-test"
                    value={draft.testFocus}
                    onChange={(e) => patch({ testFocus: e.target.value })}
                    multiline
                    minRows={3}
                    maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                    fullWidth
                  />
                  <CompactTextField
                    label="SoA / type"
                    fieldIdPrefix="line-soa"
                    value={draft.soaApplicability}
                    onChange={(e) => patch({ soaApplicability: e.target.value })}
                  />
                </Stack>
              ) : (
                <Stack spacing={1.25}>
                  <Box
                    sx={{
                      px: 1.5,
                      py: 1.25,
                      border: `1px solid ${brand.border}`,
                      borderRadius: 1.25,
                      bgcolor: brand.paper,
                    }}
                  >
                    <Typography sx={{ fontSize: '0.875rem', lineHeight: 1.55, color: brand.inkSoft }}>
                      {row.testFocus?.trim() ? row.testFocus : '-'}
                    </Typography>
                  </Box>
                  <Dossier>
                    <DossierRow label="SoA / type">{displayOrEmpty(row.soaApplicability)}</DossierRow>
                    {!canEdit && (
                      <DossierRow label="In scope">{shownInScope ? 'Yes' : 'No'}</DossierRow>
                    )}
                  </Dossier>
                </Stack>
              )}
            </Section>

            {canEdit && (
              <Stack spacing={1.25}>
                <Stack direction="row" spacing={1.5} useFlexGap flexWrap="wrap" alignItems="flex-start">
                  <FormSelect
                    label="In scope"
                    value={draft.inScope ? 'yes' : 'no'}
                    options={[
                      { value: 'yes', label: 'Yes' },
                      { value: 'no', label: 'No' },
                    ]}
                    onChange={(v) => patch({ inScope: v === 'yes' })}
                    minWidth={120}
                  />
                  <FormSelect
                    label="Conclusion"
                    value={draft.conclusion}
                    options={[
                      { value: '', label: '-' },
                      ...CHECKLIST_CONCLUSIONS.map((c) => ({ value: c.value, label: c.label })),
                    ]}
                    onChange={(v) => patch({ conclusion: v as ChecklistConclusion | '' })}
                    minWidth={180}
                  />
                </Stack>
                {row.findingDisplayCode ? (
                  <Dossier>
                    <DossierRow label="Finding ID">
                      <StatusChip label={row.findingDisplayCode} tone="danger" />
                    </DossierRow>
                  </Dossier>
                ) : null}
                <CompactTextField
                  label="Finding description"
                  fieldIdPrefix="line-fnd"
                  value={draft.findingDescription}
                  onChange={(e) => patch({ findingDescription: e.target.value })}
                  multiline
                  minRows={2}
                  maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                  fullWidth
                  required={findingNeeded}
                  error={Boolean(findingWhy)}
                  helperText={findingWhy}
                />
              </Stack>
            )}
            {!canEdit && (
              <Section title="Conclusion">
                <Dossier>
                  <DossierRow label="Conclusion">
                    {shownConclusion ? (
                      <StatusChip
                        label={conclusionLabel(shownConclusion) || shownConclusion}
                        tone={conclusionTone(shownConclusion)}
                      />
                    ) : (
                      <EmptyValue />
                    )}
                  </DossierRow>
                  {row.findingDisplayCode ? (
                    <DossierRow label="Finding ID">
                      <StatusChip label={row.findingDisplayCode} tone="danger" />
                    </DossierRow>
                  ) : null}
                  <DossierRow label="Finding description">{displayOrEmpty(row.findingDescription)}</DossierRow>
                </Dossier>
              </Section>
            )}

            <Section title="Working paper">
              {canEdit ? (
                <Stack spacing={1.25}>
                  <CompactTextField
                    label="Auditor note"
                    fieldIdPrefix="line-note"
                    value={draft.notes}
                    onChange={(e) => patch({ notes: e.target.value })}
                    multiline
                    minRows={2}
                    maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                    fullWidth
                  />
                  <CompactTextField
                    label="Evidence examined"
                    fieldIdPrefix="line-ev"
                    value={draft.evidenceExamined}
                    onChange={(e) => patch({ evidenceExamined: e.target.value })}
                    multiline
                    minRows={2}
                    maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                    fullWidth
                  />
                  <CompactTextField
                    label="Persons interviewed"
                    fieldIdPrefix="line-ppl"
                    value={draft.personsInterviewed}
                    onChange={(e) => patch({ personsInterviewed: e.target.value })}
                    fullWidth
                  />
                  {row.reviewedDate ? (
                    <Dossier>
                      <DossierRow label="Date reviewed">{formatTenantDate(row.reviewedDate)}</DossierRow>
                    </Dossier>
                  ) : null}
                </Stack>
              ) : (
                <Dossier>
                  <DossierRow label="Auditor note">{displayOrEmpty(row.notes)}</DossierRow>
                  <DossierRow label="Evidence examined">{displayOrEmpty(row.evidenceExamined)}</DossierRow>
                  <DossierRow label="Persons interviewed">{displayOrEmpty(row.personsInterviewed)}</DossierRow>
                  {row.reviewedDate ? (
                    <DossierRow label="Date reviewed">{formatTenantDate(row.reviewedDate)}</DossierRow>
                  ) : null}
                </Dossier>
              )}
            </Section>
          </Stack>
        </Box>

        <Stack
          direction="row"
          alignItems="center"
          justifyContent="space-between"
          gap={1}
          sx={{
            px: 2.25,
            py: 1.25,
            bgcolor: brand.paper,
            borderTop: `1px solid ${brand.border}`,
          }}
        >
          <Stack direction="row" spacing={0.5}>
            <Tooltip title={navWhy ?? 'Previous line'}>
              <span>
                <IconButton
                  size="small"
                  aria-label="Previous"
                  disabled={!hasPrev || busy || dirty}
                  onClick={onPrev}
                  sx={{
                    width: 32,
                    height: 32,
                    border: `1px solid ${brand.border}`,
                    borderRadius: 1,
                    color: brand.inkSoft,
                  }}
                >
                  <ChevronLeftRoundedIcon fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>
            <Tooltip title={navWhy ?? 'Next line'}>
              <span>
                <IconButton
                  size="small"
                  aria-label="Next"
                  disabled={!hasNext || busy || dirty}
                  onClick={onNext}
                  sx={{
                    width: 32,
                    height: 32,
                    border: `1px solid ${brand.border}`,
                    borderRadius: 1,
                    color: brand.inkSoft,
                  }}
                >
                  <ChevronRightRoundedIcon fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>
          </Stack>
          <Stack direction="row" spacing={1}>
            <Button size="small" onClick={requestClose} disabled={busy}>
              {canEdit ? 'Cancel' : 'Close'}
            </Button>
            {canEdit && (
              <BusyButton
                size="small"
                variant="contained"
                busy={busy}
                busyLabel="Saving..."
                disabled={Boolean(saveWhy)}
                disabledReason={saveWhy}
                onClick={() => void save()}
              >
                Save
              </BusyButton>
            )}
          </Stack>
        </Stack>
      </Stack>

      <ConfirmActionDialog
        open={leaveAsk}
        title="Leave without saving?"
        message="You have unsaved changes on this line."
        confirmLabel="Leave"
        confirmColor="warning"
        onCancel={() => setLeaveAsk(false)}
        onConfirm={() => {
          setLeaveAsk(false)
          onClose()
        }}
      />
    </Drawer>
  )
}
