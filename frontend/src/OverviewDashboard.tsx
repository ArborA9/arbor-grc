import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Box, Card, Collapse, IconButton, Stack, Typography, alpha } from '@mui/material'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded'
import CheckCircleOutlineRoundedIcon from '@mui/icons-material/CheckCircleOutlineRounded'
import SecurityRoundedIcon from '@mui/icons-material/SecurityRounded'
import HealthAndSafetyRoundedIcon from '@mui/icons-material/HealthAndSafetyRounded'
import GavelRoundedIcon from '@mui/icons-material/GavelRounded'
import LoginRoundedIcon from '@mui/icons-material/LoginRounded'
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded'
import AddCircleOutlineRoundedIcon from '@mui/icons-material/AddCircleOutlineRounded'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import ThumbUpAltOutlinedIcon from '@mui/icons-material/ThumbUpAltOutlined'
import ThumbDownAltOutlinedIcon from '@mui/icons-material/ThumbDownAltOutlined'
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined'
import NotificationsNoneRoundedIcon from '@mui/icons-material/NotificationsNoneRounded'
import SwapHorizRoundedIcon from '@mui/icons-material/SwapHorizRounded'
import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded'
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
import { ModuleListShell } from './hooks/useModulePageShell'
import { formatTenantDateTime, getTenantLocale } from './uiQuality/tenantLocale'
import { isRiskNavHidden } from './components/risk/riskNav'
import { isBcmsNavPending } from './components/bcms/bcmsNav'
import { isPrivacyNavPending } from './components/privacy/privacyNav'

const REFRESH_MS = 5 * 60 * 1000

/** RAG data colors (gold is chrome only). */
const TONE_COLORS: Record<WidgetTone, string> = {
  success: semantic.risk.low,
  warning: semantic.risk.high,
  info: semantic.risk.high,
  neutral: brand.muted,
  danger: semantic.risk.critical,
}

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

function attentionWidgets(widgets: DashboardWidget[]) {
  return widgets
    .filter((w) => needsAttention(w) && w.id !== 'risk_heat_map' && !WORK_CARD_IDS.has(w.id))
    .sort((a, b) => (TONE_RANK[a.tone] ?? 9) - (TONE_RANK[b.tone] ?? 9))
}

function isHealthy(w: DashboardWidget) {
  return isZero(w.value) && !!w.emptyMessage && (w.tone === 'success' || w.tone === 'neutral')
}

function percentValue(value: string): number | null {
  const raw = value.trim()
  if (!raw.endsWith('%')) return null
  const n = Number(raw.slice(0, -1))
  if (!Number.isFinite(n)) return null
  return Math.max(0, Math.min(100, n))
}

function parseCount(value: string): number {
  const n = Number(String(value).replace(/[^0-9.-]/g, ''))
  return Number.isFinite(n) ? Math.max(0, n) : 0
}

