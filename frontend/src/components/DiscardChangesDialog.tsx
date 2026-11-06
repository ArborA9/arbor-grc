import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from '@mui/material'

export default function DiscardChangesDialog({
  open,
  title = 'Discard unsaved changes?',
  message = 'You have unsaved changes. If you leave now, your edits will be lost.',
  onKeepEditing,
  onDiscard,
  onSaveDraft,
  saveDraftLabel = 'Save Draft',
  discardLabel = 'Discard Changes',
  keepEditingLabel = 'Keep editing',
  saveDraftBusy = false,
}: {
  open: boolean
  title?: string
  message?: string
  onKeepEditing: () => void
  onDiscard: () => void
  /** When set, offers saving a draft before leaving (e.g. BIA process form). */
  onSaveDraft?: () => void
  saveDraftLabel?: string
  discardLabel?: string
  keepEditingLabel?: string
  saveDraftBusy?: boolean
}) {
  return (
    <Dialog open={open} onClose={onKeepEditing} maxWidth="xs" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <DialogContentText>{message}</DialogContentText>
      </DialogContent>
      <DialogActions sx={{ px: 2.5, pb: 2, flexWrap: 'wrap', gap: 0.5 }}>
        <Button onClick={onKeepEditing} disabled={saveDraftBusy}>
          {keepEditingLabel}
        </Button>
        {onSaveDraft && (
          <Button variant="outlined" disabled={saveDraftBusy} onClick={onSaveDraft}>
            {saveDraftLabel}
          </Button>
        )}
        <Button color="warning" variant="contained" disabled={saveDraftBusy} onClick={onDiscard}>
          {discardLabel}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
