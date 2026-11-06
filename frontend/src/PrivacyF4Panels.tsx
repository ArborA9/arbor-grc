import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Button,
  Card,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import {
  api,
  CONSENT_METHODS,
  type BreachSeverity,
  type CreatePrivacyBreachBody,
  type CreatePrivacyConsentBody,
  type CreatePrivacyDpiaBody,
  type DpiaResidualRisk,
  type PrivacyBreach,
  type PrivacyConsent,
  type PrivacyDpia,
  type PrivacyRopa,
} from './api'
import StatusChip from './components/StatusChip'
import { CompactTextField, FormSelect } from './components/ui'
import { formatTenantDateTime } from './uiQuality/tenantLocale'
import { useAppShellPage } from './components/AppShellPageContext'

function toneForStatus(status: string): 'success' | 'warning' | 'danger' | 'neutral' | 'info' {
  switch (status) {
    case 'ACTIVE':
    case 'APPROVED':
    case 'REPORTED':
    case 'CLOSED':
      return 'success'
    case 'UNDER_REVIEW':
    case 'INVESTIGATING':
      return 'info'
    case 'DRAFT':
    case 'OPEN':
      return 'neutral'
    case 'OVERDUE':
    case 'REJECTED':
      return 'danger'
    case 'WITHDRAWN':
      return 'warning'
    default:
      return 'neutral'
  }
}