function activityIcon(actionType: string | null): ReactNode {
  switch (actionType) {
    case 'LOGIN':
      return <LoginRoundedIcon />
    case 'LOGOUT':
      return <LogoutRoundedIcon />
    case 'CREATED':
      return <AddCircleOutlineRoundedIcon />
    case 'UPDATED':
      return <EditOutlinedIcon />
    case 'DELETED':
      return <DeleteOutlineRoundedIcon />
    case 'APPROVED':
      return <ThumbUpAltOutlinedIcon />
    case 'REJECTED':
      return <ThumbDownAltOutlinedIcon />
    case 'EXPORT':
      return <FileDownloadOutlinedIcon />
    case 'IMPERSONATED':
      return <SwapHorizRoundedIcon />
    default:
      return actionType === null ? <NotificationsNoneRoundedIcon /> : <HistoryRoundedIcon />
  }
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

/** `RISK_OWNER_NAMED` -> `Risk owner named`. Leaves normal text alone. */
function humanizeCode(value: string): string {
  if (!/^[A-Z0-9]+(_[A-Z0-9]+)+$/.test(value)) return value
  const words = value.replace(/_/g, ' ').toLowerCase()
  return words.charAt(0).toUpperCase() + words.slice(1)
}

function activityAction(row: RecentActivityItem): string {
  const type = row.actionType ?? 'UPDATED'
  if (type === 'LOGIN' || type === 'LOGOUT') return ACTIVITY_VERBS[type]
  const record = row.recordName?.trim() ? humanizeCode(row.recordName.trim()) : ''
  if (row.module === 'NOTIFICATION') return record ? `Notification sent: ${record}` : 'Notification sent'
  const verb = ACTIVITY_VERBS[type] ?? humanizeCode(type)
  return record ? `${verb} ${record}` : verb
}

/** Semi-circle instrument gauge (dense command board). */
function SemiGauge({ score, color, label }: { score: number; color: string; label: string }) {
  const width = 148
  const height = 84
  const cx = 74
  const cy = 76
  const r = 56
  const stroke = 10
  const arcLen = Math.PI * r
  const pct = Math.max(0, Math.min(100, score)) / 100
  const zones = [
    { from: 0, to: 0.5, color: semantic.risk.critical },
    { from: 0.5, to: 0.8, color: semantic.risk.high },
    { from: 0.8, to: 1, color: semantic.risk.low },
  ]
  return (
    <Box sx={{ position: 'relative', width, height, flexShrink: 0 }}>
      <Box component="svg" width={width} height={height} viewBox={`0 0 ${width} ${height}`} aria-hidden sx={{ display: 'block' }}>
        {zones.map((z) => {
          const start = z.from * arcLen
          const len = (z.to - z.from) * arcLen
          return (
            <path
              key={z.from}
              d={`M ${cx - r} ${cy} A ${r} ${r} 0 0 1 ${cx + r} ${cy}`}
              fill="none"
              stroke={alpha(z.color, 0.28)}
              strokeWidth={stroke}
              strokeLinecap="butt"
              strokeDasharray={`${len} ${arcLen}`}
              strokeDashoffset={-start}
            />
          )
        })}
        <path
          d={`M ${cx - r} ${cy} A ${r} ${r} 0 0 1 ${cx + r} ${cy}`}
          fill="none"
          stroke={color}
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={`${pct * arcLen} ${arcLen}`}
          style={{ transition: 'stroke-dasharray 320ms ease' }}
        />
      </Box>
      <Box sx={{ position: 'absolute', left: 0, right: 0, bottom: 2, textAlign: 'center' }}>
        <Typography sx={{ fontWeight: 750, fontSize: '1.35rem', letterSpacing: '-0.04em', color, lineHeight: 1, fontVariantNumeric: 'tabular-nums' }}>
          {score}
        </Typography>
        <Typography sx={{ fontSize: '0.5rem', fontWeight: 700, letterSpacing: '0.08em', color: brand.muted, mt: 0.15 }}>
          {label.toUpperCase()}
        </Typography>
      </Box>
    </Box>
  )
}

const PROGRAM_RING_IDS = [
  { id: 'overall_compliance_score', short: 'Compliance', icon: <SecurityRoundedIcon /> },
  { id: 'soa_completion_pct', short: 'SoA', icon: <GavelRoundedIcon /> },
  { id: 'bia_approved_pct', short: 'BIA', icon: <HealthAndSafetyRoundedIcon /> },
] as const

function programStatus(pct: number): { label: string; color: string } {
  if (pct <= 0) return { label: 'Not started', color: brand.muted }
  if (pct < 40) return { label: 'Early', color: semantic.risk.critical }
  if (pct < 80) return { label: 'In progress', color: semantic.risk.high }
  return { label: 'On track', color: semantic.risk.low }
}

function ProgramProgressStrip({
  byId,
  onNavigate,
}: {
  byId: Map<string, DashboardWidget>
  onNavigate: (id: string) => void
}) {
  const items = PROGRAM_RING_IDS.map((def) => {
    const w = byId.get(def.id)
    const pct = w ? percentValue(w.value) : null
    if (!w || pct == null) return null
    return { ...def, widget: w, pct }
  }).filter(Boolean) as {
    id: string
    short: string
    icon: ReactNode
    widget: DashboardWidget
    pct: number
  }[]

  if (items.length === 0) return null

  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', sm: `repeat(${Math.min(3, items.length)}, 1fr)` },
        gap: 1,
      }}
    >
      {items.map((item) => {
        const status = programStatus(item.pct)
        const color = status.color
        const clickable = Boolean(item.widget.navTarget)
        return (
          <Box
            key={item.id}
            component={clickable ? 'button' : 'div'}
            type={clickable ? 'button' : undefined}
            onClick={clickable ? () => onNavigate(item.widget.navTarget as string) : undefined}
            sx={{
              textAlign: 'left',
              font: 'inherit',
              border: `1px solid ${alpha(color, 0.28)}`,
              borderRadius: '10px',
              bgcolor: alpha(color, 0.06),
              px: 1.25,
              py: 1,
              cursor: clickable ? 'pointer' : 'default',
              minWidth: 0,
              '&:hover': clickable ? { boxShadow: `0 4px 12px ${alpha(color, 0.16)}` } : undefined,
              '&:focus-visible': { outline: `2px solid ${brand.gold}`, outlineOffset: 2 },
            }}
          >
            <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={1}>
              <Stack direction="row" spacing={0.65} alignItems="center" sx={{ minWidth: 0 }}>
                <Box sx={{ color, display: 'flex', '& .MuiSvgIcon-root': { fontSize: 14 } }}>{item.icon}</Box>
                <Typography sx={{ fontSize: '0.625rem', fontWeight: 700, letterSpacing: '0.05em', color: brand.muted }}>
                  {item.short.toUpperCase()}
                </Typography>
              </Stack>
              <Typography sx={{ fontSize: '0.8125rem', fontWeight: 750, color, fontVariantNumeric: 'tabular-nums' }}>
                {Math.round(item.pct)}%
              </Typography>
            </Stack>
            <Box sx={{ mt: 0.75, height: 5, borderRadius: 99, bgcolor: alpha(color, 0.15), overflow: 'hidden' }}>
              <Box sx={{ width: `${Math.max(item.pct, item.pct <= 0 ? 0 : 4)}%`, height: '100%', bgcolor: color, borderRadius: 99 }} />
            </Box>
            <Typography sx={{ mt: 0.4, fontSize: '0.5625rem', fontWeight: 650, color }}>{status.label}</Typography>
          </Box>
        )
      })}
    </Box>
  )
}

