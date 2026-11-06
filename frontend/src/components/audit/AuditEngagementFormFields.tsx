import {
  Box,
  Checkbox,
  FormControlLabel,
  FormGroup,
  Stack,
  Typography,
} from '@mui/material'
import type { FrameworkSummary, GrcAuditDetail, GrcAuditType } from '../../api'
import {
  CompactTextField,
  FormSelect,
  SettingsCard,
  twoColFieldSx,
  DATE_FIELD_WIDTH,
} from '../ui'
import ChecklistLibraryMultiSelect from './ChecklistLibraryMultiSelect'
import { brand } from '../../theme'
import {
  AUDIT_INPUT_ID,
  SCOPE_MODULES,
  type useAuditEngagementForm,
} from './useAuditEngagementForm'

type FormState = ReturnType<typeof useAuditEngagementForm>

function leadOptionLabel(fullName: string, email: string): string {
  const name = fullName.trim()
  const mail = email.trim()
  if (!mail || name.toLowerCase() === mail.toLowerCase()) return name || mail
  return `${name} (${mail})`
}

/* ─────────────────────────────────────────────────────────────────────────────
 * Excel Field Labels — match Audit_Module_MVP_v2.xlsx Audit Initiation exactly
 * ───────────────────────────────────────────────────────────────────────────── */
const EXCEL_FIELD = {
  auditTitle: 'Audit Title',
  auditType: 'Audit Type',
  standard: 'Standard',
  checklistUsed: 'Checklist Used',
  modulesInScope: 'Modules in Scope',
  auditeeDivision: 'Auditee Division',
  leadAuditor: 'Lead Auditor',
  auditorOrganisation: 'Auditor Organisation',
  plannedStart: 'Planned Start',
  plannedEnd: 'Planned End',
} as const

/* ─────────────────────────────────────────────────────────────────────────────
 * Modules in Scope — checkbox grid (Excel column E)
 * Subtle styling: no gold highlight, just border + check
 * ───────────────────────────────────────────────────────────────────────────── */
function ScopeModulesGrid({
  modules,
  onToggle,
  error,
}: {
  modules: FormState['scopeModules']
  onToggle: (m: (typeof SCOPE_MODULES)[0]['value']) => void
  error?: string
}) {
  return (
    <Box>
      <Typography
        variant="caption"
        fontWeight={600}
        color={error ? 'error.main' : 'text.primary'}
        display="block"
        sx={{ mb: 0.5 }}
      >
        {EXCEL_FIELD.modulesInScope} *
      </Typography>
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(4, 1fr)' },
          gap: 0.5,
        }}
      >
        {SCOPE_MODULES.map((m) => {
          const checked = modules.includes(m.value)
          return (
            <FormControlLabel
              key={m.value}
              control={
                <Checkbox
                  size="small"
                  checked={checked}
                  onChange={() => onToggle(m.value)}
                  sx={{ py: 0.25 }}
                />
              }
              label={
                <Box sx={{ minWidth: 0 }}>
                  <Typography variant="caption" fontWeight={500} display="block" noWrap>
                    {m.label}
                  </Typography>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    display="block"
                    noWrap
                    sx={{ fontSize: '0.65rem' }}
                  >
                    {m.note}
                  </Typography>
                </Box>
              }
              sx={{ mr: 0, alignItems: 'flex-start' }}
            />
          )
        })}
      </Box>
      {error && (
        <Typography variant="caption" color="error.main" sx={{ mt: 0.25, display: 'block' }}>
          {error}
        </Typography>
      )}
    </Box>
  )
}

/* ─────────────────────────────────────────────────────────────────────────────
 * External Lead Auditor Invite Inputs
 * ───────────────────────────────────────────────────────────────────────────── */
function ExternalLeadInviteFields({ form }: { form: FormState }) {
  return (
    <Box
      sx={{
        p: 1,
        borderRadius: 1,
        bgcolor: brand.hover,
        border: `1px solid ${brand.border}`,
      }}
    >
      <Typography variant="caption" fontWeight={600} display="block" sx={{ mb: 0.5 }}>
        Invite new external auditor
      </Typography>
      <Box sx={twoColFieldSx}>
        <CompactTextField
          label="Email"
          type="email"
          value={form.inviteEmail}
          onChange={(e) => form.setInviteEmail(e.target.value)}
          required
        />
        <CompactTextField
          label="Full name"
          value={form.inviteName}
          onChange={(e) => form.setInviteName(e.target.value)}
          required
        />
      </Box>
    </Box>
  )
}

