import { Box, Checkbox, FormControlLabel, Stack, Typography } from '@mui/material'
import {
  POLICY_CATEGORY_LABELS,
  type FrameworkSummary,
  type OrgUnitNode,
  type PolicyAckScopeType,
  type PolicyCategory,
  type UserSummary,
} from '../../api'
import { CompactTextField, INPUT_LIMIT, SettingsCard, complexFormContainerSx } from '../ui'
import FormSelect from '../ui/FormSelect'
import FormFieldLabel, { compactSelectSx } from '../ui/FormFieldLabel'
import { MenuItem, Select } from '@mui/material'
import { brand } from '../../theme'

const ACK_SCOPES: PolicyAckScopeType[] = ['ALL_ACTIVE_USERS', 'ORG_UNITS', 'SELECTED_USERS']
const ACK_SCOPE_LABELS: Record<PolicyAckScopeType, string> = {
  ALL_ACTIVE_USERS: 'All active users',
  ORG_UNITS: 'Selected organisation units',
  SELECTED_USERS: 'Selected users',
}

export type PolicyFormState = {
  title: string
  category: PolicyCategory | ''
  orgUnitId: number | ''
  bodyText: string
  reviewDate: string
  ackRequired: boolean
  ackScopeType: PolicyAckScopeType
  ackDeadline: string
  ownerUserId: number | ''
  frameworkIds: number[]
  ackOrgUnitIds: number[]
  ackUserIds: number[]
}

