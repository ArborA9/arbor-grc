import { Box, Button, Checkbox, FormControlLabel, Stack, Typography } from '@mui/material'
import type { RiskAcceptanceForm, RiskSummary } from '../../api'
import {
  CONTROL_HEIGHT,
  CompactTextField,
  DomainBadge,
  FieldActionRow,
  FormFieldLabel,
  FormSelect,
  RiskLevelBadge,
  SettingsCard,
  apiRiskLevelToBadge,
  formFieldGridSx,
  fourColFieldSx,
  sectionAnchorSx,
  twoColFieldSx,
  NARRATIVE_MAX_ROWS,
} from '../ui'
import type { RiskLevel as BadgeLevel } from '../ui/DomainBadge'
import { brand } from '../../theme'
import { fmtLevel } from '../../riskLevelLabels'
import { excelRating, excelRatingToLevel, parseGridSize } from './riskMatrix'
import { formatTenantDate } from '../../uiQuality/tenantLocale'
import {
  EXCEL_FIELD,
  acceptanceChainState,
  acceptanceDecisionMark,
  acceptanceLevel2Label,
  acceptanceLevel3Label,
  acceptanceTurnLabel,
  withinAppetiteLabel,
} from './excelRiskFields'

export type DirectoryUser = { id: number; email: string; fullName: string }

const VALUE_SIZE = '0.78125rem'

const valueWellSx = {
  boxSizing: 'border-box' as const,
  height: CONTROL_HEIGHT,
  minHeight: CONTROL_HEIGHT,
  display: 'flex',
  alignItems: 'center',
  px: 1.25,
  border: `1px solid ${brand.border}`,
  borderRadius: '6px',
  bgcolor: brand.hover,
  fontSize: VALUE_SIZE,
  fontWeight: 600,
  color: brand.ink,
  lineHeight: 1.25,
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap' as const,
  minWidth: 0,
  width: '100%',
}

const valueWellWrapSx = {
  ...valueWellSx,
  height: 'auto',
  maxHeight: `calc(${VALUE_SIZE} * 1.4 * ${NARRATIVE_MAX_ROWS} + 12px)`,
  whiteSpace: 'pre-wrap' as const,
  wordBreak: 'break-word' as const,
  textOverflow: 'unset',
  overflowY: 'auto' as const,
  overflowX: 'hidden' as const,
  alignItems: 'flex-start',
  py: 0.75,
  lineHeight: 1.4,
}

function ReadField({
  label,
  value,
  wrap,
  tone,
}: {
  label: string
  value: string | number | null | undefined
  wrap?: boolean
  tone?: BadgeLevel
}) {
  const text =
    value == null || (typeof value === 'string' && value.trim() === '') ? '-' : String(value)
  return (
    <Box sx={{ minWidth: 0 }}>
      <FormFieldLabel wrap>{label}</FormFieldLabel>
      {tone && text !== '-' ? (
        <Box sx={{ minHeight: CONTROL_HEIGHT, display: 'flex', alignItems: 'center' }}>
          <RiskLevelBadge level={tone} label={text} />
        </Box>
      ) : (
        <Box sx={wrap ? valueWellWrapSx : valueWellSx} title={text}>
          {text}
        </Box>
      )}
    </Box>
  )
}

function optionLabel(options: { value: string; label: string }[], value: string): string {
  return options.find((o) => o.value === value)?.label || '-'
}

function riskValueText(risk: RiskSummary): string {
  if (risk.treatedRiskScore != null) return String(risk.treatedRiskScore)
  return fmtLevel(risk.residualRating)
}

function scoreTone(score: number | null | undefined, gridSize?: number | null): BadgeLevel | undefined {
  if (score == null || !Number.isFinite(Number(score))) return undefined
  return apiRiskLevelToBadge(excelRatingToLevel(excelRating(parseGridSize(gridSize), Number(score))))
}

