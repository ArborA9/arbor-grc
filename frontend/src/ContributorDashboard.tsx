import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { Box, Card, Stack, Typography, alpha } from '@mui/material'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded'
import AssignmentIndRoundedIcon from '@mui/icons-material/AssignmentIndRounded'
import FactCheckRoundedIcon from '@mui/icons-material/FactCheckRounded'
import AssignmentTurnedInRoundedIcon from '@mui/icons-material/AssignmentTurnedInRounded'
import SecurityRoundedIcon from '@mui/icons-material/SecurityRounded'
import BusinessCenterRoundedIcon from '@mui/icons-material/BusinessCenterRounded'
import HealthAndSafetyRoundedIcon from '@mui/icons-material/HealthAndSafetyRounded'
import PolicyRoundedIcon from '@mui/icons-material/PolicyRounded'
import SpeedRoundedIcon from '@mui/icons-material/SpeedRounded'
import ReportProblemRoundedIcon from '@mui/icons-material/ReportProblemRounded'
import PrivacyTipRoundedIcon from '@mui/icons-material/PrivacyTipRounded'
import AssignmentRoundedIcon from '@mui/icons-material/AssignmentRounded'
import {
  api,
  type ContributorDashboard as ContributorDashboardData,
  type ContributorDashboardModuleCount,
  type ContributorDashboardQueueItem,
  type Session,
} from './api'
import StatusChip from './components/StatusChip'
import { FilterPillRow, PageLoading, SectionHeader } from './components/ui'
import { ModuleListShell } from './hooks/useModulePageShell'
import { brand, layout, semantic } from './theme'
import { isRiskNavHidden } from './components/risk/riskNav'
import { useListReady } from './uiQuality/useListReady'
import { formatTenantDate, formatTenantDateTime, getTenantLocale } from './uiQuality/tenantLocale'
import { useTenantLocale } from './uiQuality/TenantLocaleContext'

const REFRESH_MS = 5 * 60 * 1000

type QueueFilter = 'open' | 'attention' | 'overdue' | 'week' | 'returned' | 'admin' | 'closed'

const MODULE_ICONS: Record<string, ReactNode> = {
  GAP: <AssignmentTurnedInRoundedIcon />,
  IS_RISK: <SecurityRoundedIcon />,
  ERM: <BusinessCenterRoundedIcon />,
  CAPA: <FactCheckRoundedIcon />,
  BIA: <HealthAndSafetyRoundedIcon />,
  POLICY: <PolicyRoundedIcon />,
  KRI: <SpeedRoundedIcon />,
  INCIDENT: <ReportProblemRoundedIcon />,
  DPIA: <PrivacyTipRoundedIcon />,
  ROPA: <PrivacyTipRoundedIcon />,
}

function matchesFilter(row: ContributorDashboardQueueItem, filter: QueueFilter): boolean {
  if (filter === 'closed') return !!row.finished
  if (row.finished) return false
  if (filter === 'admin') return row.waitingOnAdmin
  if (row.waitingOnAdmin) return false
  if (filter === 'attention') return row.status !== 'SUBMITTED'
  if (filter === 'open') return true
  if (filter === 'overdue') return row.overdue
  if (filter === 'week') return row.dueThisWeek
  if (filter === 'returned') return row.returned
  return true
}

function statusChip(row: ContributorDashboardQueueItem): {
  label: string
  tone: 'success' | 'warning' | 'info' | 'neutral' | 'danger'
} {
  if (row.finished) return { label: 'Closed', tone: 'success' }
  if (row.returned && !row.waitingOnAdmin) return { label: 'Returned', tone: 'danger' }
  if (row.waitingOnAdmin) return { label: row.waitingOn || 'Waiting', tone: 'info' }
  if (row.status === 'SUBMITTED') return { label: 'Approved', tone: 'success' }
  if (row.status === 'IN_PROGRESS') return { label: 'In Progress', tone: 'warning' }
  if (row.status === 'PENDING') return { label: 'Pending', tone: 'neutral' }
  return { label: row.status, tone: 'neutral' }
}

