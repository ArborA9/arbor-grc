import type { ReactNode } from 'react'
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from '@mui/material'
import BusyButton from './BusyButton'

export default function ConfirmActionDialog({
  open,
  title,
  message,
  children,
  confirmLabel = 'Delete',
  confirmColor = 'error',
  confirmDisabled,
  disabledReason,
  busy,
  busyLabel = 'Please wait...',
  onCancel,
  onConfirm,
}: {
  open: boolean
  title: string
  message: string
  children?: ReactNode
  confirmLabel?: string
  confirmColor?: 'error' | 'warning' | 'primary'
  confirmDisabled?: boolean
  disabledReason?: string
  busy?: boolean
  busyLabel?: string
  onCancel: () => void
  onConfirm: () => void
}) {
  return (
    <Dialog open={open} onClose={busy ? undefined : onCancel} maxWidth={children ? 'sm' : 'xs'} fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <DialogContentText sx={children ? { mb: 1.5 } : undefined}>{message}</DialogContentText>
        {children}
      </DialogContent>
      <DialogActions sx={{ px: 2.5, pb: 2 }}>
        <Button onClick={onCancel} disabled={busy}>
          Cancel
        </Button>
        <BusyButton
          color={confirmColor}
          variant="contained"
          busy={busy}
          busyLabel={busyLabel}
          disabled={confirmDisabled}
          disabledReason={disabledReason}
          onClick={onConfirm}
        >
          {confirmLabel}
        </BusyButton>
      </DialogActions>
    </Dialog>
  )
}
