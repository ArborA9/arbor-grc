import { useState } from 'react'
import { Box, Button, LinearProgress, Typography } from '@mui/material'
import { EVIDENCE_CLASSIFICATION_LABELS, type EvidenceClassification, type EvidenceSummary } from '../../api'
import EvidenceAttachedList from '../evidence/EvidenceAttachedList'
import { EVIDENCE_ACCEPT, fileAcceptIssue } from '../evidence/evidenceFileAccept'
import FieldActionRow from '../ui/FieldActionRow'
import FormSelect from '../ui/FormSelect'
import FormFieldLabel, { CONTROL_HEIGHT, sectionLabelSx } from '../ui/FormFieldLabel'
import { controlActionSx } from '../ui/formLayout'
import { brand } from '../../theme'

export default function GapEvidenceUpload({
  linkedEvidence,
  evidenceFile,
  evidenceClass,
  evidenceClasses,
  busy,
  disabled,
  emptyLabel,
  hint,
  evidenceAccept,
  onSelectFile,
  onEvidenceClass,
  onUpload,
  onEvidenceError,
  onRemoveEvidence,
}: {
  linkedEvidence: EvidenceSummary[]
  evidenceFile?: File | null
  evidenceClass?: EvidenceClassification | ''
  evidenceClasses?: EvidenceClassification[]
  busy?: boolean
  disabled?: boolean
  emptyLabel: string
  hint: string
  evidenceAccept?: string
  onSelectFile?: (file: File | null) => void
  onEvidenceClass?: (c: EvidenceClassification | '') => void
  onUpload?: () => void
  onEvidenceError?: (message: string) => void
  onRemoveEvidence?: (item: EvidenceSummary) => void
}) {
  const [fileIssue, setFileIssue] = useState<string | null>(null)
  const accept = evidenceAccept ?? EVIDENCE_ACCEPT
  const takeFile = (file: File | null) => {
    if (disabled || busy) return
    if (!file) {
      setFileIssue(null)
      onSelectFile?.(null)
      return
    }
    const issue = fileAcceptIssue(file, accept)
    if (issue) {
      setFileIssue(issue)
      onSelectFile?.(null)
      return
    }
    setFileIssue(null)
    onSelectFile?.(file)
  }

  return (
    <Box>
      <Typography sx={sectionLabelSx}>Evidence</Typography>
      <EvidenceAttachedList
        items={linkedEvidence}
        emptyLabel={emptyLabel}
        onError={onEvidenceError}
        onRemove={disabled ? undefined : onRemoveEvidence}
      />
      <Box
        onDragOver={(e) => {
          e.preventDefault()
        }}
        onDrop={(e) => {
          e.preventDefault()
          takeFile(e.dataTransfer.files?.[0] ?? null)
        }}
        sx={{
          mt: 1,
          px: 1.25,
          py: 1,
          border: `1px dashed ${brand.border}`,
          borderRadius: 0.75,
          bgcolor: brand.paper,
        }}
      >
        <Typography sx={{ fontSize: '0.75rem', color: brand.muted, lineHeight: 1.4 }}>
          Drop a file here, or{' '}
          <Box
            component="label"
            sx={{
              color: brand.goldDark,
              fontWeight: 600,
              cursor: disabled || busy ? 'default' : 'pointer',
            }}
          >
            browse
            <input
              type="file"
              accept={accept}
              disabled={disabled || busy}
              onChange={(e) => {
                takeFile(e.target.files?.[0] ?? null)
                e.target.value = ''
              }}
              style={{ display: 'none' }}
            />
          </Box>
        </Typography>
        <Typography
          sx={{
            fontSize: '0.6875rem',
            color: fileIssue ? '#DC2626' : brand.mutedLight,
            mt: 0.35,
            lineHeight: 1.35,
          }}
        >
          {fileIssue ?? hint}
        </Typography>
      </Box>
      {!disabled && evidenceFile && (
        <Box sx={{ mt: 1.25 }}>
          <FieldActionRow>
            <Box sx={{ flex: '1 1 160px', minWidth: 0 }}>
              <FormFieldLabel>Selected file</FormFieldLabel>
              <Typography
                noWrap
                title={evidenceFile.name}
                sx={{
                  height: CONTROL_HEIGHT,
                  display: 'flex',
                  alignItems: 'center',
                  fontSize: '0.75rem',
                  fontWeight: 500,
                  color: brand.ink,
                  lineHeight: 1.25,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  border: `1px solid ${brand.border}`,
                  borderRadius: 1,
                  px: 1,
                  bgcolor: brand.paper,
                }}
              >
                {evidenceFile.name}
              </Typography>
            </Box>
            {evidenceClasses && onEvidenceClass && (
              <FormSelect
                label="Classification"
                minWidth={180}
                value={evidenceClass ?? ''}
                onChange={onEvidenceClass}
                options={[
                  { value: '', label: 'Select classification…' },
                  ...evidenceClasses.map((c) => ({
                    value: c,
                    label: EVIDENCE_CLASSIFICATION_LABELS[c],
                  })),
                ]}
              />
            )}
            <Button
              size="small"
              variant="contained"
              disabled={!evidenceFile || !evidenceClass || busy}
              onClick={onUpload}
              sx={controlActionSx}
            >
              {busy ? 'Uploading…' : 'Upload & Link'}
            </Button>
          </FieldActionRow>
          {!evidenceClass ? (
            <Typography sx={{ fontSize: '0.6875rem', color: brand.muted, mt: 0.5 }}>
              Select a classification to upload
            </Typography>
          ) : null}
        </Box>
      )}
      {busy ? (
        <LinearProgress sx={{ mt: 1, height: 3, borderRadius: 1 }} aria-label="Upload in progress" />
      ) : null}
    </Box>
  )
}
