import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Box, Button, LinearProgress, Stack, Typography } from '@mui/material'
import AttachFileOutlinedIcon from '@mui/icons-material/AttachFileOutlined'
import { EVIDENCE_CLASSIFICATION_LABELS, type EvidenceClassification, type EvidenceSummary } from '../../api'
import EvidenceAttachedList from '../evidence/EvidenceAttachedList'
import { EVIDENCE_ACCEPT, fileAcceptIssue } from '../evidence/evidenceFileAccept'
import CompactTextField from '../ui/CompactTextField'
import FieldActionRow from '../ui/FieldActionRow'
import FormSelect from '../ui/FormSelect'
import FormFieldLabel, { CONTROL_HEIGHT } from '../ui/FormFieldLabel'
import { controlActionSx, MEDIUM_FORM_MAX_WIDTH } from '../ui/formLayout'
import { brand } from '../../theme'

type Props = {
  linkedEvidence: EvidenceSummary[]
  evidenceFile: File | null
  evidenceClass: EvidenceClassification | ''
  evidenceClasses: EvidenceClassification[]
  evidenceAccept?: string
  maxBytes?: number
  maxFiles?: number
  currentUserId?: number
  /** Override how many files count toward maxFiles (e.g. all stages, not only this list). */
  fileCountForCap?: number
  /** Record named in the at-cap message, e.g. "this question". */
  capScopeLabel?: string
  busy: boolean
  disabled?: boolean
  required?: boolean
  hint?: string
  /** Admin Evidence Required text (read-only, two-line scroll). */
  requirementText?: string | null
  /** Excel / sheet heading. Defaults to Evidence, or Evidence (required) when required. */
  heading?: string
  showHeading?: boolean
  /** Section caption in capitals, matching other assessment headings. */
  headingUppercase?: boolean
  emptyLabel?: string
  /** Hide the thin bar under the well (parent shows a page-level create wait). */
  hideInlineProgress?: boolean
  /** One bordered well for the list and Add file so the control reads as a block. */
  framed?: boolean
  /** Drop-zone presentation for the contributor question. Same upload rules. */
  workspace?: boolean
  onError?: (message: string) => void
  /** Extra controls on the post-browse row (stage, link). */
  trailingFields?: ReactNode
  onSelectFile: (file: File | null) => void
  onEvidenceClass: (classification: EvidenceClassification | '') => void
  onUpload: () => void
  onDownload: (evidenceId: number) => void
  onRemove?: (item: EvidenceSummary) => void
}