const OVERDUE_IDS = [
  'overdue_is_risks',
  'overdue_erm_risks',
  'overdue_dsrs',
  'overdue_capas',
  'overdue_risk_treatments',
] as const

const APPROACHING_IDS = [
  'dsrs_approaching_deadline',
  'vendors_review_due',
  'policies_due_for_review',
  'evidence_expiring',
] as const

function sumWidgetCounts(byId: Map<string, DashboardWidget>, ids: readonly string[]): number {
  let n = 0
  for (const id of ids) {
    const w = byId.get(id)
    if (w) n += parseCount(w.value)
  }
  return n
}

function firstNav(byId: Map<string, DashboardWidget>, ids: readonly string[]): string | null {
  for (const id of ids) {
    const w = byId.get(id)
    if (w && parseCount(w.value) > 0 && w.navTarget) return w.navTarget
  }
  for (const id of ids) {
    const w = byId.get(id)
    if (w?.navTarget) return w.navTarget
  }
  return null
}

function workspacePostureScore(widgets: DashboardWidget[], byId: Map<string, DashboardWidget>): {
  score: number
  tone: WidgetTone
  label: 'Strong' | 'Watch' | 'Critical'
} {
  let score = 100
  const overdue = sumWidgetCounts(byId, OVERDUE_IDS)
  const critical = parseCount(byId.get('critical_open_risks')?.value ?? '0')
  const dangerN = widgets.filter((w) => w.tone === 'danger' && !isHealthy(w)).length
  const warnN = widgets.filter((w) => w.tone === 'warning' && !isHealthy(w) && !WORK_CARD_IDS.has(w.id)).length
  score -= Math.min(40, overdue * 8)
  score -= Math.min(25, critical * 10)
  score -= Math.min(20, dangerN * 5)
  score -= Math.min(15, warnN * 2)
  score = Math.max(0, Math.min(100, Math.round(score)))
  const tone: WidgetTone = score >= 80 ? 'success' : score >= 50 ? 'warning' : 'danger'
  const label = tone === 'success' ? 'Strong' : tone === 'warning' ? 'Watch' : 'Critical'
  return { score, tone, label }
}

