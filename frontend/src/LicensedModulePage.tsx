import { Box, Button, Stack, Typography } from '@mui/material'
import { MODULE_LABELS, type ModuleLicense } from './api'
import {
  DomainBadge,
  SettingsCard,
  SettingsCardGrid,
  SettingsField,
  SettingsReadonlyValue,
} from './components/ui'
import { ModuleListShell } from './hooks/useModulePageShell'
import { brand } from './theme'
import {
  formatLicenseDateTime,
  moduleDescription,
  moduleShortLabel,
} from './moduleDisplay'
import { licenceTermView } from './licenseExpiry'
import { isBcmsNavPending } from './components/bcms/bcmsNav'
import { isPrivacyNavPending } from './components/privacy/privacyNav'
import { isRiskNavHidden } from './components/risk/riskNav'

/** What the client console unlocks per commercial module (read-only licence view). */
const MODULE_CAPABILITIES: Record<string, string[]> = {
  ISMS: [
    'Frameworks (activate ISO / UAE IA libraries)',
    'Compliance Assessment: Projects, SoA, Gap',
    'Asset register',
    'Information Security Risk register',
  ],
  ERM: ['ERM risk register (enterprise risks)', 'TPRM (vendor questionnaires)'],
  BCMS: ['BIA', 'Recovery plans'],
  PRIVACY: ['Data inventory', 'ROPA', 'DSR (PDPL 30-day clock)', 'DPIA', 'Consent', 'Breaches'],
  ITSM: ['ISO 20000 compliance (roadmap)'],
  AIMS: ['AI governance / ISO 42001 (roadmap)'],
  AUDIT: ['Audit Projects', 'Audit Checklist', 'CAPA'],
  QMS: ['Frameworks (activate ISO 9001)', 'Statement of Applicability', 'Assessments: Gap (ISO scoring)'],
  OHSMS: ['Frameworks (activate ISO 45001)', 'Statement of Applicability', 'Assessments: Gap (ISO scoring)'],
  SAMA: [
    'Frameworks (activate SAMA CSF)',
    'Applicability register (SoA)',
    'Assessments: Gap (maturity profile)',
  ],
}

const MODULE_WORKSPACE_SHORTCUTS: Record<string, { navId: string; label: string }[]> = {
  ISMS: [
    { navId: 'frameworks', label: 'Frameworks' },
    { navId: 'assessments-projects', label: 'Projects' },
    { navId: 'assessments', label: 'Gap Assessments' },
    { navId: 'soa', label: 'Statement of Applicability' },
    { navId: 'assets', label: 'Assets' },
    { navId: 'risk', label: 'Information Security Risk' },
  ],
  ERM: [
    { navId: 'erm-risk', label: 'Enterprise Risk' },
    { navId: 'tprm-projects', label: 'Third-Party Risk' },
    { navId: 'kri', label: 'KRI' },
    { navId: 'incidents', label: 'Incidents' },
  ].filter((item) => !isRiskNavHidden(item.navId)),
  PRIVACY: [
    { navId: 'privacy-inventory', label: 'Data Inventory' },
    { navId: 'privacy-ropa', label: 'ROPA' },
    { navId: 'privacy-dsr', label: 'DSR' },
    { navId: 'privacy-dpia-projects', label: 'DPIA Projects' },
    { navId: 'privacy-dpia-questionnaires', label: 'DPIA Questionnaire' },
    { navId: 'privacy-risk', label: 'Privacy Risk' },
  ].filter((item) => !isPrivacyNavPending(item.navId)),
  BCMS: [
    { navId: 'bcms-profiles', label: 'Dept Profiles' },
    { navId: 'bcms-bia', label: 'BIA' },
    { navId: 'bcms-plans', label: 'Recovery Plans' },
    { navId: 'bcms-exercises', label: 'Exercises' },
    { navId: 'bcms-ncema', label: 'NCEMA' },
  ].filter((item) => !isBcmsNavPending(item.navId)),
  AUDIT: [
    { navId: 'audit-register', label: 'Audit Projects' },
    { navId: 'audit-checklists', label: 'Audit Checklist' },
    { navId: 'audit-capa', label: 'CAPA' },
  ],
  QMS: [
    { navId: 'frameworks', label: 'Frameworks' },
    { navId: 'assessments-projects', label: 'Projects' },
    { navId: 'assessments', label: 'Gap Assessments' },
    { navId: 'soa', label: 'Statement of Applicability' },
  ],
  OHSMS: [
    { navId: 'frameworks', label: 'Frameworks' },
    { navId: 'assessments-projects', label: 'Projects' },
    { navId: 'assessments', label: 'Gap Assessments' },
    { navId: 'soa', label: 'Statement of Applicability' },
  ],
  SAMA: [
    { navId: 'frameworks', label: 'Frameworks' },
    { navId: 'assessments-projects', label: 'Projects' },
    { navId: 'assessments', label: 'Gap Assessments' },
    { navId: 'soa', label: 'Applicability (SoA)' },
  ],
}

