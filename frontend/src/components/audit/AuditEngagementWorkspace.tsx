import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Box,
  Button,
  MenuItem,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import {
  api,
  type AuditExternalInvite,
  type AuditPortalEvidenceItem,
  type CapaRecord,
  type GrcAuditDetail,
  type UserSummary,
} from '../../api'
import StatusChip from '../StatusChip'
import BusyButton from '../ui/BusyButton'
import {
  CompactTextField,
  DomainBadge,
  FieldActionRow,
  KpiCard,
  KpiCardGrid,
  SettingsCard,
  controlActionSx,
} from '../ui'
import ConfirmActionDialog from '../ui/ConfirmActionDialog'
import { brand } from '../../theme'
import { productErrorMessage } from '../../uiQuality/productError'
import { useOptionalUnsavedChangesGuardApi } from '../../uiQuality/UnsavedChangesGuard'
import {
  auditControlRefSx,
  auditTableContainerSx,
  auditTableHeadCellSx,
  ENGAGEMENT_KPI_ACCENTS,
  findingSeverityRiskLevel,
  selfAssessmentGapStatus,
} from './auditKit'
import AuditEngagementChrome, { type AuditWorkTab } from './AuditEngagementChrome'
import AuditEngagementFindingsPanel from './AuditEngagementFindingsPanel'
import AuditEngagementSidebar, { type EngagementWorkspaceSection } from './AuditEngagementSidebar'
import AuditEngagementSummary from './AuditEngagementSummary'
import AuditWorkingPaperPanel from './AuditWorkingPaperPanel'
import AuditCapaRegisterPanel from './AuditCapaRegisterPanel'
import {
  auditStatusLabel,
  controlReviewLabel,
  controlReviewStatus,
  controlReviewTone,
  engagementKpis,
  findingSeverityShort,
  findingTableLabel,
  groupByDomain,
  scopeModuleLabel,
} from './auditEngagementUtils'

function SectionHeader({ title, action }: { title: string; action?: ReactNode }) {
  return (
    <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
      <Typography variant="overline" color="text.secondary">
        {title}
      </Typography>
      {action}
    </Stack>
  )
}

