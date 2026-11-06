import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Box, Checkbox, FormControlLabel, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material'
import {
  api,
  ApiError,
  type AuditExternalInvite,
  type EngagementScopeModule,
  type GrcAuditDetail,
  type GrcAuditType,
  type UserSummary,
} from '../../api'
import { BusyButton, CompactTextField, FieldActionRow, SettingsCard, controlActionSx } from '../ui'
import { formFieldGridSx, NARRATIVE_FIELD_MAX_WIDTH, twoColFieldSx } from '../ui/formLayout'
import { brand, semantic } from '../../theme'
import { auditFrameworkLabel, formatEngagementDate, scopeModuleLabel } from './auditEngagementUtils'
import { SCOPE_MODULES } from './useAuditEngagementForm'
import { useRegisterUnsavedChanges } from '../../uiQuality/UnsavedChangesGuard'

function auditTypeLabel(type: GrcAuditType): string {
  switch (type) {
    case 'INTERNAL':
      return 'Internal'
    case 'EXTERNAL':
      return 'External'
    case 'EXTERNAL_ASSESSMENT':
      return 'External assessment'
    case 'FOLLOW_UP':
      return 'Follow-up'
    default:
      return type
  }
}

function MetaField({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Box sx={{ minWidth: 0 }}>
      <Typography
        sx={{
          fontSize: '0.6875rem',
          fontWeight: 600,
          letterSpacing: '0.04em',
          textTransform: 'uppercase',
          color: brand.mutedLight,
          mb: 0.4,
        }}
      >
        {label}
      </Typography>
      <Box sx={{ fontSize: '0.875rem', color: brand.ink, lineHeight: 1.45, wordBreak: 'break-word' }}>
        {children}
      </Box>
    </Box>
  )
}

function ModuleChip({ label }: { label: string }) {
  return (
    <Box
      sx={{
        px: 1,
        py: 0.4,
        border: `1px solid ${brand.goldMid}`,
        borderRadius: 1,
        bgcolor: brand.goldLight,
        fontSize: '0.75rem',
        fontWeight: 650,
        color: brand.goldDark,
      }}
    >
      {label}
    </Box>
  )
}