function riskValueTone(risk: RiskSummary, gridSize?: number | null): BadgeLevel | undefined {
  const text = riskValueText(risk)
  if (!text || text === '-') return undefined
  const n = Number(text)
  if (Number.isFinite(n) && String(n) === text.trim()) return scoreTone(n, gridSize)
  return apiRiskLevelToBadge(excelRatingToLevel(text))
}

function userOptionLabel(u: DirectoryUser): string {
  const name = u.fullName?.trim() || u.email
  return u.email && name !== u.email ? `${name} (${u.email})` : name
}

function ermCategory(risk: RiskSummary): string | null {
  if (risk.riskType !== 'ERM' || !risk.category) return null
  return risk.category === 'OTHER' && risk.categoryOther?.trim()
    ? `Other: ${risk.categoryOther.trim()}`
    : risk.category.replace(/_/g, ' ')
}

function AcceptanceMarkChip({ mark }: { mark: string }) {
  if (mark === 'Approved') return <DomainBadge variant="lifecycle" status="closed" label="Approved" />
  if (mark === 'Waiting') return <DomainBadge variant="lifecycle" status="open" label="Waiting" />
  if (mark === 'Rejected' || mark === 'Send Back') {
    return <DomainBadge variant="gap" status="nonCompliant" label={mark} />
  }
  return <DomainBadge variant="lifecycle" status="draft" label="-" />
}

function ApproverCell({
  label,
  name,
  decision,
  waiting,
  inferredApproved,
  comments,
}: {
  label: string
  name: string | null | undefined
  decision?: string | null
  waiting: boolean
  inferredApproved?: boolean
  comments?: string | null
}) {
  const mark = acceptanceDecisionMark(decision, waiting, inferredApproved)
  return (
    <Box sx={{ minWidth: 0 }}>
      <FormFieldLabel wrap>{label}</FormFieldLabel>
      <Stack spacing={0.5} sx={{ minHeight: 32, justifyContent: 'center' }}>
        <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, lineHeight: 1.35, wordBreak: 'break-word' }}>
          {name?.trim() || '-'}
        </Typography>
        <AcceptanceMarkChip mark={mark} />
        {comments?.trim() ? (
          <Typography sx={{ fontSize: '0.6875rem', color: 'text.secondary', lineHeight: 1.4 }}>
            {comments.trim()}
          </Typography>
        ) : null}
      </Stack>
    </Box>
  )
}

function AcceptanceSection1({
  risk,
  form,
  gridSize,
}: {
  risk: RiskSummary
  form: RiskAcceptanceForm | null | undefined
  gridSize?: number | null
}) {
  return (
    <SettingsCard compact autoHeight title="Pre-populated from the register" subtitle="Read only">
      <Box sx={fourColFieldSx}>
        <ReadField label={EXCEL_FIELD.acceptanceId} value={form?.displayId} />
        <ReadField label={EXCEL_FIELD.riskId} value={risk.displayId} />
        <ReadField label={EXCEL_FIELD.cycle} value={form?.cycleNo ?? 1} />
        <ReadField wrap label={EXCEL_FIELD.riskTitleRo} value={risk.title} />
        <ReadField wrap label={EXCEL_FIELD.riskCategoryRo} value={ermCategory(risk)} />
        <ReadField
          label={EXCEL_FIELD.treatedTargetResidual}
          value={risk.targetResidualScore}
          tone={scoreTone(risk.targetResidualScore, gridSize)}
        />
        <ReadField
          label={EXCEL_FIELD.riskValue}
          value={riskValueText(risk)}
          tone={riskValueTone(risk, gridSize)}
        />
        <ReadField
          label={EXCEL_FIELD.withinAppetiteRo}
          value={withinAppetiteLabel(risk.targetWithinAppetite ?? risk.withinAppetite)}
        />
      </Box>
    </SettingsCard>
  )
}

