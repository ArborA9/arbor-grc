import { Box, Typography } from '@mui/material'
import { type EvidenceClassification, type EvidenceSummary } from '../../api'
import ContributorEvidencePanel from './ContributorEvidencePanel'
import { gapContributorTextIssue } from '../gap/gapContributorText'
import CompactTextField from '../ui/CompactTextField'
import { sectionLabelSx } from '../ui/FormFieldLabel'
import { NARRATIVE_MAX_ROWS, narrativeReadScrollSx } from '../ui/formLayout'
import { INPUT_LIMIT } from '../ui/inputLimits'
import { brand, semantic } from '../../theme'

export type GapStepQuestion = {
  key: string
  questionText: string
}

const ANSWER_OPTIONS: {
  value: string
  label: string
  hint: string
  mark: string
  tone?: 'good' | 'warn' | 'bad' | 'muted'
}[] = [
  { value: 'YES', label: 'Compliant', hint: 'Fully implemented', mark: '✓', tone: 'good' },
  { value: 'PARTIAL', label: 'Partially implemented', hint: 'Some requirements are met', mark: '◐', tone: 'warn' },
  { value: 'NO', label: 'Non-compliant', hint: 'Requirement is not met', mark: '!', tone: 'bad' },
  { value: 'NA', label: 'N/A', hint: 'Not applicable', mark: '-', tone: 'muted' },
]

const TONE_STYLES = {
  good: { bg: '#F0FDF4', color: semantic.gap.compliant.text, border: '#166534' },
  warn: { bg: '#FFFBEB', color: semantic.gap.partial.text, border: '#CA8A04' },
  bad: { bg: '#FEF2F2', color: semantic.gap.nonCompliant.text, border: '#B91C1C' },
  muted: { bg: '#F5F5F4', color: '#44403C', border: '#78716C' },
}

