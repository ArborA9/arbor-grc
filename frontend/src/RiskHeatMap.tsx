import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Box, Card, Stack, ToggleButton, ToggleButtonGroup, Typography, alpha } from '@mui/material'
import { api, type HeatMapScoreBasis, type RiskHeatMap, type RiskLevel, type RiskType } from './api'
import { brand, semantic } from './theme'
import {
  ERM_IMPACT_LABEL,
  ERM_LIKELIHOOD_LABEL,
  ERM_RATING_LABEL,
  usesFcaMatrixLabels,
} from './components/risk/ermRiskLabels'

const AXIS: Record<RiskLevel, string> = {
  VERY_LOW: 'Very low',
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  VERY_HIGH: 'Very high',
}

function axisLikelihood(riskType: RiskType, level: RiskLevel): string {
  return usesFcaMatrixLabels(riskType) ? ERM_LIKELIHOOD_LABEL[level] : AXIS[level]
}

function axisImpact(riskType: RiskType, level: RiskLevel): string {
  return usesFcaMatrixLabels(riskType) ? ERM_IMPACT_LABEL[level] : AXIS[level]
}

function axisBand(riskType: RiskType, level: RiskLevel): string {
  return usesFcaMatrixLabels(riskType) ? ERM_RATING_LABEL[level] : AXIS[level]
}

type HeatmapCellStyle = { bg: string; text: string; border: string }

/** Option A heat-map cell colours: full matrix gradient even at count 0 (kit Screen 2). */
export function heatmapCellStyle(band: RiskLevel, _count: number): HeatmapCellStyle {
  switch (band) {
    case 'VERY_HIGH':
      return { bg: semantic.heatmap.critical, text: '#fff', border: semantic.heatmap.critical }
    case 'HIGH':
      return { bg: semantic.heatmap.high, text: '#fff', border: semantic.heatmap.high }
    case 'MEDIUM':
      return { bg: semantic.heatmap.medium, text: '#fff', border: semantic.heatmap.medHigh }
    case 'LOW':
      return { bg: semantic.heatmap.low, text: '#065F46', border: semantic.heatmap.low }
    case 'VERY_LOW':
    default:
      return { bg: semantic.heatmap.veryLow, text: '#94A3B8', border: brand.border }
  }
}

export type RiskHeatMapDrill = {
  scoreBasis: HeatMapScoreBasis
  rowLevel: RiskLevel
  colLevel: RiskLevel
  openOnly: boolean
}

export function riskNavTarget(riskType: RiskType): 'risk' | 'erm-risk' {
  return riskType === 'ERM' ? 'erm-risk' : 'risk'
}

const HEATMAP_CARD_SX = {
  bgcolor: brand.paper,
  border: `1px solid ${brand.border}`,
  borderRadius: 1.125,
  boxShadow: brand.shadowCard,
  p: '14px 16px',
} as const

const LEGEND: RiskLevel[] = ['VERY_HIGH', 'HIGH', 'MEDIUM', 'LOW', 'VERY_LOW']

