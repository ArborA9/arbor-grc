import { useEffect, useState } from 'react'
import { Box, Typography } from '@mui/material'
import { api, type Session } from './api'
import { PageLoading } from './components/ui'
import RiskPage from './RiskPage'

/**
 * Privacy Risk (Privacy module).
 *
 * Same register, create/edit form, detail and workflow as ERM / IS Risk:
 * this renders the shared RiskPage locked to the single backing Privacy
 * Risk project, so Privacy has no separate project home. Risks raised from
 * a DPIA are created against the same backing project and appear here.
 */
export default function PrivacyRiskPage({
  session,
  onError,
  onOk,
  openFromNotification = null,
}: {
  session: Session
  onError: (m: string) => void
  onOk: (m: string) => void
  openFromNotification?: { riskId: number; epoch: number } | null
}) {
  const canAdmin = session.roles.some((r) => r === 'OWNER' || r === 'ADMIN')
  const [projectId, setProjectId] = useState<number | null>(null)
  const [resolveState, setResolveState] = useState<'loading' | 'ready' | 'empty' | 'error'>('loading')

  useEffect(() => {
    let cancelled = false
    void (async () => {
      try {
        const projects = await api.listRiskProjects('PRIVACY_RISK')
        if (cancelled) return
        if (projects.length > 0) {
          setProjectId(projects[0].id)
          setResolveState('ready')
          return
        }
        if (!canAdmin) {
          setResolveState('empty')
          return
        }
        const year = new Date().getFullYear()
        try {
          const created = await api.createRiskProject({
            projectType: 'PRIVACY_RISK',
            name: `Privacy Risk Register ${year}`,
            year,
          })
          if (cancelled) return
          setProjectId(created.id)
          setResolveState('ready')
        } catch {
          // Concurrent first visit may have created it already; re-read.
          const again = await api.listRiskProjects('PRIVACY_RISK')
          if (cancelled) return
          if (again.length > 0) {
            setProjectId(again[0].id)
            setResolveState('ready')
          } else {
            setResolveState('error')
            onError('Failed to open the Privacy Risk register')
          }
        }
      } catch (e) {
        if (cancelled) return
        setResolveState('error')
        onError(e instanceof Error ? e.message : 'Failed to open the Privacy Risk register')
      }
    })()
    return () => {
      cancelled = true
    }
    // Resolve the backing project once on mount; session roles do not change in place.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (resolveState === 'loading') return <PageLoading label="Loading Privacy Risk register..." />

  if (resolveState === 'empty') {
    return (
      <Box sx={{ p: 3 }}>
        <Typography color="text.secondary">
          No Privacy Risks yet. Raise one from a DPIA, or ask an Admin to add the first Privacy Risk.
        </Typography>
      </Box>
    )
  }

  if (resolveState === 'error' || projectId == null) {
    return (
      <Box sx={{ p: 3 }}>
        <Typography color="text.secondary">
          The Privacy Risk register could not be opened. Use the left menu to try again.
        </Typography>
      </Box>
    )
  }

  return (
    <RiskPage
      key={projectId}
      session={session}
      riskType="PRIVACY_RISK"
      onError={onError}
      onOk={onOk}
      initialProjectId={projectId}
      lockToProject
      openFromNotification={openFromNotification}
    />
  )
}
