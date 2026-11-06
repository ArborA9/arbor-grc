import { useState, type MouseEvent, type ReactNode } from 'react'
import { Box, IconButton, Menu, MenuItem, Tooltip, Typography } from '@mui/material'
import MoreVertRoundedIcon from '@mui/icons-material/MoreVertRounded'
import type { RiskSummary } from '../../api'
import type { ColumnDefinition } from '../../hooks/columnVisibility'
import { brand } from '../../theme'
import { DomainBadge } from '../ui'
import {
  CE_EXCEL_LABEL,
  EXCEL_TREAT_LABEL,
  LIKELIHOOD_EXCEL_LABEL,
  evaluationDecisionLabel,
  treatmentProgressLabel,
  withinAppetiteLabel,
} from './excelRiskFields'
import {
  riskLevelToBadgeLevel,
  riskStatusBadge,
  type RiskSortDir,
  type RiskSortKey,
} from './riskUi'

const cellSx = {
  minWidth: 0,
  overflow: 'hidden',
} as const

const headerCellSx = {
  ...cellSx,
  lineHeight: 1.25,
  whiteSpace: 'normal' as const,
}

const textSx = {
  fontSize: '0.6875rem',
  color: brand.ink,
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
} as const

function dash(value: string | number | null | undefined): string {
  if (value == null) return '-'
  const s = String(value).trim()
  return s ? s : '-'
}

function isoDate(value: string | null | undefined): string {
  if (!value) return '-'
  return value.length >= 10 ? value.slice(0, 10) : value
}

function Ellipsis({ value, title }: { value: string; title?: string }) {
  return (
    <Typography sx={textSx} title={title ?? (value === '-' ? undefined : value)}>
      {value}
    </Typography>
  )
}

function categoryLabel(r: RiskSummary): string {
  if (r.category === 'OTHER' && r.categoryOther?.trim()) {
    return `Other: ${r.categoryOther.trim()}`
  }
  return r.category.replace(/_/g, ' ')
}

function CategoryTag({ label }: { label: string }) {
  return (
    <Box
      component="span"
      sx={{
        fontSize: '0.6875rem',
        fontWeight: 600,
        px: 1,
        py: 0.25,
        borderRadius: 0.5,
        bgcolor: '#EDE8DF',
        color: brand.muted,
        whiteSpace: 'nowrap',
        maxWidth: '100%',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        display: 'inline-block',
        verticalAlign: 'middle',
        boxSizing: 'border-box',
      }}
    >
      {label}
    </Box>
  )
}

function TreatmentTag({ option }: { option: RiskSummary['treatmentOption'] }) {
  const label = option ? EXCEL_TREAT_LABEL[option] : '-'
  const color =
    option === 'TREAT'
      ? '#5B21B6'
      : option === 'TOLERATE'
        ? '#16A34A'
        : option === 'TRANSFER'
          ? brand.muted
          : option === 'TERMINATE'
            ? '#DC2626'
            : brand.mutedLight
  return (
    <Typography sx={{ fontSize: '0.6875rem', fontWeight: 600, color, whiteSpace: 'nowrap' }}>
      {label}
    </Typography>
  )
}

function ScoreBadge({ level }: { level: RiskSummary['inherentRisk'] | null | undefined }) {
  if (!level) return <Ellipsis value="-" />
  return (
    <DomainBadge
      variant="risk"
      level={riskLevelToBadgeLevel(level)}
      label={level.replace(/_/g, ' ')}
    />
  )
}

function SortHeader({
  label,
  sortKey,
  activeKey,
  sortDir,
  onSort,
}: {
  label: string
  sortKey: RiskSortKey
  activeKey: RiskSortKey | null
  sortDir: RiskSortDir
  onSort: (key: RiskSortKey) => void
}) {
  const active = activeKey === sortKey
  return (
    <Box
      component="button"
      type="button"
      onClick={() => onSort(sortKey)}
      sx={{
        border: 0,
        p: 0,
        m: 0,
        font: 'inherit',
        fontSize: 'inherit',
        fontWeight: 'inherit',
        letterSpacing: 'inherit',
        textTransform: 'inherit',
        color: 'inherit',
        bgcolor: 'transparent',
        cursor: 'pointer',
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: 'flex-start',
        width: '100%',
        textAlign: 'left',
        whiteSpace: 'normal',
        lineHeight: 1.25,
        gap: 0.35,
        '&:hover': { color: brand.goldDark },
      }}
    >
      {label}
      <Box component="span" sx={{ opacity: active ? 1 : 0.45, fontSize: '0.5625rem' }}>
        {active ? (sortDir === 'asc' ? '↑' : '↓') : '↕'}
      </Box>
    </Box>
  )
}

