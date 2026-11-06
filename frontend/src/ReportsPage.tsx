import { useCallback, useEffect, useMemo, useState } from 'react'
import { frameworkDisplayName } from './uiQuality/frameworkLabel'
import {
  Box,
  Button,
  Card,
  FormControl,
  MenuItem,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material'
import DownloadRoundedIcon from '@mui/icons-material/DownloadRounded'
import {
  api,
  type GapAssessmentSummary,
  type GrcAuditSummary,
  type BcmsExercise,
  type ReportDescriptor,
  type Session,
  type SoaSummary,
} from './api'
import StatusChip from './components/StatusChip'
import { RegisterListFrame, RegisterSortHeader } from './components/ui'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { compareLoose, toggleSort, type SortDir } from './uiQuality/sortRows'
import { ModuleListShell } from './hooks/useModulePageShell'
import { brand } from './theme'

const REPORT_DESCRIPTIONS: Record<string, string> = {
  compliance_posture: 'Cross-module snapshot of all Gap Assessments: score, RAG, status.',
  soa: 'Statement of Applicability for a specific project + framework.',
  gap: 'Gap Assessment scoring and control-level detail for a specific assessment.',
  uae_ia_compliance:
    'UAE IA V2.1 NESA/TDRA pack: Management / Technical / domain layer scores and control detail.',
  is_risk: 'Full Information Security Risk register: inherent, control effectiveness, residual, treatment.',
  erm_risk: 'Enterprise risk summary: board-level residual and treatment status.',
  tprm_vendor_risk: 'Vendor register: type, tier, criticality, posture, and compliance.',
  tprm_categorisation: 'F1–F6 scores, inherent, assurance credit, adjusted, override.',
  tprm_questionnaire_responses: 'One row per control response and review verdict.',
  tprm_posture: 'Posture percent versus the pack tier target.',
  tprm_findings: 'Open and closed findings from failed or downgraded controls.',
  bia_summary: 'BIA processes: RTO / RPO / MTPD and criticality.',
  evidence_expiry: 'Active evidence expiring within 30 days or already expired.',
  user_activity: 'Recent audit-log activity (logins, changes, exports).',
  privacy_dsr: 'Data subject requests: type, due date, PDPL status.',
  capa_status: 'All CAPA registers: open, in progress, pending verification, closed, overdue.',
  audit_trail: 'Full insert-only audit log for this workspace.',
  internal_audit_report: 'Locked dual-signed Internal Audit PDF (generated on Admin second sign).',
  external_audit_report: 'Locked dual-signed External Audit PDF (generated on Admin second sign).',
  executive_grc_summary: 'Board-level GRC snapshot: internal (full) or external (summary) PDF.',
  ncema_compliance: 'NCEMA V3 regulatory pack: pillar scores and control detail.',
  bcp_summary: 'Approved recovery plans summary with signature block.',
  exercise_report: 'Completed exercise results, gaps, and lessons learned.',
}

function isUaeIaGap(g: GapAssessmentSummary): boolean {
  const code = (g.frameworkCode ?? '').toUpperCase()
  const name = (g.frameworkName ?? '').toUpperCase()
  if (code.includes('NCEMA') || name.includes('NCEMA')) return false
  return code.includes('UAE') || name.includes('UAE IA') || g.layerScores?.management != null
}

function isNcemaGap(g: GapAssessmentSummary): boolean {
  const code = (g.frameworkCode ?? '').toUpperCase()
  const name = (g.frameworkName ?? '').toUpperCase()
  return code.includes('NCEMA') || name.includes('NCEMA')
}

export default function ReportsPage({
  session,
  onError,
  onOk,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const canExport = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))

  const [reports, setReports] = useState<ReportDescriptor[]>([])
  const [soas, setSoas] = useState<SoaSummary[]>([])
  const [gaps, setGaps] = useState<GapAssessmentSummary[]>([])
  const [audits, setAudits] = useState<GrcAuditSummary[]>([])
  const [selectedSoaId, setSelectedSoaId] = useState<number | ''>('')
  const [selectedGapId, setSelectedGapId] = useState<number | ''>('')
  const [selectedUaeGapId, setSelectedUaeGapId] = useState<number | ''>('')
  const [selectedNcemaGapId, setSelectedNcemaGapId] = useState<number | ''>('')
  const [selectedExerciseId, setSelectedExerciseId] = useState<number | ''>('')
  const [executiveVariant, setExecutiveVariant] = useState<'internal' | 'external'>('internal')
  const [exercises, setExercises] = useState<BcmsExercise[]>([])
  const [selectedInternalAuditId, setSelectedInternalAuditId] = useState<number | ''>('')
  const [selectedExternalAuditId, setSelectedExternalAuditId] = useState<number | ''>('')
  const [busyId, setBusyId] = useState<string | null>(null)
  const { listReady, markReady } = useListReady()
  const [sortKey, setSortKey] = useState('name')
  const [sortDir, setSortDir] = useState<SortDir>('asc')

  const load = useCallback(async () => {
    try {
      setReports(await api.listReports())
    } catch (e) {
      onError(String(e))
    } finally {
      markReady()
    }
  }, [onError, markReady])

  useEffect(() => {
    load()
  }, [load])

  const hasIsms = useMemo(
    () => reports.some((r) => r.id === 'soa' || r.id === 'gap' || r.id === 'uae_ia_compliance' || r.id === 'ncema_compliance'),
    [reports],
  )
  const hasBcmsReports = useMemo(
    () => reports.some((r) => r.id === 'ncema_compliance' || r.id === 'exercise_report'),
    [reports],
  )
  const hasAuditReports = useMemo(
    () => reports.some((r) => r.id === 'internal_audit_report' || r.id === 'external_audit_report'),
    [reports],
  )

  useEffect(() => {
    if (!hasIsms) return
    api.listSoas().then(setSoas).catch(() => setSoas([]))
    api.listGapAssessments().then(setGaps).catch(() => setGaps([]))
  }, [hasIsms])

  useEffect(() => {
    if (!hasAuditReports) return
    api.listAudits().then(setAudits).catch(() => setAudits([]))
  }, [hasAuditReports])

  useEffect(() => {
    if (!hasBcmsReports) return
    api.listExercises().then(setExercises).catch(() => setExercises([]))
  }, [hasBcmsReports])

  const uaeGaps = useMemo(() => gaps.filter(isUaeIaGap), [gaps])
  const ncemaGaps = useMemo(() => gaps.filter(isNcemaGap), [gaps])
  const completedExercises = useMemo(
    () => exercises.filter((e) => e.status === 'COMPLETED'),
    [exercises],
  )
  const lockedInternal = useMemo(
    () => audits.filter((a) => a.auditType === 'INTERNAL' && a.locked),
    [audits],
  )
  const lockedExternal = useMemo(
    () => audits.filter((a) => a.auditType === 'EXTERNAL' && a.locked),
    [audits],
  )

  const runExport = async (report: ReportDescriptor, format: string) => {
    if (report.requiresProject) {
      if (report.id === 'soa' && !selectedSoaId) {
        onError('Pick a Statement of Applicability first')
        return
      }
      if (report.id === 'gap' && !selectedGapId) {
        onError('Pick a Gap Assessment first')
        return
      }
      if (report.id === 'uae_ia_compliance' && !selectedUaeGapId) {
        onError('Pick a UAE IA Gap Assessment first')
        return
      }
      if (report.id === 'ncema_compliance' && !selectedNcemaGapId) {
        onError('Pick a NCEMA Gap Assessment first')
        return
      }
      if (report.id === 'exercise_report' && !selectedExerciseId) {
        onError('Pick a completed exercise first')
        return
      }
      if (report.id === 'internal_audit_report' && !selectedInternalAuditId) {
        onError('Pick a locked Internal Audit first')
        return
      }
      if (report.id === 'external_audit_report' && !selectedExternalAuditId) {
        onError('Pick a locked External Audit first')
        return
      }
    }
    setBusyId(report.id)
    try {
      await api.exportReport(report.id, format, {
        soaId: report.id === 'soa' && selectedSoaId ? Number(selectedSoaId) : undefined,
        gapId:
          report.id === 'gap' && selectedGapId
            ? Number(selectedGapId)
            : report.id === 'uae_ia_compliance' && selectedUaeGapId
              ? Number(selectedUaeGapId)
              : report.id === 'ncema_compliance' && selectedNcemaGapId
                ? Number(selectedNcemaGapId)
                : undefined,
        auditId:
          report.id === 'internal_audit_report' && selectedInternalAuditId
            ? Number(selectedInternalAuditId)
            : report.id === 'external_audit_report' && selectedExternalAuditId
              ? Number(selectedExternalAuditId)
              : undefined,
        exerciseId:
          report.id === 'exercise_report' && selectedExerciseId
            ? Number(selectedExerciseId)
            : undefined,
        variant: report.id === 'executive_grc_summary' ? executiveVariant : undefined,
      })
      onOk(`${report.name} exported`)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusyId(null)
    }
  }

  const sortedReports = useMemo(
    () => [...reports].sort((a, b) => compareLoose(a.name, b.name, sortDir)),
    [reports, sortDir],
  )
  const paged = usePagedRows(sortedReports)

  const recordSelector = (r: ReportDescriptor) => {
    if (r.id === 'soa') {
      return (
        <FormControl size="small" fullWidth disabled={soas.length === 0}>
          <Select
            value={selectedSoaId}
            displayEmpty
            onChange={(e) => setSelectedSoaId(e.target.value as number)}
          >
            <MenuItem value="">
              <em>{soas.length === 0 ? 'No SoAs yet' : 'Choose a SoA…'}</em>
            </MenuItem>
            {soas.map((s) => (
              <MenuItem key={s.id} value={s.id}>
                {s.frameworkName} · {s.frameworkVersion} · {s.status}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      )
    }
    if (r.id === 'gap') {
      return (
        <FormControl size="small" fullWidth disabled={gaps.length === 0}>
          <Select
            value={selectedGapId}
            displayEmpty
            onChange={(e) => setSelectedGapId(e.target.value as number)}
          >
            <MenuItem value="">
              <em>{gaps.length === 0 ? 'No Gap assessments yet' : 'Choose a Gap assessment…'}</em>
            </MenuItem>
            {gaps.map((g) => (
              <MenuItem key={g.id} value={g.id}>
                {g.name} · {g.status}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      )
    }
    if (r.id === 'uae_ia_compliance') {
      return (
        <FormControl size="small" fullWidth disabled={uaeGaps.length === 0}>
          <Select
            value={selectedUaeGapId}
            displayEmpty
            onChange={(e) => setSelectedUaeGapId(e.target.value as number)}
          >
            <MenuItem value="">
              <em>{uaeGaps.length === 0 ? 'No UAE IA Gap assessments yet' : 'Choose a UAE IA Gap…'}</em>
            </MenuItem>
            {uaeGaps.map((g) => (
              <MenuItem key={g.id} value={g.id}>
                {g.name} · {frameworkDisplayName(undefined, g.frameworkCode)} · {g.status}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      )
    }
    if (r.id === 'internal_audit_report') {
      return (
        <FormControl size="small" fullWidth disabled={lockedInternal.length === 0}>
          <Select
            value={selectedInternalAuditId}
            displayEmpty
            onChange={(e) => setSelectedInternalAuditId(e.target.value as number)}
          >
            <MenuItem value="">
              <em>{lockedInternal.length === 0 ? 'No locked internal audits yet' : 'Choose a locked audit…'}</em>
            </MenuItem>
            {lockedInternal.map((a) => (
              <MenuItem key={a.id} value={a.id}>
                {a.name} · {frameworkDisplayName(undefined, a.frameworkCode)}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      )
    }
    if (r.id === 'external_audit_report') {
      return (
        <FormControl size="small" fullWidth disabled={lockedExternal.length === 0}>
          <Select
            value={selectedExternalAuditId}
            displayEmpty
            onChange={(e) => setSelectedExternalAuditId(e.target.value as number)}
          >
            <MenuItem value="">
              <em>{lockedExternal.length === 0 ? 'No locked external audits yet' : 'Choose a locked audit…'}</em>
            </MenuItem>
            {lockedExternal.map((a) => (
              <MenuItem key={a.id} value={a.id}>
                {a.name} · {frameworkDisplayName(undefined, a.frameworkCode)}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      )
    }
    if (r.id === 'ncema_compliance') {
      return (
        <FormControl size="small" fullWidth disabled={ncemaGaps.length === 0}>
          <Select
            value={selectedNcemaGapId}
            displayEmpty
            onChange={(e) => setSelectedNcemaGapId(e.target.value as number)}
          >
            <MenuItem value="">
              <em>{ncemaGaps.length === 0 ? 'No NCEMA Gap assessments yet' : 'Choose a NCEMA Gap…'}</em>
            </MenuItem>
            {ncemaGaps.map((g) => (
              <MenuItem key={g.id} value={g.id}>
                {g.name} · {frameworkDisplayName(undefined, g.frameworkCode)} · {g.status}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      )
    }
    if (r.id === 'exercise_report') {
      return (
        <FormControl size="small" fullWidth disabled={completedExercises.length === 0}>
          <Select
            value={selectedExerciseId}
            displayEmpty
            onChange={(e) => setSelectedExerciseId(e.target.value as number)}
          >
            <MenuItem value="">
              <em>{completedExercises.length === 0 ? 'No completed exercises yet' : 'Choose an exercise…'}</em>
            </MenuItem>
            {completedExercises.map((e) => (
              <MenuItem key={e.id} value={e.id}>
                {e.name} · {e.exerciseType}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      )
    }
    if (r.id === 'executive_grc_summary') {
      return (
        <FormControl size="small" fullWidth>
          <Select
            value={executiveVariant}
            onChange={(e) => setExecutiveVariant(e.target.value as 'internal' | 'external')}
          >
            <MenuItem value="internal">Internal (full detail)</MenuItem>
            <MenuItem value="external">External (summary)</MenuItem>
          </Select>
        </FormControl>
      )
    }
    return <Typography variant="caption" color="text.secondary">-</Typography>
  }

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
      <ModuleListShell title="Reports" breadcrumbTrail="Reports" />

      <Card sx={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
        <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
        <RegisterListFrame
          ready={listReady}
          count={paged.count}
          emptyMessage="No reports available yet."
          page={paged.page}
          rowsPerPage={paged.rowsPerPage}
          onPageChange={paged.setPage}
          onRowsPerPageChange={paged.setRowsPerPage}
        >
        <TableContainer sx={{ flex: 1, minHeight: 0 }}>
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow>
                <RegisterSortHeader
                  label="Report"
                  active={sortKey === 'name'}
                  dir={sortDir}
                  onClick={() => {
                    const next = toggleSort(sortKey, sortDir, 'name')
                    setSortKey(next.key)
                    setSortDir(next.dir)
                  }}
                />
                <TableCell width={90}>Module</TableCell>
                <TableCell width={110}>Formats</TableCell>
                <TableCell width={340}>Select record</TableCell>
                <TableCell align="right" width={200} sx={{ pr: 2 }}>Export</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {paged.paged.map((r) => (
                <TableRow key={r.id} hover>
                  <TableCell sx={{ pl: 2 }}>
                    <Typography variant="body2" fontWeight={650}>{r.name}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {REPORT_DESCRIPTIONS[r.id] ?? ''}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <StatusChip label={r.module} tone="neutral" />
                  </TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                      {r.formats.map((f) => (
                        <StatusChip key={f} label={f.toUpperCase()} tone="info" />
                      ))}
                    </Stack>
                  </TableCell>
                  <TableCell>{recordSelector(r)}</TableCell>
                  <TableCell align="right" sx={{ pr: 2 }}>
                    <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                      {r.formats.map((f) => (
                        <Tooltip key={f} title={canExport ? '' : 'Export requires Owner/Admin/Auditor role'}>
                          <span>
                            <Button
                              size="small"
                              variant="outlined"
                              startIcon={<DownloadRoundedIcon sx={{ fontSize: 15 }} />}
                              disabled={!canExport || busyId === r.id}
                              onClick={() => runExport(r, f)}
                            >
                              {busyId === r.id ? 'Working…' : f.toUpperCase()}
                            </Button>
                          </span>
                        </Tooltip>
                      ))}
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
        </RegisterListFrame>
        </Box>
        <Box sx={{ px: 2, py: 1, borderTop: `1px solid ${brand.hairline}`, flexShrink: 0 }}>
          <Typography variant="caption" color="text.secondary">
            Hub includes UAE IA compliance and locked Internal/External Audit PDFs. Still deferred:
            Executive Summary, NCEMA pillars, BCP Summary / Exercise packs, and scheduled delivery.
            Global Reader can view this list but cannot export.
          </Typography>
        </Box>
      </Card>
    </Stack>
  )
}
