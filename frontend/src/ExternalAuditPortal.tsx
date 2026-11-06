import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import LockRoundedIcon from '@mui/icons-material/LockRounded'
import {
  externalAuditPortalApi,
  type AuditPortalEvidenceItem,
  type AuditPortalGapItem,
  type AuditPortalRiskItem,
  type AuditPortalSession,
  type AuditPortalSoaItem,
  type AuditChecklistItem,
  type AuditFinding,
  type FindingSeverity,
  type GrcAuditDetail,
  type ValidateExternalAuditAccessResponse,
} from './api'
import StatusChip from './components/StatusChip'
import { BusyButton, CompactTextField, DomainBadge, FormSelect, KpiCard, KpiCardGrid } from './components/ui'
import { brand, layout } from './theme'
import { formatTenantDate } from './uiQuality/tenantLocale'
import {
  auditControlRefSx,
  auditTableContainerSx,
  auditTableHeadCellSx,
  ENGAGEMENT_KPI_ACCENTS,
  findingSeverityRiskLevel,
  selfAssessmentGapStatus,
} from './components/audit/auditKit'
import {
  engagementKpis,
  findingSeverityShort,
  scopeModuleLabel,
} from './components/audit/auditEngagementUtils'

type Stage = 'loading' | 'invalid' | 'otp' | 'workspace'
type PortalTab = 'controls' | 'gap' | 'soa' | 'risk' | 'evidence' | 'findings'

function useQueryParams() {
  return useMemo(() => {
    const params = new URLSearchParams(window.location.search)
    return { tenantId: params.get('tenant') ?? '', token: params.get('token') ?? '' }
  }, [])
}

function gapStatusLabel(status: string | null | undefined): string {
  switch (status) {
    case 'FULLY_IMPLEMENTED':
      return 'Compliant'
    case 'PARTIALLY_IMPLEMENTED':
      return 'Partial'
    case 'NOT_IMPLEMENTED':
      return 'Non-compliant'
    case 'PLANNED':
      return 'Planned'
    default:
      return '-'
  }
}

function gapStatusToDomain(status: string | null | undefined) {
  switch (status) {
    case 'FULLY_IMPLEMENTED':
      return 'compliant' as const
    case 'PARTIALLY_IMPLEMENTED':
      return 'partial' as const
    case 'NOT_IMPLEMENTED':
      return 'nonCompliant' as const
    case 'PLANNED':
      return 'partial' as const
    default:
      return 'notAssessed' as const
  }
}

function formatExpiry(iso: string | null | undefined): string {
  if (!iso) return '-'
  return formatTenantDate(iso)
}

