import { memo, useMemo, useState } from 'react'
import { Box, Checkbox, Collapse, Typography } from '@mui/material'
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded'
import FlagIcon from '@mui/icons-material/Flag'
import AccountBalanceRoundedIcon from '@mui/icons-material/AccountBalanceRounded'
import CloudOutlinedIcon from '@mui/icons-material/CloudOutlined'
import ShieldRoundedIcon from '@mui/icons-material/ShieldRounded'
import type { GapItem, UserSummary } from '../../api'
import { brand } from '../../theme'
import { DomainBadge } from '../ui'
import { narrativeReadScrollSx } from '../ui/formLayout'
import {
  domainGroupStats,
  gapStatusDisplayLabel,
  gapStatusFromItem,
  groupItemsByDomain,
  groupUaeIaNested,
  type GapDomainGroup,
  type GapScoringProfile,
  type UaeIaL1Group,
  type UaeIaL2Group,
  type UaeIaL3Group,
} from './gapUi'

const GRID_COLS = '90px 1fr 130px 168px 130px'
const LEAF_CODE_INDENT_PX = 20
const rowSelectSx = {
  p: 0.5,
  '& .MuiSvgIcon-root': { fontSize: 16 },
}

function controlCountLabel(n: number) {
  return `${n} control${n === 1 ? '' : 's'}`
}

function TableColumnsHeader({
  isVisible,
  gridTemplateColumns,
  controlIndentPx = 0,
  selectable = false,
  itemIds = [],
  selectedIds,
  onSetSelected,
}: {
  isVisible?: (columnId: string) => boolean
  gridTemplateColumns?: string
  controlIndentPx?: number
  selectable?: boolean
  itemIds?: number[]
  selectedIds?: ReadonlySet<number>
  onSetSelected?: (ids: number[], selected: boolean) => void
}) {
  const selectedCount = itemIds.filter((id) => selectedIds?.has(id)).length
  const allSelected = itemIds.length > 0 && selectedCount === itemIds.length
  const someSelected = selectedCount > 0 && !allSelected
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: gridTemplateColumns ?? GRID_COLS,
        gap: 1.5,
        alignItems: 'center',
        px: 1.75,
        py: 0.75,
        bgcolor: brand.tableHeader,
        borderBottom: `1px solid ${brand.border}`,
        fontSize: '0.59375rem',
        fontWeight: 700,
        letterSpacing: '0.06em',
        textTransform: 'uppercase',
        color: brand.mutedLight,
      }}
    >
      {selectable ? (
        <Checkbox
          size="small"
          checked={allSelected}
          indeterminate={someSelected}
          disabled={itemIds.length === 0}
          onClick={(event) => event.stopPropagation()}
          onChange={() => onSetSelected?.(itemIds, !allSelected)}
          inputProps={{ 'aria-label': allSelected ? 'Clear selected controls' : 'Select controls in this section' }}
          sx={rowSelectSx}
        />
      ) : null}
      <Box sx={controlIndentPx ? { pl: `${controlIndentPx}px` } : undefined}>Control</Box>
      {isVisible?.('title') !== false && <Box>Title</Box>}
      {isVisible?.('gapStatus') !== false && <Box>Gap status</Box>}
      {isVisible?.('evidence') !== false && <Box>Workflow</Box>}
      {isVisible?.('assigned') !== false && <Box>Assigned to</Box>}
    </Box>
  )
}

export function gapControlReleased(item: GapItem): boolean {
  return item.releasedToContributor ?? item.workTaskId != null
}

function decisionSaved(item: GapItem): boolean {
  return Boolean(
    item.implementationStatus ||
      item.maturity != null ||
      item.observations?.trim() ||
      item.gapDescription?.trim(),
  )
}

export function gapWorkflowTone(
  label: string,
): 'notStarted' | 'draft' | 'withContributor' | 'submitted' | 'sentBack' | 'final' | null {
  switch (label) {
    case 'Not Started':
      return 'notStarted'
    case 'Draft':
      return 'draft'
    case 'With Contributor':
      return 'withContributor'
    case 'Submitted':
      return 'submitted'
    case 'Sent Back':
      return 'sentBack'
    case 'Final':
      return 'final'
    default:
      return null
  }
}

