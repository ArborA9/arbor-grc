import { useEffect, useState, type ReactNode } from 'react'
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined'
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined'
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined'
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined'
import PictureAsPdfOutlinedIcon from '@mui/icons-material/PictureAsPdfOutlined'
import TableChartOutlinedIcon from '@mui/icons-material/TableChartOutlined'
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined'
import { api, EVIDENCE_CLASSIFICATION_LABELS, type EvidenceSummary } from '../../api'
import { evidencePreviewKind } from '../gap/gapEvidence'
import { brand } from '../../theme'
import ConfirmActionDialog from '../ui/ConfirmActionDialog'

type Preview = {
  id: number
  url: string
  name: string
  kind: 'image' | 'pdf' | 'other'
}

type BlobResult = { blob: Blob; filename: string; contentType: string }

type FileKind = 'pdf' | 'word' | 'excel' | 'image' | 'other'

function fileKindFromName(filename: string | null | undefined): FileKind {
  const name = (filename ?? '').toLowerCase()
  if (name.endsWith('.pdf')) return 'pdf'
  if (/\.(png|jpe?g|gif|webp|svg)$/.test(name)) return 'image'
  if (/\.(docx?|rtf|odt)$/.test(name)) return 'word'
  if (/\.(xlsx?|csv|ods)$/.test(name)) return 'excel'
  return 'other'
}

function fileKindLabel(kind: FileKind): string {
  if (kind === 'pdf') return 'PDF'
  if (kind === 'word') return 'Word'
  if (kind === 'excel') return 'Excel'
  if (kind === 'image') return 'Image'
  return 'File'
}

function FileKindIcon({ kind }: { kind: FileKind }) {
  const sx = { fontSize: 18 }
  if (kind === 'pdf') return <PictureAsPdfOutlinedIcon sx={sx} />
  if (kind === 'image') return <ImageOutlinedIcon sx={sx} />
  if (kind === 'word') return <DescriptionOutlinedIcon sx={sx} />
  if (kind === 'excel') return <TableChartOutlinedIcon sx={sx} />
  return <InsertDriveFileOutlinedIcon sx={sx} />
}