function AcceptanceSection2Read({
  form,
  ownerName,
}: {
  form: RiskAcceptanceForm | null | undefined
  ownerName: string | null | undefined
}) {
  return (
    <SettingsCard compact autoHeight title="Acceptance Section" subtitle="Read only">
      <Box sx={twoColFieldSx}>
        <ReadField wrap label={EXCEL_FIELD.justificationForAccepting} value={form?.justification} />
        <ReadField wrap label={EXCEL_FIELD.compensatingControls} value={form?.compensatingControls} />
      </Box>
      <Box sx={{ mt: 1 }}>
        <ReadField wrap label={EXCEL_FIELD.businessRationale} value={form?.businessRationale} />
      </Box>
      <Box sx={{ mt: 1, ...fourColFieldSx }}>
        <ReadField label={EXCEL_FIELD.acceptanceReviewDate} value={form?.reviewDate} />
        <ReadField label={EXCEL_FIELD.riskOwner} value={ownerName} />
      </Box>
    </SettingsCard>
  )
}

function chainNames(
  risk: RiskSummary,
  form: RiskAcceptanceForm | null | undefined,
  userLabel?: (id: number | null | undefined) => string,
) {
  const fallback = (id: number | null | undefined, extra?: string | null) =>
    extra?.trim() || (id != null ? userLabel?.(id) || `User #${id}` : null)
  return {
    owner: fallback(form?.approver1UserId ?? risk.riskOwnerUserId, form?.approver1Name || risk.riskOwnerName),
    ciso: fallback(form?.approver2UserId, form?.approver2Name),
    chair: fallback(form?.approver3UserId, form?.approver3Name),
    level2: acceptanceLevel2Label(risk.riskType),
    hasLevel3: form?.approver3UserId != null,
  }
}

