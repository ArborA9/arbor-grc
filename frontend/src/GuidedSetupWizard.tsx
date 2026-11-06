import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  FormControlLabel,
  FormGroup,
  Link,
  Stack,
  Step,
  StepLabel,
  Stepper,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material'
import {
  api,
  ORG_DATE_FORMATS,
  ORG_SIZE_BANDS,
  ORG_TIMEZONES,
  ssoEntryUrl,
  type FrameworkSummary,
  type OrgLevel,
  type OrgUnitNode,
  type Session,
  type TenantSsoStatus,
  type UserSummary,
  type RiskLevel,
} from './api'
import { CompactTextField, FormSelect } from './components/ui'
import { appetiteMaxResidual, parseGridSize } from './components/risk/riskMatrix'
import { brand } from './theme'
import { frameworkLabel } from './uiQuality/frameworkLabel'
import { roleLabel } from './uiQuality/roleLabels'
import AdminModulePicker from './components/users/AdminModulePicker'

const STEPS = [
  'Welcome',
  'Organisation profile',
  'Organisation structure',
  'Invite team',
  'SSO',
  'Frameworks',
  'Done',
] as const

const LAST_STEP = STEPS.length - 1

const ROLE_OPTIONS = ['ADMIN', 'CONTRIBUTOR', 'GLOBAL_READER', 'INTERNAL_AUDITOR'] as const

function countUnits(nodes: OrgUnitNode[]): number {
  return nodes.reduce((n, u) => n + 1 + countUnits(u.children ?? []), 0)
}

function flattenUnits(nodes: OrgUnitNode[]): OrgUnitNode[] {
  return nodes.flatMap((u) => [u, ...flattenUnits(u.children ?? [])])
}

