import { Alert, Box } from '@mui/material'
import type { WorkTaskSummary } from '../../api'
import { ContributorRail } from './ContributorShell'
import { isReturned } from './contribUtils'

export default function ContributorReturnedBanner({
  tasks,
  onOpen,
}: {
  tasks: WorkTaskSummary[]
  onOpen: (task: WorkTaskSummary) => void
}) {
  const returned = tasks.filter(isReturned)
  if (returned.length === 0) return null

  return (
    <Box sx={{ flexShrink: 0, pb: 0 }}>
      <ContributorRail sx={{ pt: 1.5, pb: 0 }}>
        <Alert severity="warning" sx={{ py: 0.75 }}>
          {returned.length === 1 ? (
            <>
              Returned for review -{' '}
              <Box
                component="button"
                type="button"
                onClick={() => onOpen(returned[0]!)}
                sx={{
                  border: 0,
                  p: 0,
                  m: 0,
                  font: 'inherit',
                  cursor: 'pointer',
                  bgcolor: 'transparent',
                  fontWeight: 700,
                  textDecoration: 'underline',
                }}
              >
                {returned[0]!.title}
              </Box>
              {returned[0]!.returnReason ? `: ${returned[0]!.returnReason}` : ''}
            </>
          ) : (
            <>
              {returned.length} items returned for review.{' '}
              {returned.map((task, i) => (
                <span key={task.id}>
                  {i > 0 ? ' · ' : ''}
                  <Box
                    component="button"
                    type="button"
                    onClick={() => onOpen(task)}
                    sx={{
                      border: 0,
                      p: 0,
                      m: 0,
                      font: 'inherit',
                      cursor: 'pointer',
                      bgcolor: 'transparent',
                      fontWeight: 700,
                      textDecoration: 'underline',
                    }}
                  >
                    {task.title}
                  </Box>
                </span>
              ))}
            </>
          )}
        </Alert>
      </ContributorRail>
    </Box>
  )
}