const SORTABLE: Partial<Record<string, RiskSortKey>> = {
  id: 'id',
  inherent: 'inherent',
  residual: 'residual',
}

function canEditRegisterRow(status: RiskSummary['status']): boolean {
  return status === 'DRAFT' || status === 'RE_OPENED'
}

function likelihoodLabel(level: RiskSummary['probability'] | null | undefined): string {
  if (!level) return '-'
  return LIKELIHOOD_EXCEL_LABEL[level] ?? level.replace(/_/g, ' ')
}

function ceLabel(level: RiskSummary['controlEffectiveness'] | null | undefined): string {
  if (!level) return '-'
  return CE_EXCEL_LABEL[level] ?? level.replace(/_/g, ' ')
}

function finalStatusLabel(status: RiskSummary['finalRiskStatus']): string {
  if (status === 'APPROVE') return 'Approve'
  return '-'
}

function renderCell(
  colId: string,
  r: RiskSummary,
  ownerLabel: (userId: number | null | undefined) => string,
): ReactNode {
  const plan = r.activeTreatmentPlan
  switch (colId) {
    case 'id':
      return (
        <Typography
          sx={{
            fontSize: '0.6875rem',
            fontWeight: 700,
            color: brand.goldDark,
            fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
          }}
        >
          {r.displayId}
        </Typography>
      )
    case 'title':
      return <Ellipsis value={dash(r.title)} title={r.title} />
    case 'category':
      return (
        <Box>
          <CategoryTag label={categoryLabel(r)} />
        </Box>
      )
    case 'inherent':
      return (
        <Box>
          <ScoreBadge level={r.inherentRisk} />
        </Box>
      )
    case 'residual':
      return (
        <Box>
          <ScoreBadge level={r.residualRisk} />
        </Box>
      )
    case 'treatment':
      return <TreatmentTag option={r.treatmentOption} />
    case 'status': {
      const status = riskStatusBadge(r.status)
      return (
        <Box>
          <DomainBadge variant="lifecycle" status={status.status} label={status.label} />
        </Box>
      )
    }
    case 'owner': {
      const name = r.riskOwnerName?.trim() || ownerLabel(r.riskOwnerUserId)
      return <Ellipsis value={name || '-'} title={name || undefined} />
    }
    case 'dateIdentified':
      return <Ellipsis value={isoDate(r.dateIdentified)} />
    case 'description':
      return <Ellipsis value={dash(r.description)} />
    case 'observedGap':
      return <Ellipsis value={dash(r.observedGap)} />
    case 'referenceAttachment':
      return <Ellipsis value="-" />
    case 'controlStatus':
      return <Ellipsis value="-" />
    case 'orgUnit':
      return <Ellipsis value={dash(r.orgUnitName)} />
    case 'strategicObjective':
      return <Ellipsis value={dash(r.strategicObjective)} />
    case 'cause':
      return <Ellipsis value={dash(r.cause)} />
    case 'nodeCode':
      return <Ellipsis value={dash(r.nodeCode)} />
    case 'threats':
      return <Ellipsis value={dash(r.threats)} />
    case 'custodian':
      return <Ellipsis value={dash(r.assignedContributorName)} />
    case 'likelihood':
      return <Ellipsis value={likelihoodLabel(r.probability)} />
    case 'impact':
      return <Ellipsis value={likelihoodLabel(r.impact)} />
    case 'existingControls':
      return <Ellipsis value={dash(r.existingControls)} />
    case 'controlEffectiveness':
      return <Ellipsis value={ceLabel(r.controlEffectiveness)} />
    case 'riskReduction':
      return <Ellipsis value={r.riskReductionPct == null ? '-' : `${r.riskReductionPct}%`} />
    case 'residualRating':
      return <Ellipsis value={dash(r.residualRating ?? r.residualRisk.replace(/_/g, ' '))} />
    case 'withinAppetite':
      return <Ellipsis value={withinAppetiteLabel(r.withinAppetite)} />
    case 'evaluationDecision':
      return <Ellipsis value={evaluationDecisionLabel(r.evaluationDecision)} />
    case 'treatmentDescription':
      return <Ellipsis value={dash(r.treatmentChangeJustification)} />
    case 'evidenceRequired':
      return <Ellipsis value={dash(plan?.evidenceRequired)} />
    case 'targetDate':
      return <Ellipsis value={isoDate(plan?.targetDate)} />
    case 'treatmentPlan':
      return <Ellipsis value={dash(plan?.actionDescription ?? r.treatmentPlan)} />
    case 'attachedEvidence':
      return <Ellipsis value="-" />
    case 'targetCe':
      return <Ellipsis value={ceLabel(r.targetControlEffectiveness)} />
    case 'targetResidual':
      return <Ellipsis value={dash(r.targetResidualScore)} />
    case 'treatedRiskScore':
      return <Ellipsis value={dash(r.treatedRiskScore)} />
    case 'acceptanceRequired':
      return (
        <Ellipsis
          value={
            r.acceptanceRequiredLabel?.trim()
              ? r.acceptanceRequiredLabel
              : r.acceptanceRequired == null
                ? '-'
                : r.acceptanceRequired
                  ? 'Yes'
                  : 'No'
          }
        />
      )
    case 'finalRiskStatus':
      return <Ellipsis value={finalStatusLabel(r.finalRiskStatus)} />
    case 'treatmentProgress':
      return (
        <Ellipsis
          value={treatmentProgressLabel({
            startDate: plan?.startDate,
            targetDate: plan?.targetDate,
            status: plan?.status,
          })}
        />
      )
    case 'nextReviewDate':
      return <Ellipsis value={isoDate(r.nextReviewDate)} />
    default:
      return <Ellipsis value="-" />
  }
}

