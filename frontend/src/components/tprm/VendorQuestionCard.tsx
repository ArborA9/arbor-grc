import { Box, Button, Stack, Typography } from '@mui/material'
import AttachFileOutlinedIcon from '@mui/icons-material/AttachFileOutlined'
import type { TprmAnswer, TprmAssessment, TprmAssessmentItem } from '../../api'
import { vendorPortalApi } from '../../api'
import EvidenceAttachedList from '../evidence/EvidenceAttachedList'
import {
  CompactTextField,
  DomainBadge,
  FieldActionRow,
  FormFieldLabel,
  NARRATIVE_FIELD_MAX_WIDTH,
  controlActionSx,
  readOnlyCaptionSx,
} from '../ui'
import FormSelect from '../ui/FormSelect'
import { brand, semantic } from '../../theme'
import {
  ANSWER_OPTIONS,
  MAX_EVIDENCE_FILES,
  evidenceState,
  formatPortalError,
  toEvidenceSummaries,
} from './vendorPortalItems'

function EvidenceChip({ it }: { it: TprmAssessmentItem }) {
  if (!it.evidenceMandatory) {
    return <DomainBadge variant="domain" label="Evidence optional" />
  }
  if (it.vendorAnswer === 'NA') {
    return <DomainBadge variant="soa" status="na" label="Not required (N/A)" />
  }
  const ev = evidenceState(it)
  if (ev.fileCount > 0) {
    return <DomainBadge variant="gap" status="compliant" label={`${ev.fileCount} file${ev.fileCount === 1 ? '' : 's'}`} />
  }
  if (ev.required) {
    return <DomainBadge variant="gap" status="partial" label="Evidence required" />
  }
  return <DomainBadge variant="domain" label="Evidence optional" />
}

