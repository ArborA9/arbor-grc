import { useEffect, useState } from 'react'
import {
  Box,
  Button,
  Checkbox,
  FormControlLabel,
  Stack,
  Typography,
} from '@mui/material'
import type { FrameworkDetail, FrameworkNode } from '../api'
import { api } from '../api'
import { brand } from '../theme'
import ConfirmActionDialog from './ui/ConfirmActionDialog'
import { CompactTextField, FieldActionRow, controlActionSx } from './ui'

function attr(n: FrameworkNode, key: string): string {
  return n.attributes?.[key] ?? ''
}

export default function FrameworkNodeEditorPanel({
  detail,
  node,
  onSaved,
  onError,
}: {
  detail: FrameworkDetail
  node: FrameworkNode
  onSaved: (next: FrameworkDetail) => void
  onError: (msg: string) => void
}) {
  const fwId = detail.framework.id
  const [busy, setBusy] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [code, setCode] = useState(node.code)
  const [title, setTitle] = useState(node.title)
  const [definition, setDefinition] = useState(node.definition ?? '')
  const [priority, setPriority] = useState(node.priority ?? '')
  const [recordType, setRecordType] = useState(node.recordType ?? '')
  const [whatItMeans, setWhatItMeans] = useState(attr(node, 'what_it_means'))
  const [evidence, setEvidence] = useState(attr(node, 'evidence'))
  const [sheetTab, setSheetTab] = useState(attr(node, 'sheet_tab'))
  const [defaultApp, setDefaultApp] = useState(attr(node, 'default_applicability'))
  const [inSoa, setInSoa] = useState(node.inSoa)
  const [inGap, setInGap] = useState(node.inGapAssessment)
  const [inAudit, setInAudit] = useState(node.inAuditChecklist)
  const [inRisk, setInRisk] = useState(node.inRiskRegister)
  const [newCode, setNewCode] = useState('')
  const [newTitle, setNewTitle] = useState('')
  const [tabRename, setTabRename] = useState('')

  useEffect(() => {
    setCode(node.code)
    setTitle(node.title)
    setDefinition(node.definition ?? '')
    setPriority(node.priority ?? '')
    setRecordType(node.recordType ?? '')
    setWhatItMeans(attr(node, 'what_it_means'))
    setEvidence(attr(node, 'evidence'))
    setSheetTab(attr(node, 'sheet_tab'))
    setDefaultApp(attr(node, 'default_applicability'))
    setInSoa(node.inSoa)
    setInGap(node.inGapAssessment)
    setInAudit(node.inAuditChecklist)
    setInRisk(node.inRiskRegister)
  }, [node.id, node])

  const refresh = async () => onSaved(await api.getAdminFramework(fwId))

  const save = async () => {
    setBusy(true)
    try {
      await api.updateAdminFrameworkNode(fwId, node.id, {
        code,
        title,
        definition: definition || null,
        priority: priority || null,
        recordType: recordType || null,
        inSoa,
        inGapAssessment: inGap,
        inAuditChecklist: inAudit,
        inRiskRegister: inRisk,
        attributes: {
          what_it_means: whatItMeans,
          evidence,
          sheet_tab: sheetTab,
          default_applicability: defaultApp,
        },
      })
      await refresh()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const move = async (dir: -1 | 1) => {
    const ordered = [...detail.nodes].sort(
      (a, b) => a.displayOrder - b.displayOrder || a.code.localeCompare(b.code),
    )
    const idx = ordered.findIndex((n) => n.id === node.id)
    const swap = idx + dir
    if (idx < 0 || swap < 0 || swap >= ordered.length) return
    const ids = ordered.map((n) => n.id)
    ;[ids[idx], ids[swap]] = [ids[swap], ids[idx]]
    setBusy(true)
    try {
      onSaved(await api.reorderAdminFrameworkNodes(fwId, ids))
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const addChild = async () => {
    if (!newCode.trim() || !newTitle.trim()) {
      onError('New node needs code and title')
      return
    }
    setBusy(true)
    try {
      await api.createAdminFrameworkNode(fwId, {
        parentId: node.id,
        code: newCode.trim(),
        title: newTitle.trim(),
        recordType: 'CONTROL',
        inSoa: true,
        inGapAssessment: true,
        inAuditChecklist: true,
        inRiskRegister: true,
        attributes: sheetTab ? { sheet_tab: sheetTab } : undefined,
      })
      setNewCode('')
      setNewTitle('')
      await refresh()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const retireOrRestore = async () => {
    setBusy(true)
    try {
      if (node.retired) await api.unretireAdminFrameworkNode(fwId, node.id)
      else await api.retireAdminFrameworkNode(fwId, node.id)
      await refresh()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const remove = async () => {
    setBusy(true)
    try {
      onSaved(await api.deleteAdminFrameworkNode(fwId, node.id))
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const renameTab = async () => {
    const from = attr(node, 'sheet_tab')
    if (!from || !tabRename.trim()) {
      onError('Set a new sheet tab name')
      return
    }
    setBusy(true)
    try {
      onSaved(await api.renameAdminFrameworkSheetTab(fwId, from, tabRename.trim()))
      setTabRename('')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Stack spacing={1.5} sx={{ mt: 1, p: 1.5, border: `1px solid ${brand.hairline}`, borderRadius: 1 }}>
      <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
        Edit node {node.retired ? '(retired)' : ''}
      </Typography>
      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
        <Button size="small" variant="contained" disabled={busy} onClick={() => void save()}>
          Save
        </Button>
        <Button size="small" disabled={busy} onClick={() => void move(-1)}>
          Move Up
        </Button>
        <Button size="small" disabled={busy} onClick={() => void move(1)}>
          Move Down
        </Button>
        <Button size="small" variant="outlined" disabled={busy} onClick={() => void retireOrRestore()}>
          {node.retired ? 'Unretire' : 'Retire'}
        </Button>
        <Button size="small" color="error" disabled={busy} onClick={() => setConfirmDelete(true)}>
          Delete / Retire
        </Button>
      </Stack>
      <CompactTextField label="Code" value={code} onChange={(e) => setCode(e.target.value)} />
      <CompactTextField label="Title" value={title} onChange={(e) => setTitle(e.target.value)} />
      <CompactTextField
        label="Definition"
        value={definition}
        onChange={(e) => setDefinition(e.target.value)}
        multiline
        minRows={3}
      />
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
        <CompactTextField label="Priority" value={priority} onChange={(e) => setPriority(e.target.value)} />
        <CompactTextField label="Record type" value={recordType} onChange={(e) => setRecordType(e.target.value)} />
        <CompactTextField label="Sheet tab" value={sheetTab} onChange={(e) => setSheetTab(e.target.value)} />
      </Stack>
      <CompactTextField
        label="What it means"
        value={whatItMeans}
        onChange={(e) => setWhatItMeans(e.target.value)}
        multiline
        minRows={2}
      />
      <CompactTextField
        label="Evidence"
        value={evidence}
        onChange={(e) => setEvidence(e.target.value)}
        multiline
        minRows={2}
      />
      <CompactTextField
        label="Default applicability"
        value={defaultApp}
        onChange={(e) => setDefaultApp(e.target.value)}
        helperText="e.g. Always / Optional"
      />
      <Box>
        <FormControlLabel control={<Checkbox checked={inSoa} onChange={(e) => setInSoa(e.target.checked)} />} label="SoA" />
        <FormControlLabel control={<Checkbox checked={inGap} onChange={(e) => setInGap(e.target.checked)} />} label="Gap" />
        <FormControlLabel control={<Checkbox checked={inAudit} onChange={(e) => setInAudit(e.target.checked)} />} label="Audit" />
        <FormControlLabel control={<Checkbox checked={inRisk} onChange={(e) => setInRisk(e.target.checked)} />} label="Risk" />
      </Box>

      <Typography variant="caption" color="text.secondary">
        Add child under this node
      </Typography>
      <FieldActionRow>
        <CompactTextField label="Child code" value={newCode} onChange={(e) => setNewCode(e.target.value)} />
        <CompactTextField label="Child title" value={newTitle} onChange={(e) => setNewTitle(e.target.value)} />
        <Button
          size="small"
          variant="outlined"
          disabled={busy}
          onClick={() => void addChild()}
          sx={{ height: 32, minHeight: 32, flexShrink: 0, whiteSpace: 'nowrap' }}
        >
          Add Child
        </Button>
      </FieldActionRow>

      {attr(node, 'sheet_tab') && (
        <>
          <Typography variant="caption" color="text.secondary">
            Rename sheet tab “{attr(node, 'sheet_tab')}” (all nodes on that tab)
          </Typography>
          <FieldActionRow>
            <CompactTextField
              label="New tab label"
              value={tabRename}
              onChange={(e) => setTabRename(e.target.value)}
            />
            <Button size="small" variant="outlined" disabled={busy} onClick={() => void renameTab()} sx={controlActionSx}>
              Rename Tab
            </Button>
          </FieldActionRow>
        </>
      )}
      <ConfirmActionDialog
        open={confirmDelete}
        title={`Delete or retire ${node.code}?`}
        message="If the framework is activated, the node is soft-retired. You can cancel if you clicked by mistake."
        confirmLabel="Delete / Retire"
        busy={busy}
        onCancel={() => setConfirmDelete(false)}
        onConfirm={() => {
          setConfirmDelete(false)
          void remove()
        }}
      />
    </Stack>
  )
}