function KpiStrip({
  tiles,
  onNavigate,
}: {
  tiles: { key: string; label: string; value: string; hint: string; tone: WidgetTone; nav: string | null }[]
  onNavigate: (id: string) => void
}) {
  if (tiles.length === 0) return null
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: {
          xs: '1fr 1fr',
          md: `repeat(${Math.min(4, tiles.length)}, 1fr)`,
        },
        gap: 1,
      }}
    >
      {tiles.map((t) => {
        const color = TONE_COLORS[t.tone]
        const n = parseCount(t.value)
        const fill = t.tone === 'danger' ? 100 : t.tone === 'warning' ? 72 : t.tone === 'success' && n === 0 ? 100 : 36
        const clickable = Boolean(t.nav)
        return (
          <Box
            key={t.key}
            component={clickable ? 'button' : 'div'}
            type={clickable ? 'button' : undefined}
            onClick={clickable && t.nav ? () => onNavigate(t.nav as string) : undefined}
            sx={{
              textAlign: 'left',
              font: 'inherit',
              border: `1px solid ${alpha(color, 0.28)}`,
              borderRadius: '10px',
              bgcolor: alpha(color, 0.06),
              px: 1.15,
              py: 0.9,
              cursor: clickable ? 'pointer' : 'default',
              minHeight: 64,
              '&:hover': clickable ? { boxShadow: `0 4px 12px ${alpha(color, 0.18)}` } : undefined,
              '&:focus-visible': { outline: `2px solid ${brand.gold}`, outlineOffset: 2 },
            }}
          >
            <Typography sx={{ fontSize: '0.5625rem', fontWeight: 700, letterSpacing: '0.06em', color: brand.muted, textTransform: 'uppercase' }}>
              {t.label}
            </Typography>
            <Typography
              sx={{
                mt: 0.35,
                fontWeight: 750,
                fontSize: '1.2rem',
                letterSpacing: '-0.03em',
                lineHeight: 1,
                color,
                fontVariantNumeric: 'tabular-nums',
              }}
            >
              {t.value}
            </Typography>
            <Box sx={{ mt: 0.65, height: 3, borderRadius: 99, bgcolor: alpha(color, 0.15), overflow: 'hidden' }}>
              <Box sx={{ width: `${fill}%`, height: '100%', bgcolor: color, borderRadius: 99 }} />
            </Box>
          </Box>
        )
      })}
    </Box>
  )
}