export default function ContributorEvidencePanel({
  linkedEvidence,
  evidenceFile,
  evidenceClass,
  evidenceClasses,
  evidenceAccept = EVIDENCE_ACCEPT,
  maxBytes,
  maxFiles,
  currentUserId,
  fileCountForCap,
  capScopeLabel = 'this risk',
  busy,
  disabled = false,
  required = false,
  hint,
  requirementText,
  heading,
  showHeading = true,
  headingUppercase = false,
  emptyLabel,
  hideInlineProgress = false,
  framed = false,
  workspace = false,
  onError,
  trailingFields,
  onSelectFile,
  onEvidenceClass,
  onUpload,
  onDownload: _onDownload,
  onRemove,
}: Props) {
  const [fileIssue, setFileIssue] = useState<string | null>(null)
  const classifyRowRef = useRef<HTMLDivElement | null>(null)
  useEffect(() => {
    if (!evidenceFile) return
    const frame = requestAnimationFrame(() => {
      classifyRowRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
    })
    return () => cancelAnimationFrame(frame)
  }, [evidenceFile])
  const mineCount =
    fileCountForCap ??
    (maxFiles != null && currentUserId != null
      ? linkedEvidence.filter((e) => e.createdByUserId === currentUserId).length
      : linkedEvidence.length)
  const atFileCap = maxFiles != null && mineCount >= maxFiles
  const sizeMb = Math.round((maxBytes ?? 25 * 1024 * 1024) / (1024 * 1024))
  const takeFile = (file: File | null) => {
    if (disabled || busy || atFileCap) return
    if (!file) {
      setFileIssue(null)
      onSelectFile(null)
      onEvidenceClass('')
      return
    }
    const issue = fileAcceptIssue(file, evidenceAccept, maxBytes)
    if (issue) {
      setFileIssue(issue)
      onSelectFile(null)
      onEvidenceClass('')
      return
    }
    setFileIssue(null)
    onSelectFile(file)
    onEvidenceClass('')
  }
  const requirementNote = requirementText?.trim() || ''
  const addBlocked = disabled || busy || atFileCap
  const emptyText =
    emptyLabel
    ?? (required ? 'No evidence linked yet: upload before submit' : 'No evidence linked yet')
  const list = framed && linkedEvidence.length === 0 ? (
    <Typography sx={{ fontSize: '0.75rem', color: brand.mutedLight, minHeight: 32, display: 'flex', alignItems: 'center' }}>
      {emptyText}
    </Typography>
  ) : (
    <EvidenceAttachedList
      items={linkedEvidence}
      numbered={false}
      flush={framed}
      emptyLabel={emptyText}
      onError={onError}
      onRemove={onRemove && !disabled ? onRemove : undefined}
    />
  )
  const addRow = !disabled ? (
    workspace ? (
      <>
        <Box
          onDragOver={(e) => e.preventDefault()}
          onDrop={(e) => {
            e.preventDefault()
            takeFile(e.dataTransfer.files?.[0] ?? null)
          }}
          sx={{
            border: `1px dashed ${fileIssue ? '#DC2626' : brand.border}`,
            borderRadius: 1,
            bgcolor: '#FAFAF9',
            px: 1.5,
            py: 1.5,
            textAlign: 'center',
          }}
        >
          <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: brand.ink }}>
            Drop files here
          </Typography>
          <Button
            component="label"
            disabled={addBlocked}
            sx={{ textTransform: 'none', fontWeight: 600, color: brand.goldDark, minWidth: 0, minHeight: 28, px: 0.5, fontSize: '0.75rem' }}
          >
            or browse files
            <input
              hidden
              type="file"
              accept={evidenceAccept}
              disabled={addBlocked}
              onChange={(e) => {
                takeFile(e.target.files?.[0] ?? null)
                e.target.value = ''
              }}
            />
          </Button>
          <Typography sx={{ fontSize: '0.75rem', color: brand.muted, mt: 0.5 }}>
            PDF, Word, Excel, images. Maximum {sizeMb} MB
            {maxFiles != null ? `. Up to ${maxFiles} files` : ''}
          </Typography>
        </Box>
        {fileIssue || atFileCap ? (
          <Typography sx={{ fontSize: '0.75rem', color: fileIssue ? '#DC2626' : brand.muted, mt: 0.75 }}>
            {fileIssue ?? `You have attached ${maxFiles} files (maximum).`}
          </Typography>
        ) : null}
        {evidenceFile && !atFileCap ? (
          <Stack
            ref={classifyRowRef}
            direction={{ xs: 'column', sm: 'row' }}
            spacing={1}
            alignItems={{ sm: 'flex-end' }}
            sx={{ mt: 1.25, scrollMarginBottom: 16 }}
          >
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <FormFieldLabel wrap>Selected file</FormFieldLabel>
              <Typography noWrap title={evidenceFile.name} sx={{ fontSize: '0.8125rem', fontWeight: 600 }}>
                {evidenceFile.name}
              </Typography>
            </Box>
            <FormSelect
              label="Classification"
              wrapLabel
              minWidth={160}
              value={evidenceClass}
              onChange={onEvidenceClass}
              options={[
                { value: '', label: 'Select classification…' },
                ...evidenceClasses.map((c) => ({
                  value: c,
                  label: EVIDENCE_CLASSIFICATION_LABELS[c],
                })),
              ]}
            />
            <Button
              size="small"
              variant="contained"
              disabled={!evidenceFile || !evidenceClass || busy}
              onClick={onUpload}
              sx={{ height: CONTROL_HEIGHT }}
            >
              {busy ? 'Uploading…' : 'Upload & Link'}
            </Button>
          </Stack>
        ) : null}
        {busy && !hideInlineProgress ? (
          <LinearProgress sx={{ mt: 1, height: 3, borderRadius: 1 }} aria-label="Upload in progress" />
        ) : null}
      </>
    ) : (
    <>
      <Box
        onDragOver={(e) => e.preventDefault()}
        onDrop={(e) => {
          e.preventDefault()
          takeFile(e.dataTransfer.files?.[0] ?? null)
        }}
      >
        <FieldActionRow sx={{ width: '100%', mb: fileIssue || hint || requirementNote || atFileCap ? 0 : framed ? 0 : 1.25 }}>
          <Box sx={{ flex: 1, minWidth: { xs: 0, sm: 200 } }}>
            <Typography sx={{ fontSize: '0.75rem', color: brand.muted, lineHeight: '32px' }}>
              PDF, Word, Excel, images · max {sizeMb}MB
              {maxFiles != null ? ` · up to ${maxFiles} files` : ''}
            </Typography>
          </Box>
          <Button
            size="small"
            variant="outlined"
            component="label"
            disabled={addBlocked}
            startIcon={<AttachFileOutlinedIcon sx={{ fontSize: 16 }} />}
            sx={{ ...controlActionSx, textTransform: 'none' }}
          >
            Add File
            <input
              hidden
              type="file"
              accept={evidenceAccept}
              disabled={addBlocked}
              onChange={(e) => {
                takeFile(e.target.files?.[0] ?? null)
                e.target.value = ''
              }}
            />
          </Button>
        </FieldActionRow>
        {fileIssue || hint || atFileCap ? (
          <Typography
            sx={{
              fontSize: '0.6875rem',
              color: fileIssue ? '#DC2626' : brand.mutedLight,
              mt: 0.35,
              mb: framed || requirementNote ? 0 : 1.25,
            }}
          >
            {fileIssue
              ?? (atFileCap
                ? `You have attached ${maxFiles} files on ${capScopeLabel} (maximum).`
                : hint)}
          </Typography>
        ) : null}
        {requirementNote ? (
          <Box sx={{ mt: 0.75, mb: framed ? 0 : 1.25, minWidth: 0 }}>
            <CompactTextField
              label="Evidence required"
              wrapLabel
              multiline
              minRows={2}
              maxRows={2}
              value={requirementNote}
              onChange={() => undefined}
              maxLength={false}
              slotProps={{ htmlInput: { readOnly: true } }}
            />
          </Box>
        ) : null}
      </Box>
      {evidenceFile && !atFileCap ? (
        <Stack
          ref={classifyRowRef}
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1}
          alignItems={{ sm: 'flex-end' }}
          sx={{
            maxWidth: MEDIUM_FORM_MAX_WIDTH,
            position: 'relative',
            zIndex: 1,
            bgcolor: framed ? 'transparent' : brand.paper,
            mt: framed ? 1 : 0,
            scrollMarginBottom: 16,
          }}
        >
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <FormFieldLabel wrap>Selected file</FormFieldLabel>
            <Typography
              noWrap
              title={evidenceFile.name}
              sx={{
                height: CONTROL_HEIGHT,
                display: 'flex',
                alignItems: 'center',
                fontSize: '0.75rem',
                fontWeight: 400,
              }}
            >
              {evidenceFile.name}
            </Typography>
          </Box>
          <FormSelect
            label="Classification"
            wrapLabel
            minWidth={160}
            value={evidenceClass}
            onChange={onEvidenceClass}
            options={[
              { value: '', label: 'Select classification…' },
              ...evidenceClasses.map((c) => ({
                value: c,
                label: EVIDENCE_CLASSIFICATION_LABELS[c],
              })),
            ]}
          />
          {trailingFields}
          <Button
            size="small"
            variant="contained"
            disabled={!evidenceFile || !evidenceClass || busy}
            onClick={onUpload}
            sx={{ height: CONTROL_HEIGHT, bgcolor: brand.goldDark }}
          >
            {busy ? 'Uploading…' : 'Upload & Link'}
          </Button>
        </Stack>
      ) : null}
      {busy && !hideInlineProgress ? (
        <LinearProgress sx={{ mt: 1, height: 3, borderRadius: 1 }} aria-label="Upload in progress" />
      ) : null}
    </>
    )
  ) : null

  const body = (
    <Stack spacing={framed ? 1 : 0}>
      <Box sx={{ mb: framed ? 0 : 1 }}>{list}</Box>
      {addRow}
    </Stack>
  )

  return (
    <Box>
      {showHeading ? (
        <FormFieldLabel wrap={!headingUppercase} required={required} color={headingUppercase ? brand.goldDark : undefined}>
          {heading ?? (required ? 'Evidence (required)' : 'Evidence')}
        </FormFieldLabel>
      ) : null}
      {framed ? (
        <Box
          sx={{
            border: `1px dashed ${brand.border}`,
            borderRadius: 1,
            bgcolor: brand.hover,
            px: 1.5,
            py: 1.25,
          }}
        >
          {body}
        </Box>
      ) : (
        body
      )}
    </Box>
  )
}
