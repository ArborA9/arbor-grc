import { useCallback, useEffect, useState } from 'react'
import { Box, Typography } from '@mui/material'
import type { CapaRecord, GrcAuditDetail, GrcAuditStatus, GrcAuditSummary, GrcAuditType, UserSummary } from '../../api'
import AuditEngagementSidebar, { type EngagementWorkspaceSection } from './AuditEngagementSidebar'
import AuditEngagementWorkspace from './AuditEngagementWorkspace'
import AuditMyEngagementsPanel from './AuditMyEngagementsPanel'
import type { AuditExternalInvite } from '../../api'

type EngagementStatusFilter = 'ALL' | GrcAuditStatus
type EngagementTypeFilter = 'ALL' | GrcAuditType

export default function IaFieldworkShell({
  detail,
  audits,
  users,
  isTeamMember,
  isLeadAuditor,
  canRaiseFinding,
  engagementCapas,
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
  onOpenEngagement,
  onCreateEngagement,
  canCreateEngagement = false,
  engagementSearch,
  engagementStatusFilter,
  engagementTypeFilter,
  onEngagementSearchChange,
  onEngagementStatusFilterChange,
  onEngagementTypeFilterChange,
  onClearEngagementFilters,
}: {
  detail: GrcAuditDetail | null
  audits: GrcAuditSummary[]
  users: UserSummary[]
  isTeamMember: boolean
  isLeadAuditor: boolean
  canRaiseFinding: boolean
  engagementCapas: CapaRecord[]
  capasById: Map<number, CapaRecord>
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
  onOpenEngagement: (id: number) => void | Promise<void>
  onCreateEngagement?: () => void
  canCreateEngagement?: boolean
  engagementSearch: string
  engagementStatusFilter: EngagementStatusFilter
  engagementTypeFilter: EngagementTypeFilter
  onEngagementSearchChange: (v: string) => void
  onEngagementStatusFilterChange: (v: EngagementStatusFilter) => void
  onEngagementTypeFilterChange: (v: EngagementTypeFilter) => void
  onClearEngagementFilters: () => void
}) {
  const [section, setSection] = useState<EngagementWorkspaceSection>('engagements')

  // Deep-link (?auditId=) loads detail in parent but leaves section on engagements: open workspace.
  useEffect(() => {
    if (!detail) return
    setSection((prev) => (prev === 'engagements' ? 'controls' : prev))
  }, [detail?.audit.id])

  useEffect(() => {
    if (section === 'capa' && detail) {
      onLoadCapas()
    }
  }, [section, detail, onLoadCapas])

  const handleOpenEngagement = useCallback(
    (id: number) => {
      void Promise.resolve(onOpenEngagement(id)).then(() => setSection('controls'))
    },
    [onOpenEngagement],
  )

  const findingsCount = detail?.findings.length ?? 0
  const activeEngagementId = detail?.audit.id

  return (
    <Box sx={{ display: 'flex', flex: 1, minHeight: 0, width: '100%', overflow: 'hidden' }}>
      <AuditEngagementSidebar
        iaFieldwork
        detail={detail}
        section={section}
        onSectionChange={setSection}
        findingsCount={findingsCount}
        activeEngagementId={activeEngagementId}
      />
      <Box sx={{ flex: 1, minHeight: 0, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        {section === 'engagements' ? (
          <AuditMyEngagementsPanel
            embedded
            audits={audits}
            search={engagementSearch}
            statusFilter={engagementStatusFilter}
            typeFilter={engagementTypeFilter}
            onSearchChange={onEngagementSearchChange}
            onStatusFilterChange={onEngagementStatusFilterChange}
            onTypeFilterChange={onEngagementTypeFilterChange}
            onClearFilters={onClearEngagementFilters}
            onOpen={handleOpenEngagement}
            onCreate={onCreateEngagement}
            canCreate={canCreateEngagement}
            activeEngagementId={activeEngagementId}
          />
        ) : detail ? (
          <AuditEngagementWorkspace
            detail={detail}
            users={users}
            canAdmin={false}
            isTeamMember={isTeamMember}
            isLeadAuditor={isLeadAuditor}
            iaFieldwork
            suppressSidebar
            section={section}
            onSectionChange={setSection}
            canRaiseFinding={canRaiseFinding}
            engagementCapas={engagementCapas}
            capasById={capasById}
            summary={summary}
            onSummaryChange={onSummaryChange}
            domainNarratives={domainNarratives}
            onDomainNarrativesChange={onDomainNarrativesChange}
            returnReason={returnReason}
            onReturnReasonChange={onReturnReasonChange}
            externalInvites={externalInvites}
            inviteName={inviteName}
            inviteEmail={inviteEmail}
            onInviteNameChange={onInviteNameChange}
            onInviteEmailChange={onInviteEmailChange}
            onSendInvite={onSendInvite}
            onRefresh={onRefresh}
            onEditEngagement={onEditEngagement}
            onLogFinding={onLogFinding}
            onError={onError}
            onOk={onOk}
            onLoadAudits={onLoadAudits}
            onLoadCapas={onLoadCapas}
          />
        ) : (
          <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', p: 4 }}>
            <Typography variant="body2" color="text.secondary" textAlign="center" sx={{ maxWidth: 360 }}>
              No engagement selected. Open <strong>My engagements</strong> in the sidebar to choose an
              assigned audit.
            </Typography>
          </Box>
        )}
      </Box>
    </Box>
  )
}