export default function AuditEngagementWorkspace({
  detail,
  users,
  canAdmin,
  isTeamMember,
  isLeadAuditor,
  iaFieldwork = false,
  canRaiseFinding = false,
  engagementCapas = [],
  capasById,
  summary,
  onSummaryChange,
  domainNarratives,
  onDomainNarrativesChange,
  returnReason,
  onReturnReasonChange,
  externalInvites,
  inviteName,
  inviteEmail,
  onInviteNameChange,
  onInviteEmailChange,
  onSendInvite,
  onRefresh,
  onEditEngagement,
  onLogFinding,
  onError,
  onOk,
  onLoadAudits,
  onLoadCapas,
  onBackToList,
  suppressSidebar = false,
  section: controlledSection,
  onSectionChange: onControlledSectionChange,
  onPaperDirtyChange,
  onBindCloseAudit,
  onBindSubmitReport,
  onReportBusyChange,
}: {
  detail: GrcAuditDetail
  users: UserSummary[]
  canAdmin: boolean
  isTeamMember: boolean
  isLeadAuditor: boolean
  /** Internal auditor fieldwork workspace (mock Screen 2). */
  iaFieldwork?: boolean
  canRaiseFinding?: boolean
  engagementCapas?: CapaRecord[]
  capasById?: Map<number, CapaRecord>
  summary: string
  onSummaryChange: (v: string) => void
  domainNarratives: Record<string, string>
  onDomainNarrativesChange: (v: Record<string, string>) => void
  returnReason: string
  onReturnReasonChange: (v: string) => void
  externalInvites: AuditExternalInvite[]
  inviteName: string
  inviteEmail: string
  onInviteNameChange: (v: string) => void
  onInviteEmailChange: (v: string) => void
  onSendInvite: () => void
  onRefresh: () => Promise<void>
  onEditEngagement: () => void
  onLogFinding: (prefill?: { checklistItemId?: number; controlRef?: string }) => void
  onError: (msg: string) => void
  onOk: (msg: string) => void
  onLoadAudits: () => void
  onLoadCapas: () => void
  /** Auditor inner-nav "My engagements" returns to the Audit Projects list. */
  onBackToList?: () => void
  /** Parent renders IA sidebar (Screen 2 shell). */
  suppressSidebar?: boolean
  section?: EngagementWorkspaceSection
  onSectionChange?: (s: EngagementWorkspaceSection) => void
  onPaperDirtyChange?: (dirty: boolean) => void
  onBindCloseAudit?: (open: () => void) => void
  onBindSubmitReport?: (submit: () => void) => void
  onReportBusyChange?: (busy: boolean) => void
}) {
  const isAssessment = detail.audit.auditMode === 'ASSESSMENT'
  const { requestNavigation } = useOptionalUnsavedChangesGuardApi()
  const [internalSection, setInternalSection] = useState<EngagementWorkspaceSection>('overview')
  const section = controlledSection ?? internalSection
  const setSection = onControlledSectionChange ?? setInternalSection
  const [workTab, setWorkTab] = useState<AuditWorkTab>('checklist')
  const [summaryNeeded, setSummaryNeeded] = useState(false)
  const [reportBanner, setReportBanner] = useState<string | null>(null)
  const [closeAsk, setCloseAsk] = useState(false)
  const [closeBusy, setCloseBusy] = useState(false)
  const [actionKey, setActionKey] = useState<string | null>(null)
  const [submittingReport, setSubmittingReport] = useState(false)
  const actionLock = useRef(false)
  const runAction = useCallback((key: string, work: () => Promise<unknown>) => {
    if (actionLock.current) return
    actionLock.current = true
    setActionKey(key)
    void Promise.resolve(work()).finally(() => {
      actionLock.current = false
      setActionKey(null)
    })
  }, [])
  const [closeResult, setCloseResult] = useState<string | null>(null)
  const [notesDraft, setNotesDraft] = useState<Record<number, string>>({})
  const [evidenceItems, setEvidenceItems] = useState<AuditPortalEvidenceItem[]>([])
  const kpis = useMemo(() => engagementKpis(detail), [detail])
  const domainGroups = useMemo(() => groupByDomain(detail), [detail])
  const evidenceCountByNode = useMemo(() => {
    const map = new Map<string, number>()
    for (const ev of evidenceItems) {
      if (!ev.linkedNodeCode) continue
      map.set(ev.linkedNodeCode, (map.get(ev.linkedNodeCode) ?? 0) + 1)
    }
    return map
  }, [evidenceItems])

  useEffect(() => {
    if (!detail.scopeModules?.includes('EVIDENCE')) {
      setEvidenceItems([])
      return
    }
    api
      .listAuditScopedEvidence(detail.audit.id)
      .then(setEvidenceItems)
      .catch(() => setEvidenceItems([]))
  }, [detail.audit.id, detail.scopeModules])
  const editable =
    isTeamMember && !detail.audit.locked && detail.audit.status !== 'REPORT_ISSUED'
  const checklistReadOnly =
    isAssessment && detail.gapAssessmentId != null

  /** Admin/Owner: management hub (Excel STEP 2). */
  const adminManagementView = canAdmin && !iaFieldwork
  /** Auditor: Excel STEP 3 working paper. Not the old Screen 2 fieldwork shell. */
  const excelAuditorView = iaFieldwork && !canAdmin
  const useWorkChrome = adminManagementView || excelAuditorView
  const showEngagementSidebar = false
  const activeSection: EngagementWorkspaceSection = adminManagementView || excelAuditorView ? 'overview' : section
  const readOnlySelfAssessmentOnTable = false
  const showScopedBanner = !excelAuditorView && !adminManagementView && isTeamMember
  const showWorkingPaper =
    !isAssessment &&
    ((useWorkChrome && workTab === 'checklist') ||
      (!useWorkChrome && activeSection === 'controls'))
  const paperEditable =
    !canAdmin &&
    !detail.audit.locked &&
    !['REPORT_ISSUED', 'CLOSED', 'CANCELLED'].includes(detail.audit.status) &&
    (isTeamMember || iaFieldwork)
  const findingCount = detail.findings.length
  const onPaperDirty = useCallback(
    (d: boolean) => onPaperDirtyChange?.(d),
    [onPaperDirtyChange],
  )
  const openCloseAudit = useCallback(() => setCloseAsk(true), [])
  useEffect(() => {
    onBindCloseAudit?.(openCloseAudit)
  }, [onBindCloseAudit, openCloseAudit])

  const submitReport = useCallback(() => {
    if (!summary.trim()) {
      setWorkTab('summary')
      setSummaryNeeded(true)
      setReportBanner('Enter the executive summary before submitting the report.')
      return
    }
    setSummaryNeeded(false)
    setReportBanner(null)
    setSubmittingReport(true)
    onReportBusyChange?.(true)
    api
      .updateAuditSummary(detail.audit.id, summary)
      .then(() => api.auditorSignAudit(detail.audit.id))
      .then(() => {
        onOk('Report submitted. Waiting for Admin to close the audit.')
        onLoadAudits()
        return onRefresh()
      })
      .catch((e) => {
        const msg = productErrorMessage(e) || 'The report was not submitted.'
        setReportBanner(msg)
        onError(msg)
      })
      .finally(() => {
        setSubmittingReport(false)
        onReportBusyChange?.(false)
      })
  }, [detail.audit.id, summary, onOk, onLoadAudits, onRefresh, onError, onReportBusyChange])

  useEffect(() => {
    if (workTab !== 'summary') return
    if (!summaryNeeded && !reportBanner) return
    const el = document.getElementById('exec-sum-narrative')
    if (!el) return
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    if (summaryNeeded) el.focus()
  }, [summaryNeeded, reportBanner, workTab])

  onBindSubmitReport?.(submitReport)
  const showKpiStrip =
    !useWorkChrome && (activeSection === 'overview' || activeSection === 'controls')
  const raiseFindingButton = canRaiseFinding ? (
    <Button size="small" variant="contained" onClick={() => onLogFinding()}>
      + Raise Finding
    </Button>
  ) : null

  const userLabel = (id: number) => users.find((u) => u.id === id)?.fullName ?? `#${id}`

  const findingsByChecklist = useMemo(() => {
    const map = new Map<number, typeof detail.findings>()
    for (const f of detail.findings) {
      if (f.checklistItemId == null) continue
      const list = map.get(f.checklistItemId) ?? []
      list.push(f)
      map.set(f.checklistItemId, list)
    }
    return map
  }, [detail.findings])

  const saveDomainNarratives = async () => {
    try {
      const narratives = (detail.domainNarratives ?? []).map((n) => ({
        code: n.code,
        narrative: domainNarratives[n.code] ?? '',
      }))
      await api.updateAuditDomainNarratives(detail.audit.id, narratives)
      onOk('Domain narratives saved')
      await onRefresh()
    } catch (e) {
      onError(String(e))
    }
  }

  const updateChecklist = async (
    itemId: number,
    body: { assessment?: string | null; notes?: string | null },
  ) => {
    try {
      await api.updateAuditChecklist(detail.audit.id, itemId, body)
      await onRefresh()
    } catch (e) {
      onError(String(e))
    }
  }

  const renderControlRow = (
    c: (typeof detail.checklist)[0],
    showWorkingNotes: boolean,
    readOnlySelfAssessment = false,
  ) => {
    const review = controlReviewStatus(c)
    const linkedFindings = findingsByChecklist.get(c.id) ?? []
    const notesValue = notesDraft[c.id] ?? c.notes ?? ''
    const evCount = evidenceCountByNode.get(c.nodeCode) ?? 0

    return (
      <TableRow key={c.id}>
        <TableCell>
          <Typography sx={auditControlRefSx}>{c.nodeCode}</Typography>
        </TableCell>
        <TableCell>{c.title}</TableCell>
        <TableCell>
          {readOnlySelfAssessment || checklistReadOnly ? (
            <DomainBadge variant="gap" status={selfAssessmentGapStatus(c.assessment)} />
          ) : (
            <Select
              size="small"
              displayEmpty
              value={c.assessment ?? ''}
              disabled={!editable}
              onChange={(e) => {
                const v = e.target.value as string
                void updateChecklist(c.id, { assessment: v || null })
              }}
              sx={{ minWidth: 140 }}
            >
              <MenuItem value="">-</MenuItem>
              <MenuItem value="YES">Compliant</MenuItem>
              <MenuItem value="PARTIAL">Partial</MenuItem>
              <MenuItem value="NO">Non-compliant</MenuItem>
            </Select>
          )}
        </TableCell>
        <TableCell>
          <Typography variant="caption" color={evCount > 0 ? 'text.secondary' : 'text.disabled'}>
            {evCount > 0 ? `${evCount} file${evCount === 1 ? '' : 's'}` : '0 files'}
          </Typography>
        </TableCell>
        <TableCell>
          <StatusChip label={controlReviewLabel(review)} tone={controlReviewTone(review)} />
        </TableCell>
        <TableCell>
          {linkedFindings.length > 0 ? (
            <Stack spacing={0.25}>
              {linkedFindings.map((f) => (
                <DomainBadge
                  key={f.id}
                  variant="risk"
                  level={findingSeverityRiskLevel(f.severity)}
                  label={findingTableLabel(f.severity)}
                />
              ))}
            </Stack>
          ) : canRaiseFinding ? (
            <Button
              size="small"
              variant="text"
              sx={{ minWidth: 0, px: 0.5, fontSize: '0.7rem', fontWeight: 600 }}
              onClick={() => onLogFinding({ checklistItemId: c.id, controlRef: c.nodeCode })}
            >
              Raise Finding
            </Button>
          ) : (
            <Typography variant="caption" color="text.secondary">
              -
            </Typography>
          )}
        </TableCell>
        {showWorkingNotes && (
          <TableCell sx={{ minWidth: 200 }}>
            <TextField
              size="small"
              placeholder="Working notes (private until sign-off)"
              multiline
              minRows={1}
              value={notesValue}
              disabled={!editable}
              onChange={(e) => setNotesDraft((prev) => ({ ...prev, [c.id]: e.target.value }))}
              onBlur={() => {
                const draft = notesDraft[c.id]
                if (draft === undefined || draft === (c.notes ?? '')) return
                void updateChecklist(c.id, { notes: draft || null })
              }}
              fullWidth
            />
          </TableCell>
        )}
      </TableRow>
    )
  }

  const findingsEmptyMessage = adminManagementView
    ? 'No findings yet. Add a finding or conclude NC / Observation / OFI on a checklist line.'
    : 'No findings yet. Add a finding or conclude NC on a checklist line.'

  const renderFindingsList = (trailing?: ReactNode) => (
    <AuditEngagementFindingsPanel
      findings={detail.findings}
      capasById={capasById}
      users={users}
      emptyMessage={findingsEmptyMessage}
      trailing={trailing}
    />
  )

  const controlsTableHead = (
    <TableRow>
      <TableCell sx={auditTableHeadCellSx}>Control</TableCell>
      <TableCell sx={auditTableHeadCellSx}>Title</TableCell>
      <TableCell sx={auditTableHeadCellSx} width={160}>
        Self-assessment
      </TableCell>
      <TableCell sx={auditTableHeadCellSx} width={90}>
        Evidence
      </TableCell>
      <TableCell sx={auditTableHeadCellSx} width={120}>
        Audit status
      </TableCell>
      <TableCell sx={auditTableHeadCellSx} width={120}>
        Finding
      </TableCell>
        {(isTeamMember || canAdmin) && activeSection === 'controls' && isTeamMember && (
        <TableCell sx={{ ...auditTableHeadCellSx, minWidth: 200 }}>Working notes</TableCell>
      )}
    </TableRow>
  )

  const handleSidebarSection = (s: EngagementWorkspaceSection) => {
    if (s === 'engagements') {
      onBackToList?.()
      return
    }
    setSection(s)
  }

  return (
    <Box sx={{ display: 'flex', flex: 1, minHeight: 0, width: '100%' }}>
      {showEngagementSidebar && (
        <AuditEngagementSidebar
          iaFieldwork
          detail={detail}
          section={section}
          onSectionChange={handleSidebarSection}
          findingsCount={detail.findings.length}
          activeEngagementId={detail.audit.id}
        />
      )}
      <Stack
        spacing={1.5}
        sx={{
          flex: 1,
          minHeight: 0,
          overflow: useWorkChrome && workTab === 'checklist' ? 'hidden' : 'auto',
          p: 2,
        }}
      >
        {reportBanner && (
          <Alert severity="error" variant="outlined" onClose={() => setReportBanner(null)}>
            {reportBanner}
          </Alert>
        )}

        {detail.returnReason && (
          <Alert severity="error" variant="outlined">
            Returned: {detail.returnReason}
          </Alert>
        )}

        {showScopedBanner && (
          <Alert severity="info" variant="outlined" sx={{ py: 0.5 }}>
            You are viewing a scoped engagement: <strong>{detail.audit.name}</strong>: access
            limited to assigned scope only.
          </Alert>
        )}

        {useWorkChrome && (
          <AuditEngagementChrome
            testId={adminManagementView ? 'audit-admin-management' : 'audit-auditor-checklist'}
            lines={kpis.total}
            reviewed={kpis.reviewed}
            findings={kpis.findings}
            daysRemaining={kpis.daysRemaining}
            progressPct={kpis.progressPct}
            workTab={workTab}
            onWorkTab={(tab) => requestNavigation(() => setWorkTab(tab))}
            showFindingsTab={!isAssessment}
          />
        )}

        {iaFieldwork && !excelAuditorView && isTeamMember && ['DRAFT', 'ACCESS_GRANTED'].includes(detail.audit.status) && (
          <Alert
            severity="info"
            variant="outlined"
            action={
              <BusyButton
                size="small"
                variant="contained"
                busy={actionKey === 'start'}
                busyLabel="Starting..."
                onClick={() =>
                  runAction('start', () =>
                    api
                      .startAudit(detail.audit.id)
                      .then(() => {
                        onOk('Fieldwork started')
                        onLoadAudits()
                        return onRefresh()
                      })
                      .catch((e) => onError(String(e))),
                  )
                }
              >
                Start Fieldwork
              </BusyButton>
            }
          >
            This engagement is ready for fieldwork. Start to review controls, evidence, and raise findings.
          </Alert>
        )}

        {iaFieldwork && !excelAuditorView && isTeamMember && (
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            {canRaiseFinding && (
              <Button size="small" variant="outlined" onClick={() => onLogFinding()}>
                Log Finding
              </Button>
            )}
            {(detail.audit.locked || detail.audit.status === 'CLOSED') && (
              <BusyButton
                size="small"
                variant="outlined"
                busy={actionKey === 'pdf'}
                busyLabel="Downloading..."
                onClick={() =>
                  runAction('pdf', () =>
                    api.downloadAuditReport(detail.audit.id).catch((e) => onError(String(e))),
                  )
                }
              >
                Download Report (PDF)
              </BusyButton>
            )}
          </Stack>
        )}

        {!iaFieldwork && !adminManagementView && (
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
        {canAdmin && detail.audit.status === 'DRAFT' && (
          <Button size="small" variant="outlined" onClick={onEditEngagement}>
            Edit Audit
          </Button>
        )}
        {(canAdmin || isTeamMember) && ['DRAFT', 'ACCESS_GRANTED'].includes(detail.audit.status) && (
          <BusyButton
            size="small"
            variant="outlined"
            busy={actionKey === 'start'}
            busyLabel="Starting..."
            onClick={() =>
              runAction('start', () =>
                api
                  .startAudit(detail.audit.id)
                  .then(() => {
                    onOk('Engagement started')
                    onLoadAudits()
                    return onRefresh()
                  })
                  .catch((e) => onError(String(e))),
              )
            }
          >
            Start Execution
          </BusyButton>
        )}
        {isTeamMember && !iaFieldwork && !isAssessment && editable && (
          <Button size="small" variant="outlined" onClick={() => onLogFinding()}>
            Log Finding
          </Button>
        )}
        {isLeadAuditor && detail.audit.status === 'IN_PROGRESS' && (
          <BusyButton
            size="small"
            variant="contained"
            busy={submittingReport}
            busyLabel="Submitting..."
            onClick={submitReport}
          >
            Submit Report
          </BusyButton>
        )}
        {canAdmin && detail.audit.status === 'REPORT_ISSUED' && (
          <>
            <BusyButton
              size="small"
              variant="contained"
              busy={actionKey === 'close'}
              busyLabel="Closing..."
              onClick={() =>
                runAction('close', () =>
                  api
                    .adminSignAudit(detail.audit.id)
                    .then(() => {
                      onOk(
                        isAssessment ? 'Engagement locked' : 'Engagement locked: CAPAs opened',
                      )
                      onLoadAudits()
                      onLoadCapas()
                      return onRefresh()
                    })
                    .catch((e) => onError(String(e))),
                )
              }
            >
              Close Audit (Admin)
            </BusyButton>
            <TextField
              size="small"
              placeholder="Return reason"
              value={returnReason}
              onChange={(e) => onReturnReasonChange(e.target.value)}
            />
            <BusyButton
              size="small"
              color="warning"
              busy={actionKey === 'return'}
              busyLabel="Returning..."
              disabled={!returnReason.trim()}
              onClick={() =>
                runAction('return', () =>
                  api
                    .returnAudit(detail.audit.id, returnReason.trim())
                    .then(() => {
                      onReturnReasonChange('')
                      onOk('Returned to auditor')
                      onLoadAudits()
                      return onRefresh()
                    })
                    .catch((e) => onError(String(e))),
                )
              }
            >
              Return
            </BusyButton>
          </>
        )}
      </Stack>
        )}

        {(activeSection === 'overview' || activeSection === 'controls') && showKpiStrip && (
          <KpiCardGrid columns={4}>
            <KpiCard label="Controls in scope" value={kpis.total} accentColor={ENGAGEMENT_KPI_ACCENTS.neutral} />
            <KpiCard label="Reviewed" value={kpis.reviewed} accentColor={ENGAGEMENT_KPI_ACCENTS.success} />
            {!isAssessment ? (
              <KpiCard label="Findings raised" value={kpis.findings} accentColor={ENGAGEMENT_KPI_ACCENTS.danger} />
            ) : (
              <KpiCard
                label="Non-compliant"
                value={kpis.nonCompliant}
                accentColor={ENGAGEMENT_KPI_ACCENTS.danger}
                sub={`${kpis.compliant} compliant · ${kpis.partial} partial`}
              />
            )}
            <KpiCard
              label="Days remaining"
              value={kpis.daysRemaining ?? '-'}
              accentColor={ENGAGEMENT_KPI_ACCENTS.accent}
              sub={
                iaFieldwork
                  ? undefined
                  : detail.audit.endDate
                    ? `Ends ${detail.audit.endDate}`
                    : 'No end date'
              }
            />
          </KpiCardGrid>
        )}

        {useWorkChrome && workTab === 'summary' && (
          <AuditEngagementSummary
            detail={detail}
            users={users}
            lines={kpis.total}
            reviewed={kpis.reviewed}
            findings={kpis.findings}
            canAdmin={canAdmin}
            isLeadAuditor={isLeadAuditor}
            isTeamMember={isTeamMember}
            summary={summary}
            summaryError={
              summaryNeeded && !summary.trim()
                ? 'Enter the executive summary before submitting the report'
                : reportBanner && summary.trim()
                  ? reportBanner
                  : undefined
            }
            onSummaryChange={onSummaryChange}
            onRefresh={onRefresh}
            onError={onError}
            externalInvites={externalInvites}
            inviteName={inviteName}
            inviteEmail={inviteEmail}
            onInviteNameChange={onInviteNameChange}
            onInviteEmailChange={onInviteEmailChange}
            onSendInvite={onSendInvite}
          />
        )}

        {useWorkChrome && workTab === 'findings' && !isAssessment && (
          <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
            {renderFindingsList()}
          </Box>
        )}

        {activeSection === 'overview' && !useWorkChrome && (
          <Stack spacing={1.5}>
            {iaFieldwork && (
              <SettingsCard compact title="Team and scope">
                <Typography variant="body2" color="text.secondary">
                  Lead auditor: {userLabel(detail.audit.leadAuditorUserId)}
                </Typography>
                {(detail.teamMembers ?? []).filter((m) => m.teamRole === 'MEMBER').length > 0 && (
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                    Team:{' '}
                    {(detail.teamMembers ?? [])
                      .filter((m) => m.teamRole === 'MEMBER')
                      .map((m) => userLabel(m.userId))
                      .join(', ')}
                  </Typography>
                )}
                {detail.scopeNotes && (
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                    {detail.scopeNotes}
                  </Typography>
                )}
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.75 }}>
                  Data modules:{' '}
                  {(detail.scopeModules ?? []).length > 0
                    ? (detail.scopeModules ?? []).map(scopeModuleLabel).join(', ')
                    : '-'}
                  {(detail.scopeNodeIds ?? []).length > 0 &&
                    ` · ${detail.scopeNodeIds.length} control(s) in subset`}
                </Typography>
              </SettingsCard>
            )}

            {iaFieldwork && isLeadAuditor && (
              <TextField
                label="Executive summary"
                required
                multiline
                minRows={3}
                value={summary}
                onChange={(e) => onSummaryChange(e.target.value)}
                disabled={detail.audit.locked || detail.audit.status !== 'IN_PROGRESS'}
                onBlur={() => {
                  if (detail.audit.status === 'IN_PROGRESS' && !detail.audit.locked) {
                    api.updateAuditSummary(detail.audit.id, summary).catch((e) => onError(String(e)))
                  }
                }}
                helperText="Complete before submitting the report."
              />
            )}

            {detail.audit.auditType === 'EXTERNAL' && canAdmin && (
              <SettingsCard compact title="External auditor access (OTP)">
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
                  <Button
                    size="small"
                    variant="contained"
                    disabled={!inviteName.trim() || !inviteEmail.trim()}
                    onClick={onSendInvite}
                    sx={controlActionSx}
                  >
                    Send Invite
                  </Button>
                </FieldActionRow>
              </SettingsCard>
            )}
          </Stack>
        )}

        {showWorkingPaper && (
          <AuditWorkingPaperPanel
            detail={detail}
            canEdit={paperEditable}
            onRefresh={onRefresh}
            onError={onError}
            onOk={onOk}
            onDirtyChange={onPaperDirty}
          />
        )}

        {activeSection === 'overview' && iaFieldwork && !excelAuditorView && !isAssessment && (
          <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
            {renderFindingsList()}
          </Box>
        )}

        {activeSection === 'findings' && !isAssessment && (
          <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
            {renderFindingsList(raiseFindingButton)}
          </Box>
        )}

        {activeSection === 'evidence' && (
          <Box>
            <SectionHeader title="Evidence Review" />
            <TableContainer sx={auditTableContainerSx}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell sx={auditTableHeadCellSx}>Title</TableCell>
                    <TableCell sx={auditTableHeadCellSx}>Control</TableCell>
                    <TableCell sx={auditTableHeadCellSx}>Classification</TableCell>
                    <TableCell sx={auditTableHeadCellSx}>Status</TableCell>
                    <TableCell sx={auditTableHeadCellSx} align="right">
                      Action
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {evidenceItems.map((ev) => (
                    <TableRow key={ev.id}>
                      <TableCell>{ev.title}</TableCell>
                      <TableCell>{ev.linkedNodeCode ?? '-'}</TableCell>
                      <TableCell>{ev.classification}</TableCell>
                      <TableCell>{ev.status}</TableCell>
                      <TableCell align="right">
                        <Button
                          size="small"
                          onClick={() => api.downloadEvidence(ev.id).catch((e) => onError(String(e)))}
                        >
                          Download
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                  {evidenceItems.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5}>
                        <Typography variant="body2" color="text.secondary" sx={{ py: 2, textAlign: 'center' }}>
                          No scoped evidence linked to controls for this engagement.
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}

        {activeSection === 'capa' && (
          <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
            <AuditCapaRegisterPanel
              register={detail.audit.auditType === 'EXTERNAL' ? 'EXTERNAL_AUDIT' : 'INTERNAL_AUDIT'}
              capas={engagementCapas}
              users={users}
              audits={[{ id: detail.audit.id, name: detail.audit.name }]}
              engagementId={detail.audit.id}
              onSelect={() => undefined}
            />
          </Box>
        )}

        {(activeSection === 'controls' || excelAuditorView) && isAssessment && (
        <Stack spacing={1.25}>
          {detail.gapAssessmentId != null && (
            <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
              <Typography variant="subtitle2" fontWeight={700}>
                Linked Gap assessment #{detail.gapAssessmentId}
              </Typography>
              {editable && (
                <Button
                  size="small"
                  variant="outlined"
                  onClick={() =>
                    api
                      .syncAuditGapScores(detail.audit.id)
                      .then(() => {
                        onOk('Scores refreshed from Gap')
                        return onRefresh()
                      })
                      .catch((e) => onError(String(e)))
                  }
                >
                  Refresh from Gap
                </Button>
              )}
            </Stack>
          )}
          {domainGroups.map((group) => (
            <Accordion key={group.code} defaultExpanded disableGutters elevation={0} sx={{ border: `1px solid ${brand.border}`, '&:before': { display: 'none' } }}>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                  <Typography variant="subtitle2" fontWeight={700}>
                    {group.code === '_other' ? group.name : `${group.code}: ${group.name}`}
                  </Typography>
                  {group.scoreLabel && (
                    <StatusChip label={group.scoreLabel} tone="info" />
                  )}
                  <Typography variant="caption" color="text.secondary">
                    {group.controls.length} control(s)
                    {group.findings.length > 0 ? ` · ${group.findings.length} finding(s)` : ''}
                  </Typography>
                </Stack>
              </AccordionSummary>
              <AccordionDetails sx={{ pt: 0 }}>
                {group.code !== '_other' && group.code !== 'all' && (
                  <TextField
                    label="Domain narrative"
                    multiline
                    minRows={2}
                    fullWidth
                    sx={{ mb: 1.5 }}
                    value={domainNarratives[group.code] ?? group.narrative}
                    onChange={(e) =>
                      onDomainNarrativesChange({
                        ...domainNarratives,
                        [group.code]: e.target.value,
                      })
                    }
                    disabled={!editable}
                  />
                )}
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Control</TableCell>
                        <TableCell width={160}>Self-assessment</TableCell>
                        <TableCell width={120}>Status</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {group.controls.map((c) => (
                        <TableRow key={c.id}>
                          <TableCell>
                            <Typography variant="body2" fontWeight={600}>
                              {c.nodeCode}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              {c.title}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            <DomainBadge variant="gap" status={selfAssessmentGapStatus(c.assessment)} />
                          </TableCell>
                          <TableCell>
                            <StatusChip
                              label={controlReviewLabel(controlReviewStatus(c))}
                              tone={controlReviewTone(controlReviewStatus(c))}
                            />
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
                {group.findings.length > 0 && (
                  <Stack spacing={0.75} sx={{ mt: 1.5 }}>
                    <Typography variant="caption" fontWeight={700} color="text.secondary">
                      Findings in this domain
                    </Typography>
                    {group.findings.map((f) => (
                      <Box
                        key={f.id}
                        sx={{
                          p: 1.25,
                          borderLeft: 3,
                          borderColor:
                            f.severity === 'MAJOR_NC'
                              ? 'error.main'
                              : f.severity === 'MINOR_NC'
                                ? 'warning.main'
                                : 'grey.400',
                          bgcolor: brand.hover,
                          borderRadius: 1,
                        }}
                      >
                        <Typography variant="body2" fontWeight={700}>
                          {f.displayCode} · {f.title}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {findingSeverityShort(f.severity)}
                          {f.controlRef ? ` · ${f.controlRef}` : ''}
                        </Typography>
                      </Box>
                    ))}
                  </Stack>
                )}
              </AccordionDetails>
            </Accordion>
          ))}
          {editable && (detail.domainNarratives ?? []).length > 0 && (
            <BusyButton
              size="small"
              variant="outlined"
              busy={actionKey === 'narratives'}
              busyLabel="Saving..."
              onClick={() => runAction('narratives', () => saveDomainNarratives())}
            >
              Save Domain Narratives
            </BusyButton>
          )}
        </Stack>
        )}

        {isLeadAuditor && detail.audit.status === 'REPORT_ISSUED' && (
          <Alert severity="info" variant="outlined">
            Report submitted. An administrator will close the audit.
          </Alert>
        )}
      </Stack>
      <ConfirmActionDialog
        open={closeAsk}
        title="Close Audit"
        message={
          findingCount === 0
            ? 'No findings were raised. Close this audit with no findings? Admin can still raise CAPA later if needed.'
            : `Close this audit with ${findingCount} finding${findingCount === 1 ? '' : 's'}? Admin can raise CAPA from the CAPA register.`
        }
        confirmLabel="Close Audit"
        confirmColor="primary"
        busy={closeBusy}
        busyLabel="Closing..."
        onCancel={() => setCloseAsk(false)}
        onConfirm={() => {
          setCloseBusy(true)
          api
            .closeAudit(detail.audit.id)
            .then(() => {
              setCloseAsk(false)
              setCloseResult(auditStatusLabel('CLOSED', findingCount))
              onLoadAudits()
              return onRefresh()
            })
            .catch((e) => onError(String(e)))
            .finally(() => setCloseBusy(false))
        }}
      />
      <ConfirmActionDialog
        open={closeResult != null}
        title="Audit Closed"
        message={closeResult ?? ''}
        confirmLabel="Done"
        confirmColor="primary"
        onCancel={() => {
          setCloseResult(null)
          onBackToList?.()
        }}
        onConfirm={() => {
          setCloseResult(null)
          onBackToList?.()
        }}
      />
    </Box>
  )
}