export function RiskHeatMapPanel({
  riskType,
  compact,
  embedded,
  presentation = 'card',
  refined = false,
  headerAction,
  onCellClick,
  onError,
}: {
  riskType: RiskType
  compact?: boolean
  /** @deprecated use presentation="dialog" */
  embedded?: boolean
  presentation?: 'card' | 'dialog' | 'inline'
  /** Dashboard risk picture: larger cells, legend, gold toggle. Same counts. */
  refined?: boolean
  headerAction?: ReactNode
  onCellClick: (drill: RiskHeatMapDrill) => void
  onError?: (msg: string) => void
}) {
  const mode = embedded ? 'dialog' : presentation
  const [basis, setBasis] = useState<HeatMapScoreBasis>('INHERENT')
  /** Dashboard refined view defaults to the heat matrix (the graphic users expect). */
  const [picture, setPicture] = useState<'bands' | 'matrix'>(refined ? 'matrix' : 'bands')
  const [map, setMap] = useState<RiskHeatMap | null>(null)
  const [busy, setBusy] = useState(false)

  const load = useCallback(() => {
    setBusy(true)
    api
      .getRiskHeatMap(riskType, basis, true)
      .then(setMap)
      .catch((e) => onError?.(String(e)))
      .finally(() => setBusy(false))
  }, [basis, onError, riskType])

  useEffect(() => {
    load()
  }, [load])

  const cellByKey = useMemo(() => {
    const m = new Map<string, RiskHeatMap['cells'][number]>()
    for (const c of map?.cells ?? []) {
      m.set(`${c.rowLevel}|${c.colLevel}`, c)
    }
    return m
  }, [map])

  const rows = map?.rows ?? (['VERY_HIGH', 'HIGH', 'MEDIUM', 'LOW', 'VERY_LOW'] as RiskLevel[])
  const cols = map?.cols ?? (['VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH'] as RiskLevel[])
  const rowLabel = basis === 'INHERENT' ? 'Probability' : 'CE / Residual P'
  const colLabel = basis === 'INHERENT' ? 'Impact' : 'Inherent / Residual I'
  const title = riskType === 'ERM' ? 'Enterprise Risk' : riskType === 'PRIVACY_RISK' ? 'Privacy Risk' : 'Unified Risk'
  const cellHeight = compact || mode === 'dialog' ? 22 : 28
  /** Dashboard/card/dialog: fill width with a square matrix. Inline: fixed square cells. */
  const squareStretch = mode !== 'inline'
  const nCols = cols.length
  const nRows = rows.length
  const mapMaxWidth = refined ? 400 : compact ? 260 : mode === 'dialog' ? 320 : 360
  const cellGap = refined ? '3px' : '2px'
  const showBands = refined && picture === 'bands'
  const bandTotals = useMemo(() => {
    const totals = new Map<RiskLevel, number>()
    for (const level of LEGEND) totals.set(level, 0)
    for (const cell of map?.cells ?? []) {
      totals.set(cell.band, (totals.get(cell.band) ?? 0) + cell.count)
    }
    return LEGEND.map((level) => ({ level, count: totals.get(level) ?? 0 }))
  }, [map])
  const bandMax = Math.max(1, ...bandTotals.map((b) => b.count))

  const panelBody = (
    <>
      <Stack
        direction="row"
        spacing={0.75}
        alignItems="center"
        justifyContent="space-between"
        sx={{ mb: 1.5 }}
      >
        <Box sx={{ minWidth: 0 }}>
          <Typography sx={{ fontSize: refined ? '0.6875rem' : '0.75rem', fontWeight: 700, color: refined ? brand.muted : brand.ink, lineHeight: 1.3 }}>
            {title}
            <Typography component="span" sx={{ fontSize: '0.625rem', color: brand.mutedLight, ml: 0.5, fontWeight: 600 }}>
              · {map ? `${map.total} risk${map.total === 1 ? '' : 's'} plotted` : busy ? 'loading…' : '…'}
            </Typography>
          </Typography>
          {refined && map && map.total > 0 && (
            <Stack direction="row" spacing={0.5} sx={{ mt: 0.65 }} flexWrap="wrap" useFlexGap>
              {bandTotals
                .filter((b) => b.count > 0)
                .slice(0, 4)
                .map((b) => {
                  const style = heatmapCellStyle(b.level, b.count)
                  return (
                    <Box
                      key={b.level}
                      sx={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 0.4,
                        px: 0.65,
                        py: 0.15,
                        borderRadius: '99px',
                        bgcolor: alpha(style.bg, 0.35),
                        border: `1px solid ${alpha(style.border, 0.55)}`,
                      }}
                    >
                      <Box sx={{ width: 6, height: 6, borderRadius: '50%', bgcolor: style.bg }} />
                      <Typography sx={{ fontSize: '0.5625rem', fontWeight: 700, color: brand.goldDark, lineHeight: 1.2 }}>
                        {axisBand(riskType, b.level)} {b.count}
                      </Typography>
                    </Box>
                  )
                })}
            </Stack>
          )}
        </Box>
        <Stack direction="row" spacing={0.75} alignItems="center" flexWrap="wrap" useFlexGap>
          {refined && (
            <ToggleButtonGroup
              size="small"
              exclusive
              value={picture}
              onChange={(_, v) => {
                if (v) setPicture(v)
              }}
              aria-label="Risk picture"
              sx={{
                bgcolor: brand.hover,
                borderRadius: 0.75,
                p: 0.25,
                '& .MuiToggleButton-root': {
                  border: 0,
                  px: 1.25,
                  py: 0.375,
                  fontSize: '0.5625rem',
                  fontWeight: 600,
                  color: brand.muted,
                  '&.Mui-selected': {
                    bgcolor: brand.paper,
                    color: brand.goldDark,
                    boxShadow: `inset 0 0 0 1px ${brand.gold}`,
                  },
                },
              }}
            >
              <ToggleButton value="bands">Bands</ToggleButton>
              <ToggleButton value="matrix">Matrix</ToggleButton>
            </ToggleButtonGroup>
          )}
          <ToggleButtonGroup
            size="small"
            exclusive
            value={basis}
            onChange={(_, v) => {
              if (v) setBasis(v)
            }}
            sx={{
              bgcolor: brand.hover,
              borderRadius: 0.75,
              p: 0.25,
              '& .MuiToggleButton-root': {
                border: 0,
                px: 1.25,
                py: 0.375,
                fontSize: '0.5625rem',
                fontWeight: 600,
                color: brand.muted,
                '&.Mui-selected': {
                  bgcolor: brand.paper,
                  color: refined ? brand.goldDark : brand.ink,
                  boxShadow: refined ? `inset 0 0 0 1px ${brand.gold}` : '0 1px 2px rgba(0,0,0,0.08)',
                },
              },
            }}
          >
            <ToggleButton value="INHERENT">Inherent</ToggleButton>
            <ToggleButton value="RESIDUAL">Residual</ToggleButton>
          </ToggleButtonGroup>
          {headerAction}
        </Stack>
      </Stack>

      {showBands ? (
        <Stack spacing={1.25} sx={{ pt: 0.5 }}>
          {bandTotals.map((band) => {
            const style = heatmapCellStyle(band.level, band.count)
            return (
              <Box key={band.level}>
                <Stack direction="row" justifyContent="space-between" sx={{ mb: 0.4 }}>
                  <Typography sx={{ fontSize: '0.8125rem', fontWeight: 600, color: brand.goldDark }}>
                    {axisBand(riskType, band.level)}
                  </Typography>
                  <Typography sx={{ fontSize: '0.8125rem', fontWeight: 700, color: brand.gold, fontVariantNumeric: 'tabular-nums' }}>
                    {band.count}
                  </Typography>
                </Stack>
                <Box sx={{ height: refined ? 12 : 8, bgcolor: '#F3F0EA', borderRadius: 99, overflow: 'hidden' }}>
                  <Box
                    sx={{
                      width: `${(band.count / bandMax) * 100}%`,
                      height: '100%',
                      bgcolor: style.bg,
                      borderRadius: 99,
                      boxShadow: band.count > 0 ? `inset 0 0 0 1px ${alpha(style.border, 0.4)}` : undefined,
                    }}
                  />
                </Box>
              </Box>
            )
          })}
        </Stack>
      ) : (
      <Box
        sx={{
          width: squareStretch ? '100%' : 'fit-content',
          maxWidth: squareStretch ? mapMaxWidth : undefined,
          mx: squareStretch ? 'auto' : 0,
        }}
      >
        <Box sx={{ display: 'flex', gap: 0.75, alignItems: 'stretch' }}>
          <Box
            sx={{
              display: 'flex',
              flexDirection: 'column',
              gap: 0.25,
              flexShrink: 0,
              alignSelf: 'stretch',
            }}
          >
            {rows.map((r) => (
              <Typography
                key={`y-${r}`}
                sx={{
                  flex: 1,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'flex-end',
                  pr: 0.25,
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  color: brand.mutedLight,
                  minWidth: usesFcaMatrixLabels(riskType) ? 108 : 58,
                  whiteSpace: 'nowrap',
                }}
              >
                {axisLikelihood(riskType, r)}
              </Typography>
            ))}
          </Box>

          <Box
            sx={{
              flex: squareStretch ? 1 : undefined,
              width: squareStretch ? undefined : nCols * cellHeight + (nCols - 1) * 2,
              minWidth: 0,
              aspectRatio: '1 / 1',
              display: 'grid',
              gridTemplateColumns: `repeat(${nCols}, 1fr)`,
              gridTemplateRows: `repeat(${nRows}, 1fr)`,
              gap: cellGap,
            }}
          >
            {rows.flatMap((r) =>
              cols.map((c) => {
                const cell = cellByKey.get(`${r}|${c}`)
                const count = cell?.count ?? 0
                const band = cell?.band ?? 'MEDIUM'
                const style = heatmapCellStyle(band, count)
                const hot = refined && count > 0 && (band === 'VERY_HIGH' || band === 'HIGH')
                return (
                  <Box
                    key={`${r}-${c}`}
                    component="button"
                    type="button"
                    title={`${rowLabel} ${axisLikelihood(riskType, r)} × ${colLabel} ${axisImpact(riskType, c)} · ${count}`}
                    onClick={() =>
                      onCellClick({
                        scoreBasis: basis,
                        rowLevel: r,
                        colLevel: c,
                        openOnly: true,
                      })
                    }
                    sx={{
                      m: 0,
                      p: 0,
                      width: '100%',
                      height: '100%',
                      minWidth: 0,
                      minHeight: 0,
                      border: `1px solid ${count > 0 ? style.border : brand.border}`,
                      borderRadius: refined ? '8px' : 0.5,
                      bgcolor: style.bg,
                      opacity: count === 0 ? (refined ? 0.38 : 0.55) : 1,
                      cursor: 'pointer',
                      font: 'inherit',
                      fontWeight: 700,
                      fontSize: refined
                        ? count > 0
                          ? '0.875rem'
                          : '0.6875rem'
                        : compact
                          ? '0.6875rem'
                          : '0.75rem',
                      color: style.text,
                      boxShadow: hot ? `0 0 0 1px ${style.border}, 0 6px 14px ${alpha(style.bg, 0.55)}` : undefined,
                      transition: 'opacity 100ms ease, transform 100ms ease, box-shadow 120ms ease',
                      '&:hover': {
                        opacity: 1,
                        transform: 'scale(1.07)',
                        zIndex: 1,
                      },
                    }}
                  >
                    {count > 0 ? count : refined ? '' : 0}
                  </Box>
                )
              }),
            )}
          </Box>
        </Box>

        <Box sx={{ display: 'flex', gap: cellGap, mt: 0.75, pl: '66px' }}>
          {cols.map((c) => (
            <Typography
              key={`x-${c}`}
              sx={{
                flex: 1,
                textAlign: 'center',
                fontSize: '0.75rem',
                fontWeight: 600,
                color: brand.muted,
                lineHeight: 1.2,
              }}
            >
              {axisImpact(riskType, c)}
            </Typography>
          ))}
        </Box>
        <Typography
          sx={{
            textAlign: 'center',
            mt: 0.25,
            pl: '26px',
            fontSize: '0.6875rem',
            color: brand.muted,
          }}
        >
          {colLabel}
        </Typography>
        {refined && (
          <Stack direction="row" spacing={1.25} justifyContent="center" flexWrap="wrap" useFlexGap sx={{ mt: 1.5 }}>
            {LEGEND.map((level) => {
              const style = heatmapCellStyle(level, 1)
              return (
                <Stack key={level} direction="row" spacing={0.5} alignItems="center">
                  <Box sx={{ width: 10, height: 10, borderRadius: '3px', bgcolor: style.bg }} />
                  <Typography sx={{ fontSize: '0.6875rem', color: brand.muted }}>
                    {axisBand(riskType, level)}
                  </Typography>
                </Stack>
              )
            })}
          </Stack>
        )}
      </Box>
      )}

      {map && map.total === 0 && (
        <Typography sx={{ mt: 1, color: semantic.risk.low, fontWeight: 600, textAlign: 'center', fontSize: '0.75rem' }}>
          All good: nothing to action
        </Typography>
      )}
    </>
  )

  if (mode === 'dialog') {
    return <Box sx={HEATMAP_CARD_SX}>{panelBody}</Box>
  }

  if (mode === 'inline') {
    return <Box>{panelBody}</Box>
  }

  return (
    <Card
      sx={{
        border: `1px solid ${brand.border}`,
        borderRadius: refined ? '10px' : 1.125,
        boxShadow: refined ? '0 1px 2px rgba(23,23,23,0.04)' : brand.shadowCard,
        p: refined ? 1.5 : compact ? '14px 16px' : 2.25,
        height: refined ? '100%' : undefined,
        width: '100%',
        boxSizing: 'border-box',
      }}
    >
      {panelBody}
    </Card>
  )
}

