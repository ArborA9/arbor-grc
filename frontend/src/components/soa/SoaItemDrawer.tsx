import { Stack, Typography } from '@mui/material'
import type { SoaApplicability, SoaItem } from '../../api'
import StatusChip from '../StatusChip'
import DiscardChangesDialog from '../DiscardChangesDialog'
import ReviewDossierDrawer from '../ui/ReviewDossierDrawer'
import { BusyButton, CompactTextField, FormSelect, NARRATIVE_MAX_ROWS } from '../ui'

function applicabilityTone(value: SoaApplicability | null | undefined): 'success' | 'neutral' | 'warning' {
  if (value === 'APPLICABLE') return 'success'
  if (value === 'NOT_APPLICABLE') return 'neutral'
  return 'warning'
}

function applicabilityLabel(value: SoaApplicability | null | undefined): string {
  if (value === 'APPLICABLE') return 'Applicable'
  if (value === 'NOT_APPLICABLE') return 'Not applicable'
  return 'Not decided'
}

export default function SoaItemDrawer({
  item,
  frameworkCode,
  items,
  applicability,
  justification,
  remarks,
  editable,
  busy,
  dirty,
  discardOpen,
  onApplicability,
  onJustification,
  onRemarks,
  onClose,
  onSave,
  onOpenItem,
  onKeepEditing,
  onDiscard,
}: {
  item: SoaItem | null
  frameworkCode?: string
  items: SoaItem[]
  applicability: SoaApplicability
  justification: string
  remarks: string
  editable: boolean
  busy: boolean
  dirty: boolean
  discardOpen: boolean
  onApplicability: (v: SoaApplicability) => void
  onJustification: (v: string) => void
  onRemarks: (v: string) => void
  onClose: () => void
  onSave: () => void
  onOpenItem: (item: SoaItem) => void
  onKeepEditing: () => void
  onDiscard: () => void
}) {
  const index = item ? items.findIndex((i) => i.id === item.id) : -1
  const saveWhy =
    !editable
      ? undefined
      : applicability === 'NOT_APPLICABLE' && !justification.trim()
        ? 'Enter a justification for Not applicable'
        : undefined
  const locked = Boolean(item?.applicabilityLocked)
  const quietLocked = locked && frameworkCode === 'ISO_20000'

  return (
    <>
      <ReviewDossierDrawer
        open={!!item}
        onClose={onClose}
        title={item ? `${item.nodeCode} - ${item.title}` : '-'}
        index={index < 0 ? 0 : index}
        total={items.length}
        busy={busy}
        onPrev={() => {
          if (index > 0) onOpenItem(items[index - 1])
        }}
        onNext={() => {
          if (index >= 0 && index < items.length - 1) onOpenItem(items[index + 1])
        }}
        chips={
          <>
            {quietLocked ? null : (
              <StatusChip
                label={applicabilityLabel(item?.applicability ?? null)}
                tone={applicabilityTone(item?.applicability ?? null)}
              />
            )}
            {locked && !quietLocked ? <StatusChip label="Always applicable" tone="info" /> : null}
            {editable ? (
              quietLocked ? (
                dirty ? <StatusChip label="Unsaved" tone="warning" /> : null
              ) : (
                <StatusChip label={dirty ? 'Unsaved' : 'Editable'} tone={dirty ? 'warning' : 'info'} />
              )
            ) : (
              <StatusChip label="View only" tone="neutral" />
            )}
          </>
        }
        footer={
          <>
            <BusyButton size="small" variant="text" busy={busy} onClick={onClose}>
              {editable ? 'Cancel' : 'Close'}
            </BusyButton>
            {editable ? (
              <BusyButton
                size="small"
                variant="contained"
                busy={busy}
                disabled={Boolean(saveWhy) || busy}
                disabledReason={saveWhy}
                onClick={onSave}
              >
                Save
              </BusyButton>
            ) : null}
          </>
        }
      >
        <Stack spacing={1.5}>
          {locked || !editable ? (
            <Stack spacing={0.75}>
              <StatusChip
                label={locked ? 'Applicable (locked)' : applicabilityLabel(applicability)}
                tone={applicability === 'NOT_APPLICABLE' ? 'neutral' : 'success'}
              />
              {locked ? (
                <Typography variant="caption" color="text.secondary">
                  Clause / Always-applicable control: cannot mark Not applicable.
                </Typography>
              ) : null}
            </Stack>
          ) : (
            <FormSelect
              label="Applicability"
              fullWidth
              value={applicability}
              onChange={(v) => onApplicability(v as SoaApplicability)}
              options={[
                { value: 'APPLICABLE', label: 'Applicable' },
                { value: 'NOT_APPLICABLE', label: 'Not applicable' },
              ]}
            />
          )}
          <CompactTextField
            label={
              applicability === 'NOT_APPLICABLE' ? 'Justification' : 'Justification (optional)'
            }
            required={applicability === 'NOT_APPLICABLE'}
            multiline
            minRows={3}
            maxRows={NARRATIVE_MAX_ROWS}
            fullWidth
            value={justification}
            onChange={(e) => onJustification(e.target.value)}
            disabled={!editable}
          />
          <CompactTextField
            label="Remarks (optional)"
            multiline
            minRows={2}
            maxRows={NARRATIVE_MAX_ROWS}
            fullWidth
            value={remarks}
            onChange={(e) => onRemarks(e.target.value)}
            disabled={!editable}
          />
        </Stack>
      </ReviewDossierDrawer>
      <DiscardChangesDialog
        open={discardOpen}
        title="Discard unsaved control changes?"
        message="You have unsaved SoA edits. Save to keep them, or discard to leave."
        saveDraftLabel="Save"
        onSaveDraft={() => {
          onSave()
        }}
        onKeepEditing={onKeepEditing}
        onDiscard={onDiscard}
      />
    </>
  )
}