export function gapWorkflowLabel(item: GapItem): string {
  if (item.applicability === 'NOT_APPLICABLE') return '-'
  if (item.reviewStatus === 'SENT_BACK') return 'Sent Back'
  if (item.reviewStatus === 'FINALIZED') return 'Final'
  if (item.reviewStatus === 'SUBMITTED' || item.contributorSubmittedAt) return 'Submitted'
  if (gapControlReleased(item)) return 'With Contributor'
  if (decisionSaved(item)) return 'Draft'
  return 'Not Started'
}

export function gapReleaseBlockReason(item: GapItem, sama: boolean): string | null {
  if (item.applicability === 'NOT_APPLICABLE') return `${item.nodeCode} is Not Applicable`
  if (gapControlReleased(item)) return `${item.nodeCode} is already with the contributor`
  if (!item.assignedToUserId) return `${item.nodeCode} has no contributor`
  if (!item.dueDate) return `${item.nodeCode} has no due date`
  if (sama ? item.maturity == null : !item.implementationStatus) return `${item.nodeCode} has no classification`
  if (!item.observations?.trim()) return `${item.nodeCode} has no Gap description`
  return null
}

export function gapSendBackBlockReason(item: GapItem, sama: boolean): string | null {
  if (!gapControlReleased(item) || !item.workTaskId) return `${item.nodeCode} has not been sent`
  if (item.reviewStatus === 'FINALIZED') return `${item.nodeCode} is already final`
  if (item.reviewStatus !== 'SUBMITTED') {
    return `${item.nodeCode} is not waiting for review`
  }
  if (!sama && !item.implementationStatus && !item.finalImplementationStatus) {
    return `${item.nodeCode} has no classification`
  }
  return null
}

function assigneeLabel(item: GapItem, contributors: UserSummary[]): { label: string; email?: string } {
  if (item.applicability === 'NOT_APPLICABLE') return { label: '-' }
  if (!item.assignedToUserId) return { label: 'Unassigned' }
  const user = contributors.find((c) => c.id === item.assignedToUserId)
  if (!user) return { label: `#${item.assignedToUserId}` }
  return { label: user.fullName?.trim() || user.email, email: user.email }
}

function DomainIcon({ domainKey }: { domainKey: string }) {
  const Icon =
    domainKey === 'CLOUD'
      ? CloudOutlinedIcon
      : domainKey.startsWith('SAMA_')
        ? AccountBalanceRoundedIcon
        : domainKey.startsWith('A.')
          ? ShieldRoundedIcon
          : AccountBalanceRoundedIcon
  return <Icon sx={{ fontSize: 16, color: brand.gold }} />
}

