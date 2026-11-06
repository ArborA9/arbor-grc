import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Box, Card, Collapse, IconButton, Stack, Typography, alpha } from '@mui/material'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded'
import AssignmentIndRoundedIcon from '@mui/icons-material/AssignmentIndRounded'
import ApartmentRoundedIcon from '@mui/icons-material/ApartmentRounded'
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded'
import CheckCircleOutlineRoundedIcon from '@mui/icons-material/CheckCircleOutlineRounded'
import SecurityRoundedIcon from '@mui/icons-material/SecurityRounded'
import HealthAndSafetyRoundedIcon from '@mui/icons-material/HealthAndSafetyRounded'
import GavelRoundedIcon from '@mui/icons-material/GavelRounded'
import PrivacyTipRoundedIcon from '@mui/icons-material/PrivacyTipRounded'
import {
  api,
  type DashboardSummary,
  type DashboardWidget,
  type RecentActivityItem,
  type Session,
  type WidgetTone,
} from './api'
import { brand, layout, semantic } from './theme'
import { DashboardRiskHeatMaps, type RiskHeatMapDrill } from './RiskHeatMap'
import type { RiskType } from './api'
import { SectionHeader } from './components/ui'
import { ModuleListShell } from './hooks/useModulePageShell'
import { formatTenantDateTime, getTenantLocale } from './uiQuality/tenantLocale'
import { isRiskNavHidden } from './components/risk/riskNav'
import { isBcmsNavPending } from './components/bcms/bcmsNav'
import { isPrivacyNavPending } from './components/privacy/privacyNav'

const REFRESH_MS = 5 * 60 * 1000

const TONE_COLORS: Record<WidgetTone, string> = {
  success: semantic.risk.low,
  warning: semantic.risk.medium,
  info: brand.gold,
  neutral: brand.muted,
  danger: semantic.risk.critical,
}

/** One representative KPI per commercial module for the posture strip. */
const POSTURE_WIDGET_IDS: { module: string; label: string; preferIds: string[]; icon: ReactNode }[] = [
  {
    module: 'ISMS',
    label: 'ISMS',
    preferIds: [
      'overall_compliance_score',
      'overdue_is_risks',
      'open_is_risks',
      'soa_completion_pct',
      'open_capa_gap_register',
      'gap_in_progress',
    ],
    icon: <SecurityRoundedIcon />,
  },
  {
    module: 'ERM',
    label: 'ERM / TPRM',
    preferIds: [
      'critical_open_risks',
      'overdue_erm_risks',
      'vendors_review_due',
      'issued_questionnaires',
      'high_risk_vendors',
      'open_erm_risks',
      'active_vendors',
    ],
    icon: <SecurityRoundedIcon />,
  },
  {
    module: 'BCMS',
    label: 'BCMS',
    preferIds: ['bia_approved_pct', 'bia_critical_rto_under_4h', 'recovery_plans_approved'],
    icon: <HealthAndSafetyRoundedIcon />,
  },
  {
    module: 'AUDIT',
    label: 'Audit',
    preferIds: ['open_findings', 'audits_in_progress'],
    icon: <GavelRoundedIcon />,
  },
  {
    module: 'PRIVACY',
    label: 'Privacy',
    preferIds: ['overdue_dsrs', 'dsrs_approaching_deadline', 'open_dsrs'],
    icon: <PrivacyTipRoundedIcon />,
  },
]

function isZero(value: string) {
  const n = Number(String(value).replace('%', ''))
  return Number.isFinite(n) && n === 0
}

function needsAttention(w: DashboardWidget) {
  return w.tone === 'danger' || w.tone === 'warning'
}

const TONE_RANK: Record<WidgetTone, number> = {
  danger: 0,
  warning: 1,
  info: 2,
  success: 3,
  neutral: 4,
}

/** Shown in the Work cards, so not repeated under Decide today. */
const WORK_CARD_IDS = new Set(['my_tasks_pending', 'open_work_tasks'])

const MODULE_LABELS: Record<string, string> = {
  PLATFORM: 'Platform',
  ISMS: 'Information Security',
  ERM: 'Enterprise Risk',
  TPRM: 'Third-Party Risk',
  BCMS: 'Business Continuity',
  AUDIT: 'Audit',
  PRIVACY: 'Privacy',
  POLICY: 'Policies',
  EVIDENCE: 'Evidence',
}

function moduleLabel(code: string | null | undefined): string {
  if (!code) return ''
  return MODULE_LABELS[code] ?? code.charAt(0) + code.slice(1).toLowerCase()
}