function formatSize(bytes: number | null | undefined): string | null {
  if (bytes == null || bytes <= 0) return null
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function classificationLabel(value: EvidenceSummary['classification'] | undefined): string | null {
  if (!value) return null
  return EVIDENCE_CLASSIFICATION_LABELS[value] ?? value
}

function ActionIcon({
  label,
  onClick,
  danger,
  children,
}: {
  label: string
  onClick: () => void
  danger?: boolean
  children: ReactNode
}) {
  return (
    <Tooltip title={label}>
      <IconButton
        size="small"
        aria-label={label}
        onClick={onClick}
        sx={{
          width: 28,
          height: 28,
          color: danger ? brand.mutedLight : brand.muted,
          '&:hover': danger
            ? { color: '#B91C1C', bgcolor: '#FEF2F2' }
            : { color: brand.goldDark, bgcolor: brand.goldLight },
        }}
      >
        {children}
      </IconButton>
    </Tooltip>
  )
}

export default function EvidenceAttachedList({
  items,
  numbered = true,
  emptyLabel = 'No files on this question yet',
  flush = false,
  singleColumn = false,
  confirmRemove = true,
  metaExtra,
  onError,
  onRemove,
  fetchBlob,
  download,
}: {
  items: EvidenceSummary[]
  numbered?: boolean
  emptyLabel?: string
  flush?: boolean
  /** One chip per row (narrow drawers). Default is two from `sm` up. */
  singleColumn?: boolean
  confirmRemove?: boolean
  /** Extra meta after type/size, e.g. Pending. */
  metaExtra?: string
  onError?: (message: string) => void
  onRemove?: (item: EvidenceSummary) => void
  /** Defaults to admin session evidence APIs. */
  fetchBlob?: (id: number) => Promise<BlobResult>
  download?: (id: number) => Promise<void>
}) {
  const [preview, setPreview] = useState<Preview | null>(null)
  const [removeItem, setRemoveItem] = useState<EvidenceSummary | null>(null)
  const loadBlob = fetchBlob ?? ((id: number) => api.fetchEvidenceBlob(id))
  const saveFile = download ?? ((id: number) => api.downloadEvidence(id))

  useEffect(() => {
    return () => {
      if (preview?.url) URL.revokeObjectURL(preview.url)
    }
  }, [preview?.url])

  const closePreview = () => {
    setPreview((cur) => {
      if (cur?.url) URL.revokeObjectURL(cur.url)
      return null
    })
  }

  const openPreview = async (ev: EvidenceSummary) => {
    try {
      const { blob, filename, contentType } = await loadBlob(ev.id)
      const kind = evidencePreviewKind(filename || ev.latestFilename, contentType)
      if (kind === 'other') {
        await saveFile(ev.id)
        return
      }
      setPreview((cur) => {
        if (cur?.url) URL.revokeObjectURL(cur.url)
        return {
          id: ev.id,
          url: URL.createObjectURL(blob),
          name: filename || ev.latestFilename || ev.title,
          kind,
        }
      })
    } catch (e) {
      onError?.(String(e))
    }
  }

  if (items.length === 0) {
    return (
      <Box
        sx={{
          mt: flush ? 0 : 1,
          px: 1.25,
          py: 1,
          minHeight: 40,
          display: 'flex',
          alignItems: 'center',
          border: `1px dashed ${brand.border}`,
          borderRadius: 1,
          bgcolor: brand.hover,
        }}
      >
        <Typography sx={{ fontSize: '0.75rem', color: brand.mutedLight, textAlign: 'left' }}>
          {emptyLabel}
        </Typography>
      </Box>
    )
  }

  const previewClassLabel = preview
    ? classificationLabel(items.find((item) => item.id === preview.id)?.classification)
    : null

  return (
    <>
      <Box
        sx={{
          mt: flush ? 0 : 1,
          display: 'grid',
          gridTemplateColumns: singleColumn
            ? '1fr'
            : { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))' },
          gap: 0.75,
          alignItems: 'start',
          justifyItems: 'stretch',
        }}
      >
        {items.map((ev, i) => {
          const name = ev.latestFilename ?? ev.title
          const kind = fileKindFromName(name)
          const size = formatSize(ev.latestSizeBytes)
          const classLabel = classificationLabel(ev.classification)
          const pending = ev.id < 0
          const meta = [fileKindLabel(kind), size, classLabel, pending ? 'Pending' : metaExtra]
            .filter(Boolean)
            .join(' · ')
          return (
            <Box
              key={ev.id}
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                minWidth: 0,
                px: 1,
                py: 0.75,
                border: `1px solid ${brand.border}`,
                borderRadius: 1.25,
                bgcolor: brand.paper,
                boxShadow: '0 1px 2px rgba(28, 25, 23, 0.04)',
              }}
            >
              <Box
                sx={{
                  width: 36,
                  height: 36,
                  borderRadius: 1,
                  flexShrink: 0,
                  display: 'grid',
                  placeItems: 'center',
                  bgcolor: brand.goldLight,
                  color: brand.goldDark,
                  position: 'relative',
                }}
              >
                <FileKindIcon kind={kind} />
                {numbered && items.length > 1 && (
                  <Box
                    sx={{
                      position: 'absolute',
                      top: -4,
                      left: -4,
                      width: 14,
                      height: 14,
                      borderRadius: '50%',
                      bgcolor: brand.gold,
                      color: '#fff',
                      fontSize: '0.5625rem',
                      fontWeight: 700,
                      display: 'grid',
                      placeItems: 'center',
                      lineHeight: 1,
                    }}
                  >
                    {i + 1}
                  </Box>
                )}
              </Box>
              <Box sx={{ minWidth: 0, flex: 1 }}>
                <Typography
                  component={pending ? 'span' : 'button'}
                  type={pending ? undefined : 'button'}
                  onClick={pending ? undefined : () => void openPreview(ev)}
                  title={name}
                  sx={{
                    display: 'block',
                    width: '100%',
                    border: 0,
                    p: 0,
                    m: 0,
                    bgcolor: 'transparent',
                    cursor: pending ? 'default' : 'pointer',
                    textAlign: 'left',
                    font: 'inherit',
                    fontSize: '0.8125rem',
                    fontWeight: 600,
                    color: brand.ink,
                    lineHeight: 1.3,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                    '&:hover': { color: brand.goldDark, textDecoration: 'underline' },
                  }}
                >
                  {name}
                </Typography>
                <Typography sx={{ fontSize: '0.6875rem', color: brand.mutedLight, mt: 0.15 }}>
                  {meta}
                </Typography>
              </Box>
              <Stack direction="row" spacing={0} sx={{ flexShrink: 0 }}>
                {!pending && (
                  <>
                <ActionIcon label="View" onClick={() => void openPreview(ev)}>
                  <VisibilityOutlinedIcon sx={{ fontSize: 18 }} />
                </ActionIcon>
                <ActionIcon
                  label="Download"
                  onClick={() => void saveFile(ev.id).catch((e) => onError?.(String(e)))}
                >
                  <DownloadOutlinedIcon sx={{ fontSize: 18 }} />
                </ActionIcon>
                  </>
                )}
                {onRemove && (
                  <ActionIcon
                    label="Remove"
                    danger
                    onClick={() => {
                      if (confirmRemove) setRemoveItem(ev)
                      else onRemove(ev)
                    }}
                  >
                    <DeleteOutlineRoundedIcon sx={{ fontSize: 18 }} />
                  </ActionIcon>
                )}
              </Stack>
            </Box>
          )
        })}
      </Box>
      <Dialog open={preview != null} onClose={closePreview} maxWidth="md" fullWidth>
        <DialogTitle sx={{ fontSize: '0.875rem' }}>
          {preview?.name}
          {previewClassLabel ? (
            <Typography
              component="span"
              sx={{ ml: 1, fontSize: '0.75rem', color: brand.muted, fontWeight: 500 }}
            >
              {previewClassLabel}
            </Typography>
          ) : null}
        </DialogTitle>
        <DialogContent>
          {preview?.kind === 'image' && (
            <Box
              component="img"
              src={preview.url}
              alt={preview.name}
              sx={{ maxWidth: '100%', maxHeight: '70vh', display: 'block', mx: 'auto' }}
            />
          )}
          {preview?.kind === 'pdf' && (
            <Box component="iframe" title={preview.name} src={preview.url} sx={{ width: '100%', height: '70vh', border: 0 }} />
          )}
        </DialogContent>
        <DialogActions>
          {preview && (
            <Button onClick={() => void saveFile(preview.id).catch((e) => onError?.(String(e)))}>
              Download
            </Button>
          )}
          <Button onClick={closePreview}>Close</Button>
        </DialogActions>
      </Dialog>
      <ConfirmActionDialog
        open={removeItem != null}
        title="Remove this file?"
        message={
          removeItem
            ? `Remove ${removeItem.latestFilename ?? removeItem.title} from this record.`
            : 'Remove this file from this record.'
        }
        confirmLabel="Remove"
        confirmColor="error"
        onCancel={() => setRemoveItem(null)}
        onConfirm={() => {
          if (removeItem) onRemove?.(removeItem)
          setRemoveItem(null)
        }}
      />
    </>
  )
}