/** Unfinished create form shown as a register row. Not a risk: no Risk ID or status yet. */
export type RiskDraftRow = {
  id: number
  title: string
  untitled: boolean
  treatmentOption?: RiskSummary['treatmentOption'] | null
  inherentRisk?: RiskSummary['inherentRisk'] | null
  residualRisk?: RiskSummary['residualRisk'] | null
  cells: Partial<Record<string, string>>
}

function UnfinishedChip() {
  return (
    <Box
      component="span"
      sx={{
        display: 'inline-block',
        fontSize: '0.625rem',
        fontWeight: 700,
        letterSpacing: '0.02em',
        px: 0.75,
        py: 0.2,
        borderRadius: 0.5,
        border: `1px dashed ${brand.border}`,
        color: brand.muted,
        bgcolor: brand.paper,
        whiteSpace: 'nowrap',
      }}
    >
      Unfinished
    </Box>
  )
}

function renderDraftCell(colId: string, d: RiskDraftRow): ReactNode {
  switch (colId) {
    case 'id':
    case 'status':
      return (
        <Box>
          <UnfinishedChip />
        </Box>
      )
    case 'title':
      return (
        <Typography
          sx={{ ...textSx, fontStyle: d.untitled ? 'italic' : 'normal', color: d.untitled ? brand.muted : brand.ink }}
          title={d.title}
        >
          {d.title}
        </Typography>
      )
    case 'category':
      return d.cells.category ? (
        <Box>
          <CategoryTag label={d.cells.category} />
        </Box>
      ) : (
        <Ellipsis value="-" />
      )
    case 'inherent':
      return (
        <Box>
          <ScoreBadge level={d.inherentRisk} />
        </Box>
      )
    case 'residual':
      return (
        <Box>
          <ScoreBadge level={d.residualRisk} />
        </Box>
      )
    case 'treatment':
      return d.treatmentOption ? <TreatmentTag option={d.treatmentOption} /> : <Ellipsis value="-" />
    default:
      return <Ellipsis value={dash(d.cells[colId])} />
  }
}