function attentionWidgets(widgets: DashboardWidget[]) {
  return widgets
    .filter((w) => needsAttention(w) && w.id !== 'risk_heat_map' && !WORK_CARD_IDS.has(w.id))
    .sort((a, b) => (TONE_RANK[a.tone] ?? 9) - (TONE_RANK[b.tone] ?? 9))
}

function isHealthy(w: DashboardWidget) {
  return isZero(w.value) && !!w.emptyMessage && (w.tone === 'success' || w.tone === 'neutral')
}

/** Most urgent of the module's widgets; ties keep the `ids` order. */
function pickWidget(byId: Map<string, DashboardWidget>, ids: string[]) {
  let best: DashboardWidget | null = null
  for (const id of ids) {
    const w = byId.get(id)
    if (w && (best == null || (TONE_RANK[w.tone] ?? 9) < (TONE_RANK[best.tone] ?? 9))) best = w
  }
  return best
}

function postureWord(tone: WidgetTone): 'Critical' | 'Watch' | 'Clear' {
  if (tone === 'danger') return 'Critical'
  if (tone === 'warning') return 'Watch'
  return 'Clear'
}

function percentValue(value: string): number | null {
  const raw = value.trim()
  if (!raw.endsWith('%')) return null
  const n = Number(raw.slice(0, -1))
  if (!Number.isFinite(n)) return null
  return Math.max(0, Math.min(100, n))
}