/** Portfolio severity stack for Overview (uses open inherent heat maps). */
function RiskPortfolioComposition({
  showIs,
  showErm,
  onError,
}: {
  showIs: boolean
  showErm: boolean
  onError?: (msg: string) => void
}) {
  const [erm, setErm] = useState<RiskHeatMap | null>(null)
  const [isMap, setIsMap] = useState<RiskHeatMap | null>(null)

  useEffect(() => {
    let cancelled = false
    const loads: Promise<void>[] = []
    if (showErm) {
      loads.push(
        api
          .getRiskHeatMap('ERM', 'INHERENT', true)
          .then((m) => {
            if (!cancelled) setErm(m)
          })
          .catch((e) => onError?.(String(e))),
      )
    }
    if (showIs) {
      loads.push(
        api
          .getRiskHeatMap('IS_RISK', 'INHERENT', true)
          .then((m) => {
            if (!cancelled) setIsMap(m)
          })
          .catch((e) => onError?.(String(e))),
      )
    }
    void Promise.all(loads)
    return () => {
      cancelled = true
    }
  }, [showErm, showIs, onError])

  const bands = useMemo(() => {
    const totals = new Map<RiskLevel, number>()
    for (const level of LEGEND) totals.set(level, 0)
    for (const map of [erm, isMap]) {
      if (!map) continue
      for (const cell of map.cells) {
        totals.set(cell.band, (totals.get(cell.band) ?? 0) + cell.count)
      }
    }
    return LEGEND.map((level) => ({ level, count: totals.get(level) ?? 0 }))
  }, [erm, isMap])

  const total = bands.reduce((s, b) => s + b.count, 0)
  const ermTotal = erm?.total ?? 0
  const isTotal = isMap?.total ?? 0
  const donutSize = 112
  const donutStroke = 16
  const donutR = (donutSize - donutStroke) / 2
  const donutC = 2 * Math.PI * donutR

  return (
    <Card
      sx={{
        border: `1px solid ${brand.border}`,
        borderRadius: '10px',
        boxShadow: '0 1px 2px rgba(23,23,23,0.04)',
        p: 1.5,
        height: '100%',
        width: '100%',
        boxSizing: 'border-box',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" spacing={1} sx={{ mb: 1.25 }}>
        <Box>
          <Typography sx={{ fontSize: '0.6875rem', fontWeight: 700, color: brand.muted, letterSpacing: '0.04em', textTransform: 'uppercase' }}>
            Composition
          </Typography>
          <Typography sx={{ fontSize: '0.625rem', color: brand.mutedLight, mt: 0.2 }}>
            Open inherent bands
          </Typography>
        </Box>
        <Stack direction="row" spacing={1.5} alignItems="center">
          {showErm && (
            <Typography sx={{ fontSize: '0.6875rem', fontWeight: 650, color: brand.muted }}>
              ERM {ermTotal}
            </Typography>
          )}
          {showIs && (
            <Typography sx={{ fontSize: '0.6875rem', fontWeight: 650, color: brand.muted }}>
              Unified {isTotal}
            </Typography>
          )}
          <Typography sx={{ fontSize: '0.8125rem', fontWeight: 700, color: brand.goldDark, fontVariantNumeric: 'tabular-nums' }}>
            {total} open
          </Typography>
        </Stack>
      </Stack>

      {total === 0 ? (
        <Typography sx={{ color: semantic.risk.low, fontWeight: 600, fontSize: '0.8125rem' }}>
          All good: nothing to action
        </Typography>
      ) : (
        <Stack spacing={1.25} alignItems="stretch">
          <Box sx={{ position: 'relative', width: donutSize, height: donutSize, mx: 'auto' }}>
            <Box
              component="svg"
              width={donutSize}
              height={donutSize}
              viewBox={`0 0 ${donutSize} ${donutSize}`}
              aria-hidden
              sx={{ display: 'block', transform: 'rotate(-90deg)' }}
            >
              {(() => {
                let offset = 0
                return bands.map((b) => {
                  if (b.count === 0) return null
                  const style = heatmapCellStyle(b.level, b.count)
                  const len = (b.count / total) * donutC
                  const node = (
                    <circle
                      key={b.level}
                      cx={donutSize / 2}
                      cy={donutSize / 2}
                      r={donutR}
                      fill="none"
                      stroke={style.bg}
                      strokeWidth={donutStroke}
                      strokeDasharray={`${len} ${donutC - len}`}
                      strokeDashoffset={-offset}
                      strokeLinecap="butt"
                    />
                  )
                  offset += len
                  return node
                })
              })()}
            </Box>
            <Box
              sx={{
                position: 'absolute',
                inset: 0,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Typography sx={{ fontWeight: 750, fontSize: '1.25rem', letterSpacing: '-0.04em', color: brand.inkSoft, lineHeight: 1, fontVariantNumeric: 'tabular-nums' }}>
                {total}
              </Typography>
              <Typography sx={{ fontSize: '0.625rem', fontWeight: 700, letterSpacing: '0.06em', color: brand.muted, mt: 0.35 }}>
                OPEN
              </Typography>
            </Box>
          </Box>

          <Box>
            <Box sx={{ height: 12, borderRadius: 99, overflow: 'hidden', display: 'flex', bgcolor: '#F3F0EA', mb: 1.25 }}>
              {bands.map((b) => {
                if (b.count === 0) return null
                const style = heatmapCellStyle(b.level, b.count)
                const pct = (b.count / total) * 100
                return (
                  <Box
                    key={b.level}
                    title={`${axisBand('IS_RISK', b.level)}: ${b.count} (${Math.round(pct)}%)`}
                    sx={{
                      width: `${pct}%`,
                      minWidth: pct > 0 ? 4 : 0,
                      height: '100%',
                      bgcolor: style.bg,
                      transition: 'width 240ms ease',
                    }}
                  />
                )
              })}
            </Box>
            <Stack spacing={0.75}>
              {bands.map((b) => {
                const style = heatmapCellStyle(b.level, b.count)
                const pct = total > 0 ? Math.round((b.count / total) * 100) : 0
                return (
                  <Stack key={b.level} direction="row" alignItems="center" spacing={1}>
                    <Box sx={{ width: 8, height: 8, borderRadius: '2px', bgcolor: style.bg, flexShrink: 0, opacity: b.count ? 1 : 0.35 }} />
                    <Typography sx={{ flex: 1, fontSize: '0.6875rem', fontWeight: 650, color: brand.muted }} noWrap>
                      {axisBand('IS_RISK', b.level)}
                    </Typography>
                    <Typography sx={{ fontSize: '0.75rem', fontWeight: 750, color: brand.goldDark, fontVariantNumeric: 'tabular-nums' }}>
                      {b.count}
                    </Typography>
                    <Typography sx={{ fontSize: '0.625rem', color: brand.mutedLight, width: 28, textAlign: 'right' }}>{pct}%</Typography>
                  </Stack>
                )
              })}
            </Stack>
          </Box>
        </Stack>
      )}
    </Card>
  )
}

/** Dense command-center: slim composition + one large matrix (ERM / Unified tabs). */
export function DashboardRiskHeatMaps({
  showIs,
  showErm,
  onNavigate,
  onError,
}: {
  showIs: boolean
  showErm: boolean
  onNavigate: (navId: string, drill?: RiskHeatMapDrill & { riskType: RiskType }) => void
  onError: (msg: string) => void
}) {
  const defaultType: RiskType = showIs ? 'IS_RISK' : 'ERM'
  const [active, setActive] = useState<RiskType>(defaultType)

  useEffect(() => {
    if (active === 'IS_RISK' && !showIs && showErm) setActive('ERM')
    if (active === 'ERM' && !showErm && showIs) setActive('IS_RISK')
  }, [active, showErm, showIs])

  if (!showIs && !showErm) return null

  const both = showIs && showErm
  const riskType: RiskType = both ? active : defaultType

  const registerTabs = both ? (
    <ToggleButtonGroup
      size="small"
      exclusive
      value={active}
      onChange={(_, v: RiskType | null) => {
        if (v) setActive(v)
      }}
      aria-label="Risk register heat map"
      sx={{
        bgcolor: brand.hover,
        borderRadius: 0.75,
        p: 0.25,
        '& .MuiToggleButton-root': {
          border: 0,
          px: 1.25,
          py: 0.35,
          fontSize: '0.625rem',
          fontWeight: 650,
          color: brand.muted,
          textTransform: 'none',
          '&.Mui-selected': {
            bgcolor: brand.paper,
            color: brand.goldDark,
            boxShadow: `inset 0 0 0 1px ${brand.gold}`,
          },
        },
      }}
    >
      <ToggleButton value="IS_RISK">Unified</ToggleButton>
      <ToggleButton value="ERM">Enterprise</ToggleButton>
    </ToggleButtonGroup>
  ) : undefined

  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: {
          xs: '1fr',
          lg: 'minmax(200px, 240px) minmax(0, 1fr)',
        },
        gap: 1.25,
        alignItems: 'stretch',
        height: '100%',
      }}
    >
      <Box sx={{ height: '100%', minHeight: 0, display: 'flex' }}>
        <RiskPortfolioComposition showIs={showIs} showErm={showErm} onError={onError} />
      </Box>
      <Box sx={{ minWidth: 0, height: '100%', display: 'flex', flexDirection: 'column' }}>
        <RiskHeatMapPanel
          riskType={riskType}
          refined
          headerAction={registerTabs}
          onError={onError}
          onCellClick={(drill) =>
            onNavigate(riskType === 'ERM' ? 'erm-risk' : 'risk', { ...drill, riskType })
          }
        />
      </Box>
    </Box>
  )
}