export function DpiaPanel({
  canWrite,
  onError,
  onOk,
}: {
  canWrite: boolean
  onError: (m: string) => void
  onOk: (m: string) => void
}) {
  const [rows, setRows] = useState<PrivacyDpia[]>([])
  const [ropas, setRopas] = useState<PrivacyRopa[]>([])
  const [selected, setSelected] = useState<PrivacyDpia | null>(null)
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState<CreatePrivacyDpiaBody>({ title: '', residualRisk: 'MEDIUM' })

  const load = useCallback(async () => {
    try {
      const [dpia, ropa] = await Promise.all([api.listPrivacyDpia(), api.listPrivacyRopa()])
      setRows(dpia)
      setRopas(ropa)
      setSelected((prev) => (prev ? dpia.find((d) => d.id === prev.id) ?? null : null))
    } catch (e) {
      onError(String(e))
    }
  }, [onError])

  useEffect(() => {
    void load()
  }, [load])

  const save = async () => {
    if (!(form.title ?? '').trim()) return
    try {
      await api.createPrivacyDpia({ ...form, title: (form.title ?? '').trim() })
      setOpen(false)
      setForm({ title: '', residualRisk: 'MEDIUM' })
      onOk('DPIA created')
      await load()
    } catch (e) {
      onError(String(e))
    }
  }

  const requestBack = useCallback(() => setSelected(null), [])

  const listActions = useMemo(
    () =>
      canWrite ? (
        <Button variant="contained" size="small" onClick={() => setOpen(true)}>
          New DPIA
        </Button>
      ) : undefined,
    [canWrite],
  )

  useAppShellPage(
    !open && selected
      ? {
          title: selected.displayId,
          breadcrumbTrail: selected.title,
          breadcrumbRoot: { label: 'DPIA', onClick: requestBack },
          hideContextLabel: true,
          hideRoleChips: true,
        }
      : {
          title: 'DPIA',
          breadcrumbTrail: 'Privacy',
          actions: open ? undefined : listActions,
          hideContextLabel: true,
          hideRoleChips: true,
        },
    [open, selected, listActions, requestBack],
  )

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0 }}>
      <Card sx={{ flex: 1, overflow: 'hidden' }}>
        <TableContainer sx={{ maxHeight: selected ? 240 : '100%' }}>
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell>Register</TableCell>
                <TableCell>Title</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((r) => (
                <TableRow key={r.id} hover sx={{ cursor: 'pointer' }} onClick={() => setSelected(r)}>
                  <TableCell>{r.displayId}</TableCell>
                  <TableCell>{r.title}</TableCell>
                  <TableCell>
                    <StatusChip label={r.status} tone={toneForStatus(r.status)} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>
      {selected && (
        <Card sx={{ p: 2 }}>
          <Stack spacing={1}>
            <Typography variant="subtitle1" fontWeight={700}>
              {selected.displayId}: {selected.title}
            </Typography>
            <StatusChip label={selected.status} tone={toneForStatus(selected.status)} />
            {selected.preScreeningRequired && (
              <Typography variant="body2" color="text.secondary">
                Pre-screening: {selected.preScreeningOutcome?.replace(/_/g, ' ') ?? 'pending'}
                {selected.preScreeningNotes ? `: ${selected.preScreeningNotes}` : ''}
              </Typography>
            )}
            {selected.ropaId != null && (
              <Typography variant="body2" color="text.secondary">
                Linked ROPA #{selected.ropaId}
              </Typography>
            )}
            {canWrite && selected.status === 'DRAFT' && (
              <Button
                size="small"
                variant="outlined"
                onClick={() =>
                  api
                    .submitPrivacyDpiaReview(selected.id)
                    .then(() => {
                      onOk('Submitted for review')
                      void load()
                    })
                    .catch((e) => onError(String(e)))
                }
              >
                Submit for Review
              </Button>
            )}
            {canWrite && selected.status === 'UNDER_REVIEW' && (
              <Stack direction="row" spacing={1}>
                <Button
                  size="small"
                  variant="contained"
                  onClick={() =>
                    api
                      .approvePrivacyDpia(selected.id)
                      .then(() => {
                        onOk('DPIA approved')
                        void load()
                      })
                      .catch((e) => onError(String(e)))
                  }
                >
                  Approve
                </Button>
                <Button
                  size="small"
                  color="error"
                  onClick={() => {
                    const notes = window.prompt('Rejection notes')
                    if (!notes) return
                    api
                      .rejectPrivacyDpia(selected.id, notes)
                      .then(() => {
                        onOk('DPIA rejected')
                        void load()
                      })
                      .catch((e) => onError(String(e)))
                  }}
                >
                  Reject
                </Button>
              </Stack>
            )}
          </Stack>
        </Card>
      )}
      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>New DPIA</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ mt: 1 }}>
            <CompactTextField
              label="Title"
              fieldIdPrefix="privacy-dpia"
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
            />
            <FormSelect
              label="Linked ROPA (optional)"
              value={form.ropaId == null ? '' : String(form.ropaId)}
              options={[
                { value: '', label: '-' },
                ...ropas.map((r) => ({ value: String(r.id), label: `${r.displayId}: ${r.activityName}` })),
              ]}
              onChange={(v) => setForm({ ...form, ropaId: v === '' ? null : Number(v) })}
              fullWidth
            />
            <FormSelect
              label="Residual risk"
              value={form.residualRisk ?? 'MEDIUM'}
              options={(['LOW', 'MEDIUM', 'HIGH'] as DpiaResidualRisk[]).map((v) => ({ value: v, label: v }))}
              onChange={(v) => setForm({ ...form, residualRisk: v as DpiaResidualRisk })}
              fullWidth
            />
            <CompactTextField
              label="Processing description"
              fieldIdPrefix="privacy-dpia"
              multiline
              minRows={2}
              value={form.processingDescription ?? ''}
              onChange={(e) => setForm({ ...form, processingDescription: e.target.value })}
            />
            <FormControlLabel
              sx={{ m: 0 }}
              control={
                <Checkbox
                  size="small"
                  checked={!!form.preScreeningRequired}
                  onChange={(e) => setForm({ ...form, preScreeningRequired: e.target.checked })}
                />
              }
              label={<Typography sx={{ fontSize: '0.875rem' }}>Pre-DPIA screening required</Typography>}
            />
            {form.preScreeningRequired && (
              <>
                <FormSelect
                  label="Pre-screening outcome"
                  value={form.preScreeningOutcome ?? ''}
                  options={[
                    { value: '', label: '-' },
                    { value: 'DPIA_REQUIRED', label: 'DPIA required' },
                    { value: 'DPIA_NOT_REQUIRED', label: 'DPIA not required' },
                    { value: 'NEEDS_REVIEW', label: 'Needs review' },
                  ]}
                  onChange={(v) => setForm({ ...form, preScreeningOutcome: v || null })}
                  fullWidth
                />
                <CompactTextField
                  label="Pre-screening notes"
                  fieldIdPrefix="privacy-dpia"
                  multiline
                  minRows={2}
                  value={form.preScreeningNotes ?? ''}
                  onChange={(e) => setForm({ ...form, preScreeningNotes: e.target.value })}
                />
              </>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => void save()}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}

