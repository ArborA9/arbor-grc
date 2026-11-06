import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Box, Button, Drawer, IconButton, Stack, Tooltip, Typography } from '@mui/material'
import ChevronLeftRoundedIcon from '@mui/icons-material/ChevronLeftRounded'
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded'
import CloseRoundedIcon from '@mui/icons-material/CloseRounded'
import type { TprmAssessmentItem, TprmVerdict } from '../../api'
import EvidenceAttachedList from '../evidence/EvidenceAttachedList'
import StatusChip from '../StatusChip'
import DiscardChangesDialog from '../DiscardChangesDialog'
import { BusyButton, CompactTextField, FormSelect, NARRATIVE_FIELD_MAX_WIDTH } from '../ui'
import { brand } from '../../theme'
import { evidenceState, toEvidenceSummaries } from './vendorPortalItems'

const VERDICT_LABEL: Record<TprmVerdict, string> = {
  ACCEPTED: 'Accepted',
  ACCEPTED_WITH_OBSERVATION: 'Accepted with observation',
  DOWNGRADED: 'Downgraded',
  RETURNED: 'Returned',
}

const REVIEW_VERDICT_OPTIONS: { value: TprmVerdict; label: string }[] = [
  { value: 'ACCEPTED', label: VERDICT_LABEL.ACCEPTED },
  { value: 'ACCEPTED_WITH_OBSERVATION', label: VERDICT_LABEL.ACCEPTED_WITH_OBSERVATION },
  { value: 'RETURNED', label: VERDICT_LABEL.RETURNED },
]

function answerLabel(value: string | null | undefined): string {
  switch (value) {
    case 'YES':
      return 'Yes'
    case 'PARTIAL':
      return 'Partial'
    case 'NO':
      return 'No'
    case 'NA':
      return 'N/A'
    default:
      return '-'
  }
}

function answerTone(value: string | null | undefined): 'success' | 'warning' | 'danger' | 'neutral' | 'info' {
  if (value === 'YES') return 'success'
  if (value === 'PARTIAL') return 'warning'
  if (value === 'NO') return 'danger'
  if (value === 'NA') return 'neutral'
  return 'neutral'
}