export default function VendorQuestionCard({
  it,
  vendorToken,
  showErrors,
  updateRequired = false,
  onPatch,
  onEvidenceUploaded,
  onError,
}: {
  it: TprmAssessmentItem
  vendorToken: string
  showErrors: boolean
  /** Returned control still matches the hand-off snapshot (must change before submit). */
  updateRequired?: boolean
  onPatch: (itemId: number, patch: Partial<TprmAssessmentItem>) => void
  onEvidenceUploaded: (server: TprmAssessment) => void
  onError: (msg: string) => void
}) {
  const ev = evidenceState(it)
  const answerMissing = showErrors && !it.vendorAnswer
  const justificationMissing = showErrors && !(it.justification ?? '').trim()
  const evidenceErr = showErrors && ev.missing
  const unchangedErr = showErrors && updateRequired
  const complete = Boolean(it.vendorAnswer) && Boolean((it.justification ?? '').trim()) && !ev.missing && !updateRequired
  const accent = it.returned
    ? semantic.risk.medium
    : answerMissing || justificationMissing || evidenceErr || unchangedErr
      ? brand.gold
      : complete
        ? semantic.risk.low
        : brand.gold
  const canAddFile = ev.fileCount < MAX_EVIDENCE_FILES
  const summaries = toEvidenceSummaries(it)
  const showDescription =
    Boolean(it.description?.trim()) &&
    it.description.trim().toLowerCase() !== (it.requirement ?? '').trim().toLowerCase()

  return (
    <Box
      id={`vq-${it.controlCode}`}
      sx={{
        bgcolor: brand.paper,
        border: `1px solid ${answerMissing || evidenceErr ? brand.goldMid : brand.border}`,
        borderRadius: 1.5,
        boxShadow: brand.shadowCard,
                overflow: 'hidden',
                scrollMarginTop: 16,
      }}
    >
      <Box sx={{ display: 'flex' }}>
        <Box sx={{ width: 4, flexShrink: 0, bgcolor: accent }} />
        <Box sx={{ flex: 1, minWidth: 0, p: { xs: 1.75, md: 2.25 } }}>
          <Stack direction="row" spacing={1} alignItems="flex-start" justifyContent="space-between" sx={{ mb: 1 }}>
            <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap sx={{ minWidth: 0 }}>
              <Box
                sx={{
                  minHeight: 28,
                  px: 0.75,
                  py: 0.25,
                  borderRadius: 1,
                  bgcolor: brand.goldLight,
                  color: brand.goldDark,
                  fontSize: '0.6875rem',
                  fontWeight: 700,
                  display: 'inline-flex',
                  alignItems: 'center',
                  flexShrink: 0,
                  fontFamily: 'ui-monospace, Menlo, monospace',
                  letterSpacing: '0.02em',
                  maxWidth: '100%',
                  wordBreak: 'break-word',
                }}
              >
                {it.controlCode || '-'}
              </Box>
              {it.returned && (
                <DomainBadge
                  variant="gap"
                  status="partial"
                  label={updateRequired ? 'Update required' : 'Returned'}
                />
              )}
              {complete && !it.returned && <DomainBadge variant="gap" status="compliant" label="Complete" />}
            </Stack>
            <Box sx={{ flexShrink: 0 }}>
              <EvidenceChip it={it} />
            </Box>
          </Stack>

          <Typography sx={{ fontSize: '0.9375rem', fontWeight: 600, color: brand.ink, lineHeight: 1.4, mb: 0.5 }}>
            {it.requirement}
          </Typography>
          {showDescription && (
            <Typography sx={{ fontSize: '0.8125rem', color: brand.muted, lineHeight: 1.5, mb: 1 }}>
              {it.description}
            </Typography>
          )}
          {it.reviewComment && it.returned && (
            <Box
              sx={{
                mb: 1.25,
                px: 1.25,
                py: 1,
                borderRadius: 1,
                bgcolor: '#FFFBEB',
                border: `1px solid ${brand.goldMid}`,
              }}
            >
              <Typography sx={{ ...readOnlyCaptionSx, color: brand.goldDark }}>
                Reviewer comment
              </Typography>
              <Typography sx={{ fontSize: '0.875rem', color: brand.inkSoft, lineHeight: 1.45 }}>
                {it.reviewComment}
              </Typography>
            </Box>
          )}
          {it.requiredEvidence && (
            <Box
              sx={{
                mb: 1.5,
                px: 1.25,
                py: 1,
                borderRadius: 1,
                bgcolor: brand.hover,
                border: `1px solid ${brand.border}`,
              }}
            >
              <Typography sx={readOnlyCaptionSx}>
                {ev.required ? 'Reference evidence required' : 'Suggested evidence'}
              </Typography>
              <Typography sx={{ fontSize: '0.8125rem', color: evidenceErr ? brand.goldDark : brand.muted, lineHeight: 1.45, fontWeight: ev.required ? 600 : 400 }}>
                {it.requiredEvidence}
              </Typography>
            </Box>
          )}

          <FieldActionRow sx={{ width: '100%', flexWrap: { md: 'nowrap' } }}>
            <FormSelect
              label="Answer"
              required
              value={it.vendorAnswer ?? ''}
              options={ANSWER_OPTIONS}
              onChange={(v) => onPatch(it.id, { vendorAnswer: v ? (v as TprmAnswer) : null })}
              minWidth={160}
              error={answerMissing}
              helperText={answerMissing ? 'Answer is required' : ''}
            />
            <Box sx={{ flex: 1, minWidth: { xs: 0, sm: 180 }, maxWidth: NARRATIVE_FIELD_MAX_WIDTH }}>
              <CompactTextField
                label="Justification"
                required
                value={it.justification ?? ''}
                onChange={(e) => onPatch(it.id, { justification: e.target.value })}
                maxLength={2000}
                error={justificationMissing}
                helperText={justificationMissing ? 'Justification is required' : ''}
              />
            </Box>
          </FieldActionRow>

          <Box
            sx={{
              mt: 1.5,
              pt: 1.5,
              borderTop: `1px solid ${brand.border}`,
            }}
          >
            <FieldActionRow sx={{ width: '100%', mb: summaries.length ? 1 : 0 }}>
              <Box sx={{ flex: 1, minWidth: { xs: 0, sm: 160 }, maxWidth: 320 }}>
                <CompactTextField
                  label="Details of the uploaded evidence"
                  value={it.evidenceRef ?? ''}
                  onChange={(e) => onPatch(it.id, { evidenceRef: e.target.value })}
                />
              </Box>
              <Box sx={{ minWidth: 0, flex: 1 }}>
                <FormFieldLabel required={ev.required}>
                  Evidence files (up to {MAX_EVIDENCE_FILES})
                </FormFieldLabel>
                {canAddFile ? (
                  <Button
                    size="small"
                    variant="outlined"
                    component="label"
                    startIcon={<AttachFileOutlinedIcon sx={{ fontSize: 16 }} />}
                    sx={{ ...controlActionSx, textTransform: 'none' }}
                  >
                    Add File
                    <input
                      hidden
                      type="file"
                      onChange={(e) => {
                        const file = e.target.files?.[0]
                        if (file) {
                          void vendorPortalApi
                            .uploadAnswerEvidence(vendorToken, it.id, file)
                            .then(onEvidenceUploaded)
                            .catch((err) => onError(formatPortalError(err)))
                        }
                        e.target.value = ''
                      }}
                    />
                  </Button>
                ) : (
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block', lineHeight: '32px' }}>
                    Maximum {MAX_EVIDENCE_FILES} files reached for this control.
                  </Typography>
                )}
              </Box>
            </FieldActionRow>
            <EvidenceAttachedList
              items={summaries}
              numbered={summaries.length > 1}
              flush
              emptyLabel={ev.required ? 'Attach at least one file. A text reference is not enough.' : 'No files attached'}
              onError={onError}
              fetchBlob={(id) => vendorPortalApi.fetchAnswerEvidenceBlob(vendorToken, it.id, id)}
              download={(id) => vendorPortalApi.downloadAnswerEvidence(vendorToken, it.id, id)}
              onRemove={(evItem) => {
                void vendorPortalApi
                  .removeAnswerEvidence(vendorToken, it.id, evItem.id)
                  .then(onEvidenceUploaded)
                  .catch((err) => onError(formatPortalError(err)))
              }}
            />
            {evidenceErr && (
              <Typography sx={{ fontSize: '0.6875rem', color: brand.goldDark, mt: 1, fontWeight: 600 }}>
                Attach a file. A text reference is not enough. N/A does not require evidence.
              </Typography>
            )}
          </Box>
        </Box>
      </Box>
    </Box>
  )
}
