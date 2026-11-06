import type { ReactNode } from 'react'
import {
  Box,
  Button,
  Divider,
  IconButton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material'
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined'
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined'
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined'
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined'
import PictureAsPdfOutlinedIcon from '@mui/icons-material/PictureAsPdfOutlined'
import TableChartOutlinedIcon from '@mui/icons-material/TableChartOutlined'
import {
  EVIDENCE_CLASSIFICATION_LABELS,
  type EvidenceClassification,
  type EvidenceDetail,
  type EvidenceLink,
  type EvidenceVersion,
} from '../../api'
import { EVIDENCE_ACCEPT } from './evidenceFileAccept'
import StatusChip from '../StatusChip'
import { brand } from '../../theme'
import { readOnlyCaptionSx, sectionLabelSx } from '../ui/FormFieldLabel'
import { fourColFieldSx } from '../ui/formLayout'
import { formatTenantDate, formatTenantDateTime } from '../../uiQuality/tenantLocale'

type FileKind = 'pdf' | 'word' | 'excel' | 'image' | 'other'
type ChipTone = 'success' | 'info' | 'warning' | 'danger' | 'neutral'

const MODULE_LABELS: Record<string, string> = {
  GAP: 'Gap',
  RISK: 'Risk',
  SOA: 'SoA',
  AUDIT: 'Audit',
  BCMS: 'BCMS',
  PRIVACY: 'Privacy',
  ASSET: 'Assets',
  ASSETS: 'Assets',
  TPRM: 'Third parties',
  POLICY: 'Policy',
  CAPA: 'CAPA',
  EVIDENCE: 'Evidence',
}

const RECORD_LABELS: Record<string, string> = {
  GAP_QUESTION: 'Gap question',
  GAP_ITEM: 'Gap control',
  RISK_RECORD: 'Risk',
  CAPA: 'CAPA',
  AUDIT_ENGAGEMENT: 'Audit',
}

const STAGE_LABELS: Record<string, string> = {
  CONTRIBUTOR_SUBMIT: 'Contributor submit',
  IDENTIFICATION: 'Identification',
  ASSIGNMENT: 'Assignment',
  REVIEW: 'Review',
  TREATMENT: 'Treatment',
  TOLERANCE: 'Tolerance',
  TRANSFER: 'Transfer',
}

function humanCode(raw: string | null | undefined, known: Record<string, string>): string {
  const key = (raw ?? '').trim().toUpperCase()
  if (!key) return '-'
  if (known[key]) return known[key]
  return key
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

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

function FileKindIcon({ kind, size = 22 }: { kind: FileKind; size?: number }) {
  const sx = { fontSize: size }
  if (kind === 'pdf') return <PictureAsPdfOutlinedIcon sx={sx} />
  if (kind === 'image') return <ImageOutlinedIcon sx={sx} />
  if (kind === 'word') return <DescriptionOutlinedIcon sx={sx} />
  if (kind === 'excel') return <TableChartOutlinedIcon sx={sx} />
  return <InsertDriveFileOutlinedIcon sx={sx} />
}

function formatBytes(n: number) {
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / (1024 * 1024)).toFixed(1)} MB`
}

function classificationTone(value: EvidenceClassification): ChipTone {
  if (value === 'OPEN') return 'success'
  if (value === 'CONFIDENTIAL') return 'info'
  return 'warning'
}

function scanParts(status: string): { title: string; note: string; tone: ChipTone } {
  if (status === 'CLEAN') return { title: 'Clean', note: 'Verified clean', tone: 'success' }
  if (status === 'INFECTED') return { title: 'Infected', note: 'Blocked', tone: 'danger' }
  if (status === 'PENDING') return { title: 'Scan pending', note: 'Not verified clean', tone: 'warning' }
  if (status === 'SKIPPED') return { title: 'Not scanned', note: 'Not verified clean', tone: 'warning' }
  return { title: status || 'Unknown', note: '', tone: 'warning' }
}

function FactText({ label, value }: { label: string; value: string }) {
  const text = value.trim() || '-'
  return (
    <Box sx={{ minWidth: 0 }}>
      <Typography sx={readOnlyCaptionSx}>{label}</Typography>
      <Box
        sx={{
          minHeight: 32,
          display: 'flex',
          alignItems: 'center',
          px: 1,
          borderRadius: '6px',
          border: `1px solid ${brand.border}`,
          bgcolor: brand.hover,
          fontSize: '0.75rem',
          color: text === '-' ? brand.muted : brand.ink,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
        }}
        title={text}
      >
        {text}
      </Box>
    </Box>
  )
}

function FactChip({ label, chip, note }: { label: string; chip: ReactNode; note?: string }) {
  return (
    <Box sx={{ minWidth: 0 }}>
      <Typography sx={readOnlyCaptionSx}>{label}</Typography>
      <Box sx={{ minHeight: 32, display: 'flex', alignItems: 'center' }}>{chip}</Box>
      {note ? (
        <Typography sx={{ fontSize: '0.6875rem', color: brand.mutedLight, lineHeight: 1.3, mt: 0.35 }}>
          {note}
        </Typography>
      ) : null}
    </Box>
  )
}

function SectionTitle({ children, action }: { children: string; action?: ReactNode }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1, mb: 1 }}>
      <Typography sx={{ ...sectionLabelSx, mb: 0 }}>{children}</Typography>
      {action}
    </Box>
  )
}

function latestVersion(detail: EvidenceDetail): EvidenceVersion | null {
  if (!detail.versions.length) return null
  return [...detail.versions].sort((a, b) => b.versionNumber - a.versionNumber)[0]
}

export default function EvidenceDetailView({
  detail,
  busy,
  canUpload,
  canDelete,
  onDownload,
  onUploadVersion,
  onDelete,
}: {
  detail: EvidenceDetail
  busy: boolean
  canUpload: boolean
  canDelete: boolean
  onDownload: (version?: number) => void
  onUploadVersion: (file: File) => void
  onDelete: () => void
}) {
  const latest = latestVersion(detail)
  const filename = latest?.originalFilename || detail.title
  const kind = fileKindFromName(filename)
  const scan = latest ? scanParts(latest.virusScanStatus) : null
  const versions = [...detail.versions].sort((a, b) => b.versionNumber - a.versionNumber)
  const filenameDiffers = filename.trim().toLowerCase() !== detail.title.trim().toLowerCase()

  return (
    <Stack spacing={2.5} divider={<Divider flexItem sx={{ borderColor: brand.border }} />}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 2,
          flexWrap: 'wrap',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, minWidth: 0, flex: '1 1 280px' }}>
          <Box
            sx={{
              width: 40,
              height: 40,
              borderRadius: 1,
              flexShrink: 0,
              display: 'grid',
              placeItems: 'center',
              bgcolor: brand.goldLight,
              color: brand.goldDark,
            }}
          >
            <FileKindIcon kind={kind} />
          </Box>
          <Box sx={{ minWidth: 0 }}>
            {filenameDiffers ? (
              <Typography
                sx={{ fontSize: '0.875rem', fontWeight: 700, color: brand.ink, lineHeight: 1.3 }}
                noWrap
                title={filename}
              >
                {filename}
              </Typography>
            ) : null}
            <Typography sx={{ fontSize: '0.75rem', color: brand.muted, mt: filenameDiffers ? 0.25 : 0 }}>
              {fileKindLabel(kind)}
              {latest ? ` · ${formatBytes(latest.sizeBytes)}` : ''}
              {` · Version ${detail.currentVersion}`}
              {latest?.uploadedAt ? ` · Uploaded ${formatTenantDateTime(latest.uploadedAt)}` : ''}
            </Typography>
          </Box>
        </Box>
        <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
          <Button
            variant="contained"
            size="small"
            startIcon={<DownloadOutlinedIcon sx={{ fontSize: 16 }} />}
            onClick={() => onDownload()}
          >
            Download Latest
          </Button>
          {canDelete ? (
            <Button variant="outlined" color="error" size="small" disabled={busy} onClick={onDelete}>
              Delete
            </Button>
          ) : null}
        </Stack>
      </Box>

      <Box>
        <SectionTitle>Details</SectionTitle>
        <Box sx={fourColFieldSx}>
          <FactChip
            label="Classification"
            chip={
              <StatusChip
                label={EVIDENCE_CLASSIFICATION_LABELS[detail.classification] ?? detail.classification}
                tone={classificationTone(detail.classification)}
              />
            }
          />
          <FactChip
            label="Status"
            chip={
              <StatusChip
                label={detail.status === 'EXPIRED' ? 'Expired' : 'Active'}
                tone={detail.status === 'EXPIRED' ? 'warning' : 'neutral'}
              />
            }
          />
          <FactText label="Current version" value={`v${detail.currentVersion}`} />
          <FactText label="Expires" value={detail.expiresOn ? formatTenantDate(detail.expiresOn) : '-'} />
          <FactText label="File type" value={fileKindLabel(kind)} />
          <FactText label="Size" value={latest ? formatBytes(latest.sizeBytes) : '-'} />
          <FactChip
            label="Virus scan"
            chip={scan ? <StatusChip label={scan.title} tone={scan.tone} /> : <StatusChip label="Unknown" tone="warning" />}
            note={scan?.note}
          />
          <FactText label="Added" value={formatTenantDateTime(detail.createdAt)} />
        </Box>
      </Box>

      <Box>
        <SectionTitle
          action={
            canUpload ? (
              <Button variant="outlined" component="label" size="small" disabled={busy}>
                Upload New Version
                <input
                  hidden
                  type="file"
                  accept={EVIDENCE_ACCEPT}
                  onChange={(e) => {
                    const file = e.target.files?.[0]
                    if (file) onUploadVersion(file)
                    e.target.value = ''
                  }}
                />
              </Button>
            ) : undefined
          }
        >
          Versions
        </SectionTitle>
        <Box sx={{ overflowX: 'auto' }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell width={120}>Version</TableCell>
              <TableCell>File</TableCell>
              <TableCell width={90}>Size</TableCell>
              <TableCell width={220}>Scan</TableCell>
              <TableCell width={150}>Uploaded</TableCell>
              <TableCell width={96} align="right">
                Download
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {versions.map((version) => {
              const rowScan = scanParts(version.virusScanStatus)
              const current = version.versionNumber === detail.currentVersion
              return (
                <TableRow key={version.id} sx={current ? { bgcolor: brand.hover } : undefined}>
                  <TableCell>
                    <Stack direction="row" spacing={0.75} alignItems="center">
                      <Typography sx={{ fontSize: '0.75rem', fontWeight: 650 }}>v{version.versionNumber}</Typography>
                      {current ? <StatusChip label="Current" tone="info" /> : null}
                    </Stack>
                  </TableCell>
                  <TableCell>
                    <Typography sx={{ fontSize: '0.75rem' }} noWrap title={version.originalFilename}>
                      {version.originalFilename}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography sx={{ fontSize: '0.75rem', color: brand.muted }}>{formatBytes(version.sizeBytes)}</Typography>
                  </TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={0.75} alignItems="center">
                      <StatusChip label={rowScan.title} tone={rowScan.tone} />
                      {rowScan.note ? (
                        <Typography sx={{ fontSize: '0.6875rem', color: brand.mutedLight }} noWrap>
                          {rowScan.note}
                        </Typography>
                      ) : null}
                    </Stack>
                  </TableCell>
                  <TableCell>
                    <Typography sx={{ fontSize: '0.75rem', color: brand.muted }}>
                      {formatTenantDateTime(version.uploadedAt)}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title={`Download v${version.versionNumber}`}>
                      <IconButton
                        size="small"
                        aria-label={`Download version ${version.versionNumber}`}
                        onClick={() => onDownload(version.versionNumber)}
                        sx={{ color: brand.muted, '&:hover': { color: brand.goldDark, bgcolor: brand.goldLight } }}
                      >
                        <DownloadOutlinedIcon sx={{ fontSize: 18 }} />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              )
            })}
          </TableBody>
        </Table>
        </Box>
      </Box>

      <Box>
        <SectionTitle>{detail.links.length ? `Linked records (${detail.links.length})` : 'Linked records'}</SectionTitle>
        {detail.links.length === 0 ? (
          <Box
            sx={{
              px: 1.25,
              py: 1.25,
              border: `1px dashed ${brand.border}`,
              borderRadius: 1,
              bgcolor: brand.hover,
            }}
          >
            <Typography sx={{ fontSize: '0.75rem', color: brand.mutedLight }}>
              This file is not linked to another record.
            </Typography>
          </Box>
        ) : (
          <Stack spacing={1}>
            {detail.links.map((link) => (
              <LinkCard key={link.id} link={link} />
            ))}
          </Stack>
        )}
      </Box>
    </Stack>
  )
}

function LinkCard({ link }: { link: EvidenceLink }) {
  return (
    <Box
      sx={{
        border: `1px solid ${brand.border}`,
        borderRadius: 1,
        px: 1.25,
        py: 1.25,
        bgcolor: brand.paper,
      }}
    >
      <Box sx={fourColFieldSx}>
        <FactText label="Module" value={humanCode(link.moduleCode, MODULE_LABELS)} />
        <FactText label="Record" value={humanCode(link.recordType, RECORD_LABELS)} />
        <FactText label="Reference" value={link.recordId || '-'} />
        <FactText label="Stage" value={humanCode(link.workflowStage, STAGE_LABELS)} />
      </Box>
      <Typography sx={{ fontSize: '0.6875rem', color: brand.mutedLight, mt: 1 }}>
        Linked {formatTenantDateTime(link.linkedAt)}
      </Typography>
    </Box>
  )
}
