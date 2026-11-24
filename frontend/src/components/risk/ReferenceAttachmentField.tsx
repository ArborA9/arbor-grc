import {
  EVIDENCE_CLASSIFICATIONS,
  type EvidenceClassification,
  type EvidenceSummary,
} from '../../api'
import ContributorEvidencePanel from '../contributor/ContributorEvidencePanel'
import { Box } from '@mui/material'
import FormFieldLabel from '../ui/FormFieldLabel'
import { EXCEL_FIELD } from './excelRiskFields'
import { RISK_EVIDENCE_MAX_BYTES, RISK_EVIDENCE_MAX_FILES } from './riskEvidenceLimits'

/** Client product exception vs Excel singular "reference attachment". */
export const MAX_REFERENCE_ATTACHMENTS = RISK_EVIDENCE_MAX_FILES

export default function ReferenceAttachmentField({
  pendingFiles,
  classification,
  saved,
  busy = false,
  disabled = false,
  hideInlineProgress = false,
  onPendingFilesChange,
  onClassification,
  onUploadPending,
  onRemoveSaved,
  onError,
  quotaUsed,
}: {
  pendingFiles: File[]
  classification: EvidenceClassification | ''
  saved: EvidenceSummary[]
  busy?: boolean
  disabled?: boolean
  hideInlineProgress?: boolean
  onPendingFilesChange: (files: File[]) => void
  onClassification: (c: EvidenceClassification | '') => void
  onUploadPending: () => void
  onRemoveSaved?: (item: EvidenceSummary) => void
  onError?: (message: string) => void
  quotaUsed?: number
}) {
  const used = quotaUsed ?? saved.length
  const selected = pendingFiles[0] ?? null

  return (
    <Box sx={{ minWidth: 0 }}>
      {/* Plain labelled field: no nested card, full width of its grid cell. */}
      <FormFieldLabel hint="Optional" hintPlacement="inline">
        {EXCEL_FIELD.referenceAttachment}
      </FormFieldLabel>
      <Box>
        <ContributorEvidencePanel
          showHeading={false}
          framed
          linkedEvidence={saved}
          evidenceFile={selected}
          evidenceClass={classification}
          evidenceClasses={EVIDENCE_CLASSIFICATIONS}
          maxBytes={RISK_EVIDENCE_MAX_BYTES}
          maxFiles={MAX_REFERENCE_ATTACHMENTS}
          fileCountForCap={used}
          busy={busy}
          disabled={disabled || busy}
          hideInlineProgress={hideInlineProgress}
          emptyLabel="No reference attachments yet"
          onError={onError}
          onSelectFile={(file) => onPendingFilesChange(file ? [file] : [])}
          onEvidenceClass={onClassification}
          onUpload={onUploadPending}
          onDownload={() => {}}
          onRemove={onRemoveSaved}
        />
      </Box>
    </Box>
  )
}