export function parseLicensedModuleActive(active: string): string | null {
  if (!active.startsWith('licensed-module:')) return null
  return active.slice('licensed-module:'.length) || null
}

export function licensedModuleNavId(code: string): string {
  return `licensed-module:${code}`
}

export default function LicensedModulePage({
  code,
  license,
  onOpenNav,
}: {
  code: string
  license: ModuleLicense | null
  onOpenNav?: (navId: string) => void
}) {
  const shortName = moduleShortLabel(code)
  const description = moduleDescription(code) || MODULE_LABELS[code] || code
  const caps = MODULE_CAPABILITIES[code] ?? ['Licensed commercial module']
  const shortcuts = MODULE_WORKSPACE_SHORTCUTS[code] ?? []
  const enabled = license?.enabled ?? false
  const term = licenceTermView({
    expiresOn: license?.expiresOn,
    expiresAt: license?.expiresAt,
    expired: license?.expired,
    status: license?.status,
    daysRemaining: license?.daysRemaining,
  })
  const expired = enabled && term.kind === 'expired'

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
      <ModuleListShell
        title={shortName}
        breadcrumbTrail={`Modules › ${shortName}`}
      />

      <SettingsCardGrid>
        <SettingsCard
          title="Licence status"
          subtitle={description !== shortName ? description : undefined}
        >
          <Stack spacing={1.25}>
            <Box>
              <DomainBadge
                variant="lifecycle"
                status={enabled ? (expired ? 'draft' : 'closed') : 'draft'}
                label={
                  !enabled ? 'INACTIVE' : expired ? 'EXPIRED' : term.kind === 'soon' || term.kind === 'today' ? 'EXPIRING' : 'ACTIVE'
                }
              />
            </Box>
            <SettingsReadonlyValue accent>
              Module licences are managed by Arbor (Super Admin). Contact your Arbor account team to
              add or change modules.
            </SettingsReadonlyValue>
            {!enabled && (
              <Typography sx={{ fontSize: '0.6875rem', color: 'warning.dark', fontWeight: 500 }}>
                This module is not licensed for your tenant.
              </Typography>
            )}
          </Stack>
        </SettingsCard>

        <SettingsCard title="Licence details">
          <SettingsField label="Module code" mono>
            <SettingsReadonlyValue>{code}</SettingsReadonlyValue>
          </SettingsField>
          <SettingsField label="Enabled at">
            <SettingsReadonlyValue>
              {enabled ? formatLicenseDateTime(license?.enabledAt) : '-'}
            </SettingsReadonlyValue>
          </SettingsField>
          <SettingsField label="Expiry">
            <SettingsReadonlyValue>
              {enabled
                ? term.detail
                  ? `${term.headline} · ${term.detail}`
                  : term.headline
                : '-'}
            </SettingsReadonlyValue>
          </SettingsField>
        </SettingsCard>

        <Box sx={{ gridColumn: { md: '1 / -1' } }}>
          <SettingsCard title="Included capabilities">
            <Stack spacing={0.75}>
              {caps.map((c) => (
                <Box
                  key={c}
                  sx={{
                    px: 1.5,
                    py: 1,
                    bgcolor: brand.hover,
                    border: `1px solid ${brand.border}`,
                    borderRadius: 0.75,
                  }}
                >
                  <Typography sx={{ fontSize: '0.75rem', fontWeight: 500, color: brand.ink }}>
                    {c}
                  </Typography>
                </Box>
              ))}
            </Stack>
          </SettingsCard>
        </Box>

        {enabled && shortcuts.length > 0 && onOpenNav && (
          <Box sx={{ gridColumn: { md: '1 / -1' } }}>
            <SettingsCard title="Open in workspace" subtitle="Jump to licensed features in this tenant">
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {shortcuts.map((s) => (
                  <Button
                    key={s.navId}
                    size="small"
                    variant="outlined"
                    onClick={() => onOpenNav(s.navId)}
                    sx={{ borderColor: brand.hairlineStrong }}
                  >
                    {s.label}
                  </Button>
                ))}
              </Stack>
            </SettingsCard>
          </Box>
        )}

        {enabled && shortcuts.length === 0 && (
          <Box sx={{ gridColumn: { md: '1 / -1' } }}>
            <SettingsCard title="Open in workspace">
              <Typography sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>
                Workspace navigation for this module will appear here when features are released.
              </Typography>
            </SettingsCard>
          </Box>
        )}
      </SettingsCardGrid>
    </Stack>
  )
}