function PersonLine({ name }: { name: string }) {
  const initials = name
    .split(/[\s@._-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')
  return (
    <Stack direction="row" spacing={1} alignItems="center" sx={{ minWidth: 0 }}>
      <Box
        aria-hidden
        sx={{
          width: 28,
          height: 28,
          borderRadius: '50%',
          bgcolor: brand.goldLight,
          color: brand.goldDark,
          border: `1px solid ${brand.goldMid}`,
          fontSize: '0.6875rem',
          fontWeight: 700,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
        }}
      >
        {initials || '?'}
      </Box>
      <Typography variant="body2" fontWeight={600} sx={{ minWidth: 0, wordBreak: 'break-word' }}>
        {name}
      </Typography>
    </Stack>
  )
}

function WorkStat({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <Box sx={{ minWidth: 0 }}>
      <Typography sx={{ fontSize: '1.5rem', fontWeight: 700, color, lineHeight: 1.1 }}>{value}</Typography>
      <Typography
        sx={{
          mt: 0.4,
          fontSize: '0.6875rem',
          fontWeight: 650,
          letterSpacing: '0.04em',
          lineHeight: 1.35,
          textTransform: 'uppercase',
          color: brand.mutedLight,
        }}
      >
        {label}
      </Typography>
    </Box>
  )
}

function sameModules(a: string[], b: string[]) {
  if (a.length !== b.length) return false
  const left = [...a].sort()
  const right = [...b].sort()
  return left.every((value, index) => value === right[index])
}

const summaryCard = {
  accentColor: brand.gold,
  headerBg: brand.tableHeader,
} as const

export default function AuditEngagementSummary({
  detail,
  users,
  lines,
  reviewed,
  findings,
  canAdmin,
  isLeadAuditor,
  isTeamMember,
  summary,
  summaryError,
  onSummaryChange,
  onRefresh,
  onError,
  externalInvites,
  inviteName,
  inviteEmail,
  onInviteNameChange,
  onInviteEmailChange,
  onSendInvite,
}: {
  detail: GrcAuditDetail
  users: UserSummary[]
  lines: number
  reviewed: number
  findings: number
  canAdmin: boolean
  isLeadAuditor: boolean
  isTeamMember: boolean
  summary: string
  summaryError?: string
  onSummaryChange: (v: string) => void
  onRefresh: () => Promise<void>
  onError: (msg: string) => void
  externalInvites: AuditExternalInvite[]
  inviteName: string
  inviteEmail: string
  onInviteNameChange: (v: string) => void
  onInviteEmailChange: (v: string) => void
  onSendInvite: () => void | Promise<void>
}) {
  const userLabel = (id: number) => users.find((u) => u.id === id)?.fullName ?? `#${id}`
  const leadName = detail.audit.leadAuditorName?.trim() || userLabel(detail.audit.leadAuditorUserId)
  const members = (detail.teamMembers ?? []).filter((m) => m.teamRole === 'MEMBER')
  const savedModules = detail.scopeModules ?? []
  const savedNotes = detail.scopeNotes ?? ''
  const period =
    detail.audit.startDate || detail.audit.endDate
      ? `${formatEngagementDate(detail.audit.startDate)} to ${formatEngagementDate(detail.audit.endDate)}`
      : 'Dates not set'
  const version = detail.audit.frameworkVersion?.trim()
  const framework =
    version && version !== '-'
      ? `${auditFrameworkLabel(detail.audit)} ${version}`
      : auditFrameworkLabel(detail.audit)
  const summaryEditable =
    isLeadAuditor && !detail.audit.locked && detail.audit.status === 'IN_PROGRESS'
  const scopeEditable =
    (isLeadAuditor || isTeamMember || canAdmin) &&
    !detail.audit.locked &&
    detail.audit.status === 'IN_PROGRESS'
  const openLines = Math.max(0, lines - reviewed)
  const [savedSummary, setSavedSummary] = useState(detail.executiveSummary ?? '')
  const [scopeModules, setScopeModules] = useState<EngagementScopeModule[]>(savedModules)
  const [scopeNotes, setScopeNotes] = useState(savedNotes)
  const [scopeBusy, setScopeBusy] = useState(false)
  const [inviteBusy, setInviteBusy] = useState(false)
  const [scopeError, setScopeError] = useState<string | undefined>()
  useEffect(() => {
    setSavedSummary(detail.executiveSummary ?? '')
  }, [detail.audit.id, detail.executiveSummary])
  const savedScopeKey = `${detail.audit.id}|${savedNotes}|${[...savedModules].sort().join(',')}`
  const savedScopeKeyRef = useRef(savedScopeKey)
  useEffect(() => {
    if (savedScopeKeyRef.current === savedScopeKey) return
    savedScopeKeyRef.current = savedScopeKey
    setScopeModules([...(detail.scopeModules ?? [])])
    setScopeNotes(detail.scopeNotes ?? '')
    setScopeError(undefined)
  }, [savedScopeKey, detail.scopeModules, detail.scopeNotes])
  const summaryDirty = summaryEditable && summary !== savedSummary
  const scopeDirty =
    scopeEditable &&
    (scopeNotes !== savedNotes || !sameModules(scopeModules, savedModules))
  const scopeWhy = scopeModules.length === 0 ? 'Select at least one module' : undefined

  const saveScope = async () => {
    if (scopeModules.length === 0) {
      setScopeError('Select at least one module')
      throw new Error('Select at least one module')
    }
    setScopeBusy(true)
    setScopeError(undefined)
    try {
      await api.updateAuditScope(detail.audit.id, {
        scopeNotes: scopeNotes.trim() || null,
        scopeModules,
      })
      await onRefresh()
    } catch (e) {
      const message = e instanceof ApiError ? e.message : 'Could not save the scope'
      setScopeError(message)
      onError(message)
      throw e
    } finally {
      setScopeBusy(false)
    }
  }

  useRegisterUnsavedChanges(
    summaryDirty || scopeDirty
      ? {
          dirty: true,
          title: 'Leave this audit?',
          message: summaryDirty && scopeDirty
            ? 'The executive summary and scope are not saved.'
            : summaryDirty
              ? 'The executive summary is not saved.'
              : 'The scope is not saved.',
          saveDraftLabel: 'Save',
          onSaveDraft: async () => {
            if (summaryDirty) {
              await api.updateAuditSummary(detail.audit.id, summary)
              setSavedSummary(summary)
            }
            if (scopeDirty) await saveScope()
          },
        }
      : null,
  )

  return (
    <Stack spacing={1.5}>
      <Box sx={twoColFieldSx}>
        <SettingsCard heading autoHeight title="Engagement" {...summaryCard}>
          <Box sx={formFieldGridSx}>
            <MetaField label="Audit ID">{detail.audit.displayId?.trim() || `AUD-${detail.audit.id}`}</MetaField>
            <MetaField label="Type">{auditTypeLabel(detail.audit.auditType)}</MetaField>
            <MetaField label="Framework">{framework || '-'}</MetaField>
            <MetaField label="Period">{period}</MetaField>
            <MetaField label="Last updated">{formatEngagementDate(detail.audit.updatedAt)}</MetaField>
            {detail.audit.auditorOrganisation ? (
              <MetaField label="Auditor organisation">{detail.audit.auditorOrganisation}</MetaField>
            ) : null}
          </Box>
        </SettingsCard>

        <SettingsCard heading autoHeight title="Team" {...summaryCard}>
          <Stack spacing={1.5}>
            <Box>
              <Typography
                sx={{
                  fontSize: '0.6875rem',
                  fontWeight: 600,
                  letterSpacing: '0.04em',
                  textTransform: 'uppercase',
                  color: brand.mutedLight,
                  mb: 0.75,
                }}
              >
                Lead auditor
              </Typography>
              <PersonLine name={leadName} />
            </Box>
            <Box>
              <Typography
                sx={{
                  fontSize: '0.6875rem',
                  fontWeight: 600,
                  letterSpacing: '0.04em',
                  textTransform: 'uppercase',
                  color: brand.mutedLight,
                  mb: 0.75,
                }}
              >
                Team members
              </Typography>
              {members.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No additional team members
                </Typography>
              ) : (
                <Stack spacing={0.75}>
                  {members.map((m) => (
                    <PersonLine key={m.userId} name={userLabel(m.userId)} />
                  ))}
                </Stack>
              )}
            </Box>
          </Stack>
        </SettingsCard>
      </Box>

      <Box sx={twoColFieldSx}>
        <SettingsCard heading autoHeight title="Scope" {...summaryCard}>
          <Stack spacing={1.5}>
            <Box>
              <Typography
                sx={{
                  fontSize: '0.6875rem',
                  fontWeight: 600,
                  letterSpacing: '0.04em',
                  textTransform: 'uppercase',
                  color: scopeError ? 'error.main' : brand.mutedLight,
                  mb: 0.75,
                }}
              >
                Modules in scope{scopeEditable ? ' *' : ''}
              </Typography>
              {scopeEditable ? (
                <Box
                  sx={{
                    display: 'grid',
                    gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
                    columnGap: 1,
                  }}
                >
                  {SCOPE_MODULES.map((module) => (
                    <FormControlLabel
                      key={module.value}
                      control={
                        <Checkbox
                          size="small"
                          checked={scopeModules.includes(module.value)}
                          onChange={() => {
                            setScopeError(undefined)
                            setScopeModules((current) =>
                              current.includes(module.value)
                                ? current.filter((value) => value !== module.value)
                                : [...current, module.value],
                            )
                          }}
                          sx={{ py: 0.25 }}
                        />
                      }
                      label={scopeModuleLabel(module.value)}
                      sx={{ mr: 0, my: 0 }}
                    />
                  ))}
                </Box>
              ) : savedModules.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No modules selected
                </Typography>
              ) : (
                <Stack direction="row" flexWrap="wrap" useFlexGap gap={0.75}>
                  {savedModules.map((m) => (
                    <ModuleChip key={m} label={scopeModuleLabel(m)} />
                  ))}
                </Stack>
              )}
              {scopeError ? (
                <Typography variant="caption" color="error.main" sx={{ mt: 0.5, display: 'block' }}>
                  {scopeError}
                </Typography>
              ) : null}
            </Box>
            {(detail.scopeNodeIds ?? []).length > 0 && (
              <MetaField label="Control subset">{detail.scopeNodeIds.length} controls</MetaField>
            )}
            {scopeEditable ? (
              <CompactTextField
                label="Scope notes"
                fieldIdPrefix="audit-scope-notes"
                value={scopeNotes}
                onChange={(e) => setScopeNotes(e.target.value)}
                multiline
                minRows={3}
                fullWidth
                maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
              />
            ) : (
              <MetaField label="Scope notes">{savedNotes.trim() || '-'}</MetaField>
            )}
            {scopeEditable ? (
              <Box>
                <BusyButton
                  size="small"
                  variant="contained"
                  sx={controlActionSx}
                  busy={scopeBusy}
                  busyLabel="Saving..."
                  disabled={Boolean(scopeWhy) || !scopeDirty}
                  disabledReason={scopeWhy ?? (!scopeDirty ? 'No scope changes to save' : undefined)}
                  onClick={() => {
                    void saveScope().catch(() => undefined)
                  }}
                >
                  Save Scope
                </BusyButton>
              </Box>
            ) : null}
          </Stack>
        </SettingsCard>

        <SettingsCard heading autoHeight title="Work so far" {...summaryCard}>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              columnGap: 1.5,
              rowGap: 1.25,
            }}
          >
            <WorkStat label="Total controls/clauses" value={lines} color={brand.ink} />
            <WorkStat label="Reviewed" value={reviewed} color={semantic.risk.low} />
            <WorkStat label="Pending for review" value={openLines} color={semantic.risk.medium} />
            <WorkStat label="Findings" value={findings} color={semantic.risk.critical} />
          </Box>
        </SettingsCard>
      </Box>

      <SettingsCard heading autoHeight title="Executive summary" {...summaryCard}>
        {summaryEditable ? (
          <CompactTextField
            label="Narrative"
            fieldIdPrefix="exec-sum"
            value={summary}
            onChange={(e) => onSummaryChange(e.target.value)}
            multiline
            minRows={4}
            fullWidth
            required
            error={Boolean(summaryError)}
            helperText={summaryError}
            hint="Saved when you leave this field."
            onBlur={() => {
              if (summary === savedSummary) return
              api
                .updateAuditSummary(detail.audit.id, summary)
                .then(() => setSavedSummary(summary))
                .catch((e) => onError(String(e)))
            }}
          />
        ) : (
          <Box
            sx={{
              bgcolor: summary.trim() ? brand.hover : brand.paper,
              border: `1px solid ${brand.border}`,
              borderRadius: 1,
              px: 1.5,
              py: 1.25,
            }}
          >
            <Typography
              variant="body2"
              color={summary.trim() ? 'text.primary' : 'text.secondary'}
              sx={{ whiteSpace: 'pre-wrap' }}
            >
              {summary.trim() || 'No executive summary yet.'}
            </Typography>
          </Box>
        )}
      </SettingsCard>

      {detail.audit.auditType === 'EXTERNAL' && canAdmin && (
        <SettingsCard heading autoHeight title="External auditor access" {...summaryCard}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
            Send a one-time invite. The auditor signs in with the OTP mailed to them.
          </Typography>
          <FieldActionRow>
            <CompactTextField
              label="Name"
              fieldIdPrefix="ext-inv"
              size="small"
              value={inviteName}
              onChange={(e) => onInviteNameChange(e.target.value)}
              sx={{ minWidth: 160 }}
            />
            <CompactTextField
              label="Email"
              fieldIdPrefix="ext-inv"
              size="small"
              type="email"
              value={inviteEmail}
              onChange={(e) => onInviteEmailChange(e.target.value)}
              sx={{ minWidth: 200 }}
            />
            <BusyButton
              size="small"
              variant="contained"
              busy={inviteBusy}
              busyLabel="Sending..."
              disabled={!inviteName.trim() || !inviteEmail.trim()}
              onClick={() => {
                setInviteBusy(true)
                void Promise.resolve(onSendInvite()).finally(() => setInviteBusy(false))
              }}
              sx={controlActionSx}
            >
              Send Invite
            </BusyButton>
          </FieldActionRow>
          {externalInvites.length > 0 && (
            <Table size="small" sx={{ mt: 1.5 }}>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Email</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {externalInvites.map((inv) => (
                  <TableRow key={inv.id}>
                    <TableCell>{inv.inviteeName}</TableCell>
                    <TableCell>{inv.inviteeEmail}</TableCell>
                    <TableCell>{inv.status}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </SettingsCard>
      )}
    </Stack>
  )
}
