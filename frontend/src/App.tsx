import { useEffect, useState } from 'react'
import Dashboard from './Dashboard'
import ContributorHome, { isContributorOnlySession } from './ContributorHome'
import LoginView from './LoginView'
import WorkspaceBootScreen from './components/login/WorkspaceBootScreen'
import HomeLanding from './HomeLanding'
import SuperAdminConsole from './SuperAdminConsole'
import SessionIdleGuard from './components/SessionIdleGuard'
import ChangePasswordDialog from './components/ChangePasswordDialog'
import VendorPortal from './VendorPortal'
import ExternalAuditPortal from './ExternalAuditPortal'
import { api, SSO_REAUTH_KEY, type Session } from './api'
import { markSessionEnded } from './uiQuality/sessionReason'
import { setArborTitle } from './uiQuality/pageTitle'
import { TenantLocaleProvider } from './uiQuality/TenantLocaleContext'
import {
  enterAuthenticatedLocation,
  isAppShellPath,
  rememberNextFromLocation,
} from './uiQuality/navUrl'

async function consumeSsoHash(): Promise<{ session?: Session; error?: string }> {
  const hash = window.location.hash.startsWith('#')
    ? window.location.hash.slice(1)
    : window.location.hash
  if (!hash) return {}
  const params = new URLSearchParams(hash)
  const code = params.get('sso_code')
  const error = params.get('sso_error')
  // Clear the fragment so a refresh doesn't re-apply a one-shot code.
  window.history.replaceState(null, '', window.location.pathname + window.location.search)
  if (code) {
    try {
      const session = await api.exchangeSsoCode(code)
      return { session }
    } catch (e) {
      return {
        error: e instanceof Error ? e.message : 'SSO sign-in failed. Please try again.',
      }
    }
  }
  if (error) {
    const email = params.get('email')
    const messages: Record<string, string> = {
      no_account: email
        ? `No Arbor account for ${email}. Ask your administrator to invite you first.`
        : 'No Arbor account for this user. Ask your administrator to invite you first.',
      no_email: 'Your identity provider did not return an email address.',
      sso_not_enabled: 'SSO is not enabled for this workspace.',
      idp_misconfigured: 'SSO is misconfigured (bad Entra issuer / client). Ask Arbor to check the IdP settings.',
      sso_failed: 'SSO sign-in failed. Please try again or use local sign-in.',
    }
    return { error: messages[error] ?? `SSO error: ${error}` }
  }
  return {}
}

export default function App() {
  const [session, setSession] = useState<Session | null>(null)
  const [ssoError, setSsoError] = useState('')
  const [booting, setBooting] = useState(true)
  const [ssoBooting, setSsoBooting] = useState(() => window.location.hash.includes('sso_code='))

  useEffect(() => {
    let cancelled = false
    void (async () => {
      const result = await consumeSsoHash()
      if (cancelled) return
      if (result.error) setSsoError(result.error)
      if (result.session) {
        setSession(result.session)
        setSsoBooting(false)
        setBooting(false)
        return
      }
      try {
        const existing = await api.currentSession()
        if (!cancelled && existing) setSession(existing)
      } finally {
        if (!cancelled) {
          setSsoBooting(false)
          setBooting(false)
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (ssoBooting || booting) return
    if (session) {
      try {
        sessionStorage.removeItem(SSO_REAUTH_KEY)
      } catch {
        /* private mode */
      }
      enterAuthenticatedLocation()
      return
    }
    if (isAppShellPath(window.location.pathname)) {
      rememberNextFromLocation()
      const next = `${window.location.pathname}${window.location.search}`
      window.history.replaceState(null, '', `/signin?next=${encodeURIComponent(next)}`)
    }
  }, [session, ssoBooting, booting])

  // Public vendor questionnaire portal: no session, no main nav (req §14.4, Journey 14).
  if (window.location.pathname === '/vendor-portal') {
    setArborTitle('Vendor portal')
    return <VendorPortal />
  }

  if (window.location.pathname === '/external-audit') {
    setArborTitle('External audit')
    return <ExternalAuditPortal />
  }

  const finishLogout = (endEntra: boolean) => {
    const operator = !!session?.superAdmin
    void api.logoutSession(endEntra).then((endSessionUrl) => {
      if (endSessionUrl) {
        window.location.assign(endSessionUrl)
        return
      }
      if (operator) {
        window.location.assign('/ops/signin')
        return
      }
      setSession(null)
    })
  }

  const logout = () => finishLogout(true)

  const idleLogout = () => {
    markSessionEnded('idle')
    finishLogout(false)
  }

  if (ssoBooting || booting) {
    return (
      <WorkspaceBootScreen
        title={ssoBooting ? 'Completing sign-in' : 'Arbor'}
        message={
          ssoBooting
            ? 'Confirming your company account and opening your workspace. Stay on this page.'
            : 'Checking your session.'
        }
      />
    )
  }

  if (!session) {
    const path = window.location.pathname.replace(/\/+$/, '') || '/'
    const operatorSignIn = path === '/ops/signin'
    const showSignIn =
      operatorSignIn ||
      path === '/signin' ||
      path.startsWith('/signin/') ||
      path === '/login' ||
      !!ssoError ||
      isAppShellPath(path)
    if (showSignIn) {
      return <LoginView onLogin={setSession} initialError={ssoError} operator={operatorSignIn} />
    }
    return <HomeLanding />
  }

  if (session.superAdmin && session.roles.includes('SUPER_ADMIN_PASSWORD_CHANGE')) {
    return (
      <ChangePasswordDialog
        open
        superAdmin
        required
        onClose={() => undefined}
        onChanged={setSession}
        onSignOut={logout}
      />
    )
  }

  if (session.superAdmin) {
    return <SuperAdminConsole session={session} onLogout={logout} />
  }

  const tenantApp = isContributorOnlySession(session) ? (
    <ContributorHome session={session} onLogout={logout} />
  ) : (
    <Dashboard session={session} onLogout={logout} />
  )

  return (
    <SessionIdleGuard onLogout={idleLogout}>
      <TenantLocaleProvider>{tenantApp}</TenantLocaleProvider>
    </SessionIdleGuard>
  )
}
