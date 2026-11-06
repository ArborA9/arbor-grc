import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Box, Card, Stack, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material'
import { api, type HeatMapScoreBasis, type RiskHeatMap, type RiskLevel, type RiskType } from './api'
import { brand, semantic } from './theme'

const AXIS: Record<RiskLevel, string> = {
  VERY_LOW: 'Very low',
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  VERY_HIGH: 'Very high',
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
      return { bg: semantic.heatmap.medium, text: '#92400E', border: semantic.heatmap.medHigh }
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
  const [picture, setPicture] = useState<'bands' | 'matrix'>('bands')
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
  const title = riskType === 'ERM' ? 'Enterprise Risk' : 'Information Security Risk'
  const cellHeight = compact || mode === 'dialog' ? 22 : 28
  /** Dashboard/card/dialog: fill width with a square matrix. Inline: fixed square cells. */
  const squareStretch = mode !== 'inline'
  const nCols = cols.length
  const nRows = rows.length
  const mapMaxWidth = refined ? 460 : compact ? 260 : mode === 'dialog' ? 320 : 360
  const cellGap = refined ? '4px' : '2px'
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
        <Typography sx={{ fontSize: '0.6875rem', fontWeight: 600, color: refined ? brand.goldDark : brand.ink, lineHeight: 1.3 }}>
          {title}
          <Typography component="span" sx={{ fontSize: '0.59375rem', color: brand.mutedLight, ml: 0.5 }}>
            · {map ? `${map.total} risk${map.total === 1 ? '' : 's'} plotted` : busy ? 'loading…' : '…'}
          </Typography>
        </Typography>
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
                  <Typography sx={{ fontSize: '0.8125rem', fontWeight: 600, color: brand.goldDark }}>{AXIS[band.level]}</Typography>
                  <Typography sx={{ fontSize: '0.8125rem', fontWeight: 700, color: brand.gold, fontVariantNumeric: 'tabular-nums' }}>
                    {band.count}
                  </Typography>
                </Stack>
                <Box sx={{ height: 8, bgcolor: '#F3F0EA', borderRadius: 99 }}>
                  <Box
                    sx={{
                      width: `${(band.count / bandMax) * 100}%`,
                      height: '100%',
                      bgcolor: style.bg,
                      borderRadius: 99,
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
                  minWidth: 58,
                  whiteSpace: 'nowrap',
                }}
              >
                {AXIS[r]}
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
                return (
                  <Box
                    key={`${r}-${c}`}
                    component="button"
                    type="button"
                    title={`${rowLabel} ${AXIS[r]} × ${colLabel} ${AXIS[c]} · ${count}`}
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
                      border: `1px solid ${style.border}`,
                      borderRadius: refined ? '6px' : 0.5,
                      bgcolor: style.bg,
                      cursor: 'pointer',
                      font: 'inherit',
                      fontWeight: 700,
                      fontSize: compact ? '0.6875rem' : '0.75rem',
                      color: style.text,
                      transition: 'opacity 100ms ease, transform 100ms ease',
                      '&:hover': {
                        opacity: 0.88,
                        transform: 'scale(1.06)',
                      },
                    }}
                  >
                    {count}
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
              {AXIS[c]}
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
                  <Typography sx={{ fontSize: '0.6875rem', color: brand.muted }}>{AXIS[level]}</Typography>
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
        borderRadius: refined ? '12px' : 1.125,
        boxShadow: refined ? '0 1px 2px rgba(23,23,23,0.04), 0 8px 24px rgba(23,23,23,0.04)' : brand.shadowCard,
        p: refined ? 2.5 : compact ? '14px 16px' : 2.25,
      }}
    >
      {panelBody}
    </Card>
  )
}

/** Compact dual register heat maps for the overview dashboard. */
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
  if (!showIs && !showErm) return null
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', md: showIs && showErm ? '1fr 1fr' : '1fr' },
        gap: 2.5,
      }}
    >
      {showErm && (
        <RiskHeatMapPanel
          riskType="ERM"
          refined
          onError={onError}
          onCellClick={(drill) => onNavigate('erm-risk', { ...drill, riskType: 'ERM' })}
        />
      )}
      {showIs && (
        <RiskHeatMapPanel
          riskType="IS_RISK"
          refined
          onError={onError}
          onCellClick={(drill) => onNavigate('risk', { ...drill, riskType: 'IS_RISK' })}
        />
      )}
    </Box>
  )
}
