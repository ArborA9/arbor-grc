import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Box,
  Button,
  Card,
  Checkbox,
  FormControlLabel,
  IconButton,
  Popover,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import {
  api,
  type DpiaDepartment,
  type DpiaDepartmentStatus,
  type DpiaProject,
  type DpiaQuestionnaire,
  type EvidenceSummary,
  type OrgUnitNode,
  type RopaProject,
  type Session,
  type UserSummary,
  type WorkTaskSummary,
} from './api'
import StatusChip from './components/StatusChip'
import BiaJourneyPanel, { type BiaJourneyStep } from './components/bcms/BiaJourneyPanel'
import EvidenceAttachedList from './components/evidence/EvidenceAttachedList'
import {
  CompactTextField,
  ConfirmActionDialog,
  FieldActionRow,
  FormSelect,
  KpiCard,
  PageLoading,
  RegisterToolbar,
  SettingsCard,
  SimpleFormLayout,
  controlActionSx,
} from './components/ui'
import { sectionLabelSx } from './components/ui/FormFieldLabel'
import { NARRATIVE_FIELD_MAX_WIDTH, formFieldGridSx, twoColFieldSx } from './components/ui/formLayout'
import { useAppShellPage } from './components/AppShellPageContext'
import { useListReady } from './uiQuality/useListReady'
import { useOptionalUnsavedChangesGuardApi, useRegisterUnsavedChanges } from './uiQuality/UnsavedChangesGuard'
import { brand, semantic } from './theme'

type ComplianceRating = 'COMPLIANT' | 'PARTIALLY_COMPLIANT' | 'NON_COMPLIANT' | ''

const COMPLIANCE_OPTIONS: { value: ComplianceRating; label: string }[] = [
  { value: '', label: '-' },
  { value: 'COMPLIANT', label: 'Compliant' },
  { value: 'PARTIALLY_COMPLIANT', label: 'Partially Compliant' },
  { value: 'NON_COMPLIANT', label: 'Non-Compliant' },
]

const YEARS = Array.from({ length: 10 }, (_, i) => new Date().getFullYear() - 2 + i)

function flattenOrg(nodes: OrgUnitNode[]): OrgUnitNode[] {
  const out: OrgUnitNode[] = []
  const walk = (list: OrgUnitNode[]) => {
    for (const n of list) {
      out.push(n)
      if (n.children?.length) walk(n.children)
    }
  }
  walk(nodes)
  return out
}

function statusLabel(s: string): string {
  return s.replace(/_/g, ' ')
}

function deptTone(s: DpiaDepartmentStatus): 'success' | 'info' | 'warning' | 'neutral' | 'danger' {
  if (s === 'CLOSED' || s === 'APPROVED') return 'success'
  if (s === 'DPO_ASSESSMENT' || s === 'UNDER_REVIEW' || s === 'PENDING_HEAD_SIGN_OFF') return 'info'
  if (s === 'IN_PROGRESS') return 'warning'
  return 'neutral'
}

function projectTone(s: string): 'success' | 'info' | 'neutral' {
  if (s === 'COMPLETED') return 'success'
  if (s === 'IN_PROGRESS') return 'info'
  return 'neutral'
}

function buildWorkflowSteps(dept: DpiaDepartment): BiaJourneyStep[] {
  const steps: BiaJourneyStep[] = [
    { id: 'champion', label: 'Champion', state: 'todo' },
    { id: 'dpo-review', label: 'DPO Review', state: 'todo' },
    { id: 'dpo-assess', label: 'DPO Assessment', state: 'todo' },
    { id: 'head', label: 'Head', state: 'todo' },
    { id: 'close', label: 'Close', state: 'todo' },
  ]
  const s = dept.status
  // Status flow: NOT_STARTED -> IN_PROGRESS -> UNDER_REVIEW -> DPO_ASSESSMENT -> PENDING_HEAD_SIGN_OFF -> APPROVED -> CLOSED
  if (s === 'NOT_STARTED') {
    steps[0].state = 'active'
  } else if (s === 'IN_PROGRESS') {
    steps[0].state = 'active'
    steps[0].person = dept.championName ?? undefined
    if (dept.returnFrom) {
      steps[0].hint = 'Returned'
    }
  } else if (s === 'UNDER_REVIEW') {
    steps[0].state = 'done'
    steps[1].state = 'active'
  } else if (s === 'DPO_ASSESSMENT') {
    steps[0].state = 'done'
    steps[1].state = 'done'
    steps[2].state = 'active'
  } else if (s === 'PENDING_HEAD_SIGN_OFF') {
    steps[0].state = 'done'
    steps[1].state = 'done'
    steps[2].state = 'done'
    steps[3].state = 'active'
    steps[3].person = dept.departmentHeadName ?? undefined
  } else if (s === 'APPROVED') {
    steps[0].state = 'done'
    steps[1].state = 'done'
    steps[2].state = 'done'
    steps[3].state = 'done'
    steps[4].state = 'active'
  } else if (s === 'CLOSED') {
    steps.forEach((st) => (st.state = 'done'))
  }
  return steps
}

