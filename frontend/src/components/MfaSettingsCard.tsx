import { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material'
import { api, type Session } from '../api'
import { brand } from '../theme'
import { CompactTextField, FieldActionRow, controlActionSx } from './ui'

export default function MfaSettingsCard({
  session,
  open,
  onClose,
  onError,
  onOk,
}: {
  session: Session
  open: boolean
  onClose: () => void
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const eligible =
    !session.superAdmin &&
    session.roles.some((r) => r === 'OWNER' || r === 'ADMIN')

  const [enabled, setEnabled] = useState(false)
  const [enrolled, setEnrolled] = useState(false)
  const [secret, setSecret] = useState<string | null>(null)
  const [otpauthUri, setOtpauthUri] = useState<string | null>(null)
  const [code, setCode] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    if (!eligible) return
    try {
      const s = await api.getMfaStatus()
      setEnabled(s.enabled)
      setEnrolled(s.enrolled)
    } catch {
      /* ignore for SSO-only sessions */
    }
  }, [eligible])

  useEffect(() => {
    void load()
  }, [load])

  if (!eligible) return null

  const start = async () => {
    setBusy(true)
    try {
      const r = await api.startMfaEnroll()
      setSecret(r.secret)
      setOtpauthUri(r.otpauthUri)
      setCode('')
      onOk('Scan the secret in your authenticator, then confirm with a code')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const confirm = async () => {
    setBusy(true)
    try {
      await api.confirmMfaEnroll(code.trim())
      setSecret(null)
      setOtpauthUri(null)
      setCode('')
      await load()
      onOk('Authenticator is on')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const disable = async () => {
    setBusy(true)
    try {
      await api.disableMfa(code.trim())
      setCode('')
      await load()
      onOk('Authenticator is off')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Set Up Authenticator</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
          An extra code for your own password sign-in. People who sign in with SSO use their company authenticator instead.
        </Typography>
        {enabled ? (
          <Alert severity="success" variant="outlined" sx={{ mb: 1.25 }}>
            Authenticator is on for your account.
          </Alert>
        ) : (
          <Alert severity="info" variant="outlined" sx={{ mb: 1.25 }}>
            {enrolled
              ? 'Setup is started. Enter the 6-digit code from your authenticator app.'
              : 'Not set up yet. Use this only if you sign in with a password.'}
          </Alert>
        )}
        {secret && (
          <Box
            sx={{
              mb: 1.25,
              p: 1.25,
              borderRadius: 1,
              bgcolor: brand.canvas,
              border: `1px solid ${brand.hairline}`,
            }}
          >
            <Typography variant="caption" color="text.secondary" display="block">
              Secret (enter manually in authenticator)
            </Typography>
            <Typography
              variant="body2"
              component="code"
              sx={{ fontFamily: 'ui-monospace, Menlo, monospace', wordBreak: 'break-all' }}
            >
              {secret}
            </Typography>
            {otpauthUri && (
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.75 }}>
                otpauth URI available for QR apps (copy from network if needed).
              </Typography>
            )}
          </Box>
        )}
        <FieldActionRow>
          <CompactTextField
            label="Code from the app"
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
            maxWidth={160}
            maxLength={6}
            slotProps={{ htmlInput: { inputMode: 'numeric' } }}
          />
          {!enabled && (
            <Box sx={{ display: 'flex', gap: 1, flexShrink: 0 }}>
              <Button size="small" variant="outlined" disabled={busy} onClick={() => void start()} sx={controlActionSx}>
                Start Setup
              </Button>
              <Button
                size="small"
                variant="contained"
                disabled={busy || code.length !== 6 || !secret}
                onClick={() => void confirm()}
                sx={controlActionSx}
              >
                Turn on
              </Button>
            </Box>
          )}
          {enabled && (
            <Button
              size="small"
              color="error"
              disabled={busy || code.length !== 6}
              onClick={() => void disable()}
              sx={controlActionSx}
            >
              Turn Off
            </Button>
          )}
        </FieldActionRow>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  )
}
