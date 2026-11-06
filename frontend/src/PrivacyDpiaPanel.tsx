import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Box,
  Button,
  Card,
  Checkbox,
  FormControlLabel,
  Stack,
  Step,
  StepButton,
  Stepper,
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
  DPIA_SCREENING_Q4_TO_Q7,
  type CreatePrivacyDpiaBody,
  type DpiaMethodologyBank,
  type DpiaResidualRisk,
  type PrivacyDpia,
  type PrivacyRopa,
  type UserSummary,
} from './api'
import StatusChip from './components/StatusChip'
import { CompactTextField, FormSelect, KpiCard, PageLoading, RegisterToolbar, SettingsCard } from './components/ui'
import { useAppShellPage } from './components/AppShellPageContext'
import { brand, semantic } from './theme'

type QuestionAnswer = { number: number; answer: string }
type AssessmentRow = {
  number: number
  comments: string
  likelihood: string
  impact: string
  treatment: string
}

const MODE_A_STEPS = ['Screening', 'Questions', 'Assessment'] as const

function toneForStatus(status: string): 'success' | 'warning' | 'danger' | 'neutral' | 'info' {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'UNDER_REVIEW':
    case 'ASSIGNED':
    case 'IN_PROGRESS':
      return 'info'
    case 'DRAFT':
      return 'neutral'
    case 'REJECTED':
      return 'danger'
    default:
      return 'neutral'
  }
}

function statusLabel(status: string): string {
  return status.replace(/_/g, ' ')
}

function parseQuestionsJson(raw: string | null | undefined, bank: DpiaMethodologyBank | null): QuestionAnswer[] {
  const byNumber = new Map<number, string>()
  if (raw?.trim()) {
    try {
      const parsed = JSON.parse(raw) as unknown
      if (Array.isArray(parsed)) {
        for (const row of parsed) {
          if (row && typeof row === 'object' && 'number' in row) {
            const n = Number((row as { number: unknown }).number)
            const answer = String((row as { answer?: unknown }).answer ?? '')
            if (Number.isFinite(n)) byNumber.set(n, answer)
          }
        }
      }
    } catch {
      /* keep empty answers */
    }
  }
  const source = bank?.questions?.length
    ? bank.questions
    : Array.from({ length: 17 }, (_, i) => ({ number: i + 1, principle: '', question: '' }))
  return source.map((q) => ({ number: q.number, answer: byNumber.get(q.number) ?? '' }))
}

function parseAssessmentJson(raw: string | null | undefined, bank: DpiaMethodologyBank | null): AssessmentRow[] {
  const byNumber = new Map<number, AssessmentRow>()
  if (raw?.trim()) {
    try {
      const parsed = JSON.parse(raw) as unknown
      if (Array.isArray(parsed)) {
        for (const row of parsed) {
          if (row && typeof row === 'object' && 'number' in row) {
            const n = Number((row as { number: unknown }).number)
            if (!Number.isFinite(n)) continue
            byNumber.set(n, {
              number: n,
              comments: String((row as { comments?: unknown }).comments ?? ''),
              likelihood: String((row as { likelihood?: unknown }).likelihood ?? ''),
              impact: String((row as { impact?: unknown }).impact ?? ''),
              treatment: String((row as { treatment?: unknown }).treatment ?? ''),
            })
          }
        }
      }
    } catch {
      /* keep empty rows */
    }
  }
  const source = bank?.principles?.length
    ? bank.principles
    : Array.from({ length: 11 }, (_, i) => ({ number: i + 1, name: `Principle ${i + 1}`, description: '' }))
  return source.map((p) => {
    const existing = byNumber.get(p.number)
    return (
      existing ?? {
        number: p.number,
        comments: '',
        likelihood: '',
        impact: '',
        treatment: '',
      }
    )
  })
}

function serializeQuestions(answers: QuestionAnswer[]): string {
  return JSON.stringify(answers.map((a) => ({ number: a.number, answer: a.answer })))
}

function serializeAssessment(rows: AssessmentRow[]): string {
  return JSON.stringify(
    rows.map((r) => ({
      number: r.number,
      comments: r.comments,
      likelihood: r.likelihood,
      impact: r.impact,
      treatment: r.treatment,
    })),
  )
}