export default function PrivacyDpiaJourneyPanel({
  session,
  onError,
  onOk,
  openDepartmentId,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
  openDepartmentId?: number | null
}) {
  const isAdmin = session.roles.includes('OWNER') || session.roles.includes('ADMIN')
  const guardApi = useOptionalUnsavedChangesGuardApi()

  const [projects, setProjects] = useState<DpiaProject[]>([])
  const [ropaProjects, setRopaProjects] = useState<RopaProject[]>([])
  const [orgUnits, setOrgUnits] = useState<OrgUnitNode[]>([])
  const [users, setUsers] = useState<UserSummary[]>([])
  const [questionnaires, setQuestionnaires] = useState<DpiaQuestionnaire[]>([])
  const [myChampionDeptIds, setMyChampionDeptIds] = useState<Set<number>>(new Set())
  const [myHeadDeptIds, setMyHeadDeptIds] = useState<Set<number>>(new Set())

  const { listReady, markReady } = useListReady()
  const [search, setSearch] = useState('')
  const [projectStatusFilter, setProjectStatusFilter] = useState('ALL')

  // Navigation state
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null)
  const [selectedDeptId, setSelectedDeptId] = useState<number | null>(null)
  const [creatingProject, setCreatingProject] = useState(false)
  const [addingDepartment, setAddingDepartment] = useState(false)
  const [departmentDetail, setDepartmentDetail] = useState<DpiaDepartment | null>(null)

  // Project form
  const [projectForm, setProjectForm] = useState({ name: '', year: String(new Date().getFullYear()), linkRopa: 'NO', ropaProjectId: '' })

  // Department form
  const [deptForm, setDeptForm] = useState({
    orgUnitId: '',
    reasonDescription: '',
    championUserId: '',
    headUserId: '',
    dueDate: '',
    questionnaireId: '',
  })

  // Assign form (on department detail)
  const [assignForm, setAssignForm] = useState({ championUserId: '', headUserId: '', dueDate: '' })

  // Answers (on department detail)
  const [answers, setAnswers] = useState<
    Record<number, { answerText: string; answerOptions: string[]; detailsEvidence: string }>
  >({})
  const [questionAttachments, setQuestionAttachments] = useState<Record<number, EvidenceSummary[]>>({})
  const [guidanceAnchor, setGuidanceAnchor] = useState<{ id: number; el: HTMLElement } | null>(null)
  const [dirty, setDirty] = useState(false)

  // Assessment rows
  const [assessmentEdits, setAssessmentEdits] = useState<
    Record<number, { complianceStatus: ComplianceRating; remarks: string; recommendedMeasures: string; riskTitle: string }>
  >({})

  // DPO review
  const [reviewComments, setReviewComments] = useState('')
  // Head notes
  const [headNotes, setHeadNotes] = useState('')
  const [showEarlierSteps, setShowEarlierSteps] = useState(false)
  const [showAllGuidance, setShowAllGuidance] = useState(false)

  // Confirmations
  const [confirmSubmit, setConfirmSubmit] = useState(false)
  const [confirmComplete, setConfirmComplete] = useState(false)
  const [confirmClose, setConfirmClose] = useState(false)

  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    try {
      const [projs, org, qs, ropa, tasks] = await Promise.all([
        api.listDpiaProjects(),
        api.listOrgUnits().catch(() => [] as OrgUnitNode[]),
        api.listDpiaQuestionnaires().catch(() => [] as DpiaQuestionnaire[]),
        api.listRopaProjects().catch(() => [] as RopaProject[]),
        api.listMyTasks().catch(() => [] as WorkTaskSummary[]),
      ])
      setProjects(projs)
      setOrgUnits(flattenOrg(org))
      setQuestionnaires(qs.filter((q) => q.active))
      setRopaProjects(ropa)
      // Identify tasks for this user
      const mine = tasks.filter((t) => t.sourceModule === 'PRIVACY')
      setMyChampionDeptIds(
        new Set(mine.filter((t) => t.sourceRecordType === 'DPIA_DEPARTMENT').map((t) => t.sourceRecordId)),
      )
      setMyHeadDeptIds(
        new Set(mine.filter((t) => t.sourceRecordType === 'DPIA_HEAD').map((t) => t.sourceRecordId)),
      )
      if (isAdmin) {
        const us = await api.listUsers().catch(() => [] as UserSummary[])
        setUsers(us)
      }
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Failed to load DPIA projects')
    } finally {
      markReady()
    }
  }, [isAdmin, onError, markReady])

  const loadDepartmentDetail = useCallback(
    async (id: number) => {
      try {
        const d = await api.getDpiaDepartment(id)
        setDepartmentDetail(d)
        setAssignForm({
          championUserId: d.championUserId ? String(d.championUserId) : '',
          headUserId: d.departmentHeadUserId ? String(d.departmentHeadUserId) : '',
          dueDate: d.dueDate ?? '',
        })
        // Build answers map
        const ansMap: Record<number, { answerText: string; answerOptions: string[]; detailsEvidence: string }> = {}
        if (d.questions) {
          for (const q of d.questions) {
            ansMap[q.id] = {
              answerText: q.answerText ?? '',
              answerOptions: q.answerOptions ?? [],
              detailsEvidence: q.detailsEvidence ?? '',
            }
          }
        }
        setAnswers(ansMap)
        // Build assessment edits
        const assMap: Record<number, { complianceStatus: ComplianceRating; remarks: string; recommendedMeasures: string; riskTitle: string }> = {}
        if (d.assessmentRows) {
          for (const r of d.assessmentRows) {
            assMap[r.id] = {
              complianceStatus: (r.complianceStatus as ComplianceRating) ?? '',
              remarks: r.remarks ?? '',
              recommendedMeasures: r.recommendedMeasures ?? '',
              riskTitle: r.riskTitle ?? '',
            }
          }
        }
        setAssessmentEdits(assMap)
        setReviewComments('')
        setHeadNotes('')
        setDirty(false)
        setShowEarlierSteps(false)
        setShowAllGuidance(false)
        const attachQs = (d.questions ?? []).filter((q) => q.responseType === 'FREE_TEXT_ATTACHMENT')
        if (attachQs.length > 0) {
          const attachMap: Record<number, EvidenceSummary[]> = {}
          await Promise.all(
            attachQs.map(async (q) => {
              try {
                attachMap[q.id] = await api.listEvidenceLinked('PRIVACY', 'DPIA_INSTANCE_QUESTION', String(q.id))
              } catch {
                attachMap[q.id] = []
              }
            }),
          )
          setQuestionAttachments(attachMap)
        } else {
          setQuestionAttachments({})
        }
      } catch (e) {
        onError(e instanceof Error ? e.message : 'Failed to load department')
        setSelectedDeptId(null)
      }
    },
    [onError],
  )

  const patchAnswer = (
    questionId: number,
    patch: Partial<{ answerText: string; answerOptions: string[]; detailsEvidence: string }>,
  ) => {
    setAnswers((prev) => {
      const base = prev[questionId] ?? { answerText: '', answerOptions: [] as string[], detailsEvidence: '' }
      return { ...prev, [questionId]: { ...base, ...patch } }
    })
    setDirty(true)
  }

  const refreshQuestionAttachments = async (questionId: number) => {
    const list = await api.listEvidenceLinked('PRIVACY', 'DPIA_INSTANCE_QUESTION', String(questionId))
    setQuestionAttachments((prev) => ({ ...prev, [questionId]: list }))
  }

  const handleUploadQuestionAttachment = async (questionId: number, file: File | null) => {
    if (!file || !canFillChampion) return
    setBusy(true)
    try {
      await api.uploadEvidence({
        file,
        title: file.name,
        classification: 'CONFIDENTIAL',
        moduleCode: 'PRIVACY',
        recordType: 'DPIA_INSTANCE_QUESTION',
        recordId: String(questionId),
        workflowStage: 'CHAMPION_ATTACHMENT',
      })
      await refreshQuestionAttachments(questionId)
      onOk('Attachment uploaded')
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Upload failed')
    } finally {
      setBusy(false)
    }
  }

  const handleRemoveQuestionAttachment = async (questionId: number, item: EvidenceSummary) => {
    if (!canFillChampion) return
    setBusy(true)
    try {
      await api.unlinkEvidenceFromRecord(item.id, {
        moduleCode: 'PRIVACY',
        recordType: 'DPIA_INSTANCE_QUESTION',
        recordId: String(questionId),
      })
      await refreshQuestionAttachments(questionId)
      onOk('Attachment removed')
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Remove failed')
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => {
    void load()
  }, [load])

  // Open department if specified
  useEffect(() => {
    if (openDepartmentId != null && listReady) {
      // Find the project containing this department
      for (const p of projects) {
        const d = p.departments.find((dept) => dept.id === openDepartmentId)
        if (d) {
          setSelectedProjectId(p.id)
          setSelectedDeptId(d.id)
          break
        }
      }
    }
  }, [openDepartmentId, listReady, projects])

  useEffect(() => {
    if (selectedDeptId != null) void loadDepartmentDetail(selectedDeptId)
    else setDepartmentDetail(null)
  }, [selectedDeptId, loadDepartmentDetail])

  const project = useMemo(() => projects.find((p) => p.id === selectedProjectId) ?? null, [projects, selectedProjectId])

  const myDeptIds = useMemo(() => new Set([...myChampionDeptIds, ...myHeadDeptIds]), [myChampionDeptIds, myHeadDeptIds])

  const visibleDepartments = useMemo(() => {
    if (!project) return []
    if (isAdmin) return project.departments
    return project.departments.filter((d) => myDeptIds.has(d.id))
  }, [project, isAdmin, myDeptIds])

  const visibleProjects = useMemo(() => {
    if (isAdmin) return projects
    return projects.filter((p) => p.departments.some((d) => myDeptIds.has(d.id)))
  }, [projects, isAdmin, myDeptIds])

  const filteredProjects = useMemo(() => {
    let list = visibleProjects
    if (projectStatusFilter !== 'ALL') {
      list = list.filter((p) => p.status === projectStatusFilter)
    }
    const q = search.trim().toLowerCase()
    if (q) list = list.filter((p) => p.name.toLowerCase().includes(q))
    return list
  }, [visibleProjects, projectStatusFilter, search])

  const projectQueueKpis = useMemo(() => {
    const depts = visibleProjects.flatMap((p) => p.departments ?? [])
    return {
      awaitingChampion: depts.filter((d) => d.status === 'NOT_STARTED' || d.status === 'IN_PROGRESS').length,
      needingDpo: depts.filter((d) => d.status === 'UNDER_REVIEW' || d.status === 'DPO_ASSESSMENT').length,
      needingHead: depts.filter((d) => d.status === 'PENDING_HEAD_SIGN_OFF').length,
      readyToClose: depts.filter((d) => d.status === 'APPROVED').length,
    }
  }, [visibleProjects])

  const championUsers = users.filter((u) => u.roles.includes('CONTRIBUTOR'))

  // Role checks
  const canFillChampion =
    departmentDetail != null &&
    departmentDetail.status === 'IN_PROGRESS' &&
    myChampionDeptIds.has(departmentDetail.id)
  const isHeadViewer = departmentDetail != null && myHeadDeptIds.has(departmentDetail.id)

  // Breadcrumb
  const breadcrumbTrail = useMemo(() => {
    if (creatingProject) return 'DPIA Projects / New Project'
    if (project) {
      if (addingDepartment) return `${project.name} / Add Department`
      if (departmentDetail) return `${project.name} / ${departmentDetail.orgUnitName ?? 'Department'}`
      return `DPIA Projects / ${project.name}`
    }
    return undefined
  }, [creatingProject, project, addingDepartment, departmentDetail])

  const clearDeptDrafts = () => {
    setDirty(false)
    setReviewComments('')
    setHeadNotes('')
  }

  const goToProjectList = () => {
    clearDeptDrafts()
    setSelectedProjectId(null)
    setCreatingProject(false)
    setDepartmentDetail(null)
    setSelectedDeptId(null)
    setAddingDepartment(false)
    setProjectForm({ name: '', year: String(new Date().getFullYear()), linkRopa: 'NO', ropaProjectId: '' })
    setDeptForm({
      orgUnitId: '',
      reasonDescription: '',
      championUserId: '',
      headUserId: '',
      dueDate: '',
      questionnaireId: '',
    })
  }

  const goToProjectHome = () => {
    clearDeptDrafts()
    setDepartmentDetail(null)
    setSelectedDeptId(null)
    setAddingDepartment(false)
    setDeptForm({
      orgUnitId: '',
      reasonDescription: '',
      championUserId: '',
      headUserId: '',
      dueDate: '',
      questionnaireId: '',
    })
  }

  const pageTitle = departmentDetail
    ? (departmentDetail.orgUnitName ?? 'Department')
    : addingDepartment
      ? 'Add Department'
      : creatingProject
        ? 'New DPIA Project'
        : project
          ? project.name
          : 'DPIA Projects'

  const breadcrumbRoot =
    departmentDetail || addingDepartment
      ? {
          label: project?.name ?? 'Project',
          onClick: () => guardApi.requestNavigation(goToProjectHome),
        }
      : project || creatingProject
        ? {
            label: 'DPIA Projects',
            onClick: () => guardApi.requestNavigation(goToProjectList),
          }
        : undefined

  useAppShellPage(
    {
      title: pageTitle,
      breadcrumbTrail,
      breadcrumbRoot,
      onTitleClick: breadcrumbRoot?.onClick,
      actions:
        project && !departmentDetail && !addingDepartment && !creatingProject ? (
          <StatusChip label={statusLabel(project.status)} tone={projectTone(project.status)} />
        ) : departmentDetail ? (
          <StatusChip
            label={statusLabel(departmentDetail.status)}
            tone={deptTone(departmentDetail.status)}
          />
        ) : undefined,
    },
    [pageTitle, breadcrumbTrail, breadcrumbRoot, project, departmentDetail, addingDepartment, creatingProject],
  )

  const act = async (fn: () => Promise<unknown>, successMsg: string) => {
    setBusy(true)
    try {
      await fn()
      onOk(successMsg)
      await load()
      if (selectedDeptId) await loadDepartmentDetail(selectedDeptId)
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Action failed')
    } finally {
      setBusy(false)
    }
  }

  const completedRopaProjects = useMemo(
    () => ropaProjects.filter((r) => r.status === 'COMPLETED'),
    [ropaProjects],
  )

  // ---------- Create Project ----------
  const projectCreateDisabledReason = !projectForm.name.trim()
    ? 'Name is required'
    : projectForm.linkRopa === 'YES' && !projectForm.ropaProjectId
      ? 'Select a ROPA project to link'
      : null

  const handleCreateProject = async () => {
    if (projectCreateDisabledReason) return
    setBusy(true)
    try {
      const created = await api.createDpiaProject({
        name: projectForm.name.trim(),
        year: projectForm.year ? Number(projectForm.year) : null,
        linkedRopaProjectId:
          projectForm.linkRopa === 'YES' && projectForm.ropaProjectId ? Number(projectForm.ropaProjectId) : null,
      })
      const n = created.departments?.length ?? 0
      setCreatingProject(false)
      setProjectForm({ name: '', year: String(new Date().getFullYear()), linkRopa: 'NO', ropaProjectId: '' })
      setSelectedProjectId(created.id)
      onOk(n > 0 ? `Project created with ${n} department(s) from ROPA` : 'Project created')
      await load()
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Action failed')
    } finally {
      setBusy(false)
    }
  }

  // ---------- Add Department ----------
  const deptCreateDisabledReason = !deptForm.orgUnitId
    ? 'Organisation unit is required'
    : !project?.linkedRopaProjectId && !deptForm.reasonDescription.trim()
      ? 'Reason/description is required when not linked to ROPA'
      : null

  const handleAddDepartment = async () => {
    if (!project || deptCreateDisabledReason) return
    await act(async () => {
      const created = await api.addDpiaDepartment(project.id, {
        orgUnitId: Number(deptForm.orgUnitId),
        reasonDescription: deptForm.reasonDescription || null,
        departmentHeadUserId: deptForm.headUserId ? Number(deptForm.headUserId) : null,
        championUserId: deptForm.championUserId ? Number(deptForm.championUserId) : null,
        dueDate: deptForm.dueDate || null,
        questionnaireId: deptForm.questionnaireId ? Number(deptForm.questionnaireId) : null,
      })
      setAddingDepartment(false)
      setDeptForm({ orgUnitId: '', reasonDescription: '', championUserId: '', headUserId: '', dueDate: '', questionnaireId: '' })
      setSelectedDeptId(created.id)
    }, 'Department added')
  }

  // ---------- Assign ----------
  const assignFormDirty =
    departmentDetail != null &&
    (assignForm.championUserId !== (departmentDetail.championUserId ? String(departmentDetail.championUserId) : '') ||
      assignForm.headUserId !==
        (departmentDetail.departmentHeadUserId ? String(departmentDetail.departmentHeadUserId) : '') ||
      assignForm.dueDate !== (departmentDetail.dueDate ?? ''))
  const assignSaveDisabledReason = !assignForm.championUserId
    ? 'Champion is required'
    : !assignFormDirty
      ? 'No changes to save'
      : null

  const handleAssign = async () => {
    if (!departmentDetail || assignSaveDisabledReason) return
    await act(
      () =>
        api.assignDpiaDepartment(departmentDetail.id, {
          championUserId: assignForm.championUserId ? Number(assignForm.championUserId) : null,
          departmentHeadUserId: assignForm.headUserId ? Number(assignForm.headUserId) : null,
          dueDate: assignForm.dueDate || null,
        }),
      'Assignment updated',
    )
  }

  // ---------- Save Answers ----------
  const persistAnswers = async () => {
    if (!departmentDetail) return
    const answerRows = Object.entries(answers).map(([qId, v]) => ({
      questionId: Number(qId),
      answerText: v.answerText || null,
      answerOptions: v.answerOptions.length > 0 ? v.answerOptions : null,
      detailsEvidence: v.detailsEvidence || null,
    }))
    await api.saveDpiaAnswers(departmentDetail.id, { answers: answerRows })
    setDirty(false)
  }

  const handleSaveAnswers = async () => {
    if (!departmentDetail) return
    await act(async () => {
      await persistAnswers()
    }, 'Answers saved')
  }

  // ---------- Submit ----------
  const handleSubmit = async () => {
    if (!departmentDetail) return
    if (dirty) {
      try {
        await persistAnswers()
      } catch (e) {
        onError(e instanceof Error ? e.message : 'Save answers before submit failed')
        return
      }
    }
    setConfirmSubmit(false)
    await act(() => api.submitDpiaDepartment(departmentDetail.id), 'Department submitted for review')
  }

  // ---------- DPO Review ----------
  const handleDpoAccept = async () => {
    if (!departmentDetail) return
    await act(() => api.reviewDpiaDepartment(departmentDetail.id, { outcome: 'ACCEPT', comments: reviewComments || null }), 'Accepted')
  }

  const handleDpoReturn = async () => {
    if (!departmentDetail) return
    await act(() => api.reviewDpiaDepartment(departmentDetail.id, { outcome: 'RETURN', comments: reviewComments || null }), 'Returned to champion')
  }

  // ---------- Save Assessment ----------
  const assessmentAllowsRiskTitle = (status: string | null | undefined) =>
    status === 'NON_COMPLIANT' || status === 'PARTIALLY_COMPLIANT'

  const questionAnswered = (
    q: { id: number; responseType: string; required?: boolean },
    ans?: { answerText: string; answerOptions: string[]; detailsEvidence: string },
  ) => {
    if (!ans) return false
    if (q.responseType === 'MULTI_SELECT') return (ans.answerOptions?.length ?? 0) > 0
    return Boolean((ans.answerText ?? '').trim())
  }

  const persistAssessment = async () => {
    if (!departmentDetail) return
    const assessRows = Object.entries(assessmentEdits).map(([rowId, v]) => ({
      id: Number(rowId),
      complianceStatus: v.complianceStatus || null,
      remarks: v.remarks || null,
      recommendedMeasures: v.recommendedMeasures || null,
      riskTitle: assessmentAllowsRiskTitle(v.complianceStatus) ? v.riskTitle || null : null,
    }))
    await api.saveDpiaAssessment(departmentDetail.id, { rows: assessRows })
  }

  const handleSaveAssessment = async () => {
    if (!departmentDetail) return
    await act(async () => {
      await persistAssessment()
    }, 'Assessment saved')
  }

  const finalizeAssessmentDisabledReason = (() => {
    if (!departmentDetail || departmentDetail.status !== 'DPO_ASSESSMENT') return null
    const rows = departmentDetail.assessmentRows ?? []
    for (const r of rows) {
      const edit = assessmentEdits[r.id]
      const status = (edit?.complianceStatus || r.complianceStatus || '') as string
      if (!status) return 'Compliance status is required for every principle'
      if (status === 'NON_COMPLIANT') {
        const title = (edit?.riskTitle ?? r.riskTitle ?? '').trim()
        if (!title) {
          return `Risk Title is required for Non Compliant (${r.principle || 'principle'})`
        }
      }
    }
    return null
  })()

  // ---------- Finalize Assessment ----------
  const handleFinalizeAssessment = async () => {
    if (!departmentDetail || finalizeAssessmentDisabledReason) return
    await act(async () => {
      if (assessmentDirty) await persistAssessment()
      await api.finalizeDpiaAssessment(departmentDetail.id)
    }, 'Assessment finalized')
  }

  const ensurePrivacyRiskProject = async (): Promise<number> => {
    const existing = await api.listRiskProjects('PRIVACY_RISK')
    if (existing.length > 0) return existing[0].id
    const year = new Date().getFullYear()
    const created = await api.createRiskProject({
      projectType: 'PRIVACY_RISK',
      name: `Privacy Risk Register ${year}`,
      year,
    })
    return created.id
  }

  const handleCreateRiskFromAssessment = async (rowId: number) => {
    if (!departmentDetail || !isAdmin) return
    const edit = assessmentEdits[rowId]
    const row = departmentDetail.assessmentRows?.find((r) => r.id === rowId)
    const compliance = (edit?.complianceStatus || row?.complianceStatus || '') as string
    if (!assessmentAllowsRiskTitle(compliance)) {
      onError('Risk Title applies only when Compliance is Non Compliant or Partially Compliant')
      return
    }
    const title = (edit?.riskTitle ?? row?.riskTitle ?? '').trim()
    if (!title) {
      onError(
        compliance === 'NON_COMPLIANT'
          ? 'Risk Title is required for Non Compliant before creating a Privacy Risk'
          : 'Enter a Risk Title before creating a Privacy Risk',
      )
      return
    }
    setBusy(true)
    try {
      if (departmentDetail.status === 'DPO_ASSESSMENT') {
        await persistAssessment()
      }
      const riskProjectId = await ensurePrivacyRiskProject()
      const principle = row?.principle ?? ''
      const remarks = edit?.remarks ?? row?.remarks ?? ''
      const measures = edit?.recommendedMeasures ?? row?.recommendedMeasures ?? ''
      const created = await api.createRisk({
        riskType: 'PRIVACY_RISK',
        title,
        description: [
          principle ? `DPIA principle: ${principle}` : null,
          remarks ? `Remarks: ${remarks}` : null,
          measures ? `Recommended measures: ${measures}` : null,
        ]
          .filter(Boolean)
          .join('\n') || title,
        category: 'IT',
        orgUnitId: departmentDetail.orgUnitId,
        observedGap: remarks || `Raised from DPIA assessment (${departmentDetail.orgUnitName ?? 'department'})`,
        threats: 'Privacy breach',
        probability: 'MEDIUM',
        impact: 'MEDIUM',
        controlEffectiveness: 'MEDIUM',
        treatmentOption: 'TREAT',
        treatmentJustification: measures || 'Review and treat the privacy risk from the DPIA',
        riskProjectId,
        sourceDpiaAssessmentRowId: rowId,
        dateIdentified: new Date().toISOString().slice(0, 10),
      })
      onOk(`Privacy Risk created: ${created.displayId}`)
      await load()
      if (selectedDeptId) await loadDepartmentDetail(selectedDeptId)
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Failed to create Privacy Risk')
    } finally {
      setBusy(false)
    }
  }

  const handleCreateAllRisksFromAssessment = async () => {
    if (!departmentDetail || !isAdmin) return
    const pending = (departmentDetail.assessmentRows ?? []).filter((r) => {
      const edit = assessmentEdits[r.id]
      const compliance = (edit?.complianceStatus || r.complianceStatus || '') as string
      const title = (edit?.riskTitle ?? r.riskTitle ?? '').trim()
      return assessmentAllowsRiskTitle(compliance) && Boolean(title) && !r.raisedRiskDisplayId
    })
    if (pending.length === 0) {
      onError('No Risk Titles ready to create Privacy Risks')
      return
    }
    setBusy(true)
    try {
      if (departmentDetail.status === 'DPO_ASSESSMENT') await persistAssessment()
      const riskProjectId = await ensurePrivacyRiskProject()
      let n = 0
      for (const r of pending) {
        const edit = assessmentEdits[r.id]
        const title = (edit?.riskTitle ?? r.riskTitle ?? '').trim()
        const remarks = edit?.remarks ?? r.remarks ?? ''
        const measures = edit?.recommendedMeasures ?? r.recommendedMeasures ?? ''
        await api.createRisk({
          riskType: 'PRIVACY_RISK',
          title,
          description: [
            r.principle ? `DPIA principle: ${r.principle}` : null,
            remarks ? `Remarks: ${remarks}` : null,
            measures ? `Recommended measures: ${measures}` : null,
          ]
            .filter(Boolean)
            .join('\n') || title,
          category: 'IT',
          orgUnitId: departmentDetail.orgUnitId,
          observedGap: remarks || `Raised from DPIA assessment (${departmentDetail.orgUnitName ?? 'department'})`,
          threats: 'Privacy breach',
          probability: 'MEDIUM',
          impact: 'MEDIUM',
          controlEffectiveness: 'MEDIUM',
          treatmentOption: 'TREAT',
          treatmentJustification: measures || 'Review and treat the privacy risk from the DPIA',
          riskProjectId,
          sourceDpiaAssessmentRowId: r.id,
          dateIdentified: new Date().toISOString().slice(0, 10),
        })
        n += 1
      }
      onOk(`Created ${n} Privacy Risk(s)`)
      await load()
      if (selectedDeptId) await loadDepartmentDetail(selectedDeptId)
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Failed to create Privacy Risks')
    } finally {
      setBusy(false)
    }
  }

  // ---------- Head Approve/Return ----------
  const handleHeadApprove = async () => {
    if (!departmentDetail) return
    await act(() => api.headApproveDpia(departmentDetail.id, headNotes || undefined), 'Approved by head')
  }

  const handleHeadReturn = async () => {
    if (!departmentDetail) return
    await act(() => api.headReturnDpia(departmentDetail.id, headNotes || undefined), 'Returned by head')
  }

  // ---------- Close ----------
  const handleClose = async () => {
    if (!departmentDetail) return
    setConfirmClose(false)
    await act(() => api.closeDpiaDepartment(departmentDetail.id), 'Department closed')
  }

  // ---------- Complete Project ----------
  const canComplete = project && project.departments.every((d) => d.status === 'CLOSED')

  const handleCompleteProject = async () => {
    if (!project) return
    setConfirmComplete(false)
    await act(() => api.completeDpiaProject(project.id), 'Project completed')
  }

  /* Whole-DPIA unsaved-changes guard (ROPA pattern): answers, assessment, assign,
   * review/head notes, and create/add forms. Save Draft only where a real persist exists. */
  const answersDirty = dirty
  const assessmentDirty =
    departmentDetail?.status === 'DPO_ASSESSMENT' &&
    (departmentDetail.assessmentRows ?? []).some((r) => {
      const e = assessmentEdits[r.id]
      if (!e) return false
      return (
        e.complianceStatus !== ((r.complianceStatus as ComplianceRating) ?? '') ||
        e.remarks !== (r.remarks ?? '') ||
        e.recommendedMeasures !== (r.recommendedMeasures ?? '') ||
        e.riskTitle !== (r.riskTitle ?? '')
      )
    })
  const assignDirty =
    isAdmin &&
    departmentDetail != null &&
    !addingDepartment &&
    (departmentDetail.status === 'NOT_STARTED' ||
      departmentDetail.status === 'IN_PROGRESS' ||
      departmentDetail.status === 'UNDER_REVIEW' ||
      departmentDetail.status === 'DPO_ASSESSMENT') &&
    assignFormDirty
  const reviewDirty =
    isAdmin && departmentDetail?.status === 'UNDER_REVIEW' && reviewComments.trim() !== ''
  const headNotesDirty =
    departmentDetail?.status === 'PENDING_HEAD_SIGN_OFF' &&
    isHeadViewer &&
    headNotes.trim() !== ''
  const projectFormDirty =
    creatingProject &&
    (projectForm.name.trim() !== '' ||
      projectForm.year !== String(new Date().getFullYear()) ||
      projectForm.linkRopa !== 'NO' ||
      projectForm.ropaProjectId !== '')
  const deptFormDirty =
    addingDepartment &&
    (deptForm.orgUnitId !== '' ||
      deptForm.reasonDescription.trim() !== '' ||
      deptForm.championUserId !== '' ||
      deptForm.headUserId !== '' ||
      deptForm.dueDate !== '' ||
      deptForm.questionnaireId !== '')
  const dpiaDirty =
    answersDirty ||
    assessmentDirty ||
    assignDirty ||
    reviewDirty ||
    headNotesDirty ||
    projectFormDirty ||
    deptFormDirty
  const saveOnLeave =
    answersDirty || assessmentDirty
      ? async () => {
          if (answersDirty) await persistAnswers()
          if (assessmentDirty) await persistAssessment()
          if (selectedDeptId) await loadDepartmentDetail(selectedDeptId)
          onOk(answersDirty && assessmentDirty ? 'Answers and assessment saved' : answersDirty ? 'Answers saved' : 'Assessment saved')
        }
      : assignDirty
        ? async () => {
            await handleAssign()
          }
        : undefined
  useRegisterUnsavedChanges(
    dpiaDirty
      ? {
          dirty: true,
          title: 'Unsaved DPIA changes',
          message: saveOnLeave
            ? 'You have unsaved DPIA changes. Save to keep your work, or discard to leave without saving.'
            : 'You have unsaved DPIA changes. Keep editing to stay on this screen, or discard to leave without saving.',
          onSaveDraft: saveOnLeave,
          saveDraftLabel: answersDirty ? 'Save Draft' : 'Save',
        }
      : null,
  )

  // ========== RENDER ==========
  if (!listReady) return <PageLoading />

  // ---------- Create Project Screen ----------
  if (creatingProject) {
    return (
      <SimpleFormLayout
        actions={
          <Stack spacing={0.5} alignItems="flex-end">
            <Stack direction="row" spacing={1}>
              <Button
                onClick={() =>
                  guardApi.requestNavigation(() => {
                    setCreatingProject(false)
                    setProjectForm({
                      name: '',
                      year: String(new Date().getFullYear()),
                      linkRopa: 'NO',
                      ropaProjectId: '',
                    })
                  })
                }
              >
                Cancel
              </Button>
              <Button
                variant="contained"
                disabled={busy || Boolean(projectCreateDisabledReason)}
                onClick={() => void handleCreateProject()}
              >
                Create
              </Button>
            </Stack>
            {projectCreateDisabledReason && (
              <Typography variant="caption" color="text.secondary">
                {projectCreateDisabledReason}
              </Typography>
            )}
          </Stack>
        }
      >
        <SettingsCard autoHeight title="New DPIA Project">
          <Box sx={formFieldGridSx}>
            <CompactTextField
              label="Project Name"
              required
              value={projectForm.name}
              onChange={(e) => setProjectForm({ ...projectForm, name: e.target.value })}
              fieldIdPrefix="dp"
            />
            <FormSelect
              label="Year"
              value={projectForm.year}
              options={YEARS.map((y) => ({ value: String(y), label: String(y) }))}
              onChange={(v) => setProjectForm({ ...projectForm, year: v })}
              fullWidth
            />
            <FormSelect
              label="Link to ROPA?"
              value={projectForm.linkRopa}
              options={[
                { value: 'NO', label: 'No' },
                { value: 'YES', label: 'Yes' },
              ]}
              onChange={(v) => setProjectForm({ ...projectForm, linkRopa: v, ropaProjectId: '' })}
              fullWidth
            />
            {projectForm.linkRopa === 'YES' && (
              <FormSelect
                label="ROPA Project"
                required
                value={projectForm.ropaProjectId}
                options={[
                  { value: '', label: completedRopaProjects.length ? 'Select completed ROPA project' : 'No completed ROPA projects' },
                  ...completedRopaProjects.map((r) => ({ value: String(r.id), label: r.name })),
                ]}
                onChange={(v) => setProjectForm({ ...projectForm, ropaProjectId: v })}
                fullWidth
              />
            )}
          </Box>
        </SettingsCard>
      </SimpleFormLayout>
    )
  }

  // ---------- Add Department Screen ----------
  if (project && addingDepartment) {
    const takenOrgIds = new Set(project.departments.map((d) => d.orgUnitId))
    const availableUnits = orgUnits.filter((u) => !takenOrgIds.has(u.id))
    return (
      <SimpleFormLayout
        actions={
          <Stack spacing={0.5} alignItems="flex-end">
            <Stack direction="row" spacing={1}>
              <Button
                onClick={() =>
                  guardApi.requestNavigation(() => {
                    setAddingDepartment(false)
                    setDeptForm({
                      orgUnitId: '',
                      reasonDescription: '',
                      championUserId: '',
                      headUserId: '',
                      dueDate: '',
                      questionnaireId: '',
                    })
                  })
                }
              >
                Cancel
              </Button>
              <Button
                variant="contained"
                disabled={busy || Boolean(deptCreateDisabledReason)}
                onClick={() => void handleAddDepartment()}
              >
                Create
              </Button>
            </Stack>
            {deptCreateDisabledReason && (
              <Typography variant="caption" color="text.secondary">
                {deptCreateDisabledReason}
              </Typography>
            )}
          </Stack>
        }
      >
        <SettingsCard
          autoHeight
          title="Department"
          subtitle="Department Head and Privacy Champion can also be set later on the department page"
        >
          <Stack spacing={1.5}>
            <Box sx={formFieldGridSx}>
              <FormSelect
                label="Department"
                required
                value={deptForm.orgUnitId}
                options={[
                  { value: '', label: availableUnits.length === 0 ? 'No departments left' : 'Select' },
                  ...availableUnits.map((u) => ({ value: String(u.id), label: u.name })),
                ]}
                onChange={(v) => setDeptForm({ ...deptForm, orgUnitId: v })}
                disabled={availableUnits.length === 0}
                fullWidth
              />
              <FormSelect
                label="Department Head"
                value={deptForm.headUserId}
                options={[
                  { value: '', label: '-' },
                  ...users.map((u) => ({ value: String(u.id), label: u.fullName || u.email })),
                ]}
                onChange={(v) => setDeptForm({ ...deptForm, headUserId: v })}
                fullWidth
              />
              <FormSelect
                label="Privacy Champion"
                value={deptForm.championUserId}
                options={[
                  { value: '', label: '-' },
                  ...championUsers.map((u) => ({ value: String(u.id), label: u.fullName || u.email })),
                ]}
                onChange={(v) => setDeptForm({ ...deptForm, championUserId: v })}
                fullWidth
              />
              <CompactTextField
                label="Due date"
                type="date"
                value={deptForm.dueDate}
                onChange={(e) => setDeptForm({ ...deptForm, dueDate: e.target.value })}
                fieldIdPrefix="dd"
              />
              <FormSelect
                label="Questionnaire"
                value={deptForm.questionnaireId}
                options={[
                  { value: '', label: 'Default' },
                  ...questionnaires.map((q) => ({ value: String(q.id), label: q.name })),
                ]}
                onChange={(v) => setDeptForm({ ...deptForm, questionnaireId: v })}
                fullWidth
              />
            </Box>
            {!project.linkedRopaProjectId && (
              <CompactTextField
                label="Reason / Description"
                required
                value={deptForm.reasonDescription}
                onChange={(e) => setDeptForm({ ...deptForm, reasonDescription: e.target.value })}
                multiline
                minRows={3}
                fieldIdPrefix="dd"
                maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
              />
            )}
          </Stack>
        </SettingsCard>
      </SimpleFormLayout>
    )
  }

  // ---------- Department Detail Screen ----------
  if (project && departmentDetail) {
    const steps = buildWorkflowSteps(departmentDetail)
    const questions = departmentDetail.questions ?? []
    const assessmentRows = departmentDetail.assessmentRows ?? []
    const sortedQuestions = [...questions].sort((a, b) => a.sortOrder - b.sortOrder)
    const sortedAssessment = [...assessmentRows].sort((a, b) => a.sortOrder - b.sortOrder)

    // Determine current action
    // Status flow: NOT_STARTED -> IN_PROGRESS -> UNDER_REVIEW -> DPO_ASSESSMENT -> PENDING_HEAD_SIGN_OFF -> APPROVED -> CLOSED
    let actionTitle = 'View'
    if (departmentDetail.status === 'NOT_STARTED' || departmentDetail.status === 'IN_PROGRESS') {
      actionTitle = canFillChampion ? 'Fill Questionnaire' : 'Awaiting Champion'
    } else if (departmentDetail.status === 'UNDER_REVIEW') {
      actionTitle = isAdmin ? 'DPO Review' : 'Awaiting DPO Review'
    } else if (departmentDetail.status === 'DPO_ASSESSMENT') {
      actionTitle = isAdmin ? 'DPO Assessment' : 'Awaiting Assessment'
    } else if (departmentDetail.status === 'PENDING_HEAD_SIGN_OFF') {
      actionTitle = isHeadViewer ? 'Head Sign-off' : 'Awaiting Head'
    } else if (departmentDetail.status === 'APPROVED') {
      actionTitle = isAdmin ? 'Close Department' : 'Awaiting Close'
    } else if (departmentDetail.status === 'CLOSED') {
      actionTitle = 'Completed'
    }

    const awaitingChampion =
      departmentDetail.status === 'NOT_STARTED' || departmentDetail.status === 'IN_PROGRESS'
    // Only the Champion fills while awaiting; Admin/others see Assignment + a short note (not the blank form).
    const showQuestionnaire = canFillChampion || !awaitingChampion
    const answersReadOnly = showQuestionnaire && !canFillChampion
    const status = departmentDetail.status
    const assignmentPrimary = isAdmin && awaitingChampion
    const questionnairePrimary = canFillChampion || (showQuestionnaire && status === 'UNDER_REVIEW')
    const reviewPrimary = isAdmin && status === 'UNDER_REVIEW'
    const assessmentPrimary = isAdmin && status === 'DPO_ASSESSMENT'
    const assessmentHistory = isAdmin && Boolean(departmentDetail.assessmentComplete) && status !== 'DPO_ASSESSMENT'
    const headPrimary = status === 'PENDING_HEAD_SIGN_OFF' && (isHeadViewer || isAdmin)
    const closePrimary = isAdmin && status === 'APPROVED'
    const hasEarlierSteps =
      (isAdmin && !assignmentPrimary) ||
      (showQuestionnaire && !questionnairePrimary && sortedQuestions.length > 0) ||
      assessmentHistory
    const requiredQuestions = sortedQuestions.filter((q) => q.required)
    const answeredRequired = requiredQuestions.filter((q) => questionAnswered(q, answers[q.id])).length
    const pendingRiskRows = sortedAssessment.filter((r) => {
      const edit = assessmentEdits[r.id]
      const compliance = (edit?.complianceStatus || r.complianceStatus || '') as string
      const title = (edit?.riskTitle ?? r.riskTitle ?? '').trim()
      return assessmentAllowsRiskTitle(compliance) && Boolean(title) && !r.raisedRiskDisplayId
    })
    const returnNextAction =
      departmentDetail.returnFrom === 'HEAD'
        ? canFillChampion
          ? 'Update the returned answers, then Submit again.'
          : 'Champion should update answers and submit again.'
        : departmentDetail.returnFrom === 'ADMIN'
          ? canFillChampion
            ? 'Address the DPO comments, then Submit again.'
            : 'Champion should address DPO comments and submit again.'
          : 'Review the return comments, then continue the workflow.'

    const assignmentCard = isAdmin ? (
              <SettingsCard autoHeight compact title="Assignment">
                <Stack spacing={0.5} alignItems="flex-start">
                  <FieldActionRow sx={{ flexWrap: { xs: 'wrap', md: 'nowrap' } }}>
                    <FormSelect
                      label="Champion"
                      value={assignForm.championUserId}
                      options={[
                        { value: '', label: '-' },
                        ...championUsers.map((u) => ({ value: String(u.id), label: u.fullName || u.email })),
                      ]}
                      onChange={(v) => setAssignForm({ ...assignForm, championUserId: v })}
                    />
                    <FormSelect
                      label="Department Head"
                      value={assignForm.headUserId}
                      options={[
                        { value: '', label: '-' },
                        ...users.map((u) => ({ value: String(u.id), label: u.fullName || u.email })),
                      ]}
                      onChange={(v) => setAssignForm({ ...assignForm, headUserId: v })}
                    />
                    <CompactTextField
                      label="Due Date"
                      type="date"
                      value={assignForm.dueDate}
                      onChange={(e) => setAssignForm({ ...assignForm, dueDate: e.target.value })}
                      fieldIdPrefix="da"
                    />
                    <Box sx={{ flexShrink: 0, alignSelf: { xs: 'stretch', sm: 'flex-end' }, pl: { xs: 0, sm: 2.5 } }}>
                      <Button
                        variant="contained"
                        size="small"
                        sx={controlActionSx}
                        disabled={busy || Boolean(assignSaveDisabledReason)}
                        onClick={() => void handleAssign()}
                      >
                        Save Assignment
                      </Button>
                    </Box>
                  </FieldActionRow>
                  {assignSaveDisabledReason && (
                    <Typography variant="caption" color="text.secondary">
                      {assignSaveDisabledReason}
                    </Typography>
                  )}
                </Stack>
              </SettingsCard>
    ) : null

    const awaitingNote =
      awaitingChampion && !canFillChampion ? (
        <Alert severity="info">
          Questionnaire is with the Champion
          {departmentDetail.championName ? ` (${departmentDetail.championName})` : ''}.
          {departmentDetail.dueDate ? ` Due ${departmentDetail.dueDate}.` : ''}
        </Alert>
      ) : null

    return (
      <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, overflow: 'auto', pb: 3 }}>
        {departmentDetail.returnReason && departmentDetail.returnFrom && (
          <Alert severity="warning">
            <Typography sx={{ fontWeight: 600, fontSize: '0.8125rem', mb: 0.25 }}>
              Returned by {departmentDetail.returnFrom === 'HEAD' ? 'Department Head' : 'DPO'}
            </Typography>
            <Typography sx={{ fontSize: '0.8125rem' }}>{departmentDetail.returnReason}</Typography>
            <Typography sx={{ fontSize: '0.75rem', mt: 0.75 }}>{returnNextAction}</Typography>
          </Alert>
        )}

        <BiaJourneyPanel steps={steps} actionTitle={actionTitle}>
          <Stack spacing={1.5}>
            {assignmentPrimary && assignmentCard}
            {assignmentPrimary && awaitingNote}

            {(questionnairePrimary || (showEarlierSteps && showQuestionnaire)) && sortedQuestions.length > 0 && (
              <SettingsCard autoHeight compact title="Questionnaire">
                <Stack
                  direction={{ xs: 'column', sm: 'row' }}
                  spacing={1}
                  alignItems={{ xs: 'flex-start', sm: 'center' }}
                  justifyContent="space-between"
                  sx={{ mb: 1.25 }}
                >
                  <Typography sx={{ fontSize: '0.8125rem', color: brand.muted }}>
                    {requiredQuestions.length > 0
                      ? `Answered ${answeredRequired} of ${requiredQuestions.length} required`
                      : `${sortedQuestions.length} questions`}
                  </Typography>
                  <FormControlLabel
                    control={
                      <Switch
                        size="small"
                        checked={showAllGuidance}
                        onChange={(_, v) => setShowAllGuidance(v)}
                      />
                    }
                    label={<Typography sx={{ fontSize: '0.75rem' }}>Show all guidance</Typography>}
                  />
                </Stack>
                <Stack spacing={0} sx={{ width: '100%', maxWidth: 1120 }}>
                  {sortedQuestions.map((q, idx) => {
                    const prevSection = idx > 0 ? sortedQuestions[idx - 1].sectionName : null
                    const showSection = q.sectionName && q.sectionName !== prevSection
                    const selectedOpts = answers[q.id]?.answerOptions ?? []
                    const isNarrative =
                      q.responseType === 'FREE_TEXT' || q.responseType === 'FREE_TEXT_ATTACHMENT'
                    const isEnum =
                      q.responseType === 'YES_NO' ||
                      q.responseType === 'YES_NO_PARTLY' ||
                      q.responseType === 'CONFIRMATION'

                    const detailsField = answersReadOnly ? (
                      <Box>
                        <Typography sx={{ ...sectionLabelSx, mb: 0.35 }}>Details / evidence reference</Typography>
                        <Typography sx={{ fontSize: '0.8125rem', whiteSpace: 'pre-wrap', color: brand.ink }}>
                          {(answers[q.id]?.detailsEvidence ?? '').trim() || '-'}
                        </Typography>
                      </Box>
                    ) : (
                      <CompactTextField
                        label="Details / evidence reference"
                        value={answers[q.id]?.detailsEvidence ?? ''}
                        onChange={(e) => patchAnswer(q.id, { detailsEvidence: e.target.value })}
                        multiline
                        minRows={isEnum ? 2 : 3}
                        disabled={!canFillChampion}
                        fieldIdPrefix={`qe-${q.id}`}
                        maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                      />
                    )

                    const attachmentBlock =
                      q.responseType === 'FREE_TEXT_ATTACHMENT' ? (
                        <Box sx={{ mt: 0.75 }}>
                          <Typography sx={{ ...sectionLabelSx, mb: 0.5 }}>Attachment</Typography>
                          <EvidenceAttachedList
                            items={questionAttachments[q.id] ?? []}
                            emptyLabel="No files attached"
                            flush
                            onError={onError}
                            onRemove={
                              canFillChampion
                                ? (ev) => void handleRemoveQuestionAttachment(q.id, ev)
                                : undefined
                            }
                          />
                          {canFillChampion && (
                            <Button
                              variant="outlined"
                              size="small"
                              component="label"
                              disabled={busy}
                              sx={{ mt: 1 }}
                            >
                              Upload file
                              <input
                                type="file"
                                hidden
                                onChange={(e) => {
                                  const f = e.target.files?.[0] ?? null
                                  e.target.value = ''
                                  void handleUploadQuestionAttachment(q.id, f)
                                }}
                              />
                            </Button>
                          )}
                        </Box>
                      ) : null

                    const championControl =
                      q.responseType === 'YES_NO' || q.responseType === 'YES_NO_PARTLY' ? (
                        <FormSelect
                          label="Champion response"
                          value={(answers[q.id]?.answerText as 'YES' | 'NO' | 'PARTLY' | '') ?? ''}
                          options={
                            q.responseType === 'YES_NO'
                              ? [
                                  { value: '', label: '-' },
                                  { value: 'YES', label: 'Yes' },
                                  { value: 'NO', label: 'No' },
                                ]
                              : [
                                  { value: '', label: '-' },
                                  { value: 'YES', label: 'Yes' },
                                  { value: 'NO', label: 'No' },
                                  { value: 'PARTLY', label: 'Partly' },
                                ]
                          }
                          onChange={(v) => patchAnswer(q.id, { answerText: v, answerOptions: [] })}
                          disabled={!canFillChampion}
                        />
                      ) : q.responseType === 'CONFIRMATION' ? (
                        <FormSelect
                          label="Champion response"
                          value={(answers[q.id]?.answerText as 'CONFIRMED' | '') ?? ''}
                          options={[
                            { value: '', label: '-' },
                            { value: 'CONFIRMED', label: 'Confirmed' },
                          ]}
                          onChange={(v) => patchAnswer(q.id, { answerText: v, answerOptions: [] })}
                          disabled={!canFillChampion}
                        />
                      ) : q.responseType === 'MULTI_SELECT' ? (
                        <Box>
                          <Typography
                            component="label"
                            sx={{
                              display: 'block',
                              fontSize: '0.6875rem',
                              fontWeight: 600,
                              color: brand.muted,
                              mb: 0.5,
                            }}
                          >
                            Champion response
                          </Typography>
                          <Stack spacing={0.25}>
                            {q.options.map((opt) => {
                              const checked = selectedOpts.includes(opt)
                              return (
                                <FormControlLabel
                                  key={opt}
                                  disabled={!canFillChampion}
                                  control={
                                    <Checkbox
                                      size="small"
                                      checked={checked}
                                      onChange={() => {
                                        const next = checked
                                          ? selectedOpts.filter((o) => o !== opt)
                                          : [...selectedOpts, opt]
                                        patchAnswer(q.id, { answerText: '', answerOptions: next })
                                      }}
                                    />
                                  }
                                  label={<Typography sx={{ fontSize: '0.8125rem' }}>{opt}</Typography>}
                                />
                              )
                            })}
                          </Stack>
                        </Box>
                      ) : (
                        <Box>
                          <CompactTextField
                            label="Champion response"
                            value={answers[q.id]?.answerText ?? ''}
                            onChange={(e) =>
                              patchAnswer(q.id, { answerText: e.target.value, answerOptions: [] })
                            }
                            multiline={isNarrative}
                            minRows={3}
                            disabled={!canFillChampion}
                            fieldIdPrefix={`qa-${q.id}`}
                            maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                          />
                          {attachmentBlock}
                        </Box>
                      )

                    return (
                      <Box
                        key={q.id}
                        sx={{
                          py: 1.5,
                          borderTop: idx === 0 && !showSection ? 'none' : `1px solid ${brand.borderLight}`,
                        }}
                      >
                        {showSection && (
                          <Typography
                            sx={{
                              fontSize: '0.75rem',
                              fontWeight: 700,
                              color: brand.gold,
                              textTransform: 'uppercase',
                              letterSpacing: '0.04em',
                              mb: 1.25,
                              mt: idx === 0 ? 0 : 0.5,
                            }}
                          >
                            {q.sectionName}
                          </Typography>
                        )}
                        <Stack
                          direction="row"
                          alignItems="flex-start"
                          spacing={0.5}
                          sx={{ mb: 1, width: 'fit-content', maxWidth: '100%' }}
                        >
                          <Typography sx={{ fontSize: '0.8125rem', fontWeight: 600, minWidth: 0 }}>
                            {q.code}. {q.questionText}
                            {q.required && <span style={{ color: semantic.risk.high }}> *</span>}
                          </Typography>
                          {q.guidance && !showAllGuidance && (
                            <IconButton
                              size="small"
                              aria-label={`Guidance for ${q.code}`}
                              onClick={(e) => setGuidanceAnchor({ id: q.id, el: e.currentTarget })}
                              sx={{ mt: -0.25, color: brand.muted, flexShrink: 0 }}
                            >
                              <InfoOutlinedIcon sx={{ fontSize: 16 }} />
                            </IconButton>
                          )}
                        </Stack>
                        {q.guidance && showAllGuidance && (
                          <Typography
                            sx={{
                              fontSize: '0.75rem',
                              color: brand.muted,
                              whiteSpace: 'pre-wrap',
                              mb: 1,
                              maxWidth: 800,
                            }}
                          >
                            {q.guidance}
                          </Typography>
                        )}
                        {/* Two columns for all types: enum stays compact in the left cell (align start). */}
                        <Box
                          sx={{
                            ...twoColFieldSx,
                            // Enum + narrative: left cell content-sized so Yes/No does not stretch.
                            ...(isEnum
                              ? {
                                  gridTemplateColumns: {
                                    xs: '1fr',
                                    sm: 'minmax(168px, 240px) minmax(0, 1fr)',
                                  },
                                }
                              : {}),
                          }}
                        >
                          {answersReadOnly ? (
                            <Box>
                              <Typography sx={{ ...sectionLabelSx, mb: 0.35 }}>Champion response</Typography>
                              {q.responseType === 'MULTI_SELECT' ? (
                                <Typography sx={{ fontSize: '0.8125rem' }}>
                                  {(answers[q.id]?.answerOptions ?? []).length
                                    ? (answers[q.id]?.answerOptions ?? []).join(', ')
                                    : '-'}
                                </Typography>
                              ) : (
                                <Typography sx={{ fontSize: '0.8125rem', whiteSpace: 'pre-wrap' }}>
                                  {(answers[q.id]?.answerText ?? '').trim() || '-'}
                                </Typography>
                              )}
                              {q.responseType === 'FREE_TEXT_ATTACHMENT' ? attachmentBlock : null}
                            </Box>
                          ) : (
                            championControl
                          )}
                          {detailsField}
                        </Box>
                      </Box>
                    )
                  })}
                </Stack>
                {canFillChampion && (
                  <Stack
                    spacing={0.5}
                    sx={{
                      mt: 1.5,
                      pt: 1.5,
                      borderTop: `1px solid ${brand.borderLight}`,
                      position: 'sticky',
                      bottom: 0,
                      bgcolor: 'background.paper',
                      zIndex: 1,
                    }}
                  >
                    <Stack direction="row" spacing={1}>
                      <Button
                        variant="contained"
                        size="small"
                        disabled={busy || !answersDirty}
                        onClick={() => void handleSaveAnswers()}
                      >
                        Save Answers
                      </Button>
                      <Button
                        variant="outlined"
                        size="small"
                        disabled={busy}
                        onClick={() => setConfirmSubmit(true)}
                      >
                        Submit
                      </Button>
                    </Stack>
                    {!answersDirty ? (
                      <Typography variant="caption" color="text.secondary">
                        No answer changes to save
                      </Typography>
                    ) : null}
                  </Stack>
                )}
                <Popover
                  open={guidanceAnchor != null}
                  anchorEl={guidanceAnchor?.el ?? null}
                  onClose={() => setGuidanceAnchor(null)}
                  anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
                  transformOrigin={{ vertical: 'top', horizontal: 'left' }}
                  slotProps={{ paper: { sx: { maxWidth: 420, p: 1.5 } } }}
                >
                  <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, mb: 0.5 }}>Guidance</Typography>
                  <Typography sx={{ fontSize: '0.8125rem', color: brand.ink, whiteSpace: 'pre-wrap' }}>
                    {(guidanceAnchor && sortedQuestions.find((q) => q.id === guidanceAnchor.id)?.guidance) || ''}
                  </Typography>
                </Popover>
              </SettingsCard>
            )}

            {reviewPrimary && (
              <SettingsCard autoHeight compact title="DPO Review">
                <CompactTextField
                  label="Comments"
                  value={reviewComments}
                  onChange={(e) => setReviewComments(e.target.value)}
                  multiline
                  minRows={2}
                  fieldIdPrefix="dr"
                />
                <Stack direction="row" spacing={1} sx={{ mt: 1.5 }}>
                  <Button variant="contained" size="small" disabled={busy} onClick={() => void handleDpoAccept()}>
                    Accept
                  </Button>
                  <Button variant="outlined" color="warning" size="small" disabled={busy} onClick={() => void handleDpoReturn()}>
                    Return
                  </Button>
                </Stack>
              </SettingsCard>
            )}

            {(assessmentPrimary || (showEarlierSteps && assessmentHistory)) && sortedAssessment.length > 0 && (
              <SettingsCard autoHeight compact title="Assessment">
                {pendingRiskRows.length > 0 && isAdmin && (
                  <Stack direction="row" spacing={1} sx={{ mb: 1.25 }} alignItems="center">
                    <Button
                      size="small"
                      variant="outlined"
                      disabled={busy}
                      onClick={() => void handleCreateAllRisksFromAssessment()}
                    >
                      Create {pendingRiskRows.length} Privacy Risk{pendingRiskRows.length === 1 ? '' : 's'}
                    </Button>
                    <Typography variant="caption" color="text.secondary">
                      Principles with a Risk Title and no linked Privacy Risk
                    </Typography>
                  </Stack>
                )}
                <Stack spacing={1.25}>
                  {sortedAssessment.map((r) => {
                    const editable = departmentDetail.status === 'DPO_ASSESSMENT'
                    const complianceNow = (assessmentEdits[r.id]?.complianceStatus ||
                      r.complianceStatus ||
                      '') as ComplianceRating
                    const showRiskTitle = assessmentAllowsRiskTitle(complianceNow)
                    const titleRequired = complianceNow === 'NON_COMPLIANT'
                    const titleNow = showRiskTitle
                      ? (assessmentEdits[r.id]?.riskTitle ?? r.riskTitle ?? '').trim()
                      : ''
                    const raisedId = r.raisedRiskDisplayId
                    const principleLabel = r.principle || r.descriptionText || `Principle ${r.sortOrder}`
                    const showDescription =
                      Boolean(r.descriptionText) && r.descriptionText !== r.principle
                    const complianceTone =
                      complianceNow === 'COMPLIANT'
                        ? 'success'
                        : complianceNow === 'PARTIALLY_COMPLIANT'
                          ? 'warning'
                          : complianceNow === 'NON_COMPLIANT'
                            ? 'danger'
                            : 'neutral'
                    const riskAction = raisedId ? (
                      <Typography sx={{ fontSize: '0.75rem', color: brand.muted, alignSelf: 'center' }}>
                        {raisedId}
                      </Typography>
                    ) : showRiskTitle && titleNow && isAdmin ? (
                      <Button
                        size="small"
                        variant="outlined"
                        disabled={busy}
                        onClick={() => void handleCreateRiskFromAssessment(r.id)}
                        sx={controlActionSx}
                      >
                        Create Risk
                      </Button>
                    ) : null
                    return (
                      <Box
                        key={r.id}
                        sx={{
                          border: `1px solid ${brand.border}`,
                          borderRadius: 1,
                          bgcolor: 'background.paper',
                          p: 1.25,
                        }}
                      >
                        <Box sx={{ mb: editable || showDescription ? 1 : 0 }}>
                          <Typography
                            sx={{
                              fontSize: '0.8125rem',
                              fontWeight: 600,
                              color: brand.ink,
                              lineHeight: 1.3,
                            }}
                          >
                            {principleLabel}
                          </Typography>
                          {showDescription ? (
                            <Typography
                              sx={{
                                mt: 0.35,
                                fontSize: '0.75rem',
                                color: brand.muted,
                                whiteSpace: 'pre-wrap',
                              }}
                            >
                              {r.descriptionText}
                            </Typography>
                          ) : null}
                        </Box>

                        {editable ? (
                          <Stack spacing={1}>
                            <FieldActionRow>
                              <FormSelect
                                label="Compliance"
                                required
                                value={assessmentEdits[r.id]?.complianceStatus ?? ''}
                                options={COMPLIANCE_OPTIONS}
                                onChange={(v) => {
                                  const next = v as ComplianceRating
                                  setAssessmentEdits({
                                    ...assessmentEdits,
                                    [r.id]: {
                                      ...assessmentEdits[r.id],
                                      complianceStatus: next,
                                      riskTitle: assessmentAllowsRiskTitle(next)
                                        ? assessmentEdits[r.id]?.riskTitle ?? ''
                                        : '',
                                    },
                                  })
                                }}
                              />
                              {riskAction}
                            </FieldActionRow>
                            <Box sx={twoColFieldSx}>
                              <CompactTextField
                                label="Remarks"
                                value={assessmentEdits[r.id]?.remarks ?? ''}
                                onChange={(e) =>
                                  setAssessmentEdits({
                                    ...assessmentEdits,
                                    [r.id]: { ...assessmentEdits[r.id], remarks: e.target.value },
                                  })
                                }
                                multiline
                                minRows={2}
                                fieldIdPrefix={`ar-${r.id}`}
                                maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                              />
                              <CompactTextField
                                label="Measures"
                                value={assessmentEdits[r.id]?.recommendedMeasures ?? ''}
                                onChange={(e) =>
                                  setAssessmentEdits({
                                    ...assessmentEdits,
                                    [r.id]: {
                                      ...assessmentEdits[r.id],
                                      recommendedMeasures: e.target.value,
                                    },
                                  })
                                }
                                multiline
                                minRows={2}
                                fieldIdPrefix={`am-${r.id}`}
                                maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                              />
                            </Box>
                            {showRiskTitle ? (
                              <CompactTextField
                                label="Risk Title"
                                required={titleRequired}
                                value={assessmentEdits[r.id]?.riskTitle ?? ''}
                                onChange={(e) =>
                                  setAssessmentEdits({
                                    ...assessmentEdits,
                                    [r.id]: { ...assessmentEdits[r.id], riskTitle: e.target.value },
                                  })
                                }
                                fieldIdPrefix={`at-${r.id}`}
                                placeholder={
                                  titleRequired ? 'Required for Non Compliant' : 'Optional'
                                }
                                maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                              />
                            ) : null}
                          </Stack>
                        ) : (
                          <Stack spacing={0.75}>
                            <Stack direction="row" spacing={1} alignItems="center">
                              {complianceNow ? (
                                <StatusChip
                                  label={complianceNow.replace(/_/g, ' ')}
                                  tone={complianceTone}
                                />
                              ) : (
                                <Typography sx={{ fontSize: '0.75rem', color: brand.muted }}>
                                  Compliance: -
                                </Typography>
                              )}
                              {riskAction}
                            </Stack>
                            <Box sx={twoColFieldSx}>
                              <Box>
                                <Typography sx={{ ...sectionLabelSx, mb: 0.25 }}>Remarks</Typography>
                                <Typography
                                  sx={{ fontSize: '0.75rem', whiteSpace: 'pre-wrap', color: brand.ink }}
                                >
                                  {r.remarks || '-'}
                                </Typography>
                              </Box>
                              <Box>
                                <Typography sx={{ ...sectionLabelSx, mb: 0.25 }}>Measures</Typography>
                                <Typography
                                  sx={{ fontSize: '0.75rem', whiteSpace: 'pre-wrap', color: brand.ink }}
                                >
                                  {r.recommendedMeasures || '-'}
                                </Typography>
                              </Box>
                            </Box>
                            {showRiskTitle ? (
                              <Box>
                                <Typography sx={{ ...sectionLabelSx, mb: 0.25 }}>Risk Title</Typography>
                                <Typography sx={{ fontSize: '0.75rem', color: brand.ink }}>
                                  {r.riskTitle || '-'}
                                </Typography>
                              </Box>
                            ) : null}
                          </Stack>
                        )}
                      </Box>
                    )
                  })}
                </Stack>
                                {assessmentPrimary && (
                  <Stack
                    spacing={0.5}
                    sx={{
                      mt: 1.5,
                      pt: 1.5,
                      borderTop: `1px solid ${brand.borderLight}`,
                      position: 'sticky',
                      bottom: 0,
                      bgcolor: 'background.paper',
                      zIndex: 1,
                    }}
                  >
                    <Stack direction="row" spacing={1}>
                      <Button
                        variant="contained"
                        size="small"
                        disabled={busy || !assessmentDirty}
                        onClick={() => void handleSaveAssessment()}
                      >
                        Save Assessment
                      </Button>
                      <Button
                        variant="outlined"
                        size="small"
                        disabled={busy || Boolean(finalizeAssessmentDisabledReason)}
                        onClick={() => void handleFinalizeAssessment()}
                      >
                        Finalize
                      </Button>
                    </Stack>
                    {finalizeAssessmentDisabledReason ? (
                      <Typography variant="caption" color="text.secondary">
                        {finalizeAssessmentDisabledReason}
                      </Typography>
                    ) : !assessmentDirty ? (
                      <Typography variant="caption" color="text.secondary">
                        No assessment changes to save
                      </Typography>
                    ) : null}
                  </Stack>
                )}
              </SettingsCard>
            )}

            {headPrimary && isHeadViewer && (
              <SettingsCard autoHeight compact title="Head Sign-off">
                <CompactTextField
                  label="Notes"
                  value={headNotes}
                  onChange={(e) => setHeadNotes(e.target.value)}
                  multiline
                  minRows={2}
                  fieldIdPrefix="hn"
                />
                <Stack direction="row" spacing={1} sx={{ mt: 1.5 }}>
                  <Button variant="contained" size="small" disabled={busy} onClick={() => void handleHeadApprove()}>
                    Approve
                  </Button>
                  <Button variant="outlined" color="warning" size="small" disabled={busy} onClick={() => void handleHeadReturn()}>
                    Return
                  </Button>
                </Stack>
              </SettingsCard>
            )}
            {headPrimary && !isHeadViewer && (
              <Alert severity="info">
                Awaiting Department Head
                {departmentDetail.departmentHeadName ? ` (${departmentDetail.departmentHeadName})` : ''}{' '}
                sign-off.
              </Alert>
            )}

            {closePrimary && (
              <SettingsCard autoHeight compact title="Close">
                <Typography sx={{ fontSize: '0.8125rem', mb: 1 }}>
                  The department head has approved. You can now close this DPIA department.
                </Typography>
                <Button variant="contained" size="small" disabled={busy} onClick={() => setConfirmClose(true)}>
                  Close Department
                </Button>
              </SettingsCard>
            )}

            {hasEarlierSteps && (
              <Accordion
                disableGutters
                elevation={0}
                expanded={showEarlierSteps}
                onChange={(_, expanded) => setShowEarlierSteps(expanded)}
                sx={{ border: `1px solid ${brand.border}`, borderRadius: 1, '&:before': { display: 'none' } }}
              >
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography sx={{ fontSize: '0.8125rem', fontWeight: 600 }}>Earlier steps</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Stack spacing={1.5}>
                    {!assignmentPrimary && assignmentCard}
                    {!assignmentPrimary && awaitingChampion && awaitingNote}
                    {!questionnairePrimary && showQuestionnaire && sortedQuestions.length > 0 && (
                      <Typography sx={{ fontSize: '0.8125rem', color: brand.muted }}>
                        Questionnaire answers are shown above while this panel is open.
                      </Typography>
                    )}
                    {assessmentHistory && (
                      <Typography sx={{ fontSize: '0.8125rem', color: brand.muted }}>
                        Assessment history is shown above while this panel is open.
                      </Typography>
                    )}
                  </Stack>
                </AccordionDetails>
              </Accordion>
            )}
          </Stack>
        </BiaJourneyPanel>

        {/* Confirm dialogs */}
        <ConfirmActionDialog
          open={confirmSubmit}
          title="Submit for Review"
          message="Are you sure you want to submit this questionnaire for DPO review?"
          confirmLabel="Submit"
          confirmColor="primary"
          busy={busy}
          onCancel={() => setConfirmSubmit(false)}
          onConfirm={() => void handleSubmit()}
        />
        <ConfirmActionDialog
          open={confirmClose}
          title="Close Department"
          message="Are you sure you want to close this DPIA department? This action cannot be undone."
          confirmLabel="Close"
          confirmColor="primary"
          busy={busy}
          onCancel={() => setConfirmClose(false)}
          onConfirm={() => void handleClose()}
        />
      </Stack>
    )
  }

  // ---------- Department Register (inside project) ----------
  if (project) {
    const counts = {
      total: visibleDepartments.length,
      inProgress: visibleDepartments.filter((d) => d.status === 'IN_PROGRESS' || d.status === 'NOT_STARTED').length,
      underReview: visibleDepartments.filter((d) => d.status === 'UNDER_REVIEW' || d.status === 'DPO_ASSESSMENT').length,
      withHead: visibleDepartments.filter((d) => d.status === 'PENDING_HEAD_SIGN_OFF').length,
      closed: visibleDepartments.filter((d) => d.status === 'CLOSED' || d.status === 'APPROVED').length,
    }
    const kpiCards = [
      { label: 'Departments', value: counts.total, accent: brand.gold },
      { label: 'In Progress', value: counts.inProgress, accent: counts.inProgress ? semantic.risk.medium : brand.gold },
      { label: 'Under Review', value: counts.underReview, accent: counts.underReview ? semantic.risk.high : brand.gold },
      { label: 'With Head', value: counts.withHead, accent: counts.withHead ? semantic.risk.medium : brand.gold },
      { label: 'Closed', value: counts.closed, accent: semantic.risk.low },
    ]

    return (
      <Stack spacing={1} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
        <Stack direction="row" spacing={1} alignItems="center" justifyContent="flex-end">
          {isAdmin && project.status !== 'COMPLETED' && (
            <Stack direction="row" spacing={1}>
              <Button variant="outlined" size="small" onClick={() => setAddingDepartment(true)}>
                Add Department
              </Button>
              <Button
                variant="contained"
                size="small"
                disabled={!canComplete}
                onClick={() => setConfirmComplete(true)}
              >
                Complete Project
              </Button>
            </Stack>
          )}
        </Stack>

        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'repeat(2, 1fr)', md: 'repeat(5, 1fr)' }, gap: 1 }}>
          {kpiCards.map((k) => (
            <KpiCard key={k.label} compact label={k.label} value={String(k.value)} accentColor={k.accent} />
          ))}
        </Box>

        <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <RegisterToolbar search={search} searchPlaceholder="Search departments" onSearchChange={setSearch} />
          <TableContainer sx={{ flex: 1, minHeight: 0 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell>Department</TableCell>
                  <TableCell>Champion</TableCell>
                  <TableCell>Head</TableCell>
                  <TableCell>Due Date</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {visibleDepartments.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5}>
                      <Typography color="text.secondary" sx={{ py: 2 }}>
                        {project.linkedRopaProjectId
                          ? 'No DPIA-applicable departments were found on the linked ROPA. Use Add Department to start.'
                          : 'No departments yet. Use Add Department to start.'}
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
                {visibleDepartments
                  .filter((d) => !search.trim() || (d.orgUnitName ?? '').toLowerCase().includes(search.trim().toLowerCase()))
                  .map((d) => (
                    <TableRow
                      key={d.id}
                      hover
                      sx={{ cursor: 'pointer' }}
                      onClick={() => setSelectedDeptId(d.id)}
                    >
                      <TableCell>{d.orgUnitName ?? `#${d.orgUnitId}`}</TableCell>
                      <TableCell>{d.championName ?? '-'}</TableCell>
                      <TableCell>{d.departmentHeadName ?? '-'}</TableCell>
                      <TableCell>{d.dueDate ?? '-'}</TableCell>
                      <TableCell>
                        <StatusChip label={statusLabel(d.status)} tone={deptTone(d.status)} />
                      </TableCell>
                    </TableRow>
                  ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>

        <ConfirmActionDialog
          open={confirmComplete}
          title="Complete Project"
          message="All departments must be closed before completing the project. Are you sure?"
          confirmLabel="Complete"
          confirmColor="primary"
          confirmDisabled={!canComplete}
          disabledReason={!canComplete ? 'All departments must be closed first' : undefined}
          busy={busy}
          onCancel={() => setConfirmComplete(false)}
          onConfirm={() => void handleCompleteProject()}
        />
      </Stack>
    )
  }

  // ---------- Project Register ----------
  return (
    <Stack spacing={1} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'repeat(2, 1fr)', md: 'repeat(4, 1fr)' }, gap: 1 }}>
        <KpiCard
          compact
          label="Awaiting Champion"
          value={String(projectQueueKpis.awaitingChampion)}
          accentColor={projectQueueKpis.awaitingChampion ? semantic.risk.medium : brand.gold}
        />
        <KpiCard
          compact
          label="Needs DPO"
          value={String(projectQueueKpis.needingDpo)}
          accentColor={projectQueueKpis.needingDpo ? semantic.risk.high : brand.gold}
        />
        <KpiCard
          compact
          label="Needs Head"
          value={String(projectQueueKpis.needingHead)}
          accentColor={projectQueueKpis.needingHead ? semantic.risk.medium : brand.gold}
        />
        <KpiCard
          compact
          label="Ready to Close"
          value={String(projectQueueKpis.readyToClose)}
          accentColor={projectQueueKpis.readyToClose ? semantic.risk.low : brand.gold}
        />
      </Box>
      <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        <RegisterToolbar
          search={search}
          searchPlaceholder="Search projects"
          onSearchChange={setSearch}
          filters={[
            {
              id: 'status',
              label: 'Status',
              value: projectStatusFilter,
              options: [
                { value: 'ALL', label: 'All' },
                { value: 'DRAFT', label: 'Draft' },
                { value: 'IN_PROGRESS', label: 'In Progress' },
                { value: 'COMPLETED', label: 'Completed' },
              ],
              onChange: setProjectStatusFilter,
              minWidth: 140,
            },
          ]}
          trailing={
            isAdmin ? (
              <Button
                variant="contained"
                size="small"
                onClick={() => {
                  setProjectForm({ name: '', year: String(new Date().getFullYear()), linkRopa: 'NO', ropaProjectId: '' })
                  setCreatingProject(true)
                }}
              >
                New Project
              </Button>
            ) : undefined
          }
        />
        <TableContainer sx={{ flex: 1, minHeight: 0 }}>
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell>Year</TableCell>
                <TableCell>Project</TableCell>
                <TableCell>Linked ROPA</TableCell>
                <TableCell>Departments</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredProjects.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>
                    <Typography color="text.secondary" sx={{ py: 2 }}>
                      {visibleProjects.length === 0
                        ? isAdmin
                          ? 'No DPIA projects yet. Create a project to get started.'
                          : 'No DPIA projects assigned to you yet.'
                        : 'No projects match these filters.'}
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
              {filteredProjects.map((p) => (
                <TableRow
                  key={p.id}
                  hover
                  sx={{ cursor: 'pointer' }}
                  onClick={() => setSelectedProjectId(p.id)}
                >
                  <TableCell>{p.year ?? '-'}</TableCell>
                  <TableCell>{p.name}</TableCell>
                  <TableCell>{p.linkedRopaProjectName ?? '-'}</TableCell>
                  <TableCell>{p.departments.length}</TableCell>
                  <TableCell>
                    <StatusChip label={statusLabel(p.status)} tone={projectTone(p.status)} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>
    </Stack>
  )
}