export default function ExcelAcceptanceContributor({
  risk,
  form,
  users,
  disabled,
  justification,
  onJustification,
  compensatingControls,
  onCompensatingControls,
  businessRationale,
  onBusinessRationale,
  reviewDate,
  onReviewDate,
  reviewDateMin,
  reviewDateMax,
  reviewDateError,
  declared,
  onDeclared,
  ownerUserId,
  onOwnerUserId,
  approver2UserId,
  onApprover2UserId,
  approver3UserId,
  onApprover3UserId,
  level3Enabled,
  onLevel3Enabled,
  gridSize,
}: {
  risk: RiskSummary
  form: RiskAcceptanceForm | null | undefined
  users: DirectoryUser[]
  disabled: boolean
  justification: string
  onJustification: (v: string) => void
  compensatingControls: string
  onCompensatingControls: (v: string) => void
  businessRationale: string
  onBusinessRationale: (v: string) => void
  reviewDate: string
  onReviewDate: (v: string) => void
  reviewDateMin: string
  reviewDateMax: string
  reviewDateError: string
  declared: boolean
  onDeclared: (v: boolean) => void
  ownerUserId: number | ''
  onOwnerUserId: (v: number | '') => void
  approver2UserId: number | ''
  onApprover2UserId: (v: number | '') => void
  approver3UserId: number | ''
  onApprover3UserId: (v: number | '') => void
  level3Enabled: boolean
  onLevel3Enabled: (v: boolean) => void
  gridSize?: number | null
}) {
  const level2Label = acceptanceLevel2Label(risk.riskType)
  const ownerOptions = [
    { value: '', label: 'Select' },
    ...users
      .filter((u) => u.id !== approver2UserId && u.id !== approver3UserId)
      .map((u) => ({ value: String(u.id), label: userOptionLabel(u) })),
  ]
  const ownerName =
    optionLabel(ownerOptions, ownerUserId === '' ? '' : String(ownerUserId)) !== '-'
      ? optionLabel(ownerOptions, ownerUserId === '' ? '' : String(ownerUserId))
      : risk.riskOwnerName?.trim() || (risk.riskOwnerUserId != null ? `User #${risk.riskOwnerUserId}` : '-')
  const pickerUsers = users.filter(
    (u) => u.id !== ownerUserId && u.id !== risk.assignedContributorId,
  )
  const cisoOptions = [
    { value: '', label: 'Select…' },
    ...pickerUsers
      .filter((u) => u.id !== approver3UserId)
      .map((u) => ({ value: String(u.id), label: userOptionLabel(u) })),
  ]
  const level3Options = [
    { value: '', label: 'Select…' },
    ...users
      .filter((u) => u.id !== approver2UserId)
      .map((u) => ({ value: String(u.id), label: userOptionLabel(u) })),
  ]

  return (
    <Stack spacing={1.25}>
      <AcceptanceSection1 risk={risk} form={form} gridSize={gridSize} />
      <SettingsCard compact autoHeight title="Acceptance Section">
        <Stack spacing={1.25}>
          <Box sx={twoColFieldSx}>
            <CompactTextField
              label={EXCEL_FIELD.justificationForAccepting}
              wrapLabel
              required
              multiline
              minRows={2}
              maxRows={NARRATIVE_MAX_ROWS}
              fieldIdPrefix="accept-s2"
              disabled={disabled}
              value={justification}
              onChange={(e) => onJustification(e.target.value)}
            />
            <CompactTextField
              label={EXCEL_FIELD.compensatingControls}
              wrapLabel
              required
              multiline
              minRows={2}
              maxRows={NARRATIVE_MAX_ROWS}
              fieldIdPrefix="accept-s2"
              disabled={disabled}
              value={compensatingControls}
              onChange={(e) => onCompensatingControls(e.target.value)}
            />
          </Box>
          <CompactTextField
            label={EXCEL_FIELD.businessRationale}
            wrapLabel
            multiline
            minRows={2}
            maxRows={NARRATIVE_MAX_ROWS}
            fieldIdPrefix="accept-s2"
            disabled={disabled}
            value={businessRationale}
            onChange={(e) => onBusinessRationale(e.target.value)}
          />
          <Box
            sx={{
              ...formFieldGridSx,
              '& > * > :first-child': { minHeight: 32 },
            }}
          >
            {disabled ? (
              <ReadField
                label={EXCEL_FIELD.acceptanceReviewDate}
                value={formatTenantDate(reviewDate)}
              />
            ) : (
              <CompactTextField
                label={EXCEL_FIELD.acceptanceReviewDate}
                wrapLabel
                type="date"
                required
                fieldIdPrefix="accept-s2"
                value={reviewDate}
                onChange={(e) => onReviewDate(e.target.value)}
                error={!!reviewDateError}
                helperText={reviewDateError}
                hint="Max 12 months"
                hintPlacement="inline"
                inputProps={{ min: reviewDateMin, max: reviewDateMax }}
              />
            )}
            {disabled ? (
              <ReadField label={EXCEL_FIELD.riskOwner} value={ownerName} />
            ) : (
              <FormSelect
                label={EXCEL_FIELD.riskOwner}
                wrapLabel
                required
                value={ownerUserId === '' ? '' : String(ownerUserId)}
                options={ownerOptions}
                onChange={(v) => onOwnerUserId(v === '' ? '' : Number(v))}
                disabled={disabled}
              />
            )}
            {disabled ? (
              <ReadField
                label={level2Label}
                value={optionLabel(cisoOptions, approver2UserId === '' ? '' : String(approver2UserId))}
              />
            ) : (
              <FormSelect
                label={level2Label}
                wrapLabel
                required
                fullWidth
                value={approver2UserId === '' ? '' : String(approver2UserId)}
                options={cisoOptions}
                onChange={(v) => onApprover2UserId(v === '' ? '' : Number(v))}
              />
            )}
          </Box>
          <FieldActionRow sx={{ width: '100%' }}>
            <Box sx={{ flex: 1, minWidth: 0, maxWidth: { sm: '50%' } }}>
              {level3Enabled ? (
                disabled ? (
                  <ReadField
                    label={acceptanceLevel3Label()}
                    value={optionLabel(level3Options, approver3UserId === '' ? '' : String(approver3UserId))}
                  />
                ) : (
                  <FormSelect
                    label={acceptanceLevel3Label()}
                    wrapLabel
                    required
                    fullWidth
                    value={approver3UserId === '' ? '' : String(approver3UserId)}
                    options={level3Options}
                    onChange={(v) => onApprover3UserId(v === '' ? '' : Number(v))}
                  />
                )
              ) : (
                <ReadField
                  label={acceptanceLevel3Label()}
                  value="Off: after Level 2, goes to Admin"
                />
              )}
            </Box>
            <FormControlLabel
              sx={{ ml: 0, mr: 0, height: CONTROL_HEIGHT, alignItems: 'center', flexShrink: 0 }}
              control={
                <Checkbox
                  checked={level3Enabled}
                  disabled={disabled}
                  onChange={(e) => {
                    onLevel3Enabled(e.target.checked)
                    if (!e.target.checked) onApprover3UserId('')
                  }}
                  size="small"
                />
              }
              label={
                <Typography sx={{ fontSize: '0.75rem', lineHeight: 1.35 }}>
                  Add Level 3 approver
                </Typography>
              }
            />
          </FieldActionRow>
          <FormControlLabel
            control={
              <Checkbox
                checked={declared}
                disabled={disabled}
                onChange={(e) => onDeclared(e.target.checked)}
                size="small"
              />
            }
            label={
              <Typography sx={{ fontSize: '0.75rem' }}>
                I declare the acceptance details are accurate
              </Typography>
            }
          />
        </Stack>
      </SettingsCard>
    </Stack>
  )
}