export default function PrivacyDpiaPanel({
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
  const [users, setUsers] = useState<UserSummary[]>([])
  const [bank, setBank] = useState<DpiaMethodologyBank | null>(null)
  const [selected, setSelected] = useState<PrivacyDpia | null>(null)
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [rejectNotes, setRejectNotes] = useState('')
  const [assignUserId, setAssignUserId] = useState<number | ''>('')
  const [assignDue, setAssignDue] = useState('')
  const [editQ, setEditQ] = useState<CreatePrivacyDpiaBody>({})
  const [questionAnswers, setQuestionAnswers] = useState<QuestionAnswer[]>([])
  const [assessmentRows, setAssessmentRows] = useState<AssessmentRow[]>([])
  const [modeAStep, setModeAStep] = useState(0)
  const [saving, setSaving] = useState(false)
  const [raiseTitle, setRaiseTitle] = useState('')
  const [raiseBusy, setRaiseBusy] = useState(false)

  const load = useCallback(async () => {
    try {
      const [dpia, ropa, userList, methodology] = await Promise.all([
        api.listPrivacyDpia(),
        api.listPrivacyRopa(),
        canWrite ? api.listUsers() : Promise.resolve([] as UserSummary[]),
        api.getPrivacyDpiaMethodologyBank(),
      ])
      setRows(dpia)
      setRopas(ropa)
      setUsers(userList.filter((u) => u.roles.includes('CONTRIBUTOR')))
      setBank(methodology)
      setSelected((prev) => (prev ? dpia.find((d) => d.id === prev.id) ?? null : null))
    } catch (e) {
      onError(String(e))
    } finally {
      setLoading(false)
    }
  }, [canWrite, onError])

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

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    if (!selected) return
    setEditQ({
      screeningQ1: selected.screeningQ1,
      screeningQ2: selected.screeningQ2,
      screeningQ3: selected.screeningQ3,
      screeningQ4: selected.screeningQ4,
      screeningQ5: selected.screeningQ5,
      screeningQ6: selected.screeningQ6,
      screeningQ7: selected.screeningQ7,
      processingDescription: selected.processingDescription,
      residualRisk: selected.residualRisk,
    })
    setQuestionAnswers(parseQuestionsJson(selected.questionsJson, bank))
    setAssessmentRows(parseAssessmentJson(selected.assessmentJson, bank))
    setModeAStep(0)
  }, [selected, bank])

  const saveMethodology = async () => {
    if (!selected) return
    setSaving(true)
    try {
      await api.updatePrivacyDpia(selected.id, {
        ...editQ,
        questionsJson: serializeQuestions(questionAnswers),
        assessmentJson: serializeAssessment(assessmentRows),
      })
      onOk('DPIA methodology saved')
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setSaving(false)
    }
  }

  const requestBack = useCallback(() => {
    setSelected(null)
    setRejectNotes('')
  }, [])

  const listActions = useMemo(
    () =>
      canWrite ? (
        <Stack direction="row" spacing={1}>
          <Button
            variant="outlined"
            size="small"
            onClick={() =>
              void api
                .downloadPrivacyDpiaTemplate()
                .catch((e) => onError(e instanceof Error ? e.message : 'Download failed'))
            }
          >
            Download Template
          </Button>
        </Stack>
      ) : undefined,
    [canWrite, onError],
  )

  useAppShellPage(
    selected
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
          actions: listActions,
          hideContextLabel: true,
          hideRoleChips: true,
        },
    [selected, listActions, requestBack],
  )

  const linkedRopa = useMemo(
    () => (selected ? (ropas.find((r) => r.id === selected.ropaId) ?? null) : null),
    [selected, ropas],
  )
  const filteredRows = useMemo(
    () =>
      rows.filter((r) => {
        if (statusFilter !== 'ALL' && r.status !== statusFilter) return false
        const q = search.trim().toLowerCase()
        if (!q) return true
        const ropa = ropas.find((x) => x.id === r.ropaId)
        return (
          r.title.toLowerCase().includes(q) ||
          r.displayId.toLowerCase().includes(q) ||
          (ropa?.activityName ?? '').toLowerCase().includes(q)
        )
      }),
    [rows, statusFilter, search, ropas],
  )

  const gateYes =
    !!editQ.screeningQ4 || !!editQ.screeningQ5 || !!editQ.screeningQ6 || !!editQ.screeningQ7
  const formEditable =
    selected != null &&
    selected.intakeMode === 'FORM' &&
    canWrite &&
    (selected.status === 'DRAFT' || selected.status === 'IN_PROGRESS' || selected.status === 'REJECTED')
  const showModeA = selected != null && selected.intakeMode === 'FORM'

  const likelihoodOpts = (bank?.likelihood ?? []).map((v) => ({ value: v, label: v }))
  const impactOpts = (bank?.impact ?? []).map((v) => ({ value: v, label: v }))
  const treatmentOpts = (bank?.treatment ?? []).map((v) => ({ value: v, label: v }))

  const updateAnswer = (number: number, answer: string) => {
    setQuestionAnswers((prev) => prev.map((q) => (q.number === number ? { ...q, answer } : q)))
  }

  const updateAssessment = (number: number, patch: Partial<AssessmentRow>) => {
    setAssessmentRows((prev) => prev.map((r) => (r.number === number ? { ...r, ...patch } : r)))
  }

  if (loading && !selected) return <PageLoading label="Loading DPIAs..." />

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0 }}>
      {!selected && (
        <Stack spacing={1} sx={{ flex: 1, minHeight: 0 }}>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: 'repeat(2, 1fr)', md: 'repeat(4, 1fr)' },
              gap: 1,
            }}
          >
            <KpiCard compact label="DPIAs" value={String(rows.length)} accentColor={brand.gold} />
            <KpiCard
              compact
              label="In Progress"
              value={String(rows.filter((r) => r.status === 'IN_PROGRESS' || r.status === 'DRAFT').length)}
              accentColor={brand.gold}
              onClick={() => setStatusFilter(statusFilter === 'IN_PROGRESS' ? 'ALL' : 'IN_PROGRESS')}
            />
            <KpiCard
              compact
              label="Under Review"
              value={String(rows.filter((r) => r.status === 'UNDER_REVIEW').length)}
              accentColor={rows.some((r) => r.status === 'UNDER_REVIEW') ? semantic.risk.medium : brand.gold}
              onClick={() => setStatusFilter(statusFilter === 'UNDER_REVIEW' ? 'ALL' : 'UNDER_REVIEW')}
            />
            <KpiCard
              compact
              label="Approved"
              value={String(rows.filter((r) => r.status === 'APPROVED').length)}
              accentColor={semantic.risk.low}
              onClick={() => setStatusFilter(statusFilter === 'APPROVED' ? 'ALL' : 'APPROVED')}
            />
          </Box>
          <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
            <RegisterToolbar
              search={search}
              searchPlaceholder="Search DPIA or ROPA activity"
              onSearchChange={setSearch}
              onClearFilters={() => {
                setSearch('')
                setStatusFilter('ALL')
              }}
              filters={[
                {
                  id: 'status',
                  label: 'Status',
                  value: statusFilter,
                  options: [
                    { value: 'ALL', label: 'All' },
                    ...['DRAFT', 'IN_PROGRESS', 'ASSIGNED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED'].map(
                      (s) => ({ value: s, label: statusLabel(s) }),
                    ),
                  ],
                  onChange: setStatusFilter,
                  minWidth: 160,
                },
              ]}
            />
            <TableContainer sx={{ flex: 1, minHeight: 0 }}>
              <Table size="small" stickyHeader>
                <TableHead>
                  <TableRow>
                    <TableCell>ID</TableCell>
                    <TableCell>Title</TableCell>
                    <TableCell>Source ROPA</TableCell>
                    <TableCell>Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredRows.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={4}>
                        <Typography color="text.secondary" sx={{ py: 2 }}>
                          {rows.length === 0
                            ? 'No DPIAs yet. A DPIA is created when Admin marks DPIA required on a ROPA process review.'
                            : 'No DPIAs match these filters.'}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                  {filteredRows.map((r) => {
                    const ropa = ropas.find((x) => x.id === r.ropaId) ?? null
                    return (
                      <TableRow key={r.id} hover sx={{ cursor: 'pointer' }} onClick={() => setSelected(r)}>
                        <TableCell>{r.displayId}</TableCell>
                        <TableCell>{r.title}</TableCell>
                        <TableCell>{ropa ? `${ropa.displayId}: ${ropa.activityName}` : '-'}</TableCell>
                        <TableCell>
                          <StatusChip label={statusLabel(r.status)} tone={toneForStatus(r.status)} />
                        </TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          </Card>
        </Stack>
      )}

      {selected && (
        <Card sx={{ p: 2, overflow: 'auto', flex: 1 }}>
          <Stack spacing={1.5}>
            <Box>
              <Typography sx={{ fontSize: '0.875rem', fontWeight: 650, color: brand.ink, lineHeight: 1.3 }}>
                {selected.displayId}: {selected.title}
              </Typography>
              <Stack direction="row" spacing={1} sx={{ mt: 0.5 }} flexWrap="wrap" useFlexGap>
                <StatusChip label={statusLabel(selected.status)} tone={toneForStatus(selected.status)} />
                {linkedRopa && (
                  <StatusChip label={`Source ROPA ${linkedRopa.displayId}: ${linkedRopa.activityName}`} tone="neutral" />
                )}
                {selected.fullDpiaMandatory && <StatusChip label="Full DPIA mandatory" tone="warning" />}
              </Stack>
            </Box>

            {canWrite && selected.status !== 'CANCELLED' && (
              <SettingsCard autoHeight title="Privacy Risk" subtitle="Privacy risks are mainly created here, from the DPIA">
                <Typography variant="body2" color="text.secondary">
                  Raise a Privacy Risk from this DPIA. It is added to the Privacy Risk register (Privacy -
                  Privacy Risk), where a Privacy Risk can also be added manually.
                </Typography>
                  <Stack spacing={1} sx={{ mt: 1.5 }}>
                    <CompactTextField
                      label="Risk title"
                      fieldIdPrefix="dpia-raise"
                      value={raiseTitle || `Privacy risk from ${selected.displayId}: ${selected.title}`}
                      onChange={(e) => setRaiseTitle(e.target.value)}
                    />
                    <Button
                      variant="contained"
                      disabled={raiseBusy}
                      onClick={async () => {
                        setRaiseBusy(true)
                        try {
                          const riskProjectId = await ensurePrivacyRiskProject()
                          const created = await api.createRisk({
                            riskType: 'PRIVACY_RISK',
                            title: (raiseTitle || `Privacy risk from ${selected.displayId}: ${selected.title}`).trim(),
                            description: selected.processingDescription ?? selected.title,
                            category: 'IT',
                            orgUnitId: linkedRopa?.orgUnitId ?? null,
                            observedGap: selected.risksIdentified ?? `Raised from DPIA ${selected.displayId}`,
                            threats: 'Privacy breach',
                            probability: 'MEDIUM',
                            impact: 'MEDIUM',
                            controlEffectiveness: 'MEDIUM',
                            treatmentOption: 'TREAT',
                            treatmentJustification: selected.mitigationMeasures ?? 'Review and treat the privacy risk from the DPIA',
                            riskProjectId,
                            sourceDpiaId: selected.id,
                            sourceRopaId: selected.ropaId ?? null,
                            dateIdentified: new Date().toISOString().slice(0, 10),
                          })
                          onOk(`Privacy Risk created: ${created.displayId}`)
                        } catch (e) {
                          onError(e instanceof Error ? e.message : 'Failed to raise Privacy Risk')
                        } finally {
                          setRaiseBusy(false)
                        }
                      }}
                    >
                      {raiseBusy ? 'Creating...' : 'Raise Privacy Risk'}
                    </Button>
                  </Stack>
              </SettingsCard>
            )}

            {showModeA && (
              <Stack spacing={1.5}>
                <Stepper nonLinear activeStep={modeAStep} alternativeLabel>
                  {MODE_A_STEPS.map((label, i) => (
                    <Step key={label} completed={false}>
                      <StepButton color="inherit" onClick={() => setModeAStep(i)}>
                        {label}
                      </StepButton>
                    </Step>
                  ))}
                </Stepper>

                {modeAStep === 0 && (
                  <Stack spacing={1}>
                    <Typography variant="subtitle2" fontWeight={700}>
                      Pre-DPIA screening
                    </Typography>
                    <CompactTextField
                      label="Q1: Project / process name"
                      fieldIdPrefix="dpia-edit"
                      value={editQ.screeningQ1 ?? ''}
                      onChange={(e) => setEditQ({ ...editQ, screeningQ1: e.target.value })}
                      disabled={!formEditable}
                    />
                    <CompactTextField
                      label="Q2: Functions involved"
                      fieldIdPrefix="dpia-edit"
                      value={editQ.screeningQ2 ?? ''}
                      onChange={(e) => setEditQ({ ...editQ, screeningQ2: e.target.value })}
                      disabled={!formEditable}
                    />
                    <FormControlLabel
                      control={
                        <Checkbox
                          size="small"
                          checked={editQ.screeningQ3 !== false}
                          onChange={(e) => setEditQ({ ...editQ, screeningQ3: e.target.checked })}
                          disabled={!formEditable}
                        />
                      }
                      label={<Typography sx={{ fontSize: '0.75rem' }}>Q3: Processes personal data</Typography>}
                    />
                    {DPIA_SCREENING_Q4_TO_Q7.map((q, i) => {
                      const key = (`screeningQ${i + 4}` as
                        | 'screeningQ4'
                        | 'screeningQ5'
                        | 'screeningQ6'
                        | 'screeningQ7')
                      return (
                        <FormControlLabel
                          key={key}
                          control={
                            <Checkbox
                              size="small"
                              checked={!!editQ[key]}
                              onChange={(e) => setEditQ({ ...editQ, [key]: e.target.checked })}
                              disabled={!formEditable}
                            />
                          }
                          label={
                            <Typography sx={{ fontSize: '0.75rem' }}>
                              Q{i + 4}: {q}
                            </Typography>
                          }
                        />
                      )
                    })}
                    {gateYes && (
                      <Typography variant="body2" color="warning.main">
                        Any Yes on Q4–7 ⇒ full DPIA mandatory (linked RoPA set to DPIA Required).
                      </Typography>
                    )}
                    <FormSelect
                      label="Residual risk"
                      value={editQ.residualRisk ?? selected.residualRisk ?? 'MEDIUM'}
                      options={[
                        { value: 'LOW', label: 'Low' },
                        { value: 'MEDIUM', label: 'Medium' },
                        { value: 'HIGH', label: 'High' },
                      ]}
                      onChange={(v) => setEditQ({ ...editQ, residualRisk: v as DpiaResidualRisk })}
                      fullWidth
                      disabled={!formEditable}
                    />
                  </Stack>
                )}

                {modeAStep === 1 && (
                  <Stack spacing={1.25}>
                    <Typography variant="body2" color="text.secondary">
                      Answer each methodology question. Grouped by privacy principle from the PIMS bank.
                    </Typography>
                    {(bank?.questions ?? []).map((q) => {
                      const answer = questionAnswers.find((a) => a.number === q.number)?.answer ?? ''
                      return (
                        <CompactTextField
                          key={q.number}
                          label={`Q${q.number} · ${q.principle}: ${q.question}`}
                          fieldIdPrefix="dpia-q"
                          multiline
                          minRows={2}
                          value={answer}
                          onChange={(e) => updateAnswer(q.number, e.target.value)}
                          disabled={!formEditable}
                        />
                      )
                    })}
                    {!bank?.questions?.length && (
                      <Typography variant="body2" color="text.secondary">
                        Methodology bank unavailable: reopen after reload.
                      </Typography>
                    )}
                  </Stack>
                )}

                {modeAStep === 2 && (
                  <Stack spacing={1}>
                    <Typography variant="body2" color="text.secondary">
                      Rate each principle (likelihood × impact) and choose a treatment.
                    </Typography>
                    <TableContainer sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1 }}>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell width={160}>Principle</TableCell>
                            <TableCell>Comments</TableCell>
                            <TableCell width={130}>Likelihood</TableCell>
                            <TableCell width={120}>Impact</TableCell>
                            <TableCell width={120}>Treatment</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {(bank?.principles ?? []).map((p) => {
                            const row =
                              assessmentRows.find((r) => r.number === p.number) ??
                              ({
                                number: p.number,
                                comments: '',
                                likelihood: '',
                                impact: '',
                                treatment: '',
                              } satisfies AssessmentRow)
                            return (
                              <TableRow key={p.number} sx={{ verticalAlign: 'top' }}>
                                <TableCell>
                                  <Typography variant="body2" fontWeight={600}>
                                    {p.number}. {p.name}
                                  </Typography>
                                  <Typography variant="caption" color="text.secondary">
                                    {p.description}
                                  </Typography>
                                </TableCell>
                                <TableCell>
                                  <CompactTextField
                                    label="Comments"
                                    fieldIdPrefix={`dpia-a-${p.number}`}
                                    multiline
                                    minRows={2}
                                    value={row.comments}
                                    onChange={(e) => updateAssessment(p.number, { comments: e.target.value })}
                                    disabled={!formEditable}
                                  />
                                </TableCell>
                                <TableCell>
                                  <FormSelect
                                    label="Likelihood"
                                    value={row.likelihood}
                                    options={[{ value: '', label: '-' }, ...likelihoodOpts]}
                                    onChange={(v) => updateAssessment(p.number, { likelihood: v })}
                                    fullWidth
                                    disabled={!formEditable}
                                  />
                                </TableCell>
                                <TableCell>
                                  <FormSelect
                                    label="Impact"
                                    value={row.impact}
                                    options={[{ value: '', label: '-' }, ...impactOpts]}
                                    onChange={(v) => updateAssessment(p.number, { impact: v })}
                                    fullWidth
                                    disabled={!formEditable}
                                  />
                                </TableCell>
                                <TableCell>
                                  <FormSelect
                                    label="Treatment"
                                    value={row.treatment}
                                    options={[{ value: '', label: '-' }, ...treatmentOpts]}
                                    onChange={(v) => updateAssessment(p.number, { treatment: v })}
                                    fullWidth
                                    disabled={!formEditable}
                                  />
                                </TableCell>
                              </TableRow>
                            )
                          })}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  </Stack>
                )}

                <Stack direction="row" spacing={1} justifyContent="space-between" alignItems="center">
                  <Stack direction="row" spacing={1}>
                    <Button
                      variant="outlined"
                      size="small"
                      disabled={modeAStep === 0}
                      onClick={() => setModeAStep((s) => Math.max(0, s - 1))}
                    >
                      Back
                    </Button>
                    <Button
                      variant="outlined"
                      size="small"
                      disabled={modeAStep >= MODE_A_STEPS.length - 1}
                      onClick={() => setModeAStep((s) => Math.min(MODE_A_STEPS.length - 1, s + 1))}
                    >
                      Next
                    </Button>
                  </Stack>
                  {formEditable && (
                    <Button variant="contained" size="small" disabled={saving} onClick={() => void saveMethodology()}>
                      Save Methodology
                    </Button>
                  )}
                </Stack>
              </Stack>
            )}

            {canWrite &&
              selected.intakeMode === 'WORKBOOK' &&
              (selected.status === 'DRAFT' || selected.status === 'REJECTED' || selected.status === 'ASSIGNED') && (
                <Stack spacing={1} sx={{ p: 1.5, border: '1px solid', borderColor: 'divider', borderRadius: 1 }}>
                  <Typography variant="subtitle2">Assign Contributor (Mode B)</Typography>
                  <FormSelect
                    label="Contributor"
                    value={assignUserId === '' ? '' : String(assignUserId)}
                    options={[
                      { value: '', label: '-' },
                      ...users.map((u) => ({ value: String(u.id), label: `${u.fullName} (${u.email})` })),
                    ]}
                    onChange={(v) => setAssignUserId(v ? Number(v) : '')}
                    fullWidth
                  />
                  <CompactTextField
                    label="Due date (YYYY-MM-DD)"
                    fieldIdPrefix="dpia-assign"
                    value={assignDue}
                    onChange={(e) => setAssignDue(e.target.value)}
                  />
                  <Button
                    variant="contained"
                    disabled={assignUserId === ''}
                    onClick={() =>
                      void api
                        .assignPrivacyDpia(selected.id, {
                          userId: Number(assignUserId),
                          dueDate: assignDue || null,
                        })
                        .then(() => {
                          onOk('Assigned')
                          void load()
                        })
                        .catch((e) => onError(String(e)))
                    }
                  >
                    Assign
                  </Button>
                </Stack>
              )}

            {formEditable && (
              <Button
                size="small"
                variant="contained"
                onClick={() =>
                  void api
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
              <SettingsCard autoHeight title="DPO Decision">
                <Stack spacing={1}>
                  <CompactTextField
                    label="Rejection notes"
                    fieldIdPrefix="dpia-reject"
                    multiline
                    minRows={2}
                    value={rejectNotes}
                    onChange={(e) => setRejectNotes(e.target.value)}
                  />
                  <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                    <Button
                      size="small"
                      variant="contained"
                      onClick={() =>
                        void api
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
                      variant="outlined"
                      disabled={!rejectNotes.trim()}
                      onClick={() =>
                        void api
                          .rejectPrivacyDpia(selected.id, rejectNotes.trim())
                          .then(() => {
                            setRejectNotes('')
                            onOk('Rejected')
                            void load()
                          })
                          .catch((e) => onError(String(e)))
                      }
                    >
                      Reject / Send Back
                    </Button>
                    {!rejectNotes.trim() && (
                      <Typography variant="caption" color="text.secondary">
                        Rejection notes are required to send back
                      </Typography>
                    )}
                  </Stack>
                </Stack>
              </SettingsCard>
            )}
          </Stack>
        </Card>
      )}
    </Stack>
  )
}