function ControlRow({
  item,
  contributors,
  clickable,
  onClick,
  selected,
  isVisible = () => true,
  gridTemplateColumns,
  codeIndentPx = 0,
  selectable = false,
  checked = false,
  onToggleSelected,
}: {
  item: GapItem
  contributors: UserSummary[]
  clickable: boolean
  onClick: () => void
  selected?: boolean
  isVisible?: (columnId: string) => boolean
  gridTemplateColumns?: string
  codeIndentPx?: number
  selectable?: boolean
  checked?: boolean
  onToggleSelected?: (id: number) => void
}) {
  const gapStatus = gapStatusFromItem(item)
  const workflow = gapWorkflowLabel(item)
  const workflowTone = gapWorkflowTone(workflow)
  const assignee = assigneeLabel(item, contributors)

  return (
    <Box
      role={clickable ? 'button' : undefined}
      tabIndex={clickable ? 0 : undefined}
      onClick={clickable ? onClick : undefined}
      onKeyDown={
        clickable
          ? (event) => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault()
                onClick()
              }
            }
          : undefined
      }
      sx={{
        display: 'grid',
        gridTemplateColumns: gridTemplateColumns ?? GRID_COLS,
        gap: 1.5,
        alignItems: 'center',
        width: '100%',
        px: 1.75,
        py: 1.125,
        border: 0,
        borderBottom: `1px solid ${brand.borderLight}`,
        bgcolor: selected ? brand.hover : brand.paper,
        boxShadow: selected ? `inset 3px 0 0 ${brand.gold}` : undefined,
        font: 'inherit',
        textAlign: 'left',
        cursor: clickable ? 'pointer' : 'default',
        contentVisibility: 'auto',
        containIntrinsicSize: '0 48px',
        '&:hover': clickable ? { bgcolor: brand.hover } : undefined,
        '&:last-child': {
          borderBottom: `1px solid ${brand.border}`,
          borderRadius: '0 0 6px 6px',
        },
      }}
    >
      {selectable ? (
        <Checkbox
          size="small"
          checked={checked}
          onClick={(event) => event.stopPropagation()}
          onKeyDown={(event) => event.stopPropagation()}
          onChange={() => onToggleSelected?.(item.id)}
          inputProps={{ 'aria-label': `Select ${item.nodeCode}` }}
          sx={rowSelectSx}
        />
      ) : null}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, minWidth: 0, pl: codeIndentPx ? `${codeIndentPx}px` : 0 }}>
        {item.flaggedForLater && item.reviewStatus !== 'FINALIZED' ? (
          <FlagIcon sx={{ fontSize: 16, color: '#DC2626' }} titleAccess="Flagged for Follow Up" />
        ) : null}
        <Typography
          sx={{
            fontSize: '0.65625rem',
            fontWeight: 700,
            color: brand.goldDark,
          }}
        >
          {item.nodeCode}
        </Typography>
      </Box>
      {isVisible('title') && (
      <Typography
        sx={{
          fontSize: '0.6875rem',
          fontWeight: 500,
          color: brand.ink,
          lineHeight: 1.35,
          ...narrativeReadScrollSx,
        }}
      >
        {item.title}
      </Typography>
      )}
      {isVisible('gapStatus') && (
      <Box>
        <DomainBadge variant="gap" status={gapStatus} label={gapStatusDisplayLabel(gapStatus)} />
      </Box>
      )}
      {isVisible('evidence') && (
      workflowTone ? (
        <Box>
          <DomainBadge variant="workflow" status={workflowTone} label={workflow} />
        </Box>
      ) : (
        <Typography sx={{ fontSize: '0.6875rem', color: brand.muted }}>{workflow}</Typography>
      )
      )}
      {isVisible('assigned') && (
      <Typography
        title={assignee.email}
        sx={{ fontSize: '0.6875rem', color: brand.muted, overflow: 'hidden', textOverflow: 'ellipsis' }}
      >
        {assignee.label}
      </Typography>
      )}
    </Box>
  )
}