export default function PolicyCreateEditForm({
  form,
  setForm,
  categories,
  units,
  users,
  frameworks,
  reviewDateError,
  ackDeadlineError,
  dateMin,
}: {
  form: PolicyFormState
  setForm: React.Dispatch<React.SetStateAction<PolicyFormState>>
  categories: PolicyCategory[]
  units: OrgUnitNode[]
  users: UserSummary[]
  frameworks: FrameworkSummary[]
  reviewDateError: string
  ackDeadlineError: string
  dateMin: string
}) {
  const categoryOptions = categories.map((c) => ({ value: c, label: POLICY_CATEGORY_LABELS[c] }))
  const ownerOptions = users
    .filter((u) => u.roles.includes('OWNER'))
    .map((u) => ({ value: String(u.id), label: u.fullName }))

  return (
    <Box sx={complexFormContainerSx}>
      <Stack spacing={1.25}>
        <Box id="policy-form-details">
          <SettingsCard compact title="Policy details" subtitle="Title, category, body, and review schedule">
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' },
                gap: { xs: 1, lg: 2 },
                alignItems: 'start',
              }}
            >
              <Stack spacing={1}>
                <CompactTextField
                  label="Title"
                  required
                  fieldIdPrefix="policy"
                  maxLength={INPUT_LIMIT.name}
                  value={form.title}
                  onChange={(e) => setForm((s) => ({ ...s, title: e.target.value }))}
                />
                <FormSelect
                  label="Category"
                  value={form.category}
                  options={categoryOptions}
                  onChange={(v) => setForm((s) => ({ ...s, category: v as PolicyCategory }))}
                  fullWidth
                />
                <FormSelect
                  label="Organisation unit (optional)"
                  value={form.orgUnitId === '' ? '' : String(form.orgUnitId)}
                  options={[
                    { value: '', label: '-' },
                    ...units.map((u) => ({ value: String(u.id), label: `L${u.level} · ${u.name}` })),
                  ]}
                  onChange={(v) => setForm((s) => ({ ...s, orgUnitId: v === '' ? '' : Number(v) }))}
                  fullWidth
                />
                <FormSelect
                  label="Owner"
                  value={form.ownerUserId === '' ? '' : String(form.ownerUserId)}
                  options={[{ value: '', label: '-' }, ...ownerOptions]}
                  onChange={(v) => setForm((s) => ({ ...s, ownerUserId: v === '' ? '' : Number(v) }))}
                  fullWidth
                />
                <CompactTextField
                  label="Review date"
                  type="date"
                  fieldIdPrefix="policy"
                  value={form.reviewDate}
                  onChange={(e) => setForm((s) => ({ ...s, reviewDate: e.target.value }))}
                  error={!!reviewDateError}
                  helperText={reviewDateError || undefined}
                  inputProps={{ min: dateMin }}
                />
                <Box>
                  <FormFieldLabel htmlFor="policy-frameworks">Linked frameworks (optional)</FormFieldLabel>
                  <Select
                    id="policy-frameworks"
                    multiple
                    size="small"
                    fullWidth
                    value={form.frameworkIds.map(String)}
                    onChange={(e) => {
                      const v = e.target.value
                      setForm((s) => ({
                        ...s,
                        frameworkIds: (typeof v === 'string' ? v.split(',') : v).map(Number),
                      }))
                    }}
                    displayEmpty
                    renderValue={(selected) =>
                      frameworks
                        .filter((f) => (selected as string[]).includes(String(f.id)))
                        .map((f) => f.code)
                        .join(', ') || '-'
                    }
                    sx={compactSelectSx}
                  >
                    {frameworks.map((f) => (
                      <MenuItem key={f.id} value={String(f.id)}>
                        <Checkbox size="small" checked={form.frameworkIds.includes(f.id)} />
                        {f.name} ({f.code})
                      </MenuItem>
                    ))}
                  </Select>
                </Box>
              </Stack>
              <CompactTextField
                label="Body text"
                multiline
                minRows={6}
                fieldIdPrefix="policy"
                maxLength={INPUT_LIMIT.policyBody}
                value={form.bodyText}
                onChange={(e) => setForm((s) => ({ ...s, bodyText: e.target.value }))}
                helperText="Markdown or plain text. A file can also be attached after saving."
              />
            </Box>
          </SettingsCard>
        </Box>

        <Box id="policy-form-ack">
          <SettingsCard compact title="Acknowledgement" subtitle="Required user acknowledgement on approval">
            <Stack spacing={1}>
              <FormControlLabel
                sx={{ m: 0, alignItems: 'flex-start' }}
                control={
                  <Checkbox
                    size="small"
                    checked={form.ackRequired}
                    onChange={(e) => setForm((s) => ({ ...s, ackRequired: e.target.checked }))}
                  />
                }
                label={
                  <Typography sx={{ fontSize: '0.6875rem', lineHeight: 1.35 }}>
                    Acknowledgement required on approval
                  </Typography>
                }
              />
              {form.ackRequired && (
                <Stack spacing={1}>
                  <FormSelect
                    label="Acknowledgement scope"
                    value={form.ackScopeType}
                    options={ACK_SCOPES.map((s) => ({ value: s, label: ACK_SCOPE_LABELS[s] }))}
                    onChange={(v) => setForm((s) => ({ ...s, ackScopeType: v as PolicyAckScopeType }))}
                    fullWidth
                  />
                  {form.ackScopeType === 'ORG_UNITS' && (
                    <Box>
                      <FormFieldLabel htmlFor="policy-ack-units">Organisation units</FormFieldLabel>
                      <Select
                        id="policy-ack-units"
                        multiple
                        size="small"
                        fullWidth
                        value={form.ackOrgUnitIds.map(String)}
                        onChange={(e) => {
                          const v = e.target.value
                          setForm((s) => ({
                            ...s,
                            ackOrgUnitIds: (typeof v === 'string' ? v.split(',') : v).map(Number),
                          }))
                        }}
                        renderValue={(selected) =>
                          units
                            .filter((u) => (selected as string[]).includes(String(u.id)))
                            .map((u) => u.name)
                            .join(', ')
                        }
                        sx={compactSelectSx}
                      >
                        {units.map((u) => (
                          <MenuItem key={u.id} value={String(u.id)}>
                            <Checkbox size="small" checked={form.ackOrgUnitIds.includes(u.id)} />
                            L{u.level} · {u.name}
                          </MenuItem>
                        ))}
                      </Select>
                    </Box>
                  )}
                  {form.ackScopeType === 'SELECTED_USERS' && (
                    <Box>
                      <FormFieldLabel htmlFor="policy-ack-users">Users</FormFieldLabel>
                      <Select
                        id="policy-ack-users"
                        multiple
                        size="small"
                        fullWidth
                        value={form.ackUserIds.map(String)}
                        onChange={(e) => {
                          const v = e.target.value
                          setForm((s) => ({
                            ...s,
                            ackUserIds: (typeof v === 'string' ? v.split(',') : v).map(Number),
                          }))
                        }}
                        renderValue={(selected) =>
                          users
                            .filter((u) => (selected as string[]).includes(String(u.id)))
                            .map((u) => u.fullName)
                            .join(', ')
                        }
                        sx={compactSelectSx}
                      >
                        {users.map((u) => (
                          <MenuItem key={u.id} value={String(u.id)}>
                            <Checkbox size="small" checked={form.ackUserIds.includes(u.id)} />
                            {u.fullName} ({u.email})
                          </MenuItem>
                        ))}
                      </Select>
                    </Box>
                  )}
                  <CompactTextField
                    label="Acknowledgement deadline"
                    type="date"
                    fieldIdPrefix="policy"
                    value={form.ackDeadline}
                    onChange={(e) => setForm((s) => ({ ...s, ackDeadline: e.target.value }))}
                    error={!!ackDeadlineError}
                    helperText={ackDeadlineError || undefined}
                    inputProps={{ min: dateMin }}
                  />
                </Stack>
              )}
            </Stack>
          </SettingsCard>
        </Box>
      </Stack>
    </Box>
  )
}

export const POLICY_FORM_SECTIONS = [
  { id: 'policy-form-details', label: 'Details' },
  { id: 'policy-form-ack', label: 'Acknowledgement' },
] as const