function rowDot(row: ContributorDashboardQueueItem): string {
  if (row.finished) return semantic.risk.low
  if (row.returned && !row.waitingOnAdmin) return semantic.risk.critical
  if (row.overdue) return semantic.risk.critical
  if (row.dueThisWeek) return semantic.risk.medium
  if (row.waitingOnAdmin) return brand.gold
  if (row.status === 'SUBMITTED') return semantic.risk.low
  return brand.muted
}

function queueRank(row: ContributorDashboardQueueItem): number {
  if (row.finished) return 5
  if (row.waitingOnAdmin) return 4
  if (row.returned) return 0
  if (row.overdue) return 1
  if (row.dueThisWeek) return 2
  return 3
}

function sortActionQueue(rows: ContributorDashboardQueueItem[]): ContributorDashboardQueueItem[] {
  return [...rows].sort((a, b) => {
    const byRank = queueRank(a) - queueRank(b)
    if (byRank !== 0) return byRank
    const ad = a.dueDate ?? ''
    const bd = b.dueDate ?? ''
    if (ad !== bd) return ad < bd ? -1 : 1
    return a.taskId - b.taskId
  })
}

function emptyQueueMessage(filter: QueueFilter): string {
  if (filter === 'admin') return 'Nothing is waiting.'
  if (filter === 'overdue') return 'Nothing is overdue.'
  if (filter === 'week') return 'Nothing is due this week.'
  if (filter === 'returned') return 'Nothing has been returned.'
  if (filter === 'closed') return 'Nothing is closed.'
  return 'Nothing is waiting on you.'
}