export default function ExternalAuditPortal() {
  const { tenantId, token } = useQueryParams()
  const [stage, setStage] = useState<Stage>('loading')
  const [validation, setValidation] = useState<ValidateExternalAuditAccessResponse | null>(null)
  const [otp, setOtp] = useState('')
  const [auditorToken, setAuditorToken] = useState<string | null>(null)
  const [sessionTenantId, setSessionTenantId] = useState('')
  const [portalSession, setPortalSession] = useState<AuditPortalSession | null>(null)
  const [detail, setDetail] = useState<GrcAuditDetail | null>(null)
  const [tab, setTab] = useState<PortalTab>('gap')
  const [gapItems, setGapItems] = useState<AuditPortalGapItem[]>([])
  const [soaItems, setSoaItems] = useState<AuditPortalSoaItem[]>([])
  const [riskItems, setRiskItems] = useState<AuditPortalRiskItem[]>([])
  const [evidenceItems, setEvidenceItems] = useState<AuditPortalEvidenceItem[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [noteItem, setNoteItem] = useState<AuditChecklistItem | null>(null)
  const [noteText, setNoteText] = useState('')
  const [findingItem, setFindingItem] = useState<AuditChecklistItem | null>(null)
  const [findingSeverity, setFindingSeverity] = useState<FindingSeverity>('MINOR_NC')
  const [findingTitle, setFindingTitle] = useState('')
  const [findingDescription, setFindingDescription] = useState('')

  useEffect(() => {
    if (!tenantId || !token) {
      setStage('invalid')
      return
    }
    externalAuditPortalApi
      .validate(tenantId, token)
      .then((res) => {
        setValidation(res)
        setStage(res.valid ? 'otp' : 'invalid')
      })
      .catch((e) => {
        setError(String(e))
        setStage('invalid')
      })
  }, [tenantId, token])

  const loadWorkspace = useCallback(
    async (tok: string, tid: string) => {
      const sess = await externalAuditPortalApi.session(tok, tid)
      const auditDetail = await externalAuditPortalApi.getAudit(tok, tid, sess.auditId)
      setPortalSession(sess)
      setDetail(auditDetail)
      const modules = new Set(sess.scopeModules)
      if (modules.has('GAP')) {
        setGapItems(await externalAuditPortalApi.listGap(tok, tid, sess.auditId))
      }
      if (modules.has('SOA')) {
        setSoaItems(await externalAuditPortalApi.listSoa(tok, tid, sess.auditId))
      }
      if (modules.has('RISK')) {
        setRiskItems(await externalAuditPortalApi.listRisks(tok, tid, sess.auditId))
      }
      if (modules.has('EVIDENCE')) {
        setEvidenceItems(await externalAuditPortalApi.listEvidence(tok, tid, sess.auditId))
      }
      setStage('workspace')
    },
    [],
  )

  const redeem = async () => {
    setBusy(true)
    setError('')
    try {
      const res = await externalAuditPortalApi.redeem(tenantId, token, otp.trim())
      setAuditorToken(res.token)
      setSessionTenantId(res.tenantId)
      await loadWorkspace(res.token, res.tenantId)
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const refreshDetail = async () => {
    if (!auditorToken || !portalSession) return
    setDetail(await externalAuditPortalApi.getAudit(auditorToken, sessionTenantId, portalSession.auditId))
  }

  const saveNote = async () => {
    if (!auditorToken || !portalSession || !noteItem) return
    setBusy(true)
    setError('')
    try {
      await externalAuditPortalApi.updateChecklistNotes(
        auditorToken,
        sessionTenantId,
        portalSession.auditId,
        noteItem.id,
        noteText,
      )
      setNoteItem(null)
      await refreshDetail()
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const submitFinding = async () => {
    if (!auditorToken || !portalSession || !findingItem) return
    setBusy(true)
    setError('')
    try {
      await externalAuditPortalApi.addFinding(auditorToken, sessionTenantId, portalSession.auditId, {
        severity: findingSeverity,
        title: findingTitle.trim(),
        description: findingDescription.trim(),
        controlRef: findingItem.nodeCode,
        checklistItemId: findingItem.id,
      })
      setFindingItem(null)
      setFindingTitle('')
      setFindingDescription('')
      await refreshDetail()
      setTab('findings')
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const kpis = detail ? engagementKpis(detail) : null
  const scopeModules = portalSession?.scopeModules ?? []

  const evidenceByNodeCode = useMemo(() => {
    const map = new Map<string, number>()
    for (const item of gapItems) {
      map.set(item.nodeCode, item.evidenceCount)
    }
    for (const item of evidenceItems) {
      if (item.linkedNodeCode) {
        map.set(item.linkedNodeCode, (map.get(item.linkedNodeCode) ?? 0) + 1)
      }
    }
    return map
  }, [gapItems, evidenceItems])

  const visibleTabs = useMemo(() => {
    const tabs: { id: PortalTab; label: string; count?: number }[] = []
    if (scopeModules.includes('GAP')) {
      tabs.push({ id: 'gap', label: 'Gap Assessment', count: detail?.checklist.length ?? gapItems.length })
    } else {
      tabs.push({ id: 'controls', label: 'Controls', count: detail?.checklist.length })
    }
    if (scopeModules.includes('SOA')) tabs.push({ id: 'soa', label: 'Statement of Applicability', count: soaItems.length })
    if (scopeModules.includes('RISK')) tabs.push({ id: 'risk', label: 'Information Security Risk Register', count: riskItems.length })
    if (scopeModules.includes('EVIDENCE')) tabs.push({ id: 'evidence', label: 'Evidence Library', count: evidenceItems.length })
    tabs.push({ id: 'findings', label: 'My Findings', count: detail?.findings.length })
    return tabs
  }, [scopeModules, detail, gapItems, soaItems, riskItems, evidenceItems])

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: brand.canvas }}>
      <Box
        sx={{
          bgcolor: brand.sidebar,
          borderBottom: `1px solid ${brand.border}`,
          px: 3,
          py: 1.5,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <Stack direction="row" spacing={1} alignItems="center">
          <Box
            sx={{
              width: 28,
              height: 28,
              borderRadius: 1,
              bgcolor: brand.accent,
              color: '#fff',
              fontWeight: 800,
              fontSize: 13,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            A
          </Box>
          <Box>
            <Typography variant="subtitle2" fontWeight={700}>
              Arbor GRC
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'uppercase' }}>
              External Audit Portal
            </Typography>
          </Box>
        </Stack>
        {stage === 'workspace' && (
          <Stack direction="row" spacing={1} alignItems="center">
            <Chip size="small" label="READ ONLY" />
            <Typography variant="caption" color="error.main" fontWeight={600}>
              Link expires {formatExpiry(portalSession?.accessExpiresAt)}
            </Typography>
          </Stack>
        )}
      </Box>

      <Box
        sx={{
          width: '100%',
          maxWidth: stage === 'workspace' ? layout.wideCanvasMax : 1100,
          mx: 'auto',
          px: 2,
          py: 3,
          boxSizing: 'border-box',
        }}
      >
        {stage === 'loading' && (
          <Card>
            <CardContent sx={{ py: 5, textAlign: 'center' }}>
              <CircularProgress size={28} />
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1.5 }}>
                Validating your invitation…
              </Typography>
            </CardContent>
          </Card>
        )}

        {stage === 'invalid' && (
          <Card>
            <CardContent sx={{ py: 4, textAlign: 'center' }}>
              <Typography variant="subtitle1" gutterBottom>
                This link is no longer valid
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {validation?.message ?? error ?? 'The invite link is invalid, expired, or revoked.'}
              </Typography>
            </CardContent>
          </Card>
        )}

        {stage === 'otp' && (
          <Card sx={{ maxWidth: 460, mx: 'auto' }}>
            <CardContent sx={{ py: 4 }}>
              <Stack spacing={0.5} alignItems="center" sx={{ mb: 2 }}>
                <LockRoundedIcon sx={{ color: brand.accent }} />
                <Typography variant="subtitle1">Enter your one-time passcode</Typography>
                <Typography variant="body2" color="text.secondary" textAlign="center">
                  We sent a 6-digit passcode to your email for{' '}
                  <strong>{validation?.auditName}</strong>.
                </Typography>
              </Stack>
              <Stack spacing={1.5}>
                <CompactTextField
                  label="One-time passcode"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  slotProps={{ htmlInput: { inputMode: 'numeric', autoComplete: 'one-time-code' } }}
                  autoFocus
                />
                {error && <Alert severity="error">{error}</Alert>}
                <BusyButton
                  variant="contained"
                  busy={busy}
                  disabled={otp.trim().length < 4}
                  disabledReason={otp.trim().length < 4 ? 'Enter the one-time passcode' : undefined}
                  onClick={() => void redeem()}
                >
                  Continue
                </BusyButton>
              </Stack>
            </CardContent>
          </Card>
        )}

        {stage === 'workspace' && detail && portalSession && kpis && (
          <Stack spacing={2}>
            <Box
              sx={{
                bgcolor: brand.goldLight,
                border: `1px solid ${brand.goldMid}`,
                borderRadius: 1,
                p: 2,
              }}
            >
              <Typography variant="h6" fontWeight={700}>
                {portalSession.auditName}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {portalSession.frameworkName} · External certification audit
              </Typography>
              <Stack direction="row" spacing={2} flexWrap="wrap" sx={{ mt: 1 }}>
                <Typography variant="caption" fontWeight={600} color={brand.goldDark}>
                  Audit period: {detail.audit.startDate ?? '-'} – {detail.audit.endDate ?? '-'}
                </Typography>
                <Typography variant="caption" fontWeight={600} color={brand.goldDark}>
                  Framework: {portalSession.frameworkName}
                </Typography>
                {scopeModules.map((m) => (
                  <Typography key={m} variant="caption" color={brand.goldDark}>
                    Scope: {scopeModuleLabel(m)}
                  </Typography>
                ))}
              </Stack>
            </Box>

            <KpiCardGrid columns={4}>
              <KpiCard label="Controls in scope" value={kpis.total} accentColor={ENGAGEMENT_KPI_ACCENTS.neutral} sub="Annex controls" />
              <KpiCard label="Compliant" value={kpis.compliant} accentColor={ENGAGEMENT_KPI_ACCENTS.success} />
              <KpiCard label="Partial" value={kpis.partial} accentColor="#CA8A04" />
              <KpiCard label="Non-compliant" value={kpis.nonCompliant} accentColor={ENGAGEMENT_KPI_ACCENTS.danger} />
            </KpiCardGrid>

            {error && <Alert severity="error">{error}</Alert>}

            <Tabs value={tab} onChange={(_, v: PortalTab) => setTab(v)} variant="scrollable">
              {visibleTabs.map((t) => (
                <Tab
                  key={t.id}
                  value={t.id}
                  label={
                    <Stack direction="row" spacing={0.75} alignItems="center">
                      <span>{t.label}</span>
                      {t.count != null && <Chip size="small" label={t.count} />}
                    </Stack>
                  }
                />
              ))}
            </Tabs>

            {(tab === 'controls' || tab === 'gap') && (
              <TableContainer component={Card} sx={auditTableContainerSx}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell sx={auditTableHeadCellSx}>Control</TableCell>
                      <TableCell sx={auditTableHeadCellSx}>Title</TableCell>
                      <TableCell sx={auditTableHeadCellSx}>Status</TableCell>
                      <TableCell sx={auditTableHeadCellSx}>Evidence</TableCell>
                      <TableCell sx={auditTableHeadCellSx}>Auditor notes</TableCell>
                      <TableCell sx={auditTableHeadCellSx} align="right">
                        Action
                      </TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {detail.checklist.map((item) => {
                      const evCount = evidenceByNodeCode.get(item.nodeCode) ?? 0
                      const linkedFinding = detail.findings.find(
                        (f) => f.checklistItemId === item.id || f.controlRef === item.nodeCode,
                      )
                      return (
                        <TableRow key={item.id} hover>
                          <TableCell sx={auditControlRefSx}>{item.nodeCode}</TableCell>
                          <TableCell>{item.title}</TableCell>
                          <TableCell>
                            <DomainBadge
                              variant="gap"
                              status={selfAssessmentGapStatus(item.assessment)}
                            />
                          </TableCell>
                          <TableCell>
                            <Typography
                              variant="caption"
                              fontWeight={evCount > 0 ? 600 : 400}
                              color={evCount > 0 ? brand.goldDark : 'text.disabled'}
                            >
                              {evCount > 0 ? `${evCount} file${evCount === 1 ? '' : 's'} ↓` : 'No evidence'}
                            </Typography>
                          </TableCell>
                          <TableCell sx={{ color: linkedFinding ? 'error.main' : 'text.secondary', fontSize: 13 }}>
                            {linkedFinding
                              ? 'Finding raised'
                              : item.notes?.trim() || 'No notes'}
                          </TableCell>
                          <TableCell align="right">
                            <Stack direction="row" spacing={1} justifyContent="flex-end">
                              {!linkedFinding && (
                                <>
                                  <Button
                                    size="small"
                                    onClick={() => {
                                      setNoteItem(item)
                                      setNoteText(item.notes ?? '')
                                    }}
                                  >
                                    Add Note
                                  </Button>
                                  <Button
                                    size="small"
                                    color="warning"
                                    onClick={() => {
                                      setFindingItem(item)
                                      setFindingTitle(`${item.nodeCode}: `)
                                      setFindingDescription('')
                                      setFindingSeverity('MINOR_NC')
                                    }}
                                  >
                                    Raise Finding
                                  </Button>
                                </>
                              )}
                              {linkedFinding && (
                                <Button size="small" color="error" onClick={() => setTab('findings')}>
                                  View Finding
                                </Button>
                              )}
                            </Stack>
                          </TableCell>
                        </TableRow>
                      )
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}

            {tab === 'soa' && (
              <ScopedTable
                empty="No SoA entries in engagement scope."
                rows={soaItems.map((item) => ({
                  key: item.id,
                  code: item.nodeCode,
                  title: item.title,
                  status: <Chip size="small" label={item.applicability} />,
                  extra: item.justification ?? '-',
                }))}
              />
            )}

            {tab === 'risk' && (
              <ScopedTable
                empty="No risks in engagement scope."
                rows={riskItems.map((item) => ({
                  key: item.id,
                  code: item.displayId,
                  title: item.title,
                  status: <StatusChip label={item.status} tone="info" />,
                  extra: item.nodeCode ?? '-',
                }))}
              />
            )}

            {tab === 'evidence' && (
              <ScopedTable
                empty="No evidence linked to scoped controls."
                rows={evidenceItems.map((item) => ({
                  key: item.id,
                  code: item.linkedNodeCode ?? '-',
                  title: item.title,
                  status: <Chip size="small" label={item.classification} />,
                  extra: item.status,
                }))}
              />
            )}

            {tab === 'findings' && (
              <Stack spacing={1.5}>
                {detail.findings.length === 0 && (
                  <Alert severity="info">No findings raised yet. Use Controls → Raise finding.</Alert>
                )}
                {detail.findings.map((f: AuditFinding) => (
                  <Card key={f.id} variant="outlined">
                    <CardContent>
                      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 0.5 }}>
                        <Typography variant="subtitle2" fontWeight={700}>
                          {f.displayCode} · {f.controlRef ?? '-'}: {f.title}
                        </Typography>
                        <DomainBadge
                          variant="risk"
                          level={findingSeverityRiskLevel(f.severity)}
                          label={findingSeverityShort(f.severity)}
                        />
                      </Stack>
                      <Typography variant="body2" color="text.secondary">
                        {f.description}
                      </Typography>
                    </CardContent>
                  </Card>
                ))}
              </Stack>
            )}

            <Typography variant="caption" color="text.secondary" textAlign="center" display="block">
              Read-only access to tenant GRC data within engagement scope · You may add auditor notes and findings only
            </Typography>
          </Stack>
        )}
      </Box>

      <Dialog open={noteItem != null} onClose={() => setNoteItem(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Auditor Working Note: {noteItem?.nodeCode}</DialogTitle>
        <DialogContent>
          <CompactTextField
            label="Working note"
            multiline
            minRows={4}
            value={noteText}
            onChange={(e) => setNoteText(e.target.value)}
            placeholder="Private working notes (visible to client after report sign-off)"
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setNoteItem(null)}>Cancel</Button>
          <Button variant="contained" disabled={busy} onClick={() => void saveNote()}>
            Save Note
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={findingItem != null} onClose={() => setFindingItem(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Raise Finding: {findingItem?.nodeCode}</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ mt: 1 }}>
            <FormSelect
              label="Severity"
              value={findingSeverity}
              onChange={(v) => setFindingSeverity(v as FindingSeverity)}
              options={[
                { value: 'MAJOR_NC', label: 'Major NC', tone: 'critical' },
                { value: 'MINOR_NC', label: 'Minor NC', tone: 'high' },
                { value: 'OBSERVATION', label: 'Observation', tone: 'medium' },
                { value: 'OFI', label: 'OFI', tone: 'low' },
              ]}
            />
            <CompactTextField label="Title" value={findingTitle} onChange={(e) => setFindingTitle(e.target.value)} />
            <CompactTextField
              label="Description"
              value={findingDescription}
              onChange={(e) => setFindingDescription(e.target.value)}
              multiline
              minRows={3}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFindingItem(null)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={busy || !findingTitle.trim() || !findingDescription.trim()}
            onClick={() => void submitFinding()}
          >
            Raise Finding
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

function ScopedTable({
  rows,
  empty,
}: {
  empty: string
  rows: { key: number; code: string; title: string; status: React.ReactNode; extra: string }[]
}) {
  if (rows.length === 0) {
    return <Alert severity="info">{empty}</Alert>
  }
  return (
    <TableContainer component={Card} sx={auditTableContainerSx}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell sx={auditTableHeadCellSx}>Ref</TableCell>
            <TableCell sx={auditTableHeadCellSx}>Title</TableCell>
            <TableCell sx={auditTableHeadCellSx}>Status</TableCell>
            <TableCell sx={auditTableHeadCellSx}>Detail</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row) => (
            <TableRow key={row.key}>
              <TableCell sx={auditControlRefSx}>{row.code}</TableCell>
              <TableCell>{row.title}</TableCell>
              <TableCell>{row.status}</TableCell>
              <TableCell sx={{ color: 'text.secondary', fontSize: 13 }}>{row.extra}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}