function DomainSection({
  group,
  open,
  contributors,
  clickable,
  onToggle,
  onItemClick,
  selectedItemId,
  isVisible,
  gridTemplateColumns,
  selectable = false,
  selectedIds,
  onToggleSelected,
  onSetSelected,
}: {
  group: GapDomainGroup
  open: boolean
  contributors: UserSummary[]
  clickable: boolean
  onToggle: () => void
  onItemClick: (item: GapItem) => void
  selectedItemId?: number
  isVisible?: (columnId: string) => boolean
  gridTemplateColumns?: string
  selectable?: boolean
  selectedIds?: ReadonlySet<number>
  onToggleSelected?: (id: number) => void
  onSetSelected?: (ids: number[], selected: boolean) => void
}) {
  const { assessed, compliant, partial, planned, nonCompliant, notAssessed, total } = domainGroupStats(group.items)
  const segments = [
    { n: compliant, color: '#166534' },
    { n: partial, color: '#CA8A04' },
    { n: planned, color: '#6D28D9' },
    { n: nonCompliant, color: '#B91C1C' },
    { n: notAssessed, color: '#E7E5E4' },
  ]

  return (
    <Box sx={{ borderBottom: `1px solid ${brand.border}` }}>
      <Box
        component="button"
        type="button"
        onClick={onToggle}
        aria-expanded={open}
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr auto', md: 'minmax(0, 1.4fr) 160px 88px auto' },
          alignItems: 'center',
          gap: 1.5,
          width: '100%',
          px: 0.5,
          py: 1.25,
          border: 0,
          bgcolor: 'transparent',
          cursor: 'pointer',
          font: 'inherit',
          textAlign: 'left',
          '&:hover': { bgcolor: brand.hover },
        }}
      >
        <Box sx={{ minWidth: 0 }}>
          <Typography sx={{ fontSize: '0.8125rem', fontWeight: 650, color: brand.ink }}>
            {group.label}
          </Typography>
          <Typography sx={{ fontSize: '0.75rem', color: brand.muted, mt: 0.25 }}>
            {compliant} compliant
            {partial > 0 ? ` · ${partial} partial` : ''}
            {notAssessed > 0 ? ` · ${notAssessed} not assessed` : ''}
          </Typography>
        </Box>
        <Box
          sx={{
            display: { xs: 'none', md: 'flex' },
            height: 6,
            borderRadius: 3,
            overflow: 'hidden',
            bgcolor: '#E7E5E4',
          }}
          aria-hidden
        >
          {segments.filter((part) => part.n > 0).map((part) => (
            <Box
              key={part.color}
              sx={{ width: `${total > 0 ? (part.n / total) * 100 : 0}%`, bgcolor: part.color }}
            />
          ))}
        </Box>
        <Typography
          sx={{
            fontSize: '0.75rem',
            fontWeight: 650,
            textAlign: 'right',
            color:
              total > 0 && assessed === total
                ? '#166534'
                : assessed > 0
                  ? '#CA8A04'
                  : brand.muted,
          }}
        >
          {assessed} / {total}
        </Typography>
        <ExpandMoreRoundedIcon
          sx={{
            fontSize: 16,
            color: brand.mutedLight,
            ml: 'auto',
            transform: open ? 'rotate(0deg)' : 'rotate(-90deg)',
            transition: 'transform .15s ease',
          }}
        />
      </Box>

      <Collapse in={open} unmountOnExit>
        <TableColumnsHeader
          isVisible={isVisible}
          gridTemplateColumns={gridTemplateColumns}
          selectable={selectable}
          itemIds={group.items.map((item) => item.id)}
          selectedIds={selectedIds}
          onSetSelected={onSetSelected}
        />
        {group.items.map((item) => (
          <ControlRow
            key={item.id}
            item={item}
            contributors={contributors}
            clickable={clickable}
            onClick={() => onItemClick(item)}
            selected={selectedItemId === item.id}
            isVisible={isVisible}
            gridTemplateColumns={gridTemplateColumns}
            selectable={selectable}
            checked={selectedIds?.has(item.id) ?? false}
            onToggleSelected={onToggleSelected}
          />
        ))}
      </Collapse>
    </Box>
  )
}

function NestedHeader({
  label,
  meta,
  open,
  onToggle,
  indentPx,
  badge,
}: {
  label: string
  meta?: string
  open: boolean
  onToggle: () => void
  indentPx: number
  badge?: string
}) {
  return (
    <Box
      component="button"
      type="button"
      onClick={onToggle}
      aria-expanded={open}
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 1,
        width: '100%',
        pl: `${indentPx}px`,
        pr: 1.75,
        py: 0.85,
        border: 0,
        borderBottom: `1px solid ${brand.borderLight}`,
        bgcolor: indentPx > 28 ? brand.tableHeader : brand.paper,
        cursor: 'pointer',
        font: 'inherit',
        textAlign: 'left',
        '&:hover': { bgcolor: brand.hover },
      }}
    >
      <Typography sx={{ fontSize: indentPx > 28 ? '0.6875rem' : '0.75rem', fontWeight: 650, color: brand.ink, flex: 1 }}>
        {label}
      </Typography>
      {badge ? <DomainBadge variant="domain" label={badge} /> : null}
      {meta ? (
        <Typography sx={{ fontSize: '0.6875rem', color: brand.mutedLight }}>{meta}</Typography>
      ) : null}
      <ExpandMoreRoundedIcon
        sx={{
          fontSize: 16,
          color: brand.mutedLight,
          transform: open ? 'rotate(0deg)' : 'rotate(-90deg)',
          transition: 'transform .15s ease',
        }}
      />
    </Box>
  )
}