function needsAttention(row: ContributorDashboardQueueItem): boolean {
  return !row.finished && !row.waitingOnAdmin && (row.returned || row.overdue || row.dueThisWeek)
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

function agingColor(id: string, count: number): string {
  if (count === 0) return brand.muted
  if (id === 'OVERDUE_8_PLUS') return '#991B1B'
  if (id === 'OVERDUE_1_7') return semantic.risk.critical
  if (id === 'DUE_0_3') return semantic.risk.high
  if (id === 'DUE_4_7') return semantic.risk.medium
  return brand.gold
}

function postureColor(mod: ContributorDashboardModuleCount): string {
  if (mod.overdueCount > 0) return semantic.risk.critical
  if (mod.openCount > 0) return semantic.risk.medium
  return semantic.risk.low
}

const WEEK_MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

function weekRangeLabel(weekStart: string): string {
  const start = new Date(`${weekStart}T00:00:00`)
  if (Number.isNaN(start.getTime())) return weekStart.slice(5)
  const end = new Date(start)
  end.setDate(start.getDate() + 6)
  const startMonth = WEEK_MONTHS[start.getMonth()]
  const endMonth = WEEK_MONTHS[end.getMonth()]
  if (start.getMonth() === end.getMonth()) {
    return `${startMonth} (${start.getDate()}-${end.getDate()})`
  }
  return `${start.getDate()} ${startMonth}-${end.getDate()} ${endMonth}`
}

function taskMeta(row: ContributorDashboardQueueItem): string {
  const parts = [row.moduleLabel]
  if (row.projectName && row.projectName !== '-') parts.push(row.projectName)
  if (row.assignedByName && row.assignedByName !== '-') parts.push(row.assignedByName)
  return parts.join(' · ')
}

function surfaceButtonSx(clickable: boolean) {
  return {
    textAlign: 'left' as const,
    font: 'inherit',
    width: '100%',
    cursor: clickable ? 'pointer' : 'default',
    transition: 'border-color 160ms ease, box-shadow 160ms ease',
    '&:hover': clickable
      ? { borderColor: brand.goldMid, boxShadow: '0 8px 20px rgba(23,23,23,0.06)' }
      : undefined,
    '&:focus-visible': { outline: `2px solid ${brand.gold}`, outlineOffset: 2 },
  }
}

function ModulePostureCell({
  mod,
  onNavigate,
}: {
  mod: ContributorDashboardModuleCount
  onNavigate: (id: string) => void
}) {
  const color = postureColor(mod)
  const caption = mod.overdueCount > 0 ? `${mod.overdueCount} overdue` : 'Open assigned'
  return (
    <Box
      component="button"
      type="button"
      onClick={() => onNavigate(mod.navTarget)}
      sx={{
        ...surfaceButtonSx(true),
        border: `1px solid ${brand.border}`,
        borderLeft: `3px solid ${color}`,
        borderRadius: '12px',
        bgcolor: alpha(color, 0.06),
        px: 1.75,
        py: 1.5,
        boxShadow: 'none',
        minHeight: 108,
      }}
    >
      <Stack direction="row" spacing={0.75} alignItems="center" justifyContent="space-between">
        <Stack direction="row" spacing={0.75} alignItems="center" sx={{ minWidth: 0 }}>
          <Box sx={{ display: 'flex', color: brand.muted, '& .MuiSvgIcon-root': { fontSize: 16 } }}>
            {MODULE_ICONS[mod.moduleCode] ?? <AssignmentRoundedIcon />}
          </Box>
          <Typography
            noWrap
            sx={{ fontWeight: 700, fontSize: '0.8125rem', letterSpacing: '0.02em', color: brand.goldDark }}
          >
            {mod.label}
          </Typography>
        </Stack>
      </Stack>
      <Typography
        sx={{ fontWeight: 650, fontSize: '1.65rem', letterSpacing: '-0.03em', color: brand.gold, lineHeight: 1.1, mt: 1 }}
      >
        {mod.openCount}
      </Typography>
      <Typography variant="caption" color="text.secondary" noWrap display="block" sx={{ mt: 0.35 }}>
        {caption}
      </Typography>
    </Box>
  )
}

function WorkCard({
  kicker,
  label,
  value,
  icon,
  onClick,
}: {
  kicker: string
  label: string
  value: number
  icon: ReactNode
  onClick: () => void
}) {
  return (
    <Box
      component="button"
      type="button"
      onClick={onClick}
      sx={{
        ...surfaceButtonSx(true),
        border: `1px solid ${brand.border}`,
        borderRadius: '12px',
        bgcolor: '#fff',
        boxShadow: '0 1px 2px rgba(23,23,23,0.04)',
        px: 2,
        py: 1.75,
      }}
    >
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Stack direction="row" spacing={0.75} alignItems="center">
          <Box sx={{ color: brand.goldDark, display: 'flex', '& .MuiSvgIcon-root': { fontSize: 16 } }}>{icon}</Box>
          <Typography sx={{ fontSize: '0.6875rem', fontWeight: 650, letterSpacing: '0.08em', color: brand.muted }}>
            {kicker}
          </Typography>
        </Stack>
        <ChevronRightRoundedIcon sx={{ fontSize: 18, color: brand.muted }} />
      </Stack>
      <Typography sx={{ fontWeight: 700, fontSize: '1.75rem', letterSpacing: '-0.03em', mt: 1.25, lineHeight: 1, color: brand.gold }}>
        {value}
      </Typography>
      {label ? (
        <Typography sx={{ fontSize: '0.8125rem', color: '#5F6368', mt: 0.5 }}>{label}</Typography>
      ) : null}
    </Box>
  )
}

function TaskLine({
  row,
  last,
  locale,
  onOpen,
}: {
  row: ContributorDashboardQueueItem
  last: boolean
  locale: ReturnType<typeof useTenantLocale>['locale']
  onOpen: (taskId: number) => void
}) {
  const chip = statusChip(row)
  return (
    <Box
      component="button"
      type="button"
      onClick={() => onOpen(row.taskId)}
      sx={{
        width: '100%',
        display: 'flex',
        alignItems: 'center',
        gap: 1.5,
        textAlign: 'left',
        font: 'inherit',
        border: 0,
        borderBottom: last ? 0 : `1px solid ${brand.border}`,
        bgcolor: 'transparent',
        px: 2,
        py: 1.35,
        cursor: 'pointer',
        '&:hover': { bgcolor: brand.hover },
        '&:focus-visible': { outline: `2px solid ${brand.gold}`, outlineOffset: -2 },
      }}
    >
      <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: rowDot(row), flexShrink: 0 }} />
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Typography sx={{ fontSize: '0.875rem', fontWeight: 600, color: brand.goldDark }} noWrap>
          {row.title}
        </Typography>
        <Typography variant="caption" color="text.secondary" noWrap display="block">
          {taskMeta(row)}
        </Typography>
      </Box>
      <Typography
        variant="caption"
        color="text.secondary"
        sx={{ display: { xs: 'none', md: 'block' }, flexShrink: 0 }}
      >
        {row.dueDate ? formatTenantDate(row.dueDate, locale) : '-'}
      </Typography>
      <Box sx={{ display: { xs: 'none', sm: 'block' }, flexShrink: 0 }}>
        <StatusChip label={chip.label} tone={chip.tone} />
      </Box>
      <Typography
        sx={{
          fontWeight: 650,
          fontSize: '0.75rem',
          color: brand.goldDark,
          flexShrink: 0,
          minWidth: { sm: 88 },
          textAlign: 'right',
        }}
      >
        {row.agingLabel}
      </Typography>
      <ChevronRightRoundedIcon sx={{ fontSize: 18, color: brand.goldDark, flexShrink: 0 }} />
    </Box>
  )
}