function briefingWhen(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ''
  const zone = getTenantLocale().timezone || 'Asia/Dubai'
  return new Intl.DateTimeFormat('en-GB', {
    timeZone: zone,
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(date)
}

const ACTIVITY_VERBS: Record<string, string> = {
  CREATED: 'Created',
  UPDATED: 'Updated',
  DELETED: 'Deleted',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  LOGIN: 'Signed in',
  LOGOUT: 'Signed out',
  EXPORT: 'Exported',
  IMPERSONATED: 'Impersonated',
}

const ACTIVITY_MODULES: Record<string, string> = {
  RISK: 'Risk',
  IAM: 'Users & Access',
  AUDIT: 'Audit',
  TPRM: 'TPRM',
  WORKFLOW: 'Workflow',
  REPORTS: 'Reports',
  PRIVACY: 'Privacy',
  PRIVACY_DSR: 'Privacy DSR',
  PRIVACY_BREACH: 'Privacy Breach',
  NOTIFICATION: 'Notification',
  CAPA: 'CAPA',
  GAP: 'Gap Assessment',
  SOA: 'SoA',
  BCMS: 'BCMS',
  POLICY: 'Policies',
  EVIDENCE: 'Evidence',
}

/** `RISK_OWNER_NAMED` -> `Risk owner named`. Leaves normal text alone. */
function humanizeCode(value: string): string {
  if (!/^[A-Z0-9]+(_[A-Z0-9]+)+$/.test(value)) return value
  const words = value.replace(/_/g, ' ').toLowerCase()
  return words.charAt(0).toUpperCase() + words.slice(1)
}

function activityModule(code: string | null): string {
  if (!code) return ''
  return ACTIVITY_MODULES[code] ?? humanizeCode(code)
}

function activityAction(row: RecentActivityItem): string {
  const type = row.actionType ?? 'UPDATED'
  if (type === 'LOGIN' || type === 'LOGOUT') return ACTIVITY_VERBS[type]
  const record = row.recordName?.trim() ? humanizeCode(row.recordName.trim()) : ''
  if (row.module === 'NOTIFICATION') return record ? `Notification sent: ${record}` : 'Notification sent'
  const verb = ACTIVITY_VERBS[type] ?? humanizeCode(type)
  return record ? `${verb} ${record}` : verb
}

function activityMeta(row: RecentActivityItem): string {
  const parts = [formatTenantDateTime(row.at)]
  if (row.actorEmail) parts.push(row.actorEmail)
  const hideModule = row.module === 'NOTIFICATION' || row.actionType === 'LOGIN' || row.actionType === 'LOGOUT'
  const module = hideModule ? '' : activityModule(row.module)
  if (module) parts.push(module)
  return parts.join(' · ')
}

function PostureCell({
  label,
  icon,
  widget,
  onNavigate,
}: {
  label: string
  icon: ReactNode
  widget: DashboardWidget
  onNavigate: (id: string) => void
}) {
  const word = postureWord(widget.tone)
  const color = TONE_COLORS[widget.tone] ?? brand.ink
  const clickable = Boolean(widget.navTarget)
  const pct = percentValue(widget.value)
  return (
    <Box
      component={clickable ? 'button' : 'div'}
      type={clickable ? 'button' : undefined}
      onClick={clickable ? () => onNavigate(widget.navTarget as string) : undefined}
      sx={{
        textAlign: 'left',
        border: `1px solid ${brand.border}`,
        borderLeft: `3px solid ${color}`,
        borderRadius: '12px',
        bgcolor: alpha(color, 0.06),
        px: 1.75,
        py: 1.5,
        boxShadow: 'none',
        cursor: clickable ? 'pointer' : 'default',
        font: 'inherit',
        width: '100%',
        minHeight: 108,
        transition: 'border-color 160ms ease, box-shadow 160ms ease',
        '&:hover': clickable
          ? { borderColor: brand.goldMid, boxShadow: '0 8px 20px rgba(23,23,23,0.06)' }
          : undefined,
        '&:focus-visible': { outline: `2px solid ${brand.gold}`, outlineOffset: 2 },
      }}
    >
      <Stack direction="row" spacing={0.75} alignItems="center" justifyContent="space-between">
        <Stack direction="row" spacing={0.75} alignItems="center" sx={{ minWidth: 0 }}>
          <Box sx={{ display: 'flex', color: brand.muted, '& .MuiSvgIcon-root': { fontSize: 16 } }}>{icon}</Box>
          <Typography sx={{ fontWeight: 700, fontSize: '0.8125rem', letterSpacing: '0.02em', color: brand.goldDark }}>{label}</Typography>
        </Stack>
        <Typography
          sx={{
            fontSize: '0.6875rem',
            fontWeight: 650,
            color,
            letterSpacing: '0.04em',
            px: 0.75,
            py: 0.25,
            borderRadius: '99px',
            bgcolor: alpha(color, 0.1),
          }}
        >
          {word}
        </Typography>
      </Stack>
      <Typography sx={{ fontWeight: 650, fontSize: '1.65rem', letterSpacing: '-0.03em', color: brand.gold, lineHeight: 1.1, mt: 1 }}>
        {widget.value}
      </Typography>
      <Typography variant="caption" color="text.secondary" noWrap display="block" sx={{ mt: 0.35 }}>
        {widget.title}
      </Typography>
      {pct != null && (
        <Box sx={{ mt: 1.5, height: 4, bgcolor: '#F3F0EA', borderRadius: 99, overflow: 'hidden' }}>
          <Box sx={{ width: `${pct}%`, height: '100%', bgcolor: brand.gold, borderRadius: 99 }} />
        </Box>
      )}
    </Box>
  )
}

function WorkCard({
  kicker,
  label,
  icon,
  widget,
  onNavigate,
}: {
  kicker: string
  label: string
  icon: ReactNode
  widget: DashboardWidget
  onNavigate: (id: string) => void
}) {
  const clickable = Boolean(widget.navTarget)
  return (
    <Box
      component={clickable ? 'button' : 'div'}
      type={clickable ? 'button' : undefined}
      onClick={clickable ? () => onNavigate(widget.navTarget as string) : undefined}
      sx={{
        flex: 1,
        textAlign: 'left',
        font: 'inherit',
        border: `1px solid ${brand.border}`,
        borderRadius: '12px',
        bgcolor: '#fff',
        boxShadow: '0 1px 2px rgba(23,23,23,0.04)',
        px: 2,
        py: 1.75,
        cursor: clickable ? 'pointer' : 'default',
        transition: 'border-color 160ms ease, box-shadow 160ms ease',
        '&:hover': clickable
          ? { borderColor: brand.goldMid, boxShadow: '0 8px 20px rgba(23,23,23,0.06)' }
          : undefined,
        '&:focus-visible': { outline: `2px solid ${brand.gold}`, outlineOffset: 2 },
      }}
    >
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Stack direction="row" spacing={0.75} alignItems="center">
          <Box sx={{ color: brand.goldDark, display: 'flex', '& .MuiSvgIcon-root': { fontSize: 16 } }}>{icon}</Box>
          <Typography sx={{ fontSize: '0.6875rem', fontWeight: 650, letterSpacing: '0.08em', color: brand.muted }}>
            {kicker}
          </Typography>
        </Stack>
        {clickable && <ChevronRightRoundedIcon sx={{ fontSize: 18, color: brand.muted }} />}
      </Stack>
      <Typography sx={{ fontWeight: 700, fontSize: '1.75rem', letterSpacing: '-0.03em', mt: 1.25, lineHeight: 1, color: brand.gold }}>
        {widget.value}
      </Typography>
      <Typography sx={{ fontSize: '0.8125rem', color: '#5F6368', mt: 0.5 }}>{label}</Typography>
    </Box>
  )
}

export default function OverviewDashboard({
  session: _session,
  onNavigate,
  onError,
  initialSummary = null,
}: {
  session: Session
  onNavigate: (id: string, drill?: RiskHeatMapDrill & { riskType: RiskType }) => void
  onError: (msg: string) => void
  initialSummary?: DashboardSummary | null
}) {
  const [summary, setSummary] = useState<DashboardSummary | null>(initialSummary)
  const [loading, setLoading] = useState(!initialSummary)
  const [healthyOpen, setHealthyOpen] = useState(false)

  const load = useCallback(() => {
    api
      .getDashboardSummary()
      .then((r) => setSummary(r))
      .catch((e) => onError(String(e)))
      .finally(() => setLoading(false))
  }, [onError])

  useEffect(() => {
    if (!initialSummary) load()
    const t = setInterval(load, REFRESH_MS)
    return () => clearInterval(t)
  }, [load, initialSummary])

  const widgets = useMemo(
    () =>
      (summary?.widgets ?? []).filter(
        (w) =>
          !(
            w.navTarget &&
            (isRiskNavHidden(w.navTarget) || isBcmsNavPending(w.navTarget) || isPrivacyNavPending(w.navTarget))
          ),
      ),
    [summary],
  )
  const activity = summary?.recentActivity ?? []
  const byId = useMemo(() => new Map(widgets.map((w) => [w.id, w])), [widgets])

  const attention = useMemo(() => attentionWidgets(widgets), [widgets])
  const decide = attention.slice(0, 5)
  const healthy = useMemo(() => widgets.filter(isHealthy), [widgets])

  const posture = useMemo(() => {
    const cells: { label: string; icon: ReactNode; widget: DashboardWidget }[] = []
    for (const def of POSTURE_WIDGET_IDS) {
      const w = pickWidget(byId, def.preferIds)
      if (w) cells.push({ label: def.label, icon: def.icon, widget: w })
    }
    return cells
  }, [byId])

  const myTasks = byId.get('my_tasks_pending') ?? null
  const openOrgTasks = byId.get('open_work_tasks') ?? null
  const when = summary ? briefingWhen(summary.generatedAt) : ''
  const decisionLine =
    attention.length === 0
      ? 'Nothing is waiting on you.'
      : attention.length === 1
        ? '1 area needs a decision.'
        : `${attention.length} areas need a decision.`

  const showRiskPicture = byId.has('open_erm_risks') || byId.has('open_is_risks')
  const showActivity = widgets.length > 0

  return (
    <Stack
      spacing={2.5}
      sx={{
        width: '100%',
        maxWidth: layout.wideCanvasMax,
        mx: 'auto',
        pb: 2,
        boxSizing: 'border-box',
      }}
    >
      <ModuleListShell title="Dashboard" breadcrumbTrail="Overview" />

      <Stack direction="row" alignItems="flex-start" justifyContent="space-between" spacing={2}>
        <Box>
          <Typography sx={{ fontWeight: 650, fontSize: { xs: '1.2rem', md: '1.35rem' }, letterSpacing: '-0.03em', lineHeight: 1.25, color: brand.goldDark }}>
            {when ? `${when}, ${decisionLine.charAt(0).toLowerCase()}${decisionLine.slice(1)}` : decisionLine}
          </Typography>
          {loading && !summary && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              Loading the workspace picture.
            </Typography>
          )}
        </Box>
        <Box
          component="button"
          type="button"
          onClick={load}
          aria-label="Refresh dashboard"
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 0.5,
            border: 0,
            bgcolor: 'transparent',
            cursor: 'pointer',
            color: 'text.secondary',
            font: 'inherit',
            flexShrink: 0,
            mt: 0.25,
            '&:hover': { color: brand.gold },
          }}
        >
          <RefreshRoundedIcon sx={{ fontSize: 15 }} />
          <Typography variant="caption">
            {summary ? `Updated ${formatTenantDateTime(summary.generatedAt)}` : 'Refresh'}
          </Typography>
        </Box>
      </Stack>

      {!loading && widgets.length === 0 && (
        <Card>
          <Box sx={{ py: 4, textAlign: 'center' }}>
            <Typography variant="subtitle2" gutterBottom>
              No widgets yet
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Widgets appear here as modules are licensed for this workspace.
            </Typography>
          </Box>
        </Card>
      )}

      {posture.length > 0 && (
        <Box>
          <SectionHeader title="Module posture" hint="Licensed Modules" />
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', lg: 'repeat(auto-fit, minmax(180px, 1fr))' },
              gap: 1.5,
            }}
          >
            {posture.map((cell) => (
              <PostureCell
                key={cell.label}
                label={cell.label}
                icon={cell.icon}
                widget={cell.widget}
                onNavigate={onNavigate}
              />
            ))}
          </Box>
        </Box>
      )}

      {widgets.length > 0 && (decide.length > 0 || myTasks || openOrgTasks) && (
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', lg: 'minmax(0, 1.4fr) minmax(260px, 0.8fr)' },
            gap: 2.5,
            alignItems: 'start',
          }}
        >
          <Box>
            <SectionHeader title="Decide today" hint={attention.length > 5 ? 'First 5' : attention.length === 0 ? 'Clear' : undefined} />
            {decide.length === 0 ? (
              <Card sx={{ borderRadius: '12px', px: 2, py: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Nothing is waiting on you.
                </Typography>
              </Card>
            ) : (
          <Card sx={{ overflow: 'hidden', borderRadius: '12px', boxShadow: '0 1px 2px rgba(23,23,23,0.04)', borderTop: `2px solid ${brand.gold}` }}>
              {decide.map((w, i) => {
                const clickable = Boolean(w.navTarget)
                return (
                  <Box
                    key={w.id}
                    component={clickable ? 'button' : 'div'}
                    type={clickable ? 'button' : undefined}
                    onClick={clickable ? () => onNavigate(w.navTarget as string) : undefined}
                    sx={{
                      width: '100%',
                      display: 'flex',
                      alignItems: 'center',
                      gap: 1.5,
                      textAlign: 'left',
                      font: 'inherit',
                      border: 0,
                      borderBottom: i === decide.length - 1 ? 0 : `1px solid ${brand.border}`,
                      bgcolor: 'transparent',
                      px: 2,
                      py: 1.35,
                      cursor: clickable ? 'pointer' : 'default',
                      '&:hover': clickable ? { bgcolor: brand.hover } : undefined,
                      '&:focus-visible': { outline: `2px solid ${brand.gold}`, outlineOffset: -2 },
                    }}
                  >
                    <Box
                      sx={{
                        width: 8,
                        height: 8,
                        borderRadius: '50%',
                        bgcolor: TONE_COLORS[w.tone],
                        flexShrink: 0,
                      }}
                    />
                    <Typography sx={{ flex: 1, fontSize: '0.875rem', fontWeight: 600, minWidth: 0, color: brand.goldDark }} noWrap>
                      {w.title}
                    </Typography>
                    <Typography variant="caption" color="text.secondary" sx={{ display: { xs: 'none', sm: 'block' } }}>
                      {moduleLabel(w.module)}
                    </Typography>
                    <Typography sx={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums', minWidth: 36, textAlign: 'right', color: brand.gold }}>
                      {w.value}
                    </Typography>
                    {clickable && <ChevronRightRoundedIcon sx={{ fontSize: 18, color: brand.goldDark }} />}
                  </Box>
                )
              })}
          </Card>
            )}
          </Box>
          <Box>
            <SectionHeader title="Work" />
            <Stack spacing={1.5}>
              {myTasks && (
                <WorkCard kicker="My work" label="Pending for Me" icon={<AssignmentIndRoundedIcon />} widget={myTasks} onNavigate={onNavigate} />
              )}
              {openOrgTasks && (
                <WorkCard
                  kicker="Organisation"
                  label="Open in the Organisation"
                  icon={<ApartmentRoundedIcon />}
                  widget={openOrgTasks}
                  onNavigate={onNavigate}
                />
              )}
              {!myTasks && !openOrgTasks && (
                <Typography variant="body2" color="text.secondary">
                  No open work counts on this workspace.
                </Typography>
              )}
            </Stack>
          </Box>
        </Box>
      )}

      {(showRiskPicture || showActivity) && (
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: {
            xs: '1fr',
            xl: showRiskPicture && showActivity ? 'minmax(420px, 1fr) minmax(420px, 1.15fr)' : '1fr',
          },
          gap: 2.5,
          alignItems: 'start',
        }}
      >
      {showRiskPicture && (
        <Box>
          <SectionHeader
            title="Risk picture"
            hint="Open Risks Across the Organisation"
            actionLabel="View Risk Register"
            onAction={() => onNavigate(byId.has('open_erm_risks') ? 'erm-risk' : 'risk')}
          />
          <DashboardRiskHeatMaps
            showErm={byId.has('open_erm_risks')}
            showIs={byId.has('open_is_risks')}
            onNavigate={onNavigate}
            onError={onError}
          />
        </Box>
      )}

      {showActivity && (
        <Box>
            <SectionHeader
              title="Recent activity"
              hint={activity.length ? `Last ${activity.length}` : undefined}
              actionLabel="Audit Trail"
              onAction={() => onNavigate('audit-trail')}
            />
            <Card sx={{ px: 2, py: 0.5 }}>
              {activity.length === 0 && (
                <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
                  No activity yet.
                </Typography>
              )}
              {activity.slice(0, 8).map((row) => (
                <Box
                  key={row.id}
                  sx={{ py: 1.25, borderBottom: `1px solid ${brand.border}`, '&:last-child': { borderBottom: 0 } }}
                >
                  <Stack direction="row" spacing={1.25} alignItems="center" sx={{ minWidth: 0 }}>
                    <Box sx={{ width: 6, height: 6, borderRadius: '50%', bgcolor: brand.gold, flexShrink: 0 }} />
                    <Typography
                      noWrap
                      title={activityAction(row)}
                      sx={{ fontSize: '0.875rem', fontWeight: 600, lineHeight: 1.4, color: brand.goldDark, minWidth: 0 }}
                    >
                      {activityAction(row)}
                    </Typography>
                  </Stack>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    component="div"
                    noWrap
                    sx={{ pl: '16px', mt: 0.25 }}
                  >
                    {activityMeta(row)}
                  </Typography>
                </Box>
              ))}
            </Card>
        </Box>
      )}
      </Box>
      )}

      {healthy.length > 0 && (
        <Card>
          <Box
            sx={{
              px: 2,
              py: 1,
              display: 'flex',
              alignItems: 'center',
              gap: 0.75,
              cursor: 'pointer',
            }}
            onClick={() => setHealthyOpen((v) => !v)}
          >
            <CheckCircleOutlineRoundedIcon sx={{ fontSize: 18, color: semantic.risk.low }} />
            <Typography variant="body2" sx={{ fontWeight: 650, flex: 1 }}>
              Healthy metrics ({healthy.length})
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {healthyOpen ? 'Hide' : 'Show'}
            </Typography>
            <IconButton size="small" aria-label={healthyOpen ? 'Collapse' : 'Expand'}>
              <ExpandMoreRoundedIcon
                sx={{
                  fontSize: 18,
                  transform: healthyOpen ? 'rotate(180deg)' : 'rotate(0deg)',
                  transition: 'transform .15s ease',
                }}
              />
            </IconButton>
          </Box>
          <Collapse in={healthyOpen}>
            <Box sx={{ px: 2, pb: 1.75, pt: 0.25 }}>
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(3, 1fr)' },
                  gap: 0.75,
                }}
              >
                {healthy.map((w) => (
                  <Box
                    key={w.id}
                    component={w.navTarget ? 'button' : 'div'}
                    type={w.navTarget ? 'button' : undefined}
                    onClick={w.navTarget ? () => onNavigate(w.navTarget as string) : undefined}
                    sx={{
                      font: 'inherit',
                      border: `1px solid ${alpha(semantic.risk.low, 0.2)}`,
                      bgcolor: alpha(semantic.risk.low, 0.06),
                      color: '#166534',
                      borderRadius: 1,
                      px: 1.25,
                      py: 0.85,
                      fontSize: '0.75rem',
                      lineHeight: 1.35,
                      cursor: w.navTarget ? 'pointer' : 'default',
                      textAlign: 'left',
                      width: '100%',
                    }}
                  >
                    <Typography component="span" sx={{ fontWeight: 650, fontSize: 'inherit', display: 'block' }}>
                      {w.title}
                    </Typography>
                    {w.emptyMessage && (
                      <Typography component="span" sx={{ fontSize: '0.7rem', opacity: 0.85 }}>
                        {w.emptyMessage}
                      </Typography>
                    )}
                  </Box>
                ))}
              </Box>
            </Box>
          </Collapse>
        </Card>
      )}
    </Stack>
  )
}