export default function GuidedSetupWizard({
  open,
  session,
  onClose,
  onError,
  onOk,
}: {
  open: boolean
  session: Session
  onClose: () => void
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const [step, setStep] = useState(0)
  const [busy, setBusy] = useState(false)
  const [levels, setLevels] = useState<OrgLevel[]>([])
  const [tree, setTree] = useState<OrgUnitNode[]>([])

  const [displayName, setDisplayName] = useState('')
  const [industry, setIndustry] = useState('')
  const [sizeBand, setSizeBand] = useState('')
  const [timezone, setTimezone] = useState('Asia/Dubai')
  const [timezoneOther, setTimezoneOther] = useState(false)
  const [dateFormat, setDateFormat] = useState('DD/MM/YYYY')
  const [riskMatrixGridSize, setRiskMatrixGridSize] = useState('5')
  const [riskAppetiteRating, setRiskAppetiteRating] = useState('LOW')
  const [matrixLocked, setMatrixLocked] = useState(false)
  const [rootName, setRootName] = useState('')

  const [invited, setInvited] = useState<UserSummary[]>([])
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteName, setInviteName] = useState('')
  const [inviteRole, setInviteRole] = useState<string>('CONTRIBUTOR')
  const [inviteAuthType, setInviteAuthType] = useState<'SSO' | 'LOCAL'>('SSO')
  const [invitePassword, setInvitePassword] = useState('')
  const [inviteUnitIds, setInviteUnitIds] = useState<number[]>([])
  const [inviteAdminModules, setInviteAdminModules] = useState<string[]>([])
  const [licensedModules, setLicensedModules] = useState<string[]>([])

  const [ssoStatus, setSsoStatus] = useState<TenantSsoStatus | null>(null)
  const [frameworks, setFrameworks] = useState<FrameworkSummary[]>([])
  const [selectedFrameworkIds, setSelectedFrameworkIds] = useState<number[]>([])

  const load = useCallback(async () => {
    try {
      const [p, lv, units, users, available, sso, modules] = await Promise.all([
        api.getOrgProfile(),
        api.listOrgLevels(),
        api.listOrgUnits(false),
        api.listUsers().catch(() => [] as UserSummary[]),
        api.listAvailableFrameworks().catch(() => [] as FrameworkSummary[]),
        session.tenantId
          ? api.tenantSsoStatus(session.tenantId).catch(() => null)
          : Promise.resolve(null),
        api.listMyModules().catch(() => null),
      ])
      setLicensedModules(modules?.enabled ?? [])
      setLevels(lv)
      setTree(units)
      setDisplayName(p.displayName ?? '')
      setIndustry(p.industry ?? '')
      setSizeBand(p.sizeBand ?? '')
      setTimezone(p.timezone)
      setTimezoneOther(!ORG_TIMEZONES.some((z) => z.value === p.timezone))
      setDateFormat(p.dateFormat)
      setRiskMatrixGridSize(String(p.riskMatrixGridSize ?? 5))
      setRiskAppetiteRating(p.riskAppetiteRating ?? 'LOW')
      setMatrixLocked(Boolean(p.riskMatrixLocked || p.wizardCompleted))
      setInvited(users.filter((u) => u.email !== session.email))
      setFrameworks(available)
      const activated = available.filter((f) => f.activated).map((f) => f.id)
      setSelectedFrameworkIds(activated)
      setSsoStatus(sso)
    } catch (e) {
      onError(String(e))
    }
  }, [onError, session.email, session.tenantId])

  useEffect(() => {
    if (open) {
      setStep(0)
      setRootName('')
      setInviteEmail('')
      setInviteName('')
      setInviteRole('CONTRIBUTOR')
      setInviteAuthType('SSO')
      setInvitePassword('')
      setInviteUnitIds([])
      void load()
    }
  }, [open, load])

  const level1 = useMemo(
    () => levels.find((l) => l.level === 1)?.label ?? 'Department',
    [levels],
  )
  const unitCount = countUnits(tree)
  const flatUnits = useMemo(() => flattenUnits(tree), [tree])
  const activatedCount = useMemo(
    () => frameworks.filter((f) => f.activated || selectedFrameworkIds.includes(f.id)).length,
    [frameworks, selectedFrameworkIds],
  )

  const saveProfileAndNext = async () => {
    setBusy(true)
    try {
      await api.updateOrgProfile({
        displayName: displayName.trim() || null,
        industry: industry.trim() || null,
        sizeBand: sizeBand || null,
        timezone,
        dateFormat,
        riskMatrixGridSize: Number(riskMatrixGridSize) || 5,
        riskAppetiteRating,
      })
      setStep(2)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const syncDepartments = async () => {
    setBusy(true)
    try {
      const result = await api.syncDepartments()
      if (!result.available) {
        onError(
          result.reason === 'not_entra'
            ? 'This SSO issuer is not Microsoft Entra. Add departments manually.'
            : result.reason === 'sso_disabled' || result.reason === 'not_configured'
              ? 'SSO is not configured yet. Add departments manually.'
              : 'Entra could not be read. Add departments manually.',
        )
        return
      }
      await load()
      onOk(result.added === 0 ? 'No new departments from Entra' : `Added ${result.added} from Entra`)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const addRoot = async () => {
    if (!rootName.trim()) return
    setBusy(true)
    try {
      await api.createOrgUnit({ name: rootName.trim() })
      setRootName('')
      await load()
      onOk(`${level1} added`)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const sendInvite = async () => {
    if (!inviteEmail.trim() || !inviteName.trim()) return
    if (inviteRole === 'CONTRIBUTOR' && inviteUnitIds.length === 0) {
      onError('Contributors need at least one organisation unit')
      return
    }
    if (inviteRole === 'ADMIN' && inviteAdminModules.length === 0) {
      onError(licensedModules.length === 0
        ? 'No licensed modules are available to assign'
        : 'Select at least one module')
      return
    }
    setBusy(true)
    try {
      const user = await api.createUser(
        inviteEmail.trim(),
        inviteName.trim(),
        [inviteRole],
        inviteAuthType,
        inviteAuthType === 'LOCAL' ? invitePassword : undefined,
        inviteRole === 'CONTRIBUTOR' ? inviteUnitIds : [],
        inviteRole === 'ADMIN' ? inviteAdminModules : [],
      )
      setInvited((prev) => [...prev, user])
      setInviteEmail('')
      setInviteName('')
      setInvitePassword('')
      setInviteUnitIds([])
      setInviteAdminModules([])
      onOk(`Invited ${user.email}`)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const activateSelectedFrameworks = async () => {
    setBusy(true)
    try {
      const toActivate = selectedFrameworkIds.filter(
        (id) => !frameworks.find((f) => f.id === id)?.activated,
      )
      for (const id of toActivate) {
        await api.activateFramework(id)
      }
      const refreshed = await api.listAvailableFrameworks()
      setFrameworks(refreshed)
      setSelectedFrameworkIds(refreshed.filter((f) => f.activated).map((f) => f.id))
      setStep(LAST_STEP)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const finish = async () => {
    setBusy(true)
    try {
      await api.completeOrgWizard()
      onOk('Guided Setup complete')
      onClose()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const toggleFramework = (id: number) => {
    setSelectedFrameworkIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    )
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogContent sx={{ pt: 2.5 }}>
        <Typography variant="overline" sx={{ color: brand.accent, letterSpacing: 1.2 }}>
          Guided Setup
        </Typography>
        <Typography variant="h6" sx={{ mb: 2 }}>
          Get {session.tenantId ?? 'your workspace'} ready
        </Typography>
        <Stepper activeStep={step} alternativeLabel sx={{ mb: 3 }}>
          {STEPS.map((label) => (
            <Step key={label}>
              <StepLabel>{label}</StepLabel>
            </Step>
          ))}
        </Stepper>

        {step === 0 && (
          <Stack spacing={1.5}>
            <Typography variant="body1">
              Welcome to Arbor GRC. This wizard configures your organisation, invites your team,
              checks SSO readiness, and activates licensed frameworks.
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Signed in as {session.email}
            </Typography>
          </Stack>
        )}

        {step === 1 && (
          <Stack spacing={1.5}>
            <CompactTextField
              label="Display name"
              fullWidth
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />
            <CompactTextField
              label="Industry"
              fullWidth
              value={industry}
              onChange={(e) => setIndustry(e.target.value)}
            />
            <FormSelect
              label="Organisation size"
              fullWidth
              value={sizeBand}
              onChange={setSizeBand}
              options={[
                { value: '', label: 'Not set' },
                ...ORG_SIZE_BANDS.map((b) => ({ value: b, label: b })),
              ]}
            />
            <FormSelect
              label="Timezone"
              fullWidth
              value={timezoneOther ? '__OTHER__' : timezone}
              onChange={(v) => {
                if (v === '__OTHER__') {
                  setTimezoneOther(true)
                  if (ORG_TIMEZONES.some((z) => z.value === timezone)) {
                    setTimezone('')
                  }
                } else {
                  setTimezoneOther(false)
                  setTimezone(v)
                }
              }}
              options={[
                ...ORG_TIMEZONES.map((z) => ({ value: z.value, label: z.label })),
                { value: '__OTHER__', label: 'Other (IANA id)…' },
              ]}
            />
            {timezoneOther && (
              <CompactTextField
                label="IANA timezone"
                fullWidth
                value={timezone}
                onChange={(e) => setTimezone(e.target.value)}
                helperText="e.g. Asia/Tokyo, Australia/Sydney"
              />
            )}
            <FormSelect
              label="Date format"
              fullWidth
              value={dateFormat}
              onChange={setDateFormat}
              options={ORG_DATE_FORMATS.map((f) => ({ value: f, label: f }))}
            />
            <FormSelect
              label="Risk matrix size"
              fullWidth
              value={riskMatrixGridSize}
              onChange={setRiskMatrixGridSize}
              disabled={matrixLocked}
              options={[
                { value: '3', label: '3 × 3' },
                { value: '4', label: '4 × 4' },
                { value: '5', label: '5 × 5 (default)' },
              ]}
            />
            <FormSelect
              label="Risk appetite"
              fullWidth
              value={riskAppetiteRating}
              onChange={setRiskAppetiteRating}
              disabled={matrixLocked}
              options={[
                { value: 'VERY_LOW', label: 'Very low' },
                { value: 'LOW', label: 'Low (default)' },
                { value: 'MEDIUM', label: 'Medium' },
                { value: 'HIGH', label: 'High' },
                { value: 'VERY_HIGH', label: 'Very high' },
              ]}
            />
            <Typography variant="caption" color="text.secondary">
              Maximum residual within appetite: {appetiteMaxResidual(
                parseGridSize(Number(riskMatrixGridSize)),
                (riskAppetiteRating as RiskLevel) || 'LOW',
              )}
            </Typography>
            {matrixLocked && (
              <Typography variant="caption" color="text.secondary">
                Matrix size is locked after guided setup completes.
              </Typography>
            )}
          </Stack>
        )}

        {step === 2 && (
          <Stack spacing={1.5}>
            <Typography variant="body2" color="text.secondary">
              Add at least one {level1}. You can nest divisions, sections, and teams from the
              Organisation page after setup.
            </Typography>
            <Stack direction="row" spacing={1} alignItems="flex-end">
              <CompactTextField
                fullWidth
                label={`${level1} name`}
                value={rootName}
                onChange={(e) => setRootName(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') void addRoot()
                }}
              />
              <Button
                variant="contained"
                size="small"
                disabled={!rootName.trim() || busy}
                onClick={() => void addRoot()}
                sx={{ minWidth: 72, flexShrink: 0, mb: '1px' }}
              >
                Add
              </Button>
              <Button
                variant="outlined"
                size="small"
                disabled={busy}
                onClick={() => void syncDepartments()}
                sx={{ flexShrink: 0, mb: '1px' }}
              >
                Sync from Entra
              </Button>
            </Stack>
            <Box
              sx={{
                border: `1px solid ${brand.hairline}`,
                borderRadius: 1,
                p: 1.5,
                minHeight: 88,
                bgcolor: 'rgba(15, 23, 42, 0.02)',
              }}
            >
              {tree.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No units yet.
                </Typography>
              ) : (
                <Stack spacing={0.5}>
                  {tree.map((u) => (
                    <Typography key={u.id} variant="body2" fontWeight={600}>
                      {u.name}
                      {(u.children?.length ?? 0) > 0
                        ? ` · ${countUnits(u.children)} nested`
                        : ''}
                    </Typography>
                  ))}
                </Stack>
              )}
            </Box>
            <Typography variant="caption" color="text.secondary">
              {unitCount} unit{unitCount === 1 ? '' : 's'} · minimum one required to continue
            </Typography>
          </Stack>
        )}

        {step === 3 && (
          <Stack spacing={1.5}>
            <Typography variant="body2" color="text.secondary">
              Invite colleagues now or skip and add them later from Users. Contributors must be
              assigned to at least one organisation unit.
            </Typography>
            <ToggleButtonGroup
              size="small"
              exclusive
              color="primary"
              value={inviteAuthType}
              onChange={(_, v) => v && setInviteAuthType(v)}
              fullWidth
            >
              <ToggleButton value="SSO">SSO (AD)</ToggleButton>
              <ToggleButton value="LOCAL">Local (break-glass)</ToggleButton>
            </ToggleButtonGroup>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
              <CompactTextField
                fullWidth
                label="Email"
                value={inviteEmail}
                onChange={(e) => setInviteEmail(e.target.value)}
              />
              <CompactTextField
                fullWidth
                label="Full name"
                value={inviteName}
                onChange={(e) => setInviteName(e.target.value)}
              />
            </Stack>
            <FormSelect
              label="Role"
              fullWidth
              value={inviteRole}
              onChange={setInviteRole}
              options={ROLE_OPTIONS.map((r) => ({ value: r, label: roleLabel(r) }))}
            />
            {inviteAuthType === 'LOCAL' && (
              <CompactTextField
                fullWidth
                showPasswordToggle
                label="Temporary password"
                value={invitePassword}
                onChange={(e) => setInvitePassword(e.target.value)}
              />
            )}
            {inviteRole === 'ADMIN' && (
              <AdminModulePicker
                modules={licensedModules}
                selected={inviteAdminModules}
                disabled={busy}
                onChange={setInviteAdminModules}
              />
            )}
            {inviteRole === 'CONTRIBUTOR' && (
              <Box>
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.5 }}>
                  Organisation units
                </Typography>
                <FormGroup>
                  {flatUnits.map((u) => (
                    <FormControlLabel
                      key={u.id}
                      control={
                        <Checkbox
                          size="small"
                          checked={inviteUnitIds.includes(u.id)}
                          onChange={(_, checked) =>
                            setInviteUnitIds((prev) =>
                              checked ? [...prev, u.id] : prev.filter((id) => id !== u.id),
                            )
                          }
                        />
                      }
                      label={<Typography variant="caption">{u.name}</Typography>}
                    />
                  ))}
                </FormGroup>
              </Box>
            )}
            {inviteRole === 'ADMIN' && inviteAdminModules.length === 0 && (
              <Typography variant="caption" color="text.secondary">
                {licensedModules.length === 0
                  ? 'No licensed modules are available to assign.'
                  : 'Select at least one module.'}
              </Typography>
            )}
            <Button
              variant="outlined"
              disabled={
                busy
                || !inviteEmail.trim()
                || !inviteName.trim()
                || (inviteRole === 'ADMIN' && (inviteAdminModules.length === 0 || licensedModules.length === 0))
              }
              onClick={() => void sendInvite()}
            >
              {busy
                ? inviteAuthType === 'LOCAL'
                  ? 'Creating…'
                  : 'Sending Invite…'
                : inviteAuthType === 'LOCAL'
                  ? 'Create Local User'
                  : 'Send Invite'}
            </Button>
            {invited.length > 0 && (
              <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                {invited.map((u) => (
                  <Chip key={u.id} size="small" label={u.email} />
                ))}
              </Stack>
            )}
          </Stack>
        )}

        {step === 4 && (
          <Stack spacing={1.5}>
            <Alert severity={ssoStatus?.ssoEnabled ? 'success' : 'info'} variant="outlined">
              SSO is {ssoStatus?.ssoEnabled ? 'enabled' : 'not configured yet'} for{' '}
              {ssoStatus?.name ?? session.tenantId}.
            </Alert>
            <Typography variant="body2" color="text.secondary">
              Arbor registers your tenant as an Enterprise App in Azure AD (Client ID, issuer, secret).
              After IT enables SSO, map Entra security groups to Arbor roles under Organisation →
              Notifications &amp; SSO.
            </Typography>
            {session.tenantId && (
              <Typography variant="body2">
                Test entry URL:{' '}
                <Link href={ssoEntryUrl(session.tenantId)} target="_blank" rel="noreferrer">
                  {ssoEntryUrl(session.tenantId)}
                </Link>
              </Typography>
            )}
          </Stack>
        )}

        {step === 5 && (
          <Stack spacing={1.5}>
            <Typography variant="body2" color="text.secondary">
              Activate at least one published framework for your licensed modules. You can add more
              later from Assessments → Frameworks.
            </Typography>
            {frameworks.length === 0 ? (
              <Alert severity="warning" variant="outlined">
                No frameworks are available yet. Ask your Arbor administrator to publish and assign
                frameworks to this tenant.
              </Alert>
            ) : (
              <Stack spacing={0.5}>
                {frameworks.map((f) => (
                  <FormControlLabel
                    key={f.id}
                    control={
                      <Checkbox
                        checked={selectedFrameworkIds.includes(f.id)}
                        onChange={() => toggleFramework(f.id)}
                        disabled={f.activated === true}
                      />
                    }
                    label={
                      <Typography variant="body2">
                        {frameworkLabel(f)}
                        {f.activated ? ' · already active' : ''}
                      </Typography>
                    }
                  />
                ))}
              </Stack>
            )}
            <Typography variant="caption" color="text.secondary">
              {activatedCount} framework{activatedCount === 1 ? '' : 's'} will be active · minimum
              one required
            </Typography>
          </Stack>
        )}

        {step === LAST_STEP && (
          <Stack spacing={1.5}>
            <Typography variant="body1">
              You&apos;re set. Organisation profile, structure, SSO check, and framework activation
              are complete.
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {unitCount} org unit{unitCount === 1 ? '' : 's'} · {invited.length} invited user
              {invited.length === 1 ? '' : 's'} · {activatedCount} active framework
              {activatedCount === 1 ? '' : 's'}
            </Typography>
          </Stack>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} disabled={busy}>
          Close
        </Button>
        <Box sx={{ flex: 1 }} />
        {step > 0 && step < LAST_STEP && (
          <Button onClick={() => setStep((s) => s - 1)} disabled={busy}>
            Back
          </Button>
        )}
        {step === 0 && (
          <Button variant="contained" onClick={() => setStep(1)}>
            Continue
          </Button>
        )}
        {step === 1 && (
          <Button variant="contained" disabled={busy} onClick={() => void saveProfileAndNext()}>
            Save & Continue
          </Button>
        )}
        {step === 2 && (
          <Button
            variant="contained"
            disabled={busy || unitCount < 1}
            onClick={() => setStep(3)}
          >
            Continue
          </Button>
        )}
        {step === 3 && (
          <>
            <Button disabled={busy} onClick={() => setStep(4)}>
              Skip
            </Button>
            <Button variant="contained" disabled={busy} onClick={() => setStep(4)}>
              Continue
            </Button>
          </>
        )}
        {step === 4 && (
          <>
            <Button disabled={busy} onClick={() => setStep(5)}>
              Skip
            </Button>
            <Button variant="contained" disabled={busy} onClick={() => setStep(5)}>
              Continue
            </Button>
          </>
        )}
        {step === 5 && (
          <Button
            variant="contained"
            disabled={busy || selectedFrameworkIds.length < 1}
            onClick={() => void activateSelectedFrameworks()}
          >
            Activate & Continue
          </Button>
        )}
        {step === LAST_STEP && (
          <Button variant="contained" disabled={busy} onClick={() => void finish()}>
            Finish
          </Button>
        )}
      </DialogActions>
    </Dialog>
  )
}