export function ExcelAcceptanceApprover({
  risk,
  form,
  disabled,
  comments,
  onComments,
  declared,
  onDeclared,
  busy,
  onApprove,
  onReject,
  gridSize,
}: {
  risk: RiskSummary
  form: RiskAcceptanceForm | null | undefined
  disabled: boolean
  comments: string
  onComments: (v: string) => void
  declared: boolean
  onDeclared: (v: boolean) => void
  busy: boolean
  onApprove: () => void
  onReject: () => void
  gridSize?: number | null
}) {
  const st = form?.status ?? ''
  const names = chainNames(risk, form)
  const chain = acceptanceChainState(st, names.hasLevel3)
  const yourRole =
    st === 'PENDING_RISK_OWNER'
      ? EXCEL_FIELD.riskOwner
      : st === 'PENDING_APPROVER_2'
        ? names.level2
        : st === 'PENDING_APPROVER_3'
          ? acceptanceLevel3Label()
          : 'Approver'
  const rejectBlocked = !declared || !comments.trim() || busy || disabled
  const approveBlocked = !declared || busy || disabled

  return (
    <Stack spacing={1.25}>
      <SettingsCard compact autoHeight title="Your review" subtitle={`You are ${yourRole}: approve or reject this turn only`}>
        <Box sx={fourColFieldSx}>
          <ApproverCell
            label={EXCEL_FIELD.riskOwner}
            name={names.owner}
            decision={form?.riskOwnerDecision}
            waiting={chain.ownerWaiting}
            inferredApproved={chain.ownerDone}
            comments={form?.riskOwnerComments}
          />
          <ApproverCell
            label={names.level2}
            name={names.ciso}
            decision={form?.approver2Decision}
            waiting={chain.cisoWaiting}
            inferredApproved={chain.cisoDone}
            comments={form?.approver2Comments}
          />
          {names.hasLevel3 ? (
            <ApproverCell
              label={acceptanceLevel3Label()}
              name={names.chair}
              decision={form?.approver3Decision}
              waiting={chain.chairWaiting}
              inferredApproved={chain.chairDone}
              comments={form?.approver3Comments}
            />
          ) : (
            <ReadField label={acceptanceLevel3Label()} value="Not used" />
          )}
          <ReadField label="Your role" value={yourRole} />
        </Box>
      </SettingsCard>
      <AcceptanceSection1 risk={risk} form={form} gridSize={gridSize} />
      <AcceptanceSection2Read form={form} ownerName={names.owner} />
      <SettingsCard compact autoHeight title="Decision" subtitle="Reject requires a reason">
        <Stack spacing={1.25}>
          <CompactTextField
            label="Comments"
            wrapLabel
            multiline
            minRows={2}
            maxRows={NARRATIVE_MAX_ROWS}
            fieldIdPrefix="accept-decide"
            disabled={disabled}
            value={comments}
            onChange={(e) => onComments(e.target.value)}
            helperText="Required when you reject."
          />
          <FormControlLabel
            control={
              <Checkbox
                checked={declared}
                disabled={disabled}
                onChange={(e) => onDeclared(e.target.checked)}
                size="small"
              />
            }
            label={
              <Typography sx={{ fontSize: '0.75rem' }}>I declare this acceptance review</Typography>
            }
          />
          <Stack direction="row" spacing={1}>
            <Button variant="contained" disabled={approveBlocked} onClick={onApprove}>
              Approve
            </Button>
            <Button variant="outlined" color="error" disabled={rejectBlocked} onClick={onReject}>
              Reject
            </Button>
          </Stack>
        </Stack>
      </SettingsCard>
    </Stack>
  )
}

