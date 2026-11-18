import type { Session } from './api'
import RiskPage from './RiskPage'

/**
 * Privacy Risk uses the same project home, register, and risk acceptance
 * as Enterprise Risk and Unified Risk.
 */
export default function PrivacyRiskPage({
  session,
  onError,
  onOk,
  openFromNotification = null,
  startOnAcceptance = false,
}: {
  session: Session
  onError: (m: string) => void
  onOk: (m: string) => void
  openFromNotification?: { riskId: number; epoch: number } | null
  /** Open the Risk Acceptance project list, same as Enterprise Risk and Unified Risk. */
  startOnAcceptance?: boolean
}) {
  return (
    <RiskPage
      session={session}
      riskType="PRIVACY_RISK"
      onError={onError}
      onOk={onOk}
      openFromNotification={openFromNotification}
      startOnAcceptance={startOnAcceptance}
    />
  )
}