export function ConsentPanel({
  canWrite,
  onError,
  onOk,
}: {
  canWrite: boolean
  onError: (m: string) => void
  onOk: (m: string) => void
}) {
  const [rows, setRows] = useState<PrivacyConsent[]>([])
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState<CreatePrivacyConsentBody>({
    subjectName: '',
    purpose: '',
    consentMethod: 'WEB_FORM',
  })

  const load = useCallback(async () => {
    try {
      setRows(await api.listPrivacyConsent())
    } catch (e) {
      onError(String(e))
    }
  }, [onError])

  useEffect(() => {
    void load()
  }, [load])

  const save = async () => {
    if (!form.subjectName.trim() || !form.purpose.trim()) return
    try {
      await api.createPrivacyConsent(form)
      setOpen(false)
      setForm({ subjectName: '', purpose: '', consentMethod: 'WEB_FORM' })
      onOk('Consent recorded')
      await load()
    } catch (e) {
      onError(String(e))
    }
  }

  const listActions = useMemo(
    () =>
      canWrite ? (
        <Button variant="contained" size="small" onClick={() => setOpen(true)}>
          Record Consent
        </Button>
      ) : undefined,
    [canWrite],
  )

  useAppShellPage(
    {
      title: 'Consent',
      breadcrumbTrail: 'Privacy',
      actions: open ? undefined : listActions,
      hideContextLabel: true,
      hideRoleChips: true,
    },
    [open, listActions],
  )

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0 }}>
      <Card sx={{ flex: 1, overflow: 'hidden' }}>
        <TableContainer>
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell>Register</TableCell>
                <TableCell>Subject</TableCell>
                <TableCell>Purpose</TableCell>
                <TableCell>Status</TableCell>
                <TableCell />
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((r) => (
                <TableRow key={r.id}>
                  <TableCell>{r.displayId}</TableCell>
                  <TableCell>{r.subjectName}</TableCell>
                  <TableCell>{r.purpose}</TableCell>
                  <TableCell>
                    <StatusChip label={r.status} tone={toneForStatus(r.status)} />
                  </TableCell>
                  <TableCell>
                    {canWrite && r.status === 'ACTIVE' && (
                      <Button
                        size="small"
                        onClick={() =>
                          api
                            .withdrawPrivacyConsent(r.id)
                            .then(() => {
                              onOk('Consent withdrawn')
                              void load()
                            })
                            .catch((e) => onError(String(e)))
                        }
                      >
                        Withdraw
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>
      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Record Consent</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ mt: 1 }}>
            <CompactTextField
              label="Subject name"
              fieldIdPrefix="privacy-consent"
              value={form.subjectName}
              onChange={(e) => setForm({ ...form, subjectName: e.target.value })}
            />
            <CompactTextField
              label="Purpose"
              fieldIdPrefix="privacy-consent"
              value={form.purpose}
              onChange={(e) => setForm({ ...form, purpose: e.target.value })}
            />
            <FormSelect
              label="Method"
              value={form.consentMethod}
              options={CONSENT_METHODS.map((m) => ({ value: m, label: m.replace(/_/g, ' ') }))}
              onChange={(v) => setForm({ ...form, consentMethod: v as CreatePrivacyConsentBody['consentMethod'] })}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => void save()}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}