function UaeIaNestedSections({
  items,
  contributors,
  openDomains,
  onToggleDomain,
  onItemClick,
  clickable,
  selectedItemId,
  isVisible,
  gridTemplateColumns,
  selectable = false,
  selectedIds,
  onToggleSelected,
  onSetSelected,
}: {
  items: GapItem[]
  contributors: UserSummary[]
  openDomains: Record<string, boolean>
  onToggleDomain: (key: string) => void
  onItemClick: (item: GapItem) => void
  clickable: boolean
  selectedItemId?: number
  isVisible?: (columnId: string) => boolean
  gridTemplateColumns?: string
  selectable?: boolean
  selectedIds?: ReadonlySet<number>
  onToggleSelected?: (id: number) => void
  onSetSelected?: (ids: number[], selected: boolean) => void
}) {
  const l1Groups = useMemo(() => groupUaeIaNested(items), [items])
  const [openL2, setOpenL2] = useState<Record<string, boolean>>({})
  const [openL3, setOpenL3] = useState<Record<string, boolean>>({})

  if (l1Groups.length === 0) {
    return (
      <Typography color="text.secondary" sx={{ py: 3, textAlign: 'center' }}>
        No controls match this filter.
      </Typography>
    )
  }

  const toggleL2 = (key: string) => setOpenL2((p) => ({ ...p, [key]: !p[key] }))
  const toggleL3 = (key: string) => setOpenL3((p) => ({ ...p, [key]: !p[key] }))

  const renderControlTable = (rows: GapItem[]) => (
    <>
      <TableColumnsHeader
        isVisible={isVisible}
        gridTemplateColumns={gridTemplateColumns}
        controlIndentPx={LEAF_CODE_INDENT_PX}
        selectable={selectable}
        itemIds={rows.map((item) => item.id)}
        selectedIds={selectedIds}
        onSetSelected={onSetSelected}
      />
      {rows.map((item) => (
        <ControlRow
          key={item.id}
          item={item}
          contributors={contributors}
          clickable={clickable}
          onClick={() => onItemClick(item)}
          selected={selectedItemId === item.id}
          isVisible={isVisible}
          gridTemplateColumns={gridTemplateColumns}
          codeIndentPx={LEAF_CODE_INDENT_PX}
          selectable={selectable}
          checked={selectedIds?.has(item.id) ?? false}
          onToggleSelected={onToggleSelected}
        />
      ))}
    </>
  )

  const l2OpensToControls = (l2: UaeIaL2Group) => {
    if (l2.groups.length <= 1) return true
    return l2.groups.every((g) => g.items.length <= 1)
  }

  const renderL3 = (l3: UaeIaL3Group) => {
    const open = openL3[l3.key] ?? false
    return (
      <Box key={l3.key}>
        <NestedHeader
          label={l3.label}
          meta={controlCountLabel(l3.items.length)}
          open={open}
          onToggle={() => toggleL3(l3.key)}
          indentPx={36}
        />
        <Collapse in={open} unmountOnExit>
          {renderControlTable(l3.items)}
        </Collapse>
      </Box>
    )
  }

  const renderL2 = (l2: UaeIaL2Group) => {
    const open = openL2[l2.key] ?? false
    const leafCount = l2.groups.reduce((n, g) => n + g.items.length, 0)
    const showControls = l2OpensToControls(l2)
    const leaves = l2.groups.flatMap((g) => g.items)
    return (
      <Box key={l2.key}>
        <NestedHeader
          label={l2.label}
          meta={controlCountLabel(leafCount)}
          open={open}
          onToggle={() => toggleL2(l2.key)}
          indentPx={16}
        />
        <Collapse in={open} unmountOnExit>
          {showControls ? renderControlTable(leaves) : l2.groups.map(renderL3)}
        </Collapse>
      </Box>
    )
  }

  return (
    <Box>
      {l1Groups.map((l1: UaeIaL1Group) => {
        const open = openDomains[l1.key] ?? false
        const total = l1.groups.reduce((n, l2) => n + l2.groups.reduce((m, l3) => m + l3.items.length, 0), 0)
        return (
          <Box key={l1.key} sx={{ borderBottom: `1px solid ${brand.border}` }}>
            <Box
              component="button"
              type="button"
              onClick={() => onToggleDomain(l1.key)}
              aria-expanded={open}
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1.25,
                width: '100%',
                px: 0.5,
                py: 1.25,
                border: 0,
                bgcolor: 'transparent',
                cursor: 'pointer',
                font: 'inherit',
                textAlign: 'left',
                '&:hover': { bgcolor: brand.hover },
              }}
            >
              <DomainIcon domainKey={l1.key} />
              <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: brand.ink, flex: 1 }}>
                {l1.label}
              </Typography>
              <DomainBadge variant="domain" label={`${total} items`} />
              <ExpandMoreRoundedIcon
                sx={{
                  fontSize: 16,
                  color: brand.mutedLight,
                  ml: 'auto',
                  transform: open ? 'rotate(0deg)' : 'rotate(-90deg)',
                  transition: 'transform .15s ease',
                }}
              />
            </Box>
            <Collapse in={open} unmountOnExit>
              {l1.groups.map(renderL2)}
            </Collapse>
          </Box>
        )
      })}
    </Box>
  )
}