function verdictTone(v: TprmVerdict | null | undefined): 'success' | 'info' | 'warning' | 'danger' | 'neutral' {
  switch (v) {
    case 'ACCEPTED':
      return 'success'
    case 'ACCEPTED_WITH_OBSERVATION':
      return 'info'
    case 'DOWNGRADED':
      return 'warning'
    case 'RETURNED':
      return 'danger'
    default:
      return 'neutral'
  }
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
          minWidth: 0,
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

export default function TprmReviewDrawer({
  open,
  item,
  index,
  total,
  reviewable,
  reviewEditable,
  progressHint,
  busy = false,
  dirty = false,
  onClose,
  onPrev,
  onNext,
  onPatch,
  onSave,
  onDiscard,
  onCloseProject,
  onReturnToVendor,
  onError,
}: {
  open: boolean
  item: TprmAssessmentItem
  /** 0-based index in the list */
  index: number
  total: number
  reviewable: boolean
  reviewEditable: boolean
  /** e.g. IN_PROGRESS: verdicts wait until submit */
  progressHint?: string | null
  busy?: boolean
  /** Unsaved review edits vs last Save / server load */
  dirty?: boolean
  onClose: () => void
  onPrev: () => void
  onNext: () => void
  onPatch: (patch: Partial<TprmAssessmentItem>) => void
  /** Return false (or throw) to keep the leave dialog open after a failed Save */
  onSave?: () => void | boolean | Promise<void | boolean>
  /** Restore verdicts/comments to last saved snapshot */
  onDiscard?: () => void
  /** Last question while reviewing: Close project (only when close is allowed) */
  onCloseProject?: () => void
  /** Hand off Returned controls without closing the drawer first */
  onReturnToVendor?: () => void
  onError: (msg: string) => void
}) {
  const files = toEvidenceSummaries(item)
  const ev = evidenceState(item)
  const hasPrev = index > 0
  const hasNext = index >= 0 && index < total - 1
  const verdictOptions =
    item.reviewVerdict === 'DOWNGRADED'
      ? [{ value: 'DOWNGRADED' as TprmVerdict, label: VERDICT_LABEL.DOWNGRADED }, ...REVIEW_VERDICT_OPTIONS]
      : REVIEW_VERDICT_OPTIONS
  const [leaveAsk, setLeaveAsk] = useState(false)
  const pendingNav = useRef<'next' | 'prev' | 'close' | 'return' | 'closeProject' | null>(null)

  useEffect(() => {
    if (!open) {
      setLeaveAsk(false)
      pendingNav.current = null
    }
  }, [open])

  const runNav = (kind: 'next' | 'prev' | 'close' | 'return' | 'closeProject') => {
    if (kind === 'next') {
      if (reviewEditable && !item.reviewVerdict) {
        onError('Select a verdict before Next')
        return
      }
      if (
        reviewEditable
        && item.reviewVerdict === 'ACCEPTED_WITH_OBSERVATION'
        && !(item.reviewComment ?? '').trim()
      ) {
        onError('Review comment is required for Accepted with observation')
        return
      }
      onNext()
      return
    }
    if (kind === 'prev') {
      onPrev()
      return
    }
    if (kind === 'close') {
      onClose()
      return
    }
    if (kind === 'return') {
      onReturnToVendor?.()
      return
    }
    if (reviewEditable && onCloseProject) {
      if (!item.reviewVerdict) {
        onError('Select a verdict before closing')
        return
      }
      if (item.reviewVerdict === 'ACCEPTED_WITH_OBSERVATION' && !(item.reviewComment ?? '').trim()) {
        onError('Review comment is required for Accepted with observation')
        return
      }
      onCloseProject()
      return
    }
    onClose()
  }

  const requestNav = (kind: 'next' | 'prev' | 'close' | 'return' | 'closeProject') => {
    if (reviewEditable && dirty) {
      pendingNav.current = kind
      setLeaveAsk(true)
      return
    }
    runNav(kind)
  }

  const goNext = () => requestNav('next')
  const goPrev = () => requestNav('prev')
  const requestClose = () => requestNav('close')
  const finishLast = () => requestNav(onCloseProject ? 'closeProject' : 'close')

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={busy ? undefined : () => requestClose()}
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
                  {item.controlCode}
                </Typography>
                {item.domain ? (
                  <Typography sx={{ fontSize: '0.75rem', color: brand.mutedLight }}>{item.domain}</Typography>
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
                {item.requirement || '-'}
              </Typography>
              <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap sx={{ mt: 1 }}>
                {item.vendorAnswer ? (
                  <StatusChip label={answerLabel(item.vendorAnswer)} tone={answerTone(item.vendorAnswer)} />
                ) : (
                  <StatusChip label="Not answered" tone="neutral" />
                )}
                {ev.missing ? (
                  <StatusChip label="Evidence required" tone="warning" />
                ) : files.length > 0 ? (
                  <StatusChip
                    label={`${files.length} file${files.length === 1 ? '' : 's'}`}
                    tone="success"
                  />
                ) : ev.ref ? (
                  <StatusChip label="Text reference" tone="info" />
                ) : item.evidenceMandatory ? (
                  <StatusChip label="No evidence" tone="neutral" />
                ) : (
                  <StatusChip label="Evidence optional" tone="neutral" />
                )}
                {reviewable && item.reviewVerdict ? (
                  <StatusChip label={VERDICT_LABEL[item.reviewVerdict]} tone={verdictTone(item.reviewVerdict)} />
                ) : reviewable ? (
                  <StatusChip label="Verdict pending" tone="neutral" />
                ) : null}
                {reviewEditable ? (
                  <StatusChip label={dirty ? 'Unsaved' : 'Editable'} tone={dirty ? 'warning' : 'info'} />
                ) : (
                  <StatusChip label="View only" tone="neutral" />
                )}
              </Stack>
            </Box>
            <IconButton size="small" onClick={requestClose} aria-label="Close" sx={{ mt: -0.25 }} disabled={busy}>
              <CloseRoundedIcon fontSize="small" />
            </IconButton>
          </Stack>
        </Box>

        <Box sx={{ flex: 1, overflow: 'auto', px: 2.25, py: 2 }}>
          <Stack spacing={2.25}>
            {((item.description?.trim()
              && item.description.trim().toLowerCase() !== (item.requirement ?? '').trim().toLowerCase())
              || item.requiredEvidence?.trim()) && (
              <Section title="Control">
                <Dossier>
                  {item.description?.trim()
                    && item.description.trim().toLowerCase() !== (item.requirement ?? '').trim().toLowerCase() ? (
                    <DossierRow label="Detail">{displayOrEmpty(item.description)}</DossierRow>
                  ) : null}
                  {item.requiredEvidence?.trim() ? (
                    <DossierRow label="Reference evidence required">{item.requiredEvidence.trim()}</DossierRow>
                  ) : null}
                </Dossier>
              </Section>
            )}

            <Section title="Vendor response">
              <Dossier>
                <DossierRow label="Answer">
                  {item.vendorAnswer ? (
                    <StatusChip label={answerLabel(item.vendorAnswer)} tone={answerTone(item.vendorAnswer)} />
                  ) : (
                    <EmptyValue />
                  )}
                </DossierRow>
                <DossierRow label="Justification">{displayOrEmpty(item.justification)}</DossierRow>
              </Dossier>
            </Section>

            <Section title="Evidence">
              <Dossier>
                <DossierRow label="Files">
                  {files.length > 0 ? (
                    <EvidenceAttachedList
                      items={files}
                      numbered={files.length > 1}
                      flush
                      emptyLabel=""
                      singleColumn
                      onError={onError}
                    />
                  ) : (
                    <EmptyValue />
                  )}
                </DossierRow>
                <DossierRow label="Details of the uploaded evidence">{displayOrEmpty(item.evidenceRef)}</DossierRow>
              </Dossier>
            </Section>

            {reviewable && (
              <Section title="Review">
                {reviewEditable ? (
                  <Stack spacing={1.25}>
                    <FormSelect
                      label="Admin verdict"
                      value={item.reviewVerdict ?? ''}
                      options={[{ value: '', label: 'Select' }, ...verdictOptions]}
                      onChange={(v) => onPatch({ reviewVerdict: v ? (v as TprmVerdict) : null })}
                      minWidth={220}
                    />
                    <CompactTextField
                      label="Review comment"
                      required={item.reviewVerdict === 'ACCEPTED_WITH_OBSERVATION'}
                      fieldIdPrefix="tprm-rev-c"
                      value={item.reviewComment ?? ''}
                      onChange={(e) => onPatch({ reviewComment: e.target.value })}
                      multiline
                      minRows={2}
                      maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                      fullWidth
                      error={
                        item.reviewVerdict === 'ACCEPTED_WITH_OBSERVATION'
                        && !(item.reviewComment ?? '').trim()
                      }
                      helperText={
                        item.reviewVerdict === 'ACCEPTED_WITH_OBSERVATION'
                        && !(item.reviewComment ?? '').trim()
                          ? 'Review comment is required for Accepted with observation'
                          : undefined
                      }
                    />
                  </Stack>
                ) : (
                  <Dossier>
                    <DossierRow label="Verdict">
                      {item.reviewVerdict ? (
                        <StatusChip
                          label={VERDICT_LABEL[item.reviewVerdict]}
                          tone={verdictTone(item.reviewVerdict)}
                        />
                      ) : (
                        <EmptyValue />
                      )}
                    </DossierRow>
                    <DossierRow label="Comment">{displayOrEmpty(item.reviewComment)}</DossierRow>
                  </Dossier>
                )}
              </Section>
            )}

            {progressHint ? (
              <Typography sx={{ fontSize: '0.75rem', color: brand.mutedLight, lineHeight: 1.45 }}>
                {progressHint}
              </Typography>
            ) : null}
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
          <Stack direction="row" spacing={0.5} alignItems="center">
            <Tooltip title="Previous">
              <span>
                <IconButton
                  size="small"
                  aria-label="Previous"
                  disabled={!hasPrev || busy}
                  onClick={goPrev}
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
            <Tooltip title="Next">
              <span>
                <IconButton
                  size="small"
                  aria-label="Next"
                  disabled={!hasNext || busy}
                  onClick={goNext}
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
            <Typography sx={{ fontSize: '0.75rem', color: brand.muted, ml: 0.5, fontVariantNumeric: 'tabular-nums' }}>
              {index + 1} of {total}
            </Typography>
          </Stack>
          <Stack direction="row" spacing={1} alignItems="center">
            <Button size="small" onClick={requestClose} disabled={busy}>
              Close
            </Button>
            {reviewEditable && onSave && (
              <BusyButton
                size="small"
                variant="outlined"
                busy={busy}
                disabled={!dirty}
                disabledReason={!dirty ? 'No review changes to save' : undefined}
                onClick={() => void onSave()}
                sx={{ minWidth: 72 }}
              >
                Save
              </BusyButton>
            )}
            {reviewEditable && onReturnToVendor && (
              <BusyButton
                size="small"
                variant="outlined"
                color="warning"
                busy={busy}
                onClick={() => requestNav('return')}
              >
                Return to Vendor
              </BusyButton>
            )}
            {reviewEditable && !hasNext && onCloseProject && (
              <BusyButton size="small" variant="contained" busy={busy} onClick={finishLast}>
                Close Project
              </BusyButton>
            )}
          </Stack>
        </Stack>
      </Stack>
      <DiscardChangesDialog
        open={leaveAsk}
        title="Leave without saving?"
        message="You have unsaved review changes. Save them, discard them, or keep editing."
        keepEditingLabel="Keep editing"
        saveDraftLabel="Save Review"
        discardLabel="Discard Changes"
        saveDraftBusy={busy}
        onKeepEditing={() => {
          setLeaveAsk(false)
          pendingNav.current = null
        }}
        onSaveDraft={
          onSave
            ? () => {
                void (async () => {
                  try {
                    const ok = await onSave()
                    if (ok === false) return
                    const kind = pendingNav.current
                    setLeaveAsk(false)
                    pendingNav.current = null
                    if (kind) runNav(kind)
                  } catch {
                    /* parent surfaces error; stay on dialog */
                  }
                })()
              }
            : undefined
        }
        onDiscard={() => {
          onDiscard?.()
          const kind = pendingNav.current
          setLeaveAsk(false)
          pendingNav.current = null
          if (kind) runNav(kind)
        }}
      />
    </Drawer>
  )
}
