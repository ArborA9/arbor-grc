import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  api,
  type EngagementScopeModule,
  type FrameworkNode,
  type FrameworkSummary,
  type GapAssessmentSummary,
  type GrcAuditDetail,
  type GrcAuditType,
  type AuditStandard,
  type ChecklistTemplateKey,
  type AuditChecklistLibrary,
  type UserSummary,
} from '../../api'

/** Field IDs for validation error tracking - mirrors Excel Audit Initiation columns */
export const AUDIT_INPUT_ID = {
  title: 'audit-title',
  auditType: 'audit-type',
  standard: 'audit-standard',
  checklistTemplate: 'audit-checklist',
  leadAuditor: 'audit-lead',
  auditorOrganisation: 'audit-org',
  auditeeOrgUnitId: 'audit-auditee',
  scopeModules: 'audit-modules',
  plannedStart: 'audit-start',
  plannedEnd: 'audit-end',
} as const

function isAuditorRole(roles: string[]): boolean {
  return roles.some((r) => r === 'INTERNAL_AUDITOR' || r === 'EXTERNAL_AUDITOR')
}

export const SCOPE_MODULES: { value: EngagementScopeModule; label: string; note: string }[] = [
  { value: 'DASHBOARD', label: 'Dashboard', note: 'Overview widgets' },
  { value: 'GAP', label: 'Gap Assessment', note: 'Control results' },
  { value: 'SOA', label: 'Statement of Applicability', note: 'SoA with decisions' },
  { value: 'RISK', label: 'Information Security Risk', note: 'Read only' },
  { value: 'ERM', label: 'Enterprise Risk', note: 'Read only' },
  { value: 'BCMS', label: 'BCMS / BIA', note: 'Read only' },
  { value: 'PRIVACY', label: 'Privacy (ROPA, DPIA)', note: 'Read only' },
  { value: 'ASSET', label: 'Assets', note: 'Read only' },
  { value: 'EVIDENCE', label: 'Evidence Library', note: 'Download and review' },
]

export function isAssessmentFramework(f: FrameworkSummary): boolean {
  const code = f.code.toUpperCase()
  const name = f.name.toUpperCase()
  return code.includes('UAE') || name.includes('UAE') || code.includes('NCEMA') || name.includes('NCEMA')
}

export function defaultScopeModules(moduleCode: string): EngagementScopeModule[] {
  switch (moduleCode) {
    case 'ISMS':
      return ['GAP', 'SOA', 'RISK']
    case 'ERM':
      return ['RISK']
    case 'BCMS':
    case 'PRIVACY':
      return ['GAP']
    default:
      return ['GAP', 'SOA', 'RISK']
  }
}