export default function RiskRegisterTable({
  rows,
  totalCount: _totalCount,
  selectedId,
  emptyMessage,
  onRowClick,
  onEdit,
  onCopy,
  ownerLabel,
  sortKey,
  sortDir,
  onSort,
  onViewAll,
  visibleColumns,
  gridTemplateColumns,
  draftRows = [],
  onDraftOpen,
  onDraftDelete,
}: {
  rows: RiskSummary[]
  totalCount: number
  selectedId?: number | null
  emptyMessage: string
  onRowClick: (id: number) => void
  onEdit?: (id: number) => void
  onCopy?: (id: number) => void
  ownerLabel: (userId: number | null | undefined) => string
  sortKey: RiskSortKey | null
  sortDir: RiskSortDir
  onSort: (key: RiskSortKey) => void
  onViewAll?: () => void
  visibleColumns: ColumnDefinition[]
  gridTemplateColumns: string
  draftRows?: RiskDraftRow[]
  onDraftOpen?: (draftId: number) => void
  onDraftDelete?: (draftId: number) => void
}) {
  const showActions = Boolean(onCopy || onEdit || (draftRows.length > 0 && (onDraftOpen || onDraftDelete)))
  const tableColumns = showActions ? `${gridTemplateColumns} 2.25rem` : gridTemplateColumns
  const [menu, setMenu] = useState<{ anchor: HTMLElement; row: RiskSummary } | null>(null)
  const [draftMenu, setDraftMenu] = useState<{ anchor: HTMLElement; row: RiskDraftRow } | null>(null)
  const rowSx = {
    display: 'grid',
    gridTemplateColumns: tableColumns,
    columnGap: 1,
    alignItems: 'center',
    width: '100%',
    boxSizing: 'border-box',
    px: 1.75,
    py: 1,
    border: 0,
    borderBottom: `1px solid ${brand.borderLight}`,
    font: 'inherit',
    textAlign: 'left',
    cursor: 'pointer',
    transition: 'background-color .12s ease',
    '& > *': cellSx,
  } as const

  const openMenu = (e: MouseEvent<HTMLElement>, row: RiskSummary) => {
    e.stopPropagation()
    e.preventDefault()
    setMenu({ anchor: e.currentTarget, row })
  }
  const closeMenu = () => setMenu(null)

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: 0, flex: 1 }}>
      <Box sx={{ overflowX: 'hidden', flex: 1, minWidth: 0 }}>
        <Box sx={{ width: '100%', minWidth: 0, maxWidth: '100%' }}>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: tableColumns,
              columnGap: 1,
              alignItems: 'end',
              width: '100%',
              boxSizing: 'border-box',
              px: 1.75,
              py: 0.85,
              bgcolor: brand.tableHeader,
              borderBottom: `1px solid ${brand.border}`,
              fontSize: '0.59375rem',
              fontWeight: 700,
              letterSpacing: '0.06em',
              textTransform: 'uppercase',
              color: brand.mutedLight,
              '& > *': headerCellSx,
            }}
          >
            {visibleColumns.map((col) => {
              const sortable = SORTABLE[col.id]
              return (
                <Box key={col.id}>
                  {sortable ? (
                    <SortHeader
                      label={col.label}
                      sortKey={sortable}
                      activeKey={sortKey}
                      sortDir={sortDir}
                      onSort={onSort}
                    />
                  ) : (
                    col.label
                  )}
                </Box>
              )
            })}
            {showActions ? (
              <Box
                aria-hidden
                sx={{ position: 'sticky', right: 0, bgcolor: brand.tableHeader, zIndex: 1 }}
              />
            ) : null}
          </Box>

          {draftRows.map((d) => (
            <Box
              key={`draft-${d.id}`}
              role="button"
              tabIndex={0}
              aria-label={`Continue draft ${d.title}`}
              data-testid="risk-draft-row"
              onClick={() => onDraftOpen?.(d.id)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault()
                  onDraftOpen?.(d.id)
                }
              }}
              sx={{ ...rowSx, bgcolor: brand.paper, '&:hover': { bgcolor: brand.hover } }}
            >
              {visibleColumns.map((col) => (
                <Box key={col.id}>{renderDraftCell(col.id, d)}</Box>
              ))}
              {showActions ? (
                <Box
                  onClick={(e) => e.stopPropagation()}
                  onKeyDown={(e) => e.stopPropagation()}
                  sx={{
                    display: 'flex',
                    justifyContent: 'flex-end',
                    alignItems: 'center',
                    minWidth: 0,
                    position: 'sticky',
                    right: 0,
                    zIndex: 1,
                    bgcolor: 'inherit',
                  }}
                >
                  <Tooltip title="More actions">
                    <IconButton
                      size="small"
                      aria-label={`Actions for draft ${d.title}`}
                      aria-haspopup="menu"
                      aria-expanded={draftMenu?.row.id === d.id}
                      onClick={(e) => {
                        e.stopPropagation()
                        e.preventDefault()
                        setDraftMenu({ anchor: e.currentTarget, row: d })
                      }}
                      sx={{ width: 28, height: 28, color: brand.muted }}
                    >
                      <MoreVertRoundedIcon sx={{ fontSize: 18 }} />
                    </IconButton>
                  </Tooltip>
                </Box>
              ) : null}
            </Box>
          ))}
          {rows.length === 0 ? (
            draftRows.length === 0 ? (
              <Typography color="text.secondary" sx={{ py: 3, textAlign: 'center' }}>
                {emptyMessage}
              </Typography>
            ) : null
          ) : (
            rows.map((r) => {
              const selected = selectedId === r.id
              return (
                <Box
                  key={r.id}
                  role="button"
                  tabIndex={0}
                  onClick={() => onRowClick(r.id)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault()
                      onRowClick(r.id)
                    }
                  }}
                  sx={{
                    display: 'grid',
                    gridTemplateColumns: tableColumns,
                    columnGap: 1,
                    alignItems: 'center',
                    width: '100%',
                    boxSizing: 'border-box',
                    px: 1.75,
                    py: 1,
                    border: 0,
                    borderBottom: `1px solid ${brand.borderLight}`,
                    bgcolor: selected ? brand.goldLight : brand.paper,
                    font: 'inherit',
                    textAlign: 'left',
                    cursor: 'pointer',
                    transition: 'background-color .12s ease',
                    '&:hover': { bgcolor: selected ? brand.goldLight : brand.hover },
                    '&:last-child': { borderBottom: 0 },
                    '& > *': cellSx,
                  }}
                >
                  {visibleColumns.map((col) => (
                    <Box key={col.id}>{renderCell(col.id, r, ownerLabel)}</Box>
                  ))}
                  {showActions ? (
                    <Box
                      onClick={(e) => e.stopPropagation()}
                      onKeyDown={(e) => e.stopPropagation()}
                      sx={{
                        display: 'flex',
                        justifyContent: 'flex-end',
                        alignItems: 'center',
                        minWidth: 0,
                        position: 'sticky',
                        right: 0,
                        zIndex: 1,
                        bgcolor: 'inherit',
                      }}
                    >
                      {(Boolean(onEdit && canEditRegisterRow(r.status)) || Boolean(onCopy)) ? (
                        <Tooltip title="More actions">
                          <IconButton
                            size="small"
                            aria-label={`Actions for ${r.displayId}`}
                            aria-haspopup="menu"
                            aria-expanded={menu?.row.id === r.id}
                            onClick={(e) => openMenu(e, r)}
                            sx={{ width: 28, height: 28, color: brand.muted }}
                          >
                            <MoreVertRoundedIcon sx={{ fontSize: 18 }} />
                          </IconButton>
                        </Tooltip>
                      ) : null}
                    </Box>
                  ) : null}
                </Box>
              )
            })
          )}
        </Box>
      </Box>

      {onViewAll && (
        <Box
          sx={{
            px: 1.75,
            py: 0.75,
            borderTop: `1px solid ${brand.borderLight}`,
            fontSize: '0.75rem',
            color: brand.mutedLight,
            flexShrink: 0,
          }}
        >
          <Box
            component="button"
            type="button"
            onClick={onViewAll}
            sx={{
              border: 0,
              p: 0,
              m: 0,
              font: 'inherit',
              fontWeight: 600,
              color: brand.goldDark,
              cursor: 'pointer',
              bgcolor: 'transparent',
              '&:hover': { textDecoration: 'underline' },
            }}
          >
            Clear filters
          </Box>
        </Box>
      )}

      <Menu
        anchorEl={menu?.anchor}
        open={Boolean(menu)}
        onClose={closeMenu}
        onClick={(e) => e.stopPropagation()}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        {menu && onEdit && canEditRegisterRow(menu.row.status) ? (
          <MenuItem
            aria-label={`Edit ${menu.row.displayId}`}
            onClick={() => {
              closeMenu()
              onEdit(menu.row.id)
            }}
          >
            Edit
          </MenuItem>
        ) : null}
        {menu && onCopy ? (
          <MenuItem
            aria-label={`Copy ${menu.row.displayId}`}
            onClick={() => {
              closeMenu()
              onCopy(menu.row.id)
            }}
          >
            Copy
          </MenuItem>
        ) : null}
      </Menu>

      <Menu
        anchorEl={draftMenu?.anchor}
        open={Boolean(draftMenu)}
        onClose={() => setDraftMenu(null)}
        onClick={(e) => e.stopPropagation()}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        {draftMenu && onDraftOpen ? (
          <MenuItem
            onClick={() => {
              const id = draftMenu.row.id
              setDraftMenu(null)
              onDraftOpen(id)
            }}
          >
            Continue
          </MenuItem>
        ) : null}
        {draftMenu && onDraftDelete ? (
          <MenuItem
            sx={{ color: '#DC2626' }}
            onClick={() => {
              const id = draftMenu.row.id
              setDraftMenu(null)
              onDraftDelete(id)
            }}
          >
            Delete Draft
          </MenuItem>
        ) : null}
      </Menu>
    </Box>
  )
}
