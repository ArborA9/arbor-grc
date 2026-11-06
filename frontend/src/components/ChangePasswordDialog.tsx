import { useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  Typography,
} from '@mui/material'
import { api, type Session } from '../api'
import { BusyButton, CompactTextField } from './ui'

export default function ChangePasswordDialog({
  open,
  superAdmin,
  required,
  onClose,
  onChanged,
  onSignOut,
}: {
  open: boolean
  superAdmin?: boolean
  /** Forced change after an emergency reset: no Cancel, cannot be dismissed. */
  required?: boolean
  onClose: () => void
  onChanged?: (session: Session) => void
  onSignOut?: () => void
}) {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const why = useMemo(() => {
    if (!currentPassword) return required ? 'Enter the temporary password' : 'Enter your current password'
    if (newPassword.length < 8) return 'New password must be at least 8 characters'
    if (newPassword !== confirmPassword) return 'New password and confirmation must match'
    if (currentPassword === newPassword) return 'New password must be different from the current password'
    return ''
  }, [confirmPassword, currentPassword, newPassword, required])

  const reset = () => {
    setCurrentPassword('')
    setNewPassword('')
    setConfirmPassword('')
    setError('')
    setBusy(false)
  }

  const close = () => {
    if (busy || required) return
    reset()
    onClose()
  }

  const save = async () => {
    if (why) return
    setBusy(true)
    setError('')
    try {
      const session = await api.changePassword(currentPassword, newPassword, superAdmin)
      onChanged?.(session)
      reset()
      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open={open} onClose={close} maxWidth="xs" fullWidth>
      <DialogTitle>{required ? 'Set a New Password' : 'Change Password'}</DialogTitle>
      <DialogContent>
        <Stack spacing={1.5} sx={{ pt: 0.5 }}>
          <Typography variant="body2" color="text.secondary">
            {required
              ? 'Your password was reset. Enter the temporary password, then choose a new one to continue.'
              : 'Choose a new password for this local account.'}
          </Typography>
          <CompactTextField
            label={required ? 'Temporary password' : 'Current password'}
            required
            showPasswordToggle
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            autoComplete="current-password"
          />
          <CompactTextField
            label="New password"
            required
            showPasswordToggle
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            autoComplete="new-password"
            helperText="Minimum 8 characters"
          />
          <CompactTextField
            label="Confirm new password"
            required
            showPasswordToggle
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            autoComplete="new-password"
          />
          {error && (
            <Alert severity="error" onClose={() => setError('')} role="alert">
              {error}
            </Alert>
          )}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 2.5, pb: 2 }}>
        {required ? (
          onSignOut && (
            <Button onClick={onSignOut} disabled={busy}>
              Sign Out
            </Button>
          )
        ) : (
          <Button onClick={close} disabled={busy}>
            Cancel
          </Button>
        )}
        <BusyButton
          variant="contained"
          busy={busy}
          disabled={!!why}
          disabledReason={why || undefined}
          onClick={() => void save()}
        >
          {busy ? 'Saving…' : 'Save'}
        </BusyButton>
      </DialogActions>
    </Dialog>
  )
}
