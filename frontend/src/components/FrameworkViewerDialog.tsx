import { useEffect, useMemo, useState, type ReactNode } from 'react'
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  IconButton,
  InputAdornment,
  Stack,
  Switch,
  TextField,
  Typography,
} from '@mui/material'
import CloseRoundedIcon from '@mui/icons-material/CloseRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded'
import { api, type FrameworkDetail, type FrameworkNode, type FrameworkQuestion } from '../api'
import { brand } from '../theme'
import StatusChip from './StatusChip'
import FrameworkNodeEditorPanel from './FrameworkNodeEditorPanel'
import {
  applicabilityOf,
  bodyFieldLabel,
  evidenceOf,
  guidanceOf,
  resolveViewerProfile,
  statusPercentOf,
  whatItMeansOf,
} from './frameworkViewerProfile'

const MONO = 'ui-monospace, SFMono-Regular, Menlo, monospace'

function frameworkStatusTone(status: string): 'success' | 'warning' | 'info' | 'neutral' {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'VALIDATED') return 'info'
  if (status === 'DRAFT') return 'warning'
  return 'neutral'
}

/** Split concatenated Description + Requirement from checklist import. */
export function splitDefinition(def: string | null | undefined): {
  description: string | null
  requirement: string | null
} {
  if (!def || !def.trim()) return { description: null, requirement: null }
  const trimmed = def.trim()
  const parts = trimmed.split(/\n\n+/)
  if (parts.length >= 2) {
    return { description: parts[0], requirement: parts.slice(1).join('\n\n') }
  }
  if (/^Expectation:/i.test(trimmed)) {
    return { description: null, requirement: trimmed }
  }
  return { description: trimmed, requirement: null }
}

function isControlNode(n: FrameworkNode): boolean {
  const kind = (n.recordType || '').toUpperCase()
  if (kind === 'SECTION') return false
  if (n.inSoa || n.inGapAssessment || n.inAuditChecklist || n.inRiskRegister) return true
  if (['CONTROL', 'ANNEX_CONTROL', 'CLAUSE', 'CLOUD', 'ARTICLE'].includes(kind)) return true
  return Boolean(n.definition && n.definition.trim())
}

function nodeAttr(n: FrameworkNode, key: string): string | null {
  const v = n.attributes?.[key]
  return v && v.trim() ? v.trim() : null
}

function sheetTabOf(n: FrameworkNode): string | null {
  return nodeAttr(n, 'sheet_tab')
}

/** Distinct Excel sheet tabs in first-seen displayOrder. */
function deriveSheetTabs(nodes: FrameworkNode[]): string[] {
  const seen = new Set<string>()
  const tabs: string[] = []
  const ordered = [...nodes].sort((a, b) => a.displayOrder - b.displayOrder || a.code.localeCompare(b.code))
  for (const n of ordered) {
    const tab = sheetTabOf(n)
    if (tab && !seen.has(tab)) {
      seen.add(tab)
      tabs.push(tab)
    }
  }
  return tabs
}

function byDisplayOrder(a: FrameworkNode, b: FrameworkNode): number {
  return a.displayOrder - b.displayOrder || a.code.localeCompare(b.code)
}

/** Domain objective on this node or nearest ancestor (UAE IA Domain Objectives sheet). */
function resolveDomainObjective(
  node: FrameworkNode,
  byId: Map<number, FrameworkNode>,
): string | null {
  let cur: FrameworkNode | undefined = node
  const guard = new Set<number>()
  while (cur && !guard.has(cur.id)) {
    guard.add(cur.id)
    const obj = nodeAttr(cur, 'domain_objective')
    if (obj) return obj
    cur = cur.parentId != null ? byId.get(cur.parentId) : undefined
  }
  return null
}

function recordTypeLabel(recordType: string | null | undefined): string | null {
  if (!recordType) return null
  switch (recordType.toUpperCase()) {
    case 'CLAUSE':
      return 'Clause'
    case 'ANNEX_CONTROL':
      return 'Annexure'
    case 'CLOUD':
      return 'Cloud'
    case 'ARTICLE':
      return 'Article'
    case 'CONTROL':
      return 'Control'
    case 'SECTION':
      return 'Section'
    default:
      return recordType
  }
}

