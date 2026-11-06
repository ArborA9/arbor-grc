import { Box, Button, Stack, Typography } from '@mui/material'
import type {
  FrameworkSummary,
  GapAssessmentSummary,
  GrcAuditDetail,
  UserSummary,
} from '../../api'
import { SettingsCard, complexFormContainerSx, BusyButton } from '../ui'
import { AuditInitiationFields } from './AuditEngagementFormFields'
import { useAuditEngagementForm } from './useAuditEngagementForm'
import { useRegisterUnsavedChanges } from '../../uiQuality/UnsavedChangesGuard'

export default function AuditEngagementCreateView({
  users,
  frameworks,
  gaps,
  orgUnits,
  onCancel,
  onSaved,
  onError,
  internalPlanner = false,
  defaultLeadUserId,
}: {
  users: UserSummary[]
  frameworks: FrameworkSummary[]
  gaps: GapAssessmentSummary[]
  orgUnits: { id: number; name: string; level: number }[]
  onCancel: () => void
  onSaved: (d: GrcAuditDetail) => void
  onError: (msg: string) => void
  /** Internal Auditor self-plan: internal engagements only. */
  internalPlanner?: boolean
  defaultLeadUserId?: number
}) {
  const form = useAuditEngagementForm({
    mode: 'create',
    users,
    frameworks,
    gaps,
    active: true,
    defaultLeadUserId,
    internalPlannerOnly: internalPlanner,
  })

  useRegisterUnsavedChanges(
    form.dirty && !form.busy
      ? { dirty: true, title: 'Leave Audit Initiation?', message: 'You have unsaved changes.' }
      : null,
  )

  const canSubmit = form.validate() === null
  const submit = async () => {
    try {
      const d = await form.save()
      onSaved(d)
    } catch (e) {
      onError(String(e))
    }
  }

  return (
    <Stack spacing={2} sx={{ flex: 1, minHeight: 0, width: '100%', overflow: 'auto', p: 2 }}>
      <Stack
        direction="row"
        alignItems="center"
        justifyContent="space-between"
        flexWrap="wrap"
        gap={1}
      >
        <Typography variant="h6" fontWeight={700}>
          Audit Initiation
        </Typography>
        <Stack direction="row" spacing={1} alignItems="center">
          <Button size="small" variant="outlined" onClick={onCancel} disabled={form.busy}>
            Cancel
          </Button>
          <BusyButton
            size="small"
            variant="contained"
            busy={form.busy}
            busyLabel="Creating..."
            disabled={!canSubmit}
            disabledReason={!canSubmit ? form.validate() ?? 'Complete required fields' : undefined}
            onClick={() => void submit()}
          >
            Create Audit
          </BusyButton>
        </Stack>
      </Stack>

      <Box sx={{ ...complexFormContainerSx, flex: 1, minWidth: 0 }}>
        <SettingsCard compact heading autoHeight title="Audit details">
          <AuditInitiationFields
            mode="create"
            orgUnits={orgUnits}
            form={form}
            internalPlannerOnly={internalPlanner}
          />
        </SettingsCard>
      </Box>
    </Stack>
  )
}
