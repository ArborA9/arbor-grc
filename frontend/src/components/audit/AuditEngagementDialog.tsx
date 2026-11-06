import { useState } from 'react'
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack } from '@mui/material'
import BusyButton from '../ui/BusyButton'
import DiscardChangesDialog from '../DiscardChangesDialog'
import type {
  FrameworkSummary,
  GapAssessmentSummary,
  GrcAuditDetail,
  UserSummary,
} from '../../api'
import {
  AuditorAssignmentFields,
  EngagementDetailsFields,
} from './AuditEngagementFormFields'
import { useAuditEngagementForm } from './useAuditEngagementForm'

export default function AuditEngagementDialog({
  open,
  mode,
  detail,
  users,
  frameworks,
  gaps,
  orgUnits,
  onClose,
  onSaved,
  onError,
}: {
  open: boolean
  mode: 'create' | 'edit'
  detail?: GrcAuditDetail | null
  users: UserSummary[]
  frameworks: FrameworkSummary[]
  gaps: GapAssessmentSummary[]
  orgUnits: { id: number; name: string; level: number }[]
  onClose: () => void
  onSaved: (d: GrcAuditDetail) => void
  onError: (msg: string) => void
}) {
  const form = useAuditEngagementForm({
    mode,
    detail,
    users,
    frameworks,
    gaps,
    active: open,
  })
  const [leaveAsk, setLeaveAsk] = useState(false)

  const requestClose = () => {
    if (form.dirty && !form.busy) {
      setLeaveAsk(true)
      return
    }
    onClose()
  }

  const save = async () => {
    const err = form.validate()
    if (err) {
      onError(err)
      return
    }
    try {
      const d = await form.save()
      onSaved(d)
      onClose()
    } catch (e) {
      onError(String(e))
    }
  }

  return (
    <>
    <Dialog open={open} onClose={requestClose} fullWidth maxWidth="md" scroll="paper">
      <DialogTitle>
        {mode === 'create' ? 'Create Audit Engagement' : 'Edit Audit Engagement'}
      </DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2} sx={{ pt: 0.5 }}>
          <Alert severity="info" variant="outlined" sx={{ py: 0.5 }}>
            Engagement period, scope, and auditor team. External auditors receive OTP after save.
          </Alert>
          <EngagementDetailsFields
            mode={mode}
            detail={detail}
            frameworks={frameworks}
            form={form}
          />
          <AuditorAssignmentFields orgUnits={orgUnits} form={form} />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={requestClose} disabled={form.busy}>
          Cancel
        </Button>
        <BusyButton
          variant="contained"
          busy={form.busy}
          busyLabel={mode === 'create' ? 'Creating...' : 'Saving...'}
          onClick={() => void save()}
        >
          {mode === 'create' ? 'Create Engagement' : 'Save Changes'}
        </BusyButton>
      </DialogActions>
    </Dialog>
    <DiscardChangesDialog
      open={leaveAsk}
      title="Leave without saving?"
      message="You have unsaved changes on this audit. Discard them to close."
      onKeepEditing={() => setLeaveAsk(false)}
      onDiscard={() => {
        setLeaveAsk(false)
        onClose()
      }}
    />
    </>
  )
}