export default function FrameworkViewerDialog({
  open,
  detail,
  onClose,
  headerActions,
  extraTabs,
  editable = false,
  onDetailChange,
  onEditError,
  questions = [],
}: {
  open: boolean
  detail: FrameworkDetail | null
  onClose: () => void
  /** Buttons next to the title (Validate / Publish / etc.). */
  headerActions?: ReactNode
  /** Optional tabs below the header (e.g. Super Admin Tenants panel). */
  extraTabs?: { id: string; label: string; content: ReactNode }[]
  /** Super Admin Phase 6C: enable in-place catalog editing. */
  editable?: boolean
  onDetailChange?: (detail: FrameworkDetail) => void
  onEditError?: (message: string) => void
  /** Active question bank for this framework (filtered per selected node). */
  questions?: FrameworkQuestion[]
}) {
  const [query, setQuery] = useState('')
  const [showFolders, setShowFolders] = useState(true)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [tab, setTab] = useState('library')
  /** Tenant viewer: load questions per node when parent did not pass the bank. */
  const [nodeQuestionCache, setNodeQuestionCache] = useState<FrameworkQuestion[]>([])
  const [expandedIds, setExpandedIds] = useState<Set<number>>(() => new Set())

  const sheetTabs = useMemo(() => (detail ? deriveSheetTabs(detail.nodes) : []), [detail])
  const byId = useMemo(() => {
    const m = new Map<number, FrameworkNode>()
    detail?.nodes.forEach((n) => m.set(n.id, n))
    return m
  }, [detail])

  const nodesInSheet = useMemo(() => {
    if (!detail) return []
    if (sheetTabs.length === 0) return [...detail.nodes].sort(byDisplayOrder)
    if (sheetTabs.length === 1 || !sheetTabs.includes(tab)) {
      const only = sheetTabs[0]
      return detail.nodes
        .filter((n) => {
          const st = sheetTabOf(n)
          if (st === only) return true
          if (st) return false
          // Ancestors without sheet_tab: keep if any descendant is on this sheet
          return detail.nodes.some(
            (d) => sheetTabOf(d) === only && isAncestorOf(n.id, d, byId),
          )
        })
        .sort(byDisplayOrder)
    }
    return detail.nodes
      .filter((n) => {
        const st = sheetTabOf(n)
        if (st === tab) return true
        if (st) return false
        return detail.nodes.some((d) => sheetTabOf(d) === tab && isAncestorOf(n.id, d, byId))
      })
      .sort(byDisplayOrder)
  }, [detail, sheetTabs, tab, byId])

  useEffect(() => {
    if (!detail) {
      setSelectedId(null)
      setQuery('')
      setTab('library')
      return
    }
    const tabs = deriveSheetTabs(detail.nodes)
    setTab(tabs[0] ?? 'library')
    setQuery('')
  }, [detail?.framework.id])

  useEffect(() => {
    if (!detail) return
    const pool = nodesInSheet
    const firstControl = pool.find(isControlNode) ?? pool[0]
    setSelectedId(firstControl?.id ?? null)
  }, [detail?.framework.id, tab])

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return nodesInSheet.filter((n) => {
      if (!showFolders && !isControlNode(n)) return false
      if (!q) return true
      const { description, requirement } = splitDefinition(n.definition)
      const hay = [
        n.code,
        n.title,
        n.controlType,
        n.priority,
        description,
        requirement,
        nodeAttr(n, 'what_it_means'),
        nodeAttr(n, 'evidence'),
        nodeAttr(n, 'evidence_examples'),
        resolveDomainObjective(n, byId),
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
      return hay.includes(q)
    })
  }, [nodesInSheet, query, showFolders, byId])

  const viewerProfile = resolveViewerProfile(detail?.framework.code)
  const useUaeIaTree = viewerProfile === 'UAE_IA' && showFolders

  const sheetIdSet = useMemo(() => new Set(nodesInSheet.map((n) => n.id)), [nodesInSheet])

  const treeVisibleIds = useMemo(() => {
    if (!useUaeIaTree) return new Set<number>()
    const q = query.trim()
    if (!q) return new Set(nodesInSheet.map((n) => n.id))
    const vis = new Set<number>()
    for (const n of filtered) {
      vis.add(n.id)
      let cur = n.parentId != null ? byId.get(n.parentId) : undefined
      const guard = new Set<number>()
      while (cur && !guard.has(cur.id)) {
        guard.add(cur.id)
        vis.add(cur.id)
        cur = cur.parentId != null ? byId.get(cur.parentId) : undefined
      }
    }
    return vis
  }, [useUaeIaTree, query, filtered, nodesInSheet, byId])

  const treeRoots = useMemo(() => {
    if (!useUaeIaTree) return []
    return nodesInSheet
      .filter((n) => treeVisibleIds.has(n.id) && (n.parentId == null || !sheetIdSet.has(n.parentId)))
      .sort(byDisplayOrder)
  }, [useUaeIaTree, nodesInSheet, treeVisibleIds, sheetIdSet])

  useEffect(() => {
    if (!detail || resolveViewerProfile(detail.framework.code) !== 'UAE_IA') return
    const l1 = nodesInSheet.filter((n) => n.level === 1)
    const ids = (l1.length > 0 ? l1 : nodesInSheet.filter((n) => n.parentId == null)).map((n) => n.id)
    setExpandedIds(new Set(ids))
  }, [detail?.framework.id, tab])

  useEffect(() => {
    if (!useUaeIaTree || !query.trim()) return
    setExpandedIds((prev) => {
      const next = new Set(prev)
      for (const id of treeVisibleIds) {
        const kids = nodesInSheet.some((n) => n.parentId === id && treeVisibleIds.has(n.id))
        if (kids) next.add(id)
      }
      return next
    })
  }, [useUaeIaTree, query, treeVisibleIds, nodesInSheet])

  const selected = useMemo(
    () => detail?.nodes.find((n) => n.id === selectedId) ?? filtered[0] ?? null,
    [detail, selectedId, filtered],
  )

  useEffect(() => {
    if (selected && !filtered.some((n) => n.id === selected.id) && filtered[0]) {
      setSelectedId(filtered[0].id)
    }
  }, [filtered, selected])

  const nodeQuestions = useMemo(() => {
    if (!selected) return []
    const pool = questions.length > 0 ? questions : nodeQuestionCache
    return pool
      .filter((q) => q.frameworkNodeId === selected.id)
      .slice()
      .sort((a, b) => a.displayOrder - b.displayOrder || a.questionCode.localeCompare(b.questionCode))
  }, [questions, nodeQuestionCache, selected])

  useEffect(() => {
    if (!detail || !selected || questions.length > 0) {
      if (questions.length > 0) setNodeQuestionCache([])
      return
    }
    let cancelled = false
    void api
      .listFrameworkNodeQuestions(detail.framework.id, selected.id)
      .then((qs) => {
        if (!cancelled) setNodeQuestionCache(qs)
      })
      .catch(() => {
        if (!cancelled) setNodeQuestionCache([])
      })
    return () => {
      cancelled = true
    }
  }, [detail?.framework.id, selected?.id, questions.length])

  if (!detail) return null

  const fw = detail.framework
  const profile = resolveViewerProfile(fw.code)
  const { description, requirement } = splitDefinition(selected?.definition)
  const isFolder = selected ? !isControlNode(selected) : false
  const selectedObjective = selected ? resolveDomainObjective(selected, byId) : null
  const whatItMeans = selected ? whatItMeansOf(selected) : null
  const evidence = selected ? evidenceOf(selected) : null
  const guidance = selected ? guidanceOf(selected) : null
  const bodyLabel = bodyFieldLabel(profile, fw.code)
  /** Pack imports use a single definition cell: only show legacy Requirement when checklist split found both. */
  const showLegacyRequirement = Boolean(description && requirement)
  const libraryTabActive = !extraTabs?.some((t) => t.id === tab)

  return (
    <Dialog
      open={open}
      onClose={onClose}
      fullWidth
      maxWidth={false}
      PaperProps={{
        sx: {
          width: 'min(1280px, calc(100vw - 32px))',
          height: 'min(900px, calc(100vh - 32px))',
          maxHeight: 'calc(100vh - 32px)',
          m: 2,
          display: 'flex',
          flexDirection: 'column',
        },
      }}
    >
      <DialogTitle
        sx={{
          px: 2.5,
          py: 1.5,
          borderBottom: `1px solid ${brand.hairline}`,
          flexShrink: 0,
        }}
      >
        <Stack direction="row" alignItems="flex-start" justifyContent="space-between" spacing={1.5}>
          <Box sx={{ minWidth: 0 }}>
            <Stack direction="row" alignItems="center" spacing={1} flexWrap="wrap" useFlexGap>
              <Typography variant="h6" sx={{ fontSize: '1.1rem', fontWeight: 700 }}>
                {fw.name}
              </Typography>
              <StatusChip label={fw.status} tone={frameworkStatusTone(fw.status)} />
              {fw.activated ? <StatusChip label="Activated" tone="success" /> : null}
            </Stack>
            <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.35 }}>
              {fw.moduleCode}
              {fw.clauseCount != null || fw.annexControlCount != null
                ? ` · ${fw.clauseCount ?? 0} clauses · ${fw.annexControlCount ?? 0} annexures`
                : ` · ${fw.controlCount} controls`}
            </Typography>
          </Box>
          <Stack direction="row" spacing={1} alignItems="center" flexShrink={0}>
            {headerActions}
            <IconButton size="small" onClick={onClose} aria-label="Close">
              <CloseRoundedIcon sx={{ fontSize: 20 }} />
            </IconButton>
          </Stack>
        </Stack>

        <Stack direction="row" spacing={0.75} sx={{ mt: 1.5 }} flexWrap="wrap" useFlexGap>
          {sheetTabs.length > 1
            ? sheetTabs.map((name) => (
                <TabChip
                  key={name}
                  label={name}
                  active={tab === name}
                  onClick={() => setTab(name)}
                />
              ))
            : (
                <TabChip
                  label={sheetTabs[0] ?? 'Library'}
                  active={libraryTabActive}
                  onClick={() => setTab(sheetTabs[0] ?? 'library')}
                />
              )}
          {extraTabs?.map((t) => (
            <TabChip
              key={t.id}
              label={t.label}
              active={tab === t.id}
              onClick={() => setTab(t.id)}
            />
          ))}
        </Stack>
      </DialogTitle>

      <DialogContent sx={{ p: 0, flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
        {!libraryTabActive && extraTabs ? (
          <Box sx={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
            {extraTabs.find((t) => t.id === tab)?.content}
          </Box>
        ) : (
          <>
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              spacing={1.5}
              alignItems={{ sm: 'center' }}
              sx={{ px: 2.5, py: 1.25, borderBottom: `1px solid ${brand.hairline}`, flexShrink: 0 }}
            >
              <TextField
                size="small"
                placeholder="Search ref, title, description, evidence…"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                sx={{ flex: 1, maxWidth: 480 }}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchRoundedIcon sx={{ fontSize: 18, color: 'text.secondary' }} />
                    </InputAdornment>
                  ),
                }}
              />
              <FormControlLabel
                control={
                  <Switch
                    size="small"
                    checked={showFolders}
                    onChange={(e) => setShowFolders(e.target.checked)}
                  />
                }
                label={<Typography variant="body2">Show section folders</Typography>}
              />
              <Typography variant="caption" color="text.secondary" sx={{ ml: { sm: 'auto' } }}>
                {filtered.length} of {nodesInSheet.length} nodes
                {sheetTabs.length > 1 ? ` · ${tab}` : ''}
              </Typography>
            </Stack>

            <Box
              sx={{
                flex: 1,
                minHeight: 0,
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', md: 'minmax(280px, 360px) 1fr' },
                overflow: 'hidden',
              }}
            >
              <Box
                sx={{
                  borderRight: { md: `1px solid ${brand.hairline}` },
                  borderBottom: { xs: `1px solid ${brand.hairline}`, md: 0 },
                  overflow: 'auto',
                  maxHeight: { xs: 220, md: 'none' },
                  bgcolor: 'rgba(11,18,32,0.015)',
                }}
              >
                {filtered.length === 0 ? (
                  <Box sx={{ p: 3 }}>
                    <Typography variant="body2" color="text.secondary">
                      No matching clauses or controls.
                    </Typography>
                  </Box>
                ) : useUaeIaTree ? (
                  treeRoots.map((n) => (
                    <UaeIaTreeNode
                      key={n.id}
                      node={n}
                      nodesInSheet={nodesInSheet}
                      visibleIds={treeVisibleIds}
                      expandedIds={expandedIds}
                      selectedId={selected?.id ?? null}
                      onSelect={(id) => setSelectedId(id)}
                      onToggle={(id) => {
                        setExpandedIds((prev) => {
                          const next = new Set(prev)
                          if (next.has(id)) next.delete(id)
                          else next.add(id)
                          return next
                        })
                      }}
                    />
                  ))
                ) : (
                  filtered.map((n) => {
                    const active = selected?.id === n.id
                    const folder = !isControlNode(n)
                    const kind = recordTypeLabel(n.recordType)
                    return (
                      <Box
                        key={n.id}
                        component="button"
                        type="button"
                        onClick={() => setSelectedId(n.id)}
                        sx={{
                          display: 'block',
                          width: '100%',
                          textAlign: 'left',
                          border: 0,
                          borderBottom: `1px solid ${brand.hairline}`,
                          bgcolor: active ? brand.accentMuted : 'transparent',
                          cursor: 'pointer',
                          px: 2,
                          py: 1,
                          pl: 2 + (n.level - 1) * 1.25,
                          '&:hover': { bgcolor: active ? brand.accentMuted : 'action.hover' },
                        }}
                      >
                        <Typography
                          variant="body2"
                          noWrap
                          title={`${n.code} ${n.title}`}
                          sx={{
                            fontWeight: folder ? 650 : 500,
                            fontSize: '0.8rem',
                            lineHeight: 1.35,
                            textDecoration: n.retired ? 'line-through' : undefined,
                            opacity: n.retired ? 0.65 : 1,
                          }}
                        >
                          <Box
                            component="span"
                            sx={{
                              fontFamily: MONO,
                              color: folder ? 'text.secondary' : brand.accentHover,
                              fontWeight: 650,
                              mr: 0.75,
                            }}
                          >
                            {n.code}
                            {n.retired ? ' (retired)' : ''}
                          </Box>
                          {n.title}
                        </Typography>
                        {(n.controlType || n.priority || kind) && (
                          <Typography variant="caption" color="text.secondary" display="block">
                            {[kind, n.controlType, n.priority].filter(Boolean).join(' · ')}
                          </Typography>
                        )}
                      </Box>
                    )
                  })
                )}
              </Box>

              <Box sx={{ overflow: 'auto', p: { xs: 2, md: 2.5 }, minHeight: 0 }}>
                {!selected ? (
                  <Typography variant="body2" color="text.secondary">
                    Select a clause or control to view full details.
                  </Typography>
                ) : (
                  <Stack spacing={2.25}>
                    <Box>
                      <Typography variant="caption" sx={{ fontFamily: MONO, color: brand.accentHover, fontWeight: 700 }}>
                        {selected.code}
                      </Typography>
                      <Typography variant="h6" sx={{ fontSize: '1.15rem', fontWeight: 700, mt: 0.25 }}>
                        {selected.title}
                      </Typography>
                      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 1 }}>
                        {selected.officialRef && (
                          <MetaChip label="Official ref" value={selected.officialRef} />
                        )}
                        {selected.controlType && <MetaChip label="Control type" value={selected.controlType} />}
                        {selected.priority && <MetaChip label="Priority" value={selected.priority} />}
                        {profile === 'UAE_IA' && applicabilityOf(selected) && (
                          <MetaChip label="Applicability" value={applicabilityOf(selected)!} />
                        )}
                        {profile === 'UAE_IA' && statusPercentOf(selected) && (
                          <MetaChip label="Status %" value={statusPercentOf(selected)!} />
                        )}
                        {nodeAttr(selected, 'default_applicability') && profile !== 'UAE_IA' && (
                          <MetaChip label="Default applicability" value={nodeAttr(selected, 'default_applicability')!} />
                        )}
                      </Stack>
                    </Box>

                    {selectedObjective && (
                      <FieldBlock label="Domain objective" body={selectedObjective} />
                    )}

                    {isFolder ? (
                      <Typography variant="body2" color="text.secondary">
                        Section grouping node: select a child clause or control to read the content.
                      </Typography>
                    ) : (
                      <>
                        <FieldBlock
                          label={bodyLabel}
                          body={description || (!showLegacyRequirement ? selected.definition : null)}
                        />
                        {showLegacyRequirement && <FieldBlock label="Requirement" body={requirement} />}
                        {profile === 'NIST_800_53' && guidance && (
                          <FieldBlock label="Discussion / guidance" body={guidance} />
                        )}
                        {whatItMeans && <FieldBlock label="What it means" body={whatItMeans} />}
                        {evidence && (
                          <FieldBlock
                            label={profile === 'SAMA' ? 'Evidence examples' : 'Evidence'}
                            body={evidence}
                          />
                        )}
                        <Box>
                          <Typography variant="subtitle2" sx={{ mb: 0.75 }}>
                            Questions{nodeQuestions.length ? ` (${nodeQuestions.length})` : ''}
                          </Typography>
                          {nodeQuestions.length === 0 ? (
                            <Typography variant="body2" color="text.secondary">
                              -
                            </Typography>
                          ) : (
                            <Stack spacing={1}>
                              {nodeQuestions.map((q, i) => (
                                <Box
                                  key={q.id}
                                  sx={{
                                    borderLeft: `3px solid ${brand.hairlineStrong}`,
                                    pl: 1.25,
                                  }}
                                >
                                  {profile !== 'SAMA' && (
                                    <Typography
                                      variant="caption"
                                      color="text.secondary"
                                      sx={{ fontFamily: MONO }}
                                    >
                                      {q.questionCode || `Q${i + 1}`}
                                    </Typography>
                                  )}
                                  <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                                    {q.questionText}
                                  </Typography>
                                </Box>
                              ))}
                            </Stack>
                          )}
                        </Box>
                      </>
                    )}
                    {editable && onDetailChange && (
                      <FrameworkNodeEditorPanel
                        detail={detail}
                        node={selected}
                        onSaved={onDetailChange}
                        onError={(msg) => onEditError?.(msg)}
                      />
                    )}
                  </Stack>
                )}
              </Box>
            </Box>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}