function GapDomainSections({
  items,
  contributors,
  openDomains,
  onToggleDomain,
  onItemClick,
  clickable,
  selectedItemId,
  isVisible,
  gridTemplateColumns,
  scoringProfile = 'ISO_PACK',
  selectable = false,
  selectedIds,
  onToggleSelected,
  onSetSelected,
}: {
  items: GapItem[]
  contributors: UserSummary[]
  openDomains: Record<string, boolean>
  onToggleDomain: (key: string) => void
  onItemClick: (item: GapItem) => void
  clickable: boolean
  selectedItemId?: number
  isVisible?: (columnId: string) => boolean
  gridTemplateColumns?: string
  scoringProfile?: GapScoringProfile
  selectable?: boolean
  selectedIds?: ReadonlySet<number>
  onToggleSelected?: (id: number) => void
  onSetSelected?: (ids: number[], selected: boolean) => void
}) {
  if (scoringProfile === 'UAE_IA') {
    return (
      <UaeIaNestedSections
        items={items}
        contributors={contributors}
        openDomains={openDomains}
        onToggleDomain={onToggleDomain}
        onItemClick={onItemClick}
        clickable={clickable}
        selectedItemId={selectedItemId}
        isVisible={isVisible}
        gridTemplateColumns={gridTemplateColumns}
        selectable={selectable}
        selectedIds={selectedIds}
        onToggleSelected={onToggleSelected}
        onSetSelected={onSetSelected}
      />
    )
  }

  const groups = groupItemsByDomain(items, scoringProfile)

  if (groups.length === 0) {
    return (
      <Typography color="text.secondary" sx={{ py: 3, textAlign: 'center' }}>
        No controls match this filter.
      </Typography>
    )
  }

  return (
    <Box>
      {groups.map((group) => (
        <DomainSection
          key={group.key}
          group={group}
          open={openDomains[group.key] ?? false}
          contributors={contributors}
          clickable={clickable}
          onToggle={() => onToggleDomain(group.key)}
          onItemClick={onItemClick}
          selectedItemId={selectedItemId}
          isVisible={isVisible}
          gridTemplateColumns={gridTemplateColumns}
          selectable={selectable}
          selectedIds={selectedIds}
          onToggleSelected={onToggleSelected}
          onSetSelected={onSetSelected}
        />
      ))}
    </Box>
  )
}

export default memo(GapDomainSections)