/** Read-only Owner / ERM Head / Level 3 (and Admin when in turn) progress. */
export function AcceptanceProgressCard({
  risk,
  form,
  userLabel,
}: {
  risk: RiskSummary
  form: RiskAcceptanceForm
  userLabel?: (id: number | null | undefined) => string
}) {
  const names = chainNames(risk, form, userLabel)
  const chain = acceptanceChainState(form.status, names.hasLevel3)
  const showAdmin = Boolean(form.adminDecision) || chain.adminWaiting
  return (
    <SettingsCard
      compact
      autoHeight
      title="Risk Acceptance"
      subtitle={acceptanceTurnLabel(form.status, risk.riskType)}
    >
      <Box sx={fourColFieldSx}>
        <ApproverCell
          label={EXCEL_FIELD.riskOwner}
          name={names.owner}
          decision={form.riskOwnerDecision}
          waiting={chain.ownerWaiting}
          inferredApproved={chain.ownerDone || form.riskOwnerDecision === 'APPROVED'}
          comments={form.riskOwnerComments}
        />
        <ApproverCell
          label={names.level2}
          name={names.ciso}
          decision={form.approver2Decision}
          waiting={chain.cisoWaiting}
          inferredApproved={chain.cisoDone}
          comments={form.approver2Comments}
        />
        {names.hasLevel3 ? (
          <ApproverCell
            label={acceptanceLevel3Label()}
            name={names.chair}
            decision={form.approver3Decision}
            waiting={chain.chairWaiting}
            inferredApproved={chain.chairDone}
            comments={form.approver3Comments}
          />
        ) : (
          <ReadField label={acceptanceLevel3Label()} value="Not used" />
        )}
        {showAdmin ? (
          <ApproverCell
            label="Admin"
            name="Admin"
            decision={form.adminDecision}
            waiting={chain.adminWaiting && !form.adminDecision}
            comments={form.adminComments}
          />
        ) : (
          <ReadField label={EXCEL_FIELD.acceptanceId} value={form.displayId} />
        )}
      </Box>
    </SettingsCard>
  )
}

/** Admin click: packed Risk Acceptance sheet (Excel Section 1–3). */
export function ExcelAcceptanceDossier({
  risk,
  form,
  userLabel,
  showChain = true,
  gridSize,
}: {
  risk: RiskSummary
  form: RiskAcceptanceForm
  userLabel: (id: number | null | undefined) => string
  showChain?: boolean
  gridSize?: number | null
}) {
  const names = chainNames(risk, form, userLabel)
  return (
    <Box id="risk-section-acceptance" sx={sectionAnchorSx}>
    <Stack spacing={1.25}>
      {showChain ? <AcceptanceProgressCard risk={risk} form={form} userLabel={userLabel} /> : null}
      <AcceptanceSection1 risk={risk} form={form} gridSize={gridSize} />
      <AcceptanceSection2Read form={form} ownerName={names.owner} />
    </Stack>
    </Box>
  )
}