function isAncestorOf(
  ancestorId: number,
  node: FrameworkNode,
  byId: Map<number, FrameworkNode>,
): boolean {
  let cur: FrameworkNode | undefined = node
  const guard = new Set<number>()
  while (cur && !guard.has(cur.id)) {
    if (cur.parentId === ancestorId) return true
    guard.add(cur.id)
    cur = cur.parentId != null ? byId.get(cur.parentId) : undefined
  }
  return false
}

function UaeIaTreeNode({
  node,
  nodesInSheet,
  visibleIds,
  expandedIds,
  selectedId,
  onSelect,
  onToggle,
}: {
  node: FrameworkNode
  nodesInSheet: FrameworkNode[]
  visibleIds: Set<number>
  expandedIds: Set<number>
  selectedId: number | null
  onSelect: (id: number) => void
  onToggle: (id: number) => void
}) {
  const children = nodesInSheet
    .filter((n) => n.parentId === node.id && visibleIds.has(n.id))
    .sort(byDisplayOrder)
  const hasKids = children.length > 0
  const expanded = expandedIds.has(node.id)
  const active = selectedId === node.id
  const folder = !isControlNode(node)
  const kind = recordTypeLabel(node.recordType)

  return (
    <Box>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'stretch',
          borderBottom: `1px solid ${brand.hairline}`,
          bgcolor: active ? brand.accentMuted : 'transparent',
          '&:hover': { bgcolor: active ? brand.accentMuted : 'action.hover' },
        }}
      >
        <Box
          component="span"
          onClick={(e) => {
            e.stopPropagation()
            if (hasKids) onToggle(node.id)
          }}
          sx={{
            width: 28,
            flexShrink: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: hasKids ? 'pointer' : 'default',
            color: 'text.secondary',
            pl: (node.level - 1) * 1.25,
          }}
        >
          {hasKids ? (
            <ExpandMoreRoundedIcon
              sx={{
                fontSize: 18,
                transform: expanded ? 'rotate(0deg)' : 'rotate(-90deg)',
                transition: 'transform .15s ease',
              }}
            />
          ) : null}
        </Box>
        <Box
          component="button"
          type="button"
          onClick={() => {
            onSelect(node.id)
            if (hasKids && !expanded) onToggle(node.id)
          }}
          sx={{
            flex: 1,
            minWidth: 0,
            textAlign: 'left',
            border: 0,
            bgcolor: 'transparent',
            cursor: 'pointer',
            px: 1,
            py: 1,
            font: 'inherit',
          }}
        >
          <Typography
            variant="body2"
            noWrap
            title={`${node.code} ${node.title}`}
            sx={{
              fontWeight: folder ? 650 : 500,
              fontSize: '0.8rem',
              lineHeight: 1.35,
              textDecoration: node.retired ? 'line-through' : undefined,
              opacity: node.retired ? 0.65 : 1,
            }}
          >
            <Box
              component="span"
              sx={{
                fontFamily: MONO,
                color: folder ? 'text.secondary' : brand.accentHover,
                fontWeight: 650,
                mr: 0.75,
              }}
            >
              {node.code}
              {node.retired ? ' (retired)' : ''}
            </Box>
            {node.title}
          </Typography>
          {(node.controlType || node.priority || kind) && (
            <Typography variant="caption" color="text.secondary" display="block">
              {[kind, node.controlType, node.priority].filter(Boolean).join(' · ')}
            </Typography>
          )}
        </Box>
      </Box>
      {hasKids && expanded
        ? children.map((child) => (
            <UaeIaTreeNode
              key={child.id}
              node={child}
              nodesInSheet={nodesInSheet}
              visibleIds={visibleIds}
              expandedIds={expandedIds}
              selectedId={selectedId}
              onSelect={onSelect}
              onToggle={onToggle}
            />
          ))
        : null}
    </Box>
  )
}