export default function ContributorGapQuestionStep({
  questions,
  questionIndex,
  answers,
  disabled,
  onAnswer,
  onNote,
  showUpload,
  linkedEvidence,
  evidenceFile,
  evidenceClass,
  evidenceClasses,
  busy,
  onSelectFile,
  onEvidenceClass,
  onUpload,
  evidenceAccept,
  maxBytes,
  maxFiles,
  currentUserId,
  onEvidenceError,
  onRemoveEvidence,
  onDownload,
  onGoToQuestion,
}: {
  questions: GapStepQuestion[]
  questionIndex: number
  answers: Record<string, { answer: string; note: string }>
  disabled: boolean
  onAnswer: (key: string, answer: string) => void
  onNote: (key: string, note: string) => void
  showUpload?: boolean
  linkedEvidence?: EvidenceSummary[]
  evidenceFile?: File | null
  evidenceClass?: EvidenceClassification | ''
  evidenceClasses?: EvidenceClassification[]
  busy?: boolean
  onSelectFile?: (file: File | null) => void
  onEvidenceClass?: (c: EvidenceClassification | '') => void
  onUpload?: () => void
  evidenceAccept?: string
  maxBytes?: number
  maxFiles?: number
  currentUserId?: number
  onEvidenceError?: (message: string) => void
  onRemoveEvidence?: (item: EvidenceSummary) => void
  onDownload?: (evidenceId: number) => void
  onGoToQuestion?: (index: number) => void
  controlCode?: string
  controlTitle?: string
  returned?: boolean
}) {
  if (questions.length === 0) return null
  const q = questions[questionIndex]
  if (!q) return null
  const selected = answers[q.key]?.answer ?? ''
  const note = answers[q.key]?.note ?? ''
  const noteIssue =
    gapContributorTextIssue(note) ??
    (selected && !note.trim() ? 'Your response is required' : null)
  const answeredCount = questions.filter(
    (item) => Boolean(answers[item.key]?.answer) && Boolean(answers[item.key]?.note?.trim()),
  ).length
  const pct = questions.length > 0 ? Math.round((answeredCount / questions.length) * 100) : 0

  return (
    <Box
      key={q.key}
      sx={{
        '@keyframes gapQuestionIn': {
          from: { opacity: 0, transform: 'translateY(4px)' },
          to: { opacity: 1, transform: 'none' },
        },
        animation: 'gapQuestionIn 160ms ease',
      }}
    >
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 1fr) minmax(180px, 220px)' },
        gap: { xs: 3, md: 4 },
        alignItems: 'start',
        bgcolor: brand.paper,
        borderRadius: 1,
        p: 2,
      }}
    >
      <Box sx={{ minWidth: 0 }}>
        <Typography sx={{ ...sectionLabelSx, mb: 0.5, color: brand.goldDark, fontWeight: 700 }}>Question</Typography>
        <Typography
          sx={{
            fontSize: '0.875rem',
            fontWeight: 600,
            color: brand.ink,
            lineHeight: 1.35,
            letterSpacing: '-0.01em',
            mb: 1.5,
            ...narrativeReadScrollSx,
          }}
        >
          {q.questionText}
        </Typography>

        <Box sx={{ borderTop: `1px solid ${brand.border}`, pt: 1.5, mb: 1.5 }}>
          <Typography sx={{ ...sectionLabelSx, mb: 0.75, color: brand.goldDark, fontWeight: 700 }}>Compliance Status</Typography>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 0.75 }}>
            {ANSWER_OPTIONS.map((opt) => {
              const active = selected === opt.value
              const tone = TONE_STYLES[opt.tone ?? 'muted']
              return (
                <Box
                  key={opt.value}
                  component="button"
                  type="button"
                  aria-label={opt.label}
                  aria-pressed={active}
                  disabled={disabled}
                  onClick={() => onAnswer(q.key, opt.value)}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1,
                    width: '100%',
                    minHeight: 56,
                    px: 1.25,
                    py: 0.75,
                    textAlign: 'left',
                    borderRadius: 1,
                    border: `1px solid ${active ? tone.border : brand.border}`,
                    boxShadow: active ? `inset 0 0 0 1px ${tone.border}` : 'none',
                    cursor: disabled ? 'default' : 'pointer',
                    font: 'inherit',
                    bgcolor: active ? tone.bg : brand.paper,
                    color: brand.ink,
                    opacity: disabled ? 0.65 : 1,
                    transition: 'border-color 140ms ease, background-color 140ms ease, box-shadow 140ms ease',
                    '&:hover': disabled ? undefined : { borderColor: active ? tone.border : brand.goldMid },
                  }}
                >
                  <Box
                    sx={{
                    width: 16,
                    height: 16,
                    borderRadius: '50%',
                    flexShrink: 0,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '0.625rem',
                    fontWeight: 700,
                      bgcolor: active ? tone.border : 'transparent',
                      color: active ? '#fff' : brand.muted,
                      border: active ? 0 : `1px solid ${brand.border}`,
                    }}
                  >
                    {active ? '✓' : opt.mark}
                  </Box>
                  <Box sx={{ minWidth: 0 }}>
                    <Typography
                      sx={{
                        fontSize: '0.75rem',
                        fontWeight: 600,
                        letterSpacing: '0.02em',
                        textTransform: 'uppercase',
                        lineHeight: 1.2,
                        color: active ? tone.color : brand.ink,
                      }}
                    >
                      {opt.label}
                    </Typography>
                    <Typography sx={{ fontSize: '0.6875rem', color: brand.muted, mt: 0.125, lineHeight: 1.3 }}>{opt.hint}</Typography>
                  </Box>
                </Box>
              )
            })}
          </Box>
        </Box>
      </Box>
      <Box
        sx={{
          position: { md: 'sticky' },
          top: { md: 8 },
          order: { xs: 1, md: 0 },
          pl: { md: 2.5 },
          borderLeft: { md: `1px solid ${brand.border}` },
        }}
      >
        <Typography sx={{ ...sectionLabelSx, mb: 0.75, color: brand.goldDark, fontWeight: 700 }}>Question progress</Typography>
        <Box sx={{ display: 'flex', flexDirection: 'column' }}>
          {questions.map((item, i) => {
            const done =
              Boolean(answers[item.key]?.answer) && Boolean(answers[item.key]?.note?.trim())
            const current = i === questionIndex
            const last = i === questions.length - 1
            return (
              <Box
                key={item.key}
                component="button"
                type="button"
                onClick={() => onGoToQuestion?.(i)}
                sx={{
                  display: 'flex',
                  alignItems: 'stretch',
                  gap: 1.25,
                  width: '100%',
                  p: 0,
                  border: 0,
                  bgcolor: 'transparent',
                  cursor: 'pointer',
                  font: 'inherit',
                  textAlign: 'left',
                  color: current ? brand.ink : brand.muted,
                }}
              >
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', width: 16, flexShrink: 0 }}>
                  <Box
                    sx={{
                      width: 10,
                      height: 10,
                      mt: '5px',
                      borderRadius: '50%',
                      flexShrink: 0,
                      bgcolor: current ? brand.gold : done ? brand.ink : 'transparent',
                      border: current || done ? 0 : `1.5px solid ${brand.border}`,
                      boxShadow: current ? `0 0 0 3px ${brand.goldLight}` : 'none',
                    }}
                  />
                  {!last ? <Box sx={{ width: '1px', flex: 1, minHeight: 22, bgcolor: brand.border }} /> : null}
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, pb: last ? 0 : 1.75, minHeight: 36 }}>
                  <Typography sx={{ fontSize: '0.6875rem', fontWeight: 650, fontVariantNumeric: 'tabular-nums', width: 16 }}>
                    {String(i + 1).padStart(2, '0')}
                  </Typography>
                  <Typography sx={{ fontSize: '0.6875rem', fontWeight: current ? 650 : 500 }}>
                    {done && !current ? 'Completed' : current ? 'Current' : ''}
                  </Typography>
                </Box>
              </Box>
            )
          })}
        </Box>
        <Typography sx={{ fontSize: '0.75rem', color: brand.ink, mt: 1, fontWeight: 600 }}>
          {questionIndex + 1} of {questions.length}
        </Typography>
        <Typography sx={{ fontSize: '0.6875rem', color: brand.muted }}>{pct}% complete</Typography>
      </Box>
    </Box>
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', md: showUpload ? '1fr 1fr' : '1fr' },
        columnGap: 2,
        rowGap: 1.5,
        alignItems: 'start',
        bgcolor: brand.paper,
        borderRadius: 1,
        p: 2,
        mt: 2,
      }}
    >
      <CompactTextField
        label="Your response"
        labelColor={brand.goldDark}
        required
        multiline
        minRows={4}
        maxRows={NARRATIVE_MAX_ROWS}
        fullWidth
        maxLength={INPUT_LIMIT.notes}
        disabled={disabled}
        value={note}
        onChange={(e) => onNote(q.key, e.target.value)}
        placeholder="Describe the evidence, observations or rationale supporting your assessment."
        error={Boolean(noteIssue)}
        helperText={noteIssue || undefined}
        sx={{
          '& .MuiOutlinedInput-root.MuiInputBase-multiline': {
            bgcolor: brand.paper,
            px: 0.5,
            py: 0.5,
            '& .MuiOutlinedInput-notchedOutline': { borderColor: '#D6D3D1' },
            '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: brand.gold },
          },
          '& .MuiInputBase-inputMultiline': {
            fontSize: '0.75rem',
            lineHeight: 1.4,
            py: 0.75,
          },
        }}
      />
      {showUpload ? (
        <ContributorEvidencePanel
          headingUppercase
          linkedEvidence={linkedEvidence ?? []}
          evidenceFile={evidenceFile ?? null}
          evidenceClass={evidenceClass ?? ''}
          evidenceClasses={evidenceClasses ?? []}
          busy={Boolean(busy)}
          disabled={disabled}
          emptyLabel="No files on this question yet"
          evidenceAccept={evidenceAccept}
          maxBytes={maxBytes}
          maxFiles={maxFiles}
          currentUserId={currentUserId}
          capScopeLabel="this question"
          onSelectFile={onSelectFile ?? (() => undefined)}
          onEvidenceClass={onEvidenceClass ?? (() => undefined)}
          onUpload={onUpload ?? (() => undefined)}
          onError={onEvidenceError}
          onDownload={onDownload ?? (() => undefined)}
          onRemove={onRemoveEvidence}
        />
      ) : null}
    </Box>
    </Box>
  )
}