/* ─────────────────────────────────────────────────────────────────────────────
 * MAIN: Audit Initiation Form Fields (Excel columns A-L order)
 *
 * Layout per Excel:
 * Row 1: Audit Type, Standard, Checklist Used
 * Row 2: Audit Title, Lead Auditor (same row)
 * Row 3: Auditee Division, Planned Start, Planned End (same row)
 * Row 4: Auditor Organisation (if external)
 * Row 5: Modules in Scope (checkbox grid)
 * ───────────────────────────────────────────────────────────────────────────── */
export function AuditInitiationFields({
  mode,
  detail,
  orgUnits,
  form,
  internalPlannerOnly = false,
}: {
  mode: 'create' | 'edit'
  detail?: GrcAuditDetail | null
  orgUnits: { id: number; name: string; level: number }[]
  form: FormState
  internalPlannerOnly?: boolean
}) {
  const err = (id: string) => form.fieldErrors[id] || undefined
  const clear = (id: string) => form.clearFieldError(id)

  const externalNewLead =
    form.auditType === 'EXTERNAL' && form.inviteEmail.trim() !== '' && form.inviteName.trim() !== ''

  return (
    <Stack spacing={1.25}>
      {/* Read-only summary for edit mode */}
      {mode === 'edit' && detail && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
          {detail.audit.displayId} · {detail.audit.auditStandard ?? detail.audit.frameworkCode} (
          {detail.audit.auditType})
        </Typography>
      )}

      {/* Row 1: Audit Type, Standard, Checklist Used */}
      {mode === 'create' && (
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, minmax(0, 1fr))' },
            gap: 1,
            alignItems: 'start',
          }}
        >
          <FormSelect
            label={EXCEL_FIELD.auditType}
            wrapLabel
            required
            value={form.auditType}
            options={
              internalPlannerOnly
                ? [{ value: 'INTERNAL', label: 'Internal' }]
                : [
                    { value: 'INTERNAL', label: 'Internal' },
                    { value: 'EXTERNAL_ASSESSMENT', label: 'External Assessment' },
                    { value: 'FOLLOW_UP', label: 'Follow-up' },
                  ]
            }
            onChange={(v) => {
              clear(AUDIT_INPUT_ID.auditType)
              form.setAuditType(v as GrcAuditType)
            }}
          />
          <FormSelect
            label={EXCEL_FIELD.standard}
            wrapLabel
            required
            value={form.standard}
            options={[
              { value: 'ISO_27001', label: 'ISO 27001' },
              { value: 'ISO_20000', label: 'ISO 20000' },
              { value: 'UAE_IA', label: 'UAE IA' },
              { value: 'ISO_27701', label: 'ISO 27701' },
              { value: 'MULTIPLE', label: 'Multiple' },
            ]}
            onChange={(v) => {
              clear(AUDIT_INPUT_ID.standard)
              const s = v as typeof form.standard
              form.setStandard(s)
              form.pickLibraryForStandard(s)
            }}
          />
          {form.standard === 'MULTIPLE' ? (
            <ChecklistLibraryMultiSelect
              id={AUDIT_INPUT_ID.checklistTemplate}
              label={EXCEL_FIELD.checklistUsed}
              required
              options={form.libraries.map((l) => ({ id: l.id, label: l.name }))}
              value={form.checklistLibraryIds}
              onChange={(ids) => {
                clear(AUDIT_INPUT_ID.checklistTemplate)
                form.setChecklistLibraryIds(ids)
              }}
              error={Boolean(form.fieldErrors[AUDIT_INPUT_ID.checklistTemplate])}
              helperText={
                form.fieldErrors[AUDIT_INPUT_ID.checklistTemplate] ??
                'Select every checklist this audit will use'
              }
            />
          ) : (
            <FormSelect
              label={EXCEL_FIELD.checklistUsed}
              wrapLabel
              required
              value={form.checklistLibraryId === '' ? '' : String(form.checklistLibraryId)}
              options={
                form.libraries.length
                  ? form.libraries.map((l) => ({ value: String(l.id), label: l.name }))
                  : [
                      { value: 'ISO_27001', label: 'Checklist ISO 27001' },
                      { value: 'ISO_20000', label: 'Checklist ISO 20000' },
                      { value: 'UAE_IA', label: 'Checklist UAE IA' },
                    ]
              }
              onChange={(v) => {
                clear(AUDIT_INPUT_ID.checklistTemplate)
                const id = Number(v)
                if (!Number.isFinite(id)) {
                  form.setChecklistTemplate(v as typeof form.checklistTemplate)
                  return
                }
                form.setChecklistLibraryId(id)
                const lib = form.libraries.find((l) => l.id === id)
                if (lib && lib.templateKey !== 'CUSTOM') form.setChecklistTemplate(lib.templateKey)
              }}
              error={Boolean(form.fieldErrors[AUDIT_INPUT_ID.checklistTemplate])}
              helperText={form.fieldErrors[AUDIT_INPUT_ID.checklistTemplate]}
            />
          )}
        </Box>
      )}

      {/* Row 2: Audit Title + Lead Auditor (same row) */}
      <Box sx={twoColFieldSx}>
        <CompactTextField
          label={EXCEL_FIELD.auditTitle}
          wrapLabel
          required
          value={form.name}
          onChange={(e) => {
            clear(AUDIT_INPUT_ID.title)
            form.setName(e.target.value)
          }}
          error={Boolean(err(AUDIT_INPUT_ID.title))}
          fullWidth
        />
        <FormSelect
          label={EXCEL_FIELD.leadAuditor}
          wrapLabel
          required={form.auditType !== 'EXTERNAL' || !externalNewLead}
          value={form.leadId === '' ? '' : String(form.leadId)}
          options={[
            ...(form.auditType === 'EXTERNAL'
              ? [{ value: '', label: '- Invite new -' }]
              : []),
            ...form.leadCandidates.map((u) => ({
              value: String(u.id),
              label: leadOptionLabel(u.fullName, u.email),
            })),
          ]}
          onChange={(v) => {
            clear(AUDIT_INPUT_ID.leadAuditor)
            form.setLeadId(v === '' ? '' : Number(v))
            if (v !== '') {
              form.setInviteEmail('')
              form.setInviteName('')
            }
          }}
          error={Boolean(err(AUDIT_INPUT_ID.leadAuditor))}
          disabled={externalNewLead}
        />
      </Box>

      {/* Row 3: Auditee Division, Planned Start, Planned End (same row) */}
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', sm: '1fr auto auto' },
          gap: 1,
          alignItems: 'start',
        }}
      >
        <FormSelect
          label={EXCEL_FIELD.auditeeDivision}
          wrapLabel
          required
          value={form.auditeeOrgUnitId === '' ? '' : String(form.auditeeOrgUnitId)}
          options={[
            { value: '', label: 'Select division…' },
            ...orgUnits.map((u) => ({
              value: String(u.id),
              label: u.name,
            })),
          ]}
          onChange={(v) => {
            clear(AUDIT_INPUT_ID.auditeeOrgUnitId)
            form.setAuditee(v === '' ? '' : Number(v))
          }}
          error={Boolean(err(AUDIT_INPUT_ID.auditeeOrgUnitId))}
        />
        <CompactTextField
          label={EXCEL_FIELD.plannedStart}
          wrapLabel
          type="date"
          value={form.startDate}
          onChange={(e) => form.setStartDate(e.target.value)}
          InputLabelProps={{ shrink: true }}
          maxWidth={DATE_FIELD_WIDTH}
        />
        <CompactTextField
          label={EXCEL_FIELD.plannedEnd}
          wrapLabel
          type="date"
          value={form.endDate}
          onChange={(e) => form.setEndDate(e.target.value)}
          InputLabelProps={{ shrink: true }}
          maxWidth={DATE_FIELD_WIDTH}
        />
      </Box>

      {/* Row 4: Auditor Organisation (External only) */}
      {(form.auditType === 'EXTERNAL_ASSESSMENT' || form.auditType === 'EXTERNAL') && (
        <CompactTextField
          label={EXCEL_FIELD.auditorOrganisation}
          wrapLabel
          required={form.auditType === 'EXTERNAL_ASSESSMENT'}
          value={form.auditorOrganisation}
          onChange={(e) => {
            clear(AUDIT_INPUT_ID.auditorOrganisation)
            form.setAuditorOrganisation(e.target.value)
          }}
          error={Boolean(err(AUDIT_INPUT_ID.auditorOrganisation))}
          maxWidth={320}
        />
      )}

      {/* External invite fields when no existing lead selected */}
      {form.auditType === 'EXTERNAL' && form.leadId === '' && (
        <ExternalLeadInviteFields form={form} />
      )}

      {/* Row 5: Modules in Scope */}
      <ScopeModulesGrid
        modules={form.scopeModules}
        onToggle={form.toggleScopeModule}
        error={err(AUDIT_INPUT_ID.scopeModules)}
      />
    </Stack>
  )
}