function TabChip({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <Box
      component="button"
      type="button"
      onClick={onClick}
      sx={{
        border: `1px solid ${active ? brand.accent : brand.hairlineStrong}`,
        bgcolor: active ? brand.accentMuted : 'transparent',
        color: active ? brand.accentHover : 'text.secondary',
        borderRadius: 1,
        px: 1.25,
        py: 0.4,
        fontSize: '0.75rem',
        fontWeight: 650,
        cursor: 'pointer',
        fontFamily: 'inherit',
        maxWidth: 220,
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
      }}
      title={label}
    >
      {label}
    </Box>
  )
}

function MetaChip({ label, value }: { label: string; value: string }) {
  return (
    <Box
      sx={{
        border: `1px solid ${brand.hairlineStrong}`,
        borderRadius: 1,
        px: 1,
        py: 0.35,
        bgcolor: 'background.paper',
      }}
    >
      <Typography variant="caption" color="text.secondary" display="block" sx={{ lineHeight: 1.2 }}>
        {label}
      </Typography>
      <Typography variant="body2" sx={{ fontWeight: 650, fontSize: '0.8rem' }}>
        {value}
      </Typography>
    </Box>
  )
}

function FieldBlock({ label, body }: { label: string; body: string | null }) {
  if (!body) {
    return (
      <Box>
        <Typography variant="subtitle2" sx={{ mb: 0.75 }}>
          {label}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          -
        </Typography>
      </Box>
    )
  }
  return (
    <Box>
      <Typography variant="subtitle2" sx={{ mb: 0.75 }}>
        {label}
      </Typography>
      <Typography
        variant="body2"
        sx={{
          whiteSpace: 'pre-wrap',
          lineHeight: 1.55,
          color: 'text.primary',
          p: 1.5,
          borderRadius: 1,
          bgcolor: 'rgba(11,18,32,0.03)',
          border: `1px solid ${brand.hairline}`,
        }}
      >
        {body}
      </Typography>
    </Box>
  )
}