export default function ContributorDashboard({
  onError,
  onNavigate,
  onOpenTask,
}: {
  session: Session
  onError: (msg: string) => void
  onNavigate: (navId: string) => void
  onOpenTask: (taskId: number) => void
}) {
  const { locale } = useTenantLocale()
  const { listReady, markReady } = useListReady()
  const [data, setData] = useState<ContributorDashboardData | null>(null)
  const [filter, setFilter] = useState<QueueFilter>('attention')
  const queueRef = useRef<HTMLDivElement>(null)

  const load = useCallback(() => {
    api
      .getContributorDashboard()
      .then(setData)
      .catch((e) => onError(String(e)))
      .finally(markReady)
  }, [onError, markReady])

  useEffect(() => {
    load()
    const t = setInterval(load, REFRESH_MS)
    return () => clearInterval(t)
  }, [load])

  const queue = useMemo(
    () => (data?.queue ?? []).filter((r) => !isRiskNavHidden(r.navTarget)),
    [data?.queue],
  )
  const byModule = useMemo(
    () => (data?.byModule ?? []).filter((m) => !isRiskNavHidden(m.navTarget)),
    [data?.byModule],
  )
  const openCount = useMemo(
    () => queue.filter((r) => !r.finished && !r.waitingOnAdmin).length,
    [queue],
  )
  const submittedCount = useMemo(() => queue.filter((r) => r.waitingOnAdmin).length, [queue])
  const overdueCount = useMemo(
    () => queue.filter((r) => !r.finished && !r.waitingOnAdmin && r.overdue).length,
    [queue],
  )
  const dueThisWeekCount = useMemo(
    () => queue.filter((r) => !r.finished && !r.waitingOnAdmin && r.dueThisWeek).length,
    [queue],
  )
  const returnedCount = useMemo(
    () => queue.filter((r) => !r.finished && !r.waitingOnAdmin && r.returned).length,
    [queue],
  )
  const closedCount = useMemo(() => queue.filter((r) => r.finished).length, [queue])
  const aging = useMemo(() => {
    const counts = [0, 0, 0, 0, 0]
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    for (const r of queue) {
      if (r.finished || r.waitingOnAdmin) continue
      if (!r.dueDate) {
        counts[0]++
        continue
      }
      const due = new Date(`${r.dueDate}T00:00:00`)
      const days = Math.round((due.getTime() - today.getTime()) / 86_400_000)
      if (days <= -8) counts[4]++
      else if (days < 0) counts[3]++
      else if (days <= 3) counts[2]++
      else if (days <= 7) counts[1]++
      else counts[0]++
    }
    return [
      { id: 'NOT_DUE', label: 'Not due (8d+)', count: counts[0] },
      { id: 'DUE_4_7', label: 'Due in 4-7d', count: counts[1] },
      { id: 'DUE_0_3', label: 'Due in 0-3d', count: counts[2] },
      { id: 'OVERDUE_1_7', label: 'Overdue 1-7d', count: counts[3] },
      { id: 'OVERDUE_8_PLUS', label: 'Overdue 8d+', count: counts[4] },
    ]
  }, [queue])
  const filtered = useMemo(
    () => sortActionQueue(queue.filter((r) => matchesFilter(r, filter))),
    [queue, filter],
  )
  const decideRows = useMemo(() => sortActionQueue(queue.filter(needsAttention)), [queue])
  const decide = decideRows.slice(0, 5)

  const focusQueue = useCallback((next: QueueFilter) => {
    setFilter(next)
    queueRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [])

  if (!listReady || !data) {
    return (
      <>
        <ModuleListShell title="Dashboard" breadcrumbTrail="" />
        <PageLoading label="Loading your assigned work." />
      </>
    )
  }

  const when = briefingWhen(data.generatedAt)
  const decisionLine =
    decideRows.length === 1
      ? '1 item needs attention.'
      : decideRows.length > 1
        ? `${decideRows.length} items need attention.`
        : openCount === 0
          ? 'Nothing is waiting on you.'
          : 'Nothing is due or returned.'
  const weekMax = Math.max(1, ...data.weeklySubmitted.map((w) => w.submittedCount))
  const agingTotal = aging.reduce((sum, bucket) => sum + bucket.count, 0)

  return (
    <Box sx={{ flex: 1, minHeight: 0, width: '100%', overflow: 'auto' }}>
    <Stack
      spacing={2.5}
      sx={{
        width: '100%',
        maxWidth: layout.wideCanvasMax,
        mx: 'auto',
        px: { xs: 2, sm: 2.5 },
        py: 2.5,
        pb: 3,
        boxSizing: 'border-box',
      }}
    >
      <ModuleListShell title="Dashboard" breadcrumbTrail="" />

      <Stack direction="row" alignItems="flex-start" justifyContent="space-between" spacing={2}>
        <Typography
          sx={{
            fontWeight: 650,
            fontSize: { xs: '1.2rem', md: '1.35rem' },
            letterSpacing: '-0.03em',
            lineHeight: 1.25,
            color: brand.goldDark,
          }}
        >
          {when ? `${when}, ${decisionLine.charAt(0).toLowerCase()}${decisionLine.slice(1)}` : decisionLine}
        </Typography>
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
          <Typography variant="caption">Updated {formatTenantDateTime(data.generatedAt, locale)}</Typography>
        </Box>
      </Stack>

      {byModule.length > 0 && (
        <Box>
          <SectionHeader title="Your modules" hint="Open assigned work" />
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', lg: 'repeat(auto-fit, minmax(180px, 1fr))' },
              gap: 1.5,
            }}
          >
            {byModule.map((mod) => (
              <ModulePostureCell key={mod.moduleCode} mod={mod} onNavigate={onNavigate} />
            ))}
          </Box>
        </Box>
      )}

      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', lg: 'minmax(0, 1.4fr) minmax(260px, 0.8fr)' },
          gap: 2.5,
          alignItems: 'start',
        }}
      >
        <Box>
          <SectionHeader
            title="Decide today"
            hint={decideRows.length > 5 ? 'First 5' : decideRows.length === 0 ? 'Clear' : undefined}
            actionLabel="View Queue"
            onAction={() => focusQueue(decideRows.length > 0 ? 'attention' : 'open')}
          />
          {decide.length === 0 ? (
            <Card sx={{ borderRadius: '12px', px: 2, py: 2 }}>
              <Typography variant="body2" color="text.secondary">
                {openCount === 0 ? 'Nothing is waiting on you.' : 'Nothing is due or returned.'}
              </Typography>
            </Card>
          ) : (
            <Card
              sx={{
                overflow: 'hidden',
                borderRadius: '12px',
                boxShadow: '0 1px 2px rgba(23,23,23,0.04)',
                borderTop: `2px solid ${brand.gold}`,
              }}
            >
              {decide.map((row, i) => (
                <TaskLine
                  key={row.taskId}
                  row={row}
                  last={i === decide.length - 1}
                  locale={locale}
                  onOpen={onOpenTask}
                />
              ))}
            </Card>
          )}
        </Box>
        <Box>
          <SectionHeader title="Work" />
          <Stack spacing={1.5}>
            <WorkCard
              kicker="My work"
              label="Open assigned"
              value={openCount}
              icon={<AssignmentIndRoundedIcon />}
              onClick={() => focusQueue('open')}
            />
            <WorkCard
              kicker="Waiting"
              label=""
              value={submittedCount}
              icon={<FactCheckRoundedIcon />}
              onClick={() => focusQueue('admin')}
            />
          </Stack>
        </Box>
      </Box>

      {openCount > 0 && (
        <Box>
          <SectionHeader title="Due picture" hint="Open assigned work by due date" />
          <Card sx={{ borderRadius: '12px', boxShadow: '0 1px 2px rgba(23,23,23,0.04)', px: 2, py: 1.75 }}>
            <Box sx={{ display: 'flex', height: 8, borderRadius: 99, overflow: 'hidden', bgcolor: '#F3F0EA' }}>
              {aging.map((bucket) => {
                if (bucket.count === 0 || agingTotal === 0) return null
                return (
                  <Box
                    key={bucket.id}
                    sx={{ width: `${(bucket.count / agingTotal) * 100}%`, bgcolor: agingColor(bucket.id, bucket.count) }}
                  />
                )
              })}
            </Box>
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: { xs: '1fr 1fr', sm: 'repeat(5, 1fr)' },
                gap: 1.5,
                mt: 1.75,
              }}
            >
              {aging.map((bucket) => {
                const color = agingColor(bucket.id, bucket.count)
                return (
                  <Box key={bucket.id}>
                    <Typography
                      sx={{ fontWeight: 700, fontSize: '1.35rem', letterSpacing: '-0.03em', color, lineHeight: 1 }}
                    >
                      {bucket.count}
                    </Typography>
                    <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                      {bucket.label}
                    </Typography>
                  </Box>
                )
              })}
            </Box>
          </Card>
        </Box>
      )}

      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', xl: 'minmax(0, 1fr) 420px' },
          gap: 2.5,
          alignItems: 'start',
        }}
      >
      <Box ref={queueRef}>
        <SectionHeader title="Your queue" />
        <Box sx={{ mb: 1.5 }}>
          <FilterPillRow
            value={filter}
            onChange={setFilter}
            options={[
              { value: 'attention', label: 'Needs action', count: openCount },
              { value: 'open', label: 'Open', count: openCount },
              { value: 'overdue', label: 'Overdue', count: overdueCount },
              { value: 'week', label: 'This week', count: dueThisWeekCount },
              { value: 'returned', label: 'Returned', count: returnedCount },
              { value: 'admin', label: 'Waiting', count: submittedCount },
              { value: 'closed', label: 'Closed', count: closedCount },
            ]}
          />
        </Box>
        <Card
          sx={{
            overflow: 'hidden',
            borderRadius: '12px',
            boxShadow: '0 1px 2px rgba(23,23,23,0.04)',
            borderTop: `2px solid ${brand.gold}`,
          }}
        >
          {filtered.length === 0 ? (
            <Typography variant="body2" color="text.secondary" sx={{ px: 2, py: 2 }}>
              {emptyQueueMessage(filter)}
            </Typography>
          ) : (
            filtered.map((row, i) => (
              <TaskLine
                key={row.taskId}
                row={row}
                last={i === filtered.length - 1}
                locale={locale}
                onOpen={onOpenTask}
              />
            ))
          )}
        </Card>
      </Box>

      <Box>
        <SectionHeader title="Submitted in the last 6 weeks" />
        <Card sx={{ borderRadius: '12px', boxShadow: '0 1px 2px rgba(23,23,23,0.04)', px: 2, py: 2 }}>
          <Stack direction="row" spacing={1} alignItems="flex-end" sx={{ height: 132 }}>
            {data.weeklySubmitted.map((w) => {
              const bar = Math.max(w.submittedCount > 0 ? 8 : 4, (w.submittedCount / weekMax) * 88)
              return (
                <Stack key={w.weekStart} spacing={0.5} alignItems="center" justifyContent="flex-end" sx={{ flex: 1, minWidth: 0, height: '100%' }}>
                  <Typography sx={{ fontSize: '0.6875rem', fontWeight: 650, color: brand.goldDark, fontVariantNumeric: 'tabular-nums' }}>
                    {w.submittedCount}
                  </Typography>
                  <Box
                    title={`${w.weekStart}: ${w.submittedCount}`}
                    sx={{
                      width: '70%',
                      maxWidth: 36,
                      height: bar,
                      bgcolor: w.submittedCount > 0 ? brand.gold : '#F3F0EA',
                      borderRadius: '3px 3px 0 0',
                    }}
                  />
                  <Typography sx={{ fontSize: '0.625rem', color: brand.muted }} noWrap>
                    {weekRangeLabel(w.weekStart)}
                  </Typography>
                </Stack>
              )
            })}
          </Stack>
        </Card>
      </Box>
      </Box>

    </Stack>
    </Box>
  )
}