/* ─────────────────────────────────────────────────────────────────────────────
 * LEGACY EXPORTS — keep for backward compatibility
 * ───────────────────────────────────────────────────────────────────────────── */

/** @deprecated Use AuditInitiationFields instead */
export function EngagementDetailsFields({
  mode,
  detail,
  frameworks,
  form,
  internalPlannerOnly = false,
}: {
  mode: 'create' | 'edit'
  detail?: GrcAuditDetail | null
  frameworks: FrameworkSummary[]
  form: FormState
  internalPlannerOnly?: boolean
}) {
  return (
    <Stack spacing={1.5}>
      <CompactTextField
        label="Audit title"
        fieldIdPrefix="audit-eng"
        value={form.name}
        onChange={(e) => form.setName(e.target.value)}
        fullWidth
        required
      />
      {mode === 'create' && (
        <>
          <FormSelect
            label="Audit type"
            value={form.auditType}
            options={[
              { value: 'INTERNAL', label: 'Internal' },
              { value: 'EXTERNAL_ASSESSMENT', label: 'External assessment' },
              { value: 'FOLLOW_UP', label: 'Follow-up' },
            ]}
            onChange={(v) => form.setAuditType(v as GrcAuditType)}
          />
          <FormSelect
            label="Standard"
            value={form.standard}
            options={[
              { value: 'ISO_27001', label: 'ISO 27001' },
              { value: 'ISO_20000', label: 'ISO 20000' },
              { value: 'UAE_IA', label: 'UAE IA' },
              { value: 'ISO_27701', label: 'ISO 27701' },
              { value: 'MULTIPLE', label: 'Multiple' },
            ]}
            onChange={(v) => {
              const s = v as typeof form.standard
              form.setStandard(s)
              if (s === 'ISO_20000') form.setChecklistTemplate('ISO_20000')
              else if (s === 'UAE_IA') form.setChecklistTemplate('UAE_IA')
              else form.setChecklistTemplate('ISO_27001')
            }}
          />
          <FormSelect
            label="Checklist used"
            value={form.checklistTemplate}
            options={[
              { value: 'ISO_27001', label: 'Checklist ISO 27001' },
              { value: 'ISO_20000', label: 'Checklist ISO 20000' },
              { value: 'UAE_IA', label: 'Checklist UAE IA' },
            ]}
            onChange={(v) => form.setChecklistTemplate(v as typeof form.checklistTemplate)}
          />
          {form.auditType === 'EXTERNAL_ASSESSMENT' && (
            <CompactTextField
              label="Auditor organisation"
              fieldIdPrefix="audit-org"
              value={form.auditorOrganisation}
              onChange={(e) => form.setAuditorOrganisation(e.target.value)}
              required
              fullWidth
            />
          )}
        </>
      )}
      {mode === 'edit' && detail && (
        <Typography variant="body2" color="text.secondary">
          {detail.audit.displayId} · {detail.audit.auditStandard ?? detail.audit.frameworkCode} ({detail.audit.auditType})
        </Typography>
      )}
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
        <CompactTextField
          label="Planned start"
          type="date"
          fieldIdPrefix="audit-eng"
          value={form.startDate}
          onChange={(e) => form.setStartDate(e.target.value)}
          InputLabelProps={{ shrink: true }}
          fullWidth
        />
        <CompactTextField
          label="Planned end"
          type="date"
          fieldIdPrefix="audit-eng"
          value={form.endDate}
          onChange={(e) => form.setEndDate(e.target.value)}
          InputLabelProps={{ shrink: true }}
          fullWidth
        />
      </Stack>
    </Stack>
  )
}

/** @deprecated */
export function AuditorAssignmentFields({
  orgUnits,
  form,
}: {
  orgUnits: { id: number; name: string; level: number }[]
  form: FormState
}) {
  return null // Deprecated - use AuditInitiationFields
}

/** @deprecated */
export function ExternalLeadAuditorInputs({ form }: { form: FormState }) {
  return <ExternalLeadInviteFields form={form} />
}

/** @deprecated */
export function ExternalInviteFields({ form }: { form: FormState }) {
  return null
}