export function BreachPanel({
  canWrite,
  onError,
  onOk,
}: {
  canWrite: boolean
  onError: (m: string) => void
  onOk: (m: string) => void
}) {
  const [rows, setRows] = useState<PrivacyBreach[]>([])
  const [selected, setSelected] = useState<PrivacyBreach | null>(null)
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState<CreatePrivacyBreachBody>({
    title: '',
    severity: 'HIGH',
    discoveredAt: new Date().toISOString(),
  })

  const load = useCallback(async () => {
    try {
      const list = await api.listPrivacyBreaches()
      setRows(list)
      setSelected((prev) => (prev ? list.find((b) => b.id === prev.id) ?? null : null))
    } catch (e) {
      onError(String(e))
    }
  }, [onError])

  useEffect(() => {
    void load()
  }, [load])

  const save = async () => {
    if (!form.title.trim()) return
    try {
      await api.createPrivacyBreach(form)
      setOpen(false)
      setForm({ title: '', severity: 'HIGH', discoveredAt: new Date().toISOString() })
      onOk('Breach logged: 72h clock started')
      await load()
    } catch (e) {
      onError(String(e))
    }
  }

  const requestBack = useCallback(() => setSelected(null), [])

  const listActions = useMemo(
    () =>
      canWrite ? (
        <Button variant="contained" size="small" onClick={() => setOpen(true)}>
          Log Breach
        </Button>
      ) : undefined,
    [canWrite],
  )

  useAppShellPage(
    !open && selected
      ? {
          title: selected.displayId,
          breadcrumbTrail: selected.title,
          breadcrumbRoot: { label: 'Breaches', onClick: requestBack },
          hideContextLabel: true,
          hideRoleChips: true,
        }
      : {
          title: 'Breaches',
          breadcrumbTrail: 'Privacy',
          actions: open ? undefined : listActions,
          hideContextLabel: true,
          hideRoleChips: true,
        },
    [open, selected, listActions, requestBack],
  )

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0 }}>
      <Card sx={{ flex: 1, overflow: 'hidden' }}>
        <TableContainer sx={{ maxHeight: selected ? 220 : '100%' }}>
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell>Register</TableCell>
                <TableCell>Title</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Deadline</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((r) => (
                <TableRow key={r.id} hover sx={{ cursor: 'pointer' }} onClick={() => setSelected(r)}>
                  <TableCell>{r.displayId}</TableCell>
                  <TableCell>{r.title}</TableCell>
                  <TableCell>
                    <StatusChip label={r.status} tone={toneForStatus(r.status)} />
                  </TableCell>
                  <TableCell>{formatTenantDateTime(r.reportDeadlineAt)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>
      {selected && (
        <Card sx={{ p: 2 }}>
          <Stack spacing={1} direction="row" flexWrap="wrap" useFlexGap>
            <Typography variant="subtitle2" sx={{ width: '100%' }}>
              {selected.displayId}: {selected.title}
            </Typography>
            {canWrite && ['OPEN', 'OVERDUE'].includes(selected.status) && (
              <Button
                size="small"
                variant="outlined"
                onClick={() =>
                  api
                    .investigatePrivacyBreach(selected.id)
                    .then(() => {
                      onOk('Investigation started')
                      void load()
                    })
                    .catch((e) => onError(String(e)))
                }
              >
                Start Investigation
              </Button>
            )}
            {canWrite && selected.status !== 'REPORTED' && selected.status !== 'CLOSED' && (
              <Button
                size="small"
                variant="contained"
                onClick={() =>
                  api
                    .reportPrivacyBreach(selected.id, window.prompt('Authority reference') ?? undefined)
                    .then(() => {
                      onOk('Reported to authority')
                      void load()
                    })
                    .catch((e) => onError(String(e)))
                }
              >
                Mark Reported
              </Button>
            )}
            {canWrite && selected.status !== 'CLOSED' && (
              <Button
                size="small"
                color="inherit"
                onClick={() =>
                  api
                    .closePrivacyBreach(selected.id)
                    .then(() => {
                      onOk('Breach closed')
                      void load()
                    })
                    .catch((e) => onError(String(e)))
                }
              >
                Close
              </Button>
            )}
          </Stack>
        </Card>
      )}
      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Log Personal Data Breach</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ mt: 1 }}>
            <CompactTextField
              label="Title"
              fieldIdPrefix="privacy-breach"
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
            />
            <FormSelect
              label="Severity"
              value={form.severity ?? 'HIGH'}
              options={(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as BreachSeverity[]).map((v) => ({
                value: v,
                label: v,
                tone: v === 'CRITICAL' ? 'critical' : v === 'HIGH' ? 'high' : v === 'MEDIUM' ? 'medium' : 'low',
              }))}
              onChange={(v) => setForm({ ...form, severity: v as BreachSeverity })}
              fullWidth
            />
            <CompactTextField
              label="Description"
              fieldIdPrefix="privacy-breach"
              multiline
              minRows={2}
              value={form.description ?? ''}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => void save()}>
            Log Breach
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
