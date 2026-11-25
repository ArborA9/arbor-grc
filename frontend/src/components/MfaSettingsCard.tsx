import { useCallback, useEffect, useRef, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  InputAdornment,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material'
import ContentCopyRoundedIcon from '@mui/icons-material/ContentCopyRounded'
import { api, type Session } from '../api'
import {
  BusyButton,
  CompactTextField,
  ConfirmActionDialog,
  FieldActionRow,
  controlActionSx,
} from './ui'
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
  const forSuperAdmin = !!session.superAdmin
  const eligible =
    forSuperAdmin ||
    (!session.superAdmin && session.roles.some((r) => r === 'OWNER' || r === 'ADMIN'))

  const [enabled, setEnabled] = useState(false)
  const [secret, setSecret] = useState<string | null>(null)
  const [code, setCode] = useState('')
  const [busy, setBusy] = useState(false)
  const [statusReady, setStatusReady] = useState(false)
  const [disableOpen, setDisableOpen] = useState(false)
  const [disableCode, setDisableCode] = useState('')
  const [disableError, setDisableError] = useState('')
  const autoStartedRef = useRef(false)

  const load = useCallback(async () => {
    if (!eligible) return
    try {
      const s = forSuperAdmin ? await api.getSuperAdminMfaStatus() : await api.getMfaStatus()
      setEnabled(s.enabled)
      setStatusReady(true)
    } catch {
      setStatusReady(true)
    }
  }, [eligible, forSuperAdmin])

  useEffect(() => {
    if (!open) {
      setDisableOpen(false)
      setDisableCode('')
      setDisableError('')
      setCode('')
      setSecret(null)
      setStatusReady(false)
      autoStartedRef.current = false
      return
    }
    void load()
  }, [open, load])

  const start = useCallback(async () => {
    setBusy(true)
    try {
      const r = forSuperAdmin ? await api.startSuperAdminMfaEnroll() : await api.startMfaEnroll()
      setSecret(r.secret)
      setCode('')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }, [forSuperAdmin, onError])

  // One screen: when MFA is off, create the secret as soon as the dialog opens.
  useEffect(() => {
    if (!open || !statusReady || enabled || secret || busy || autoStartedRef.current) return
    autoStartedRef.current = true
    void start()
  }, [open, statusReady, enabled, secret, busy, start])

  if (!eligible) return null

  const confirm = async () => {
    setBusy(true)
    try {
      if (forSuperAdmin) {
        await api.confirmSuperAdminMfaEnroll(code.trim())
      } else {
        await api.confirmMfaEnroll(code.trim())
      }
      setSecret(null)
      setCode('')
      await load()
      onOk('Authenticator is on')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const copySecret = async () => {
    if (!secret) return
    try {
      await navigator.clipboard.writeText(secret)
      onOk('Secret copied')
    } catch (e) {
      onError(e instanceof Error ? e.message : String(e))
    }
  }

  const openDisable = () => {
    setDisableCode('')
    setDisableError('')
    setDisableOpen(true)
  }

  const closeDisable = () => {
    if (busy) return
    setDisableOpen(false)
    setDisableCode('')
    setDisableError('')
  }

  const disable = async () => {
    if (disableCode.length !== 6) return
    setBusy(true)
    setDisableError('')
    try {
      if (forSuperAdmin) {
        await api.disableSuperAdminMfa(disableCode.trim())
      } else {
        await api.disableMfa(disableCode.trim())
      }
      setDisableOpen(false)
      setDisableCode('')
      setSecret(null)
      setCode('')
      setEnabled(false)
      onOk('Authenticator is off')
      // Close the parent dialog so auto-enroll does not start a new setup immediately.
      onClose()
    } catch (e) {
      setDisableError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const preparing = !enabled && !secret
  const setupInProgress = !!secret && !enabled
  const turnOnWhy = code.length !== 6 ? 'Enter the 6-digit code from your app' : ''

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
        <DialogTitle>{enabled ? 'Authenticator' : 'Set Up Authenticator'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 0.5 }}>
            {enabled ? (
              <>
                <Typography variant="body2" color="text.secondary">
                  {forSuperAdmin
                    ? 'Required at Super Admin sign-in with your password.'
                    : 'Required at password sign-in. SSO sign-in uses your company authenticator instead.'}
                </Typography>
                <Alert severity="success" variant="outlined">
                  Authenticator is on for your account.
                </Alert>
              </>
            ) : (
              <Typography variant="body2" color="text.secondary">
                Add the secret to your authenticator app, then enter the 6-digit code it shows to turn it on.
              </Typography>
            )}

            {preparing && (
              <Stack direction="row" spacing={1.25} alignItems="center" sx={{ py: 1 }}>
                <CircularProgress size={18} />
                <Typography variant="body2" color="text.secondary">
                  Preparing authenticator secret…
                </Typography>
              </Stack>
            )}

            {setupInProgress && (
              <>
                <FieldActionRow>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <CompactTextField
                      label="Authenticator secret"
                      value={secret ?? ''}
                      maxLength={false}
                      slotProps={{
                        htmlInput: {
                          readOnly: true,
                          style: { fontFamily: 'ui-monospace, Menlo, monospace', fontSize: '0.75rem' },
                        },
                        input: {
                          endAdornment: (
                            <InputAdornment position="end">
                              <Tooltip title="Copy secret">
                                <span>
                                  <IconButton
                                    size="small"
                                    color="primary"
                                    aria-label="Copy secret"
                                    disabled={busy}
                                    onClick={() => void copySecret()}
                                    edge="end"
                                  >
                                    <ContentCopyRoundedIcon sx={{ fontSize: 16 }} />
                                  </IconButton>
                                </span>
                              </Tooltip>
                            </InputAdornment>
                          ),
                        },
                      }}
                    />
                  </Box>
                  <BusyButton
                    size="small"
                    variant="outlined"
                    color="primary"
                    busy={busy}
                    onClick={() => void start()}
                    sx={controlActionSx}
                  >
                    New secret
                  </BusyButton>
                </FieldActionRow>

                <CompactTextField
                  label="Verification code"
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  maxWidth={160}
                  maxLength={6}
                  slotProps={{ htmlInput: { inputMode: 'numeric' } }}
                />
              </>
            )}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 2.5, pb: 2 }}>
          <Button onClick={onClose} disabled={busy}>
            Close
          </Button>
          {enabled && (
            <BusyButton color="error" variant="contained" busy={busy} onClick={openDisable}>
              Turn Off
            </BusyButton>
          )}
          {setupInProgress && (
            <BusyButton
              variant="contained"
              color="primary"
              busy={busy}
              disabled={!!turnOnWhy}
              disabledReason={turnOnWhy || undefined}
              onClick={() => void confirm()}
            >
              Turn on
            </BusyButton>
          )}
        </DialogActions>
      </Dialog>

      <ConfirmActionDialog
        open={disableOpen}
        title="Turn Off Authenticator"
        message="Enter the current 6-digit code from your authenticator app to turn it off."
        confirmLabel="Turn Off"
        confirmColor="error"
        busy={busy}
        busyLabel="Turning off…"
        confirmDisabled={disableCode.length !== 6}
        disabledReason="Enter the 6-digit code from your app"
        onCancel={closeDisable}
        onConfirm={() => void disable()}
      >
        <CompactTextField
          label="Verification code"
          value={disableCode}
          onChange={(e) => {
            setDisableCode(e.target.value.replace(/\D/g, '').slice(0, 6))
            setDisableError('')
          }}
          maxWidth={160}
          maxLength={6}
          slotProps={{ htmlInput: { inputMode: 'numeric', autoFocus: true } }}
        />
        {disableError && (
          <Alert severity="error" sx={{ mt: 1.25 }} onClose={() => setDisableError('')} role="alert">
            {disableError}
          </Alert>
        )}
      </ConfirmActionDialog>
    </>
  )
}