export function useAuditEngagementForm({
  mode,
  detail,
  users,
  frameworks,
  gaps,
  active,
  defaultLeadUserId,
  internalPlannerOnly = false,
}: {
  mode: 'create' | 'edit'
  detail?: GrcAuditDetail | null
  users: UserSummary[]
  frameworks: FrameworkSummary[]
  gaps: GapAssessmentSummary[]
  active: boolean
  /** When set on create, pre-select lead auditor (IA self-plan). */
  defaultLeadUserId?: number
  /** Hide external audit type: Internal Auditor planning only. */
  internalPlannerOnly?: boolean
}) {
  const [name, setName] = useState('')
  const [auditType, setAuditType] = useState<GrcAuditType>('INTERNAL')
  const [standard, setStandard] = useState<AuditStandard>('ISO_27001')
  const [checklistTemplate, setChecklistTemplate] = useState<ChecklistTemplateKey>('ISO_27001')
  const [checklistLibraryId, setChecklistLibraryId] = useState<number | ''>('')
  const [checklistLibraryIds, setChecklistLibraryIds] = useState<number[]>([])
  const [libraries, setLibraries] = useState<AuditChecklistLibrary[]>([])
  const [auditorOrganisation, setAuditorOrganisation] = useState('')
  const [auditeeOrgUnitId, setAuditeeOrgUnitId] = useState<number | ''>('')
  const [auditMode, setAuditMode] = useState<'COMPLIANCE' | 'ASSESSMENT'>('COMPLIANCE')
  const [frameworkId, setFrameworkId] = useState<number | ''>('')
  const [gapId, setGapId] = useState<number | ''>('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [scopeNotes, setScopeNotes] = useState('')
  const [orgUnitIds, setOrgUnitIds] = useState<number[]>([])
  const [scopeModules, setScopeModules] = useState<EngagementScopeModule[]>([])
  const [limitControls, setLimitControls] = useState(false)
  const [scopeNodeIds, setScopeNodeIds] = useState<number[]>([])
  const [frameworkNodes, setFrameworkNodes] = useState<FrameworkNode[]>([])
  const [leadId, setLeadId] = useState<number | ''>('')
  const [teamIds, setTeamIds] = useState<number[]>([])
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteName, setInviteName] = useState('')
  const [busy, setBusy] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const clearFieldError = useCallback((id: string) => {
    setFieldErrors((prev) => {
      if (!prev[id]) return prev
      const next = { ...prev }
      delete next[id]
      return next
    })
  }, [])

  const selectedFramework = frameworks.find((f) => f.id === frameworkId)
  const assessmentCapable = selectedFramework ? isAssessmentFramework(selectedFramework) : false
  const linkableGaps = gaps.filter(
    (g) =>
      g.frameworkId === frameworkId &&
      (g.status === 'IN_PROGRESS' || g.status === 'COMPLETED'),
  )

  const resetFromDetail = useCallback(() => {
    if (mode === 'edit' && detail) {
      setName(detail.audit.name)
      setAuditType(detail.audit.auditType)
      setStandard(detail.audit.auditStandard ?? 'ISO_27001')
      setAuditorOrganisation(detail.audit.auditorOrganisation ?? '')
      const ou = detail.audit.auditeeOrgUnitId ?? detail.orgUnitIds?.[0]
      setAuditeeOrgUnitId(ou == null ? '' : ou)
      setAuditMode(detail.audit.auditMode === 'ASSESSMENT' ? 'ASSESSMENT' : 'COMPLIANCE')
      setFrameworkId(detail.audit.frameworkId ?? '')
      setGapId(detail.gapAssessmentId ?? '')
      setStartDate(detail.audit.startDate ?? '')
      setEndDate(detail.audit.endDate ?? '')
      setScopeNotes(detail.scopeNotes ?? '')
      setOrgUnitIds(detail.orgUnitIds ?? [])
      setScopeModules(detail.scopeModules?.length ? [...detail.scopeModules] : [])
      const nodes = detail.scopeNodeIds ?? []
      setLimitControls(nodes.length > 0)
      setScopeNodeIds(nodes)
      setLeadId(detail.audit.leadAuditorUserId)
      setTeamIds(
        (detail.teamMembers ?? [])
          .filter((m) => m.teamRole === 'MEMBER')
          .map((m) => m.userId),
      )
    } else {
      setName('')
      setAuditType('INTERNAL')
      setStandard('ISO_27001')
      setChecklistTemplate('ISO_27001')
      setChecklistLibraryId('')
      setChecklistLibraryIds([])
      setAuditorOrganisation('')
      setAuditeeOrgUnitId('')
      setAuditMode('COMPLIANCE')
      setFrameworkId('')
      setGapId('')
      setStartDate('')
      setEndDate('')
      setScopeNotes('')
      setOrgUnitIds([])
      setScopeModules(['GAP', 'SOA', 'RISK'])
      setLimitControls(false)
      setScopeNodeIds([])
      setLeadId('')
      setTeamIds([])
      setInviteEmail('')
      setInviteName('')
      if (internalPlannerOnly) {
        setAuditType('INTERNAL')
      }
    }
  }, [detail, mode, internalPlannerOnly])

  useEffect(() => {
    if (active) resetFromDetail()
  }, [active, resetFromDetail])

  useEffect(() => {
    if (!active || frameworkId === '') {
      setFrameworkNodes([])
      return
    }
    api
      .getTenantFramework(Number(frameworkId))
      .then((fw) => {
        setFrameworkNodes(fw.nodes ?? [])
        if (mode === 'create') {
          setScopeModules(defaultScopeModules(fw.framework.moduleCode))
        }
      })
      .catch(() => setFrameworkNodes([]))
  }, [active, frameworkId, mode])

  const leadCandidates = useMemo(
    () => users.filter((u) => isAuditorRole(u.roles)),
    [users],
  )

  useEffect(() => {
    if (users.length === 0 || leadId === '') return
    const selected = users.find((u) => u.id === leadId)
    if (selected && !isAuditorRole(selected.roles)) setLeadId('')
  }, [users, leadId])

  const defaultLeadApplied = useRef(false)
  useEffect(() => {
    if (!active) defaultLeadApplied.current = false
  }, [active])
  useEffect(() => {
    if (defaultLeadApplied.current || mode !== 'create' || defaultLeadUserId == null || users.length === 0) {
      return
    }
    const mine = users.find((u) => u.id === defaultLeadUserId)
    if (!mine || !isAuditorRole(mine.roles)) return
    defaultLeadApplied.current = true
    setLeadId((current) => (current === '' ? defaultLeadUserId : current))
  }, [active, mode, defaultLeadUserId, users])

  const teamCandidates = useMemo(
    () => leadCandidates.filter((u) => u.id !== leadId),
    [leadCandidates, leadId],
  )

  useEffect(() => {
    if (auditType !== 'EXTERNAL') return
    const normalized = inviteEmail.trim().toLowerCase()
    if (!normalized) return
    const match = leadCandidates.find((u) => u.email.toLowerCase() === normalized)
    if (match) {
      setLeadId(match.id)
      setInviteEmail('')
      setInviteName('')
    }
  }, [auditType, inviteEmail, leadCandidates])

  useEffect(() => {
    if (!active || mode !== 'create') return
    let cancelled = false
    api
      .listAuditChecklistLibraries()
      .then((list) => {
        if (cancelled) return
        setLibraries(list)
        setChecklistLibraryId((prev) => {
          if (prev !== '') return prev
          const match =
            list.find((r) => r.templateKey === checklistTemplate) ??
            list.find((r) => r.systemSeeded) ??
            list[0]
          return match?.id ?? ''
        })
      })
      .catch(() => {
        if (!cancelled) setLibraries([])
      })
    return () => {
      cancelled = true
    }
    // checklistTemplate is the default seed only; user can change Checklist Used after load
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active, mode])

  const pickLibraryForStandard = (s: AuditStandard) => {
    if (s === 'MULTIPLE') {
      setChecklistLibraryId('')
      setChecklistLibraryIds([])
      return
    }
    setChecklistLibraryIds([])
    const key: ChecklistTemplateKey =
      s === 'ISO_20000' ? 'ISO_20000' : s === 'UAE_IA' ? 'UAE_IA' : 'ISO_27001'
    setChecklistTemplate(key)
    const match = libraries.find((r) => r.templateKey === key)
    if (match) setChecklistLibraryId(match.id)
  }

  const toggleChecklistLibrary = (id: number) => {
    setChecklistLibraryIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]))
  }

  const toggleOrgUnit = (id: number) => {
    setOrgUnitIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]))
  }

  const toggleScopeModule = (m: EngagementScopeModule) => {
    setScopeModules((prev) => {
      if (prev.includes(m)) {
        const next = prev.filter((x) => x !== m)
        return next.length === 0 ? prev : next
      }
      return [...prev, m]
    })
  }

  const toggleTeam = (id: number) => {
    setTeamIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]))
  }

  const toggleControl = (id: number) => {
    setScopeNodeIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]))
  }

  const setAuditee = (id: number | '') => {
    setAuditeeOrgUnitId(id)
    setOrgUnitIds(id === '' ? [] : [id])
  }

  /** Compute validation errors without updating state - safe to call during render */
  const computeErrors = useCallback((): Record<string, string> => {
    const errors: Record<string, string> = {}
    if (!name.trim()) {
      errors[AUDIT_INPUT_ID.title] = 'Audit title is required'
    }
    const externalLeadByInvite =
      auditType === 'EXTERNAL' && inviteEmail.trim() !== '' && inviteName.trim() !== ''
    if (auditType === 'INTERNAL' && leadId === '') {
      errors[AUDIT_INPUT_ID.leadAuditor] = 'Lead auditor is required'
    }
    if ((auditType === 'FOLLOW_UP' || auditType === 'EXTERNAL_ASSESSMENT') && leadId === '') {
      errors[AUDIT_INPUT_ID.leadAuditor] = 'Lead auditor is required'
    }
    if (auditType === 'EXTERNAL' && leadId === '' && !externalLeadByInvite) {
      errors[AUDIT_INPUT_ID.leadAuditor] = 'Select lead or enter invite details'
    }
    if (auditType === 'EXTERNAL' && leadId !== '' && externalLeadByInvite) {
      errors[AUDIT_INPUT_ID.leadAuditor] = 'Use dropdown or invite, not both'
    }
    if (auditType === 'EXTERNAL_ASSESSMENT' && !auditorOrganisation.trim()) {
      errors[AUDIT_INPUT_ID.auditorOrganisation] = 'Required for external assessment'
    }
    if (auditeeOrgUnitId === '') {
      errors[AUDIT_INPUT_ID.auditeeOrgUnitId] = 'Auditee division is required'
    }
    if (scopeModules.length === 0) {
      errors[AUDIT_INPUT_ID.scopeModules] = 'Select at least one module'
    }
    if (mode === 'create' && standard === 'MULTIPLE' && checklistLibraryIds.length < 2) {
      errors[AUDIT_INPUT_ID.checklistTemplate] = 'Select at least two checklists'
    } else if (mode === 'create' && standard !== 'MULTIPLE' && checklistLibraryId === '') {
      errors[AUDIT_INPUT_ID.checklistTemplate] = 'Select a checklist'
    }
    return errors
  }, [name, auditType, inviteEmail, inviteName, leadId, auditorOrganisation, auditeeOrgUnitId, scopeModules, mode, checklistLibraryId, checklistLibraryIds, standard])

  /** Returns first error message for display (does NOT update field errors state) */
  const validate = (): string | null => {
    const errors = computeErrors()
    const keys = Object.keys(errors)
    if (keys.length > 0) return errors[keys[0]]
    return null
  }

  /** Validate and update field errors state - call only on submit */
  const validateAndSetErrors = (): string | null => {
    const errors = computeErrors()
    setFieldErrors(errors)
    const keys = Object.keys(errors)
    if (keys.length > 0) return errors[keys[0]]
    return null
  }

  const save = async (): Promise<GrcAuditDetail> => {
    const err = validateAndSetErrors()
    if (err) throw new Error(err)
    setBusy(true)
    try {
      const nodePayload = limitControls ? scopeNodeIds : []
      if (mode === 'edit' && detail) {
        return await api.updateAuditEngagement(detail.audit.id, {
          name: name.trim(),
          leadAuditorUserId: Number(leadId),
          startDate: startDate || null,
          endDate: endDate || null,
          scopeNotes: scopeNotes.trim() || null,
          orgUnitIds,
          teamMemberUserIds: teamIds,
          scopeModules,
          scopeNodeIds: nodePayload,
        })
      }
      const d = await api.createAudit({
        name: name.trim(),
        auditType,
        standard,
        ...(standard === 'MULTIPLE'
          ? { checklistLibraryIds }
          : {
              checklistTemplate,
              ...(checklistLibraryId !== '' ? { checklistLibraryId: Number(checklistLibraryId) } : {}),
            }),
        auditMode: assessmentCapable ? auditMode : 'COMPLIANCE',
        ...(frameworkId !== '' ? { frameworkId: Number(frameworkId) } : {}),
        ...(leadId !== ''
          ? { leadAuditorUserId: Number(leadId) }
          : {
              externalLeadEmail: inviteEmail.trim(),
              externalLeadName: inviteName.trim(),
            }),
        startDate: startDate || null,
        endDate: endDate || null,
        scopeNotes: scopeNotes.trim() || null,
        orgUnitIds: auditeeOrgUnitId === '' ? orgUnitIds : [Number(auditeeOrgUnitId)],
        auditeeOrgUnitId: auditeeOrgUnitId === '' ? undefined : Number(auditeeOrgUnitId),
        auditorOrganisation: auditorOrganisation.trim() || null,
        gapAssessmentId: assessmentCapable && auditMode === 'ASSESSMENT' ? Number(gapId) : undefined,
        teamMemberUserIds: teamIds,
        scopeModules,
        scopeNodeIds: nodePayload,
      })
      if (auditType === 'EXTERNAL' && inviteEmail.trim() && inviteName.trim()) {
        await api.createAuditExternalInvite(d.audit.id, {
          name: inviteName.trim(),
          email: inviteEmail.trim(),
        })
      }
      return d
    } finally {
      setBusy(false)
    }
  }

  const dirty = useMemo(() => {
    if (!active) return false
    if (mode === 'edit' && detail) {
      const savedMembers = (detail.teamMembers ?? [])
        .filter((m) => m.teamRole === 'MEMBER')
        .map((m) => m.userId)
      const savedOu = detail.audit.auditeeOrgUnitId ?? detail.orgUnitIds?.[0] ?? ''
      const sameIds = <T extends string | number>(a: readonly T[], b: readonly T[]) => {
        if (a.length !== b.length) return false
        const byValue = (x: T, y: T) => (typeof x === 'number' && typeof y === 'number' ? x - y : 0)
        const right = [...b].sort(byValue)
        return [...a].sort(byValue).every((id, i) => id === right[i])
      }
      return (
        name !== detail.audit.name ||
        auditType !== detail.audit.auditType ||
        standard !== (detail.audit.auditStandard ?? 'ISO_27001') ||
        auditorOrganisation !== (detail.audit.auditorOrganisation ?? '') ||
        auditeeOrgUnitId !== savedOu ||
        startDate !== (detail.audit.startDate ?? '') ||
        endDate !== (detail.audit.endDate ?? '') ||
        scopeNotes !== (detail.scopeNotes ?? '') ||
        leadId !== detail.audit.leadAuditorUserId ||
        frameworkId !== (detail.audit.frameworkId ?? '') ||
        !sameIds(teamIds, savedMembers) ||
        !sameIds(scopeModules, detail.scopeModules ?? []) ||
        !sameIds(scopeNodeIds, detail.scopeNodeIds ?? [])
      )
    }
    return (
      name.trim() !== '' ||
      startDate !== '' ||
      endDate !== '' ||
      scopeNotes.trim() !== '' ||
      auditorOrganisation.trim() !== '' ||
      frameworkId !== '' ||
      gapId !== '' ||
      auditeeOrgUnitId !== '' ||
      teamIds.length > 0 ||
      checklistLibraryIds.length > 0 ||
      inviteEmail.trim() !== '' ||
      inviteName.trim() !== '' ||
      standard !== 'ISO_27001' ||
      auditType !== 'INTERNAL' ||
      auditMode !== 'COMPLIANCE' ||
      limitControls ||
      scopeNodeIds.length > 0 ||
      (leadId !== '' && leadId !== (defaultLeadUserId ?? ''))
    )
  }, [
    active,
    mode,
    detail,
    name,
    auditType,
    standard,
    auditorOrganisation,
    auditeeOrgUnitId,
    startDate,
    endDate,
    scopeNotes,
    leadId,
    frameworkId,
    teamIds,
    scopeModules,
    scopeNodeIds,
    gapId,
    checklistLibraryIds,
    inviteEmail,
    inviteName,
    auditMode,
    limitControls,
    defaultLeadUserId,
  ])

  return {
    dirty,
    name,
    setName,
    auditType,
    setAuditType,
    standard,
    setStandard,
    checklistTemplate,
    setChecklistTemplate,
    checklistLibraryId,
    setChecklistLibraryId,
    checklistLibraryIds,
    setChecklistLibraryIds,
    toggleChecklistLibrary,
    libraries,
    pickLibraryForStandard,
    auditorOrganisation,
    setAuditorOrganisation,
    auditeeOrgUnitId,
    setAuditee,
    auditMode,
    setAuditMode,
    frameworkId,
    setFrameworkId,
    gapId,
    setGapId,
    setScopeNodeIds,
    startDate,
    setStartDate,
    endDate,
    setEndDate,
    scopeNotes,
    setScopeNotes,
    orgUnitIds,
    scopeModules,
    limitControls,
    setLimitControls,
    scopeNodeIds,
    frameworkNodes,
    leadId,
    setLeadId,
    teamIds,
    inviteEmail,
    setInviteEmail,
    inviteName,
    setInviteName,
    busy,
    assessmentCapable,
    linkableGaps,
    leadCandidates,
    teamCandidates,
    toggleOrgUnit,
    toggleScopeModule,
    toggleTeam,
    toggleControl,
    save,
    validate,
    fieldErrors,
    clearFieldError,
  }
}