function DecideWorkboard({
  overdue,
  approaching,
  awaitingMe,
  onNavigate,
}: {
  overdue: { count: number; nav: string | null }
  approaching: { count: number; nav: string | null }
  awaitingMe: { count: number; nav: string | null }
  onNavigate: (id: string) => void
}) {
  const tiles = [
    {
      key: 'overdue',
      title: 'Overdue',
      hint: 'Past review or due date',
      count: overdue.count,
      nav: overdue.nav,
      color: semantic.risk.critical,
    },
    {
      key: 'approaching',
      title: 'Due soon',
      hint: 'Approaching deadlines',
      count: approaching.count,
      nav: approaching.nav,
      color: semantic.risk.medium,
    },
    {
      key: 'awaiting',
      title: 'Awaiting me',
      hint: 'My queue',
      count: awaitingMe.count,
      nav: awaitingMe.nav,
      color: semantic.risk.high,
    },
  ] as const

  return (
    <Stack spacing={0.85}>
      {tiles.map((t) => {
        const hot = t.count > 0
        const clickable = Boolean(t.nav)
        const color = hot ? t.color : semantic.risk.low
        return (
          <Box
            key={t.key}
            component={clickable ? 'button' : 'div'}
            type={clickable ? 'button' : undefined}
            onClick={clickable && t.nav ? () => onNavigate(t.nav as string) : undefined}
            sx={{
              textAlign: 'left',
              font: 'inherit',
              border: `1px solid ${alpha(color, 0.3)}`,
              borderRadius: '10px',
              bgcolor: alpha(color, hot ? 0.08 : 0.05),
              px: 1.25,
              py: 0.9,
              cursor: clickable ? 'pointer' : 'default',
              display: 'flex',
              alignItems: 'center',
              gap: 1.25,
              width: '100%',
              '&:hover': clickable ? { boxShadow: `0 4px 12px ${alpha(color, 0.16)}` } : undefined,
              '&:focus-visible': { outline: `2px solid ${brand.gold}`, outlineOffset: 2 },
            }}
          >
            <Box sx={{ minWidth: 0, flex: 1 }}>
              <Typography sx={{ fontSize: '0.625rem', fontWeight: 700, letterSpacing: '0.06em', color: brand.muted }}>
                {t.title.toUpperCase()}
              </Typography>
              <Box sx={{ mt: 0.55, height: 4, bgcolor: alpha(color, 0.15), borderRadius: 99, overflow: 'hidden' }}>
                <Box
                  sx={{
                    width: `${hot ? Math.min(100, 24 + t.count * 8) : 100}%`,
                    height: '100%',
                    bgcolor: color,
                    borderRadius: 99,
                  }}
                />
              </Box>
            </Box>
            <Typography
              sx={{
                fontWeight: 750,
                fontSize: '1.25rem',
                letterSpacing: '-0.03em',
                lineHeight: 1,
                color,
                fontVariantNumeric: 'tabular-nums',
                flexShrink: 0,
              }}
            >
              {t.count}
            </Typography>
          </Box>
        )
      })}
    </Stack>
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
  const [healthyOpen, setHealthyOpen] = useState(true)

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
  const healthy = useMemo(() => widgets.filter(isHealthy), [widgets])

  const myTasks = byId.get('my_tasks_pending') ?? null
  const when = summary ? briefingWhen(summary.generatedAt) : ''
  const decisionLine =
    attention.length === 0
      ? 'Nothing is waiting on you.'
      : attention.length === 1
        ? '1 area needs a decision.'
        : `${attention.length} areas need a decision.`

  const postureScore = useMemo(() => workspacePostureScore(widgets, byId), [widgets, byId])
  const workboard = useMemo(
    () => ({
      overdue: {
        count: sumWidgetCounts(byId, OVERDUE_IDS),
        nav: firstNav(byId, OVERDUE_IDS),
      },
      approaching: {
        count: sumWidgetCounts(byId, APPROACHING_IDS),
        nav: firstNav(byId, APPROACHING_IDS),
      },
      awaitingMe: {
        count: parseCount(myTasks?.value ?? '0'),
        nav: myTasks?.navTarget ?? null,
      },
    }),
    [byId, myTasks],
  )

  const showRiskPicture = byId.has('open_erm_risks') || byId.has('open_is_risks')
  const showActivity = widgets.length > 0

  const kpiTiles = useMemo(() => {
    const tiles: { key: string; label: string; value: string; hint: string; tone: WidgetTone; nav: string | null }[] = []
    const ur = byId.get('open_is_risks')
    if (ur) {
      tiles.push({
        key: ur.id,
        label: 'Open Unified',
        value: ur.value,
        hint: ur.title,
        tone: ur.tone,
        nav: ur.navTarget,
      })
    }
    const erm = byId.get('open_erm_risks')
    if (erm) {
      tiles.push({
        key: erm.id,
        label: 'Open ERM',
        value: erm.value,
        hint: erm.title,
        tone: erm.tone,
        nav: erm.navTarget,
      })
    }
    tiles.push({
      key: 'overdue',
      label: 'Overdue',
      value: String(workboard.overdue.count),
      hint: 'Past review or due date',
      tone: workboard.overdue.count > 0 ? 'danger' : 'success',
      nav: workboard.overdue.nav,
    })
    tiles.push({
      key: 'awaiting',
      label: 'Awaiting me',
      value: String(workboard.awaitingMe.count),
      hint: 'Pending in my queue',
      tone: workboard.awaitingMe.count > 0 ? 'warning' : 'success',
      nav: workboard.awaitingMe.nav,
    })
    if (tiles.length < 4) {
      const findings = byId.get('open_findings')
      if (findings) {
        tiles.splice(2, 0, {
          key: findings.id,
          label: 'Open findings',
          value: findings.value,
          hint: findings.title,
          tone: findings.tone,
          nav: findings.navTarget,
        })
      }
    }
    return tiles.slice(0, 4)
  }, [byId, workboard])

  const gaugeColor = TONE_COLORS[postureScore.tone]

  return (
    <Stack
      spacing={1.35}
      sx={{
        width: '100%',
        maxWidth: layout.wideCanvasMax,
        mx: 'auto',
        pb: 2,
        boxSizing: 'border-box',
      }}
    >
      <ModuleListShell title="Dashboard" breadcrumbTrail="Overview" />

      <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={2}>
        <Box sx={{ minWidth: 0 }}>
          <Typography sx={{ fontWeight: 650, fontSize: { xs: '1.05rem', md: '1.15rem' }, letterSpacing: '-0.02em', color: brand.goldDark }} noWrap>
            {when ? `${when}` : 'Overview'}
            <Typography component="span" sx={{ fontWeight: 500, color: brand.muted, ml: 1, fontSize: '0.875rem' }}>
              {decisionLine}
            </Typography>
          </Typography>
          {loading && !summary && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.35 }}>
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

      {widgets.length > 0 && <KpiStrip tiles={kpiTiles} onNavigate={onNavigate} />}

      {widgets.length > 0 && <ProgramProgressStrip byId={byId} onNavigate={onNavigate} />}

      {widgets.length > 0 && showRiskPicture && (
        <Box>
          <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 0.85 }}>
            <Typography sx={{ fontSize: '0.6875rem', fontWeight: 700, letterSpacing: '0.04em', color: brand.muted, textTransform: 'uppercase' }}>
              Risk picture
            </Typography>
            <Typography
              component="button"
              type="button"
              onClick={() => onNavigate(byId.has('open_erm_risks') ? 'erm-risk' : 'risk')}
              sx={{
                border: 0,
                bgcolor: 'transparent',
                cursor: 'pointer',
                font: 'inherit',
                fontSize: '0.6875rem',
                fontWeight: 650,
                color: brand.gold,
                '&:hover': { textDecoration: 'underline' },
              }}
            >
              View Risk Register
            </Typography>
          </Stack>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: {
                xs: '1fr',
                lg: 'minmax(168px, 190px) minmax(0, 1fr)',
              },
              gap: 1.25,
              alignItems: 'stretch',
            }}
          >
            <Card
              sx={{
                border: `1px solid ${alpha(gaugeColor, 0.35)}`,
                borderRadius: '10px',
                p: 1.25,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                bgcolor: alpha(gaugeColor, 0.07),
                height: '100%',
                boxSizing: 'border-box',
              }}
            >
              <Typography sx={{ fontSize: '0.5625rem', fontWeight: 700, letterSpacing: '0.08em', color: brand.muted, alignSelf: 'flex-start', mb: 0.5 }}>
                HEALTH
              </Typography>
              <SemiGauge score={postureScore.score} color={gaugeColor} label={postureScore.label} />
            </Card>
            <Box sx={{ minWidth: 0, minHeight: { lg: 260 } }}>
              <DashboardRiskHeatMaps
                showErm={byId.has('open_erm_risks')}
                showIs={byId.has('open_is_risks')}
                onNavigate={onNavigate}
                onError={onError}
              />
            </Box>
          </Box>
        </Box>
      )}

      {widgets.length > 0 && !showRiskPicture && (
        <Card
          sx={{
            border: `1px solid ${alpha(gaugeColor, 0.35)}`,
            borderRadius: '10px',
            p: 1.25,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            maxWidth: 200,
            bgcolor: alpha(gaugeColor, 0.07),
          }}
        >
          <Typography sx={{ fontSize: '0.5625rem', fontWeight: 700, letterSpacing: '0.08em', color: brand.muted, alignSelf: 'flex-start', mb: 0.5 }}>
            HEALTH
          </Typography>
          <SemiGauge score={postureScore.score} color={gaugeColor} label={postureScore.label} />
        </Card>
      )}

      {widgets.length > 0 && (
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
            gap: 1.25,
            alignItems: 'start',
          }}
        >
          <Box>
            <Typography sx={{ fontSize: '0.6875rem', fontWeight: 700, letterSpacing: '0.04em', color: brand.muted, textTransform: 'uppercase', mb: 0.75 }}>
              Decide today
            </Typography>
            <DecideWorkboard
              overdue={workboard.overdue}
              approaching={workboard.approaching}
              awaitingMe={workboard.awaitingMe}
              onNavigate={onNavigate}
            />
          </Box>
          {showActivity && (
            <Box>
              <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 0.75 }}>
                <Typography sx={{ fontSize: '0.6875rem', fontWeight: 700, letterSpacing: '0.04em', color: brand.muted, textTransform: 'uppercase' }}>
                  Activity
                </Typography>
                <Typography
                  component="button"
                  type="button"
                  onClick={() => onNavigate('audit-trail')}
                  sx={{ border: 0, bgcolor: 'transparent', cursor: 'pointer', font: 'inherit', fontSize: '0.6875rem', fontWeight: 650, color: brand.gold }}
                >
                  Audit Trail
                </Typography>
              </Stack>
              <Card sx={{ px: 1.1, py: 0.5, borderRadius: '10px', border: `1px solid ${brand.border}` }}>
                {activity.length === 0 && (
                  <Typography sx={{ py: 1, px: 0.5, fontSize: '0.75rem', color: brand.mutedLight }}>No activity yet.</Typography>
                )}
                {activity.slice(0, 6).map((row) => (
                  <Box
                    key={row.id}
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 1,
                      py: 0.65,
                      borderBottom: `1px solid ${brand.border}`,
                      '&:last-child': { borderBottom: 0 },
                    }}
                  >
                    <Box
                      sx={{
                        width: 22,
                        height: 22,
                        borderRadius: '6px',
                        bgcolor: alpha(semantic.risk.high, 0.1),
                        color: semantic.risk.high,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0,
                        '& .MuiSvgIcon-root': { fontSize: 13 },
                      }}
                    >
                      {row.module === 'NOTIFICATION' ? <NotificationsNoneRoundedIcon /> : activityIcon(row.actionType)}
                    </Box>
                    <Box sx={{ minWidth: 0, flex: 1 }}>
                      <Typography noWrap sx={{ fontSize: '0.75rem', fontWeight: 600, color: brand.inkSoft }}>
                        {activityAction(row)}
                      </Typography>
                      <Typography noWrap sx={{ fontSize: '0.625rem', color: brand.mutedLight }}>
                        {formatTenantDateTime(row.at)}
                      </Typography>
                    </Box>
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
