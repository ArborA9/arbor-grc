import { Box, Tooltip } from '@mui/material'
import { brand } from '../../theme'
import { auditFrameworkNames } from './auditEngagementUtils'

/** One line in a register row. Extra frameworks stay on a +N, with the full list on hover. */
export default function AuditFrameworkLabel({
  audit,
}: {
  audit: {
    auditStandard?: string | null
    frameworkCode?: string | null
    frameworkName?: string | null
  }
}) {
  const names = auditFrameworkNames(audit)
  if (names.length === 0) return <>-</>
  const title = names.join(', ')
  if (names.length === 1) {
    return (
      <Tooltip title={title}>
        <Box
          component="span"
          sx={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
        >
          {names[0]}
        </Box>
      </Tooltip>
    )
  }
  return (
    <Tooltip title={title}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, minWidth: 0, maxWidth: '100%' }}>
        <Box
          component="span"
          sx={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', minWidth: 0 }}
        >
          {names[0]}
        </Box>
        <Box
          component="span"
          sx={{
            flexShrink: 0,
            fontSize: '0.6875rem',
            fontWeight: 650,
            lineHeight: 1,
            color: brand.goldDark,
            bgcolor: brand.goldLight,
            borderRadius: '999px',
            px: 0.75,
            py: '2px',
          }}
        >
          +{names.length - 1}
        </Box>
      </Box>
    </Tooltip>
  )
}
