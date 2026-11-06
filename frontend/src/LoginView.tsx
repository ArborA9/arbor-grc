import { useEffect, useRef, useState, type ComponentProps } from 'react'
import {
  Alert,
  Box,
  Button,
  Checkbox,
  CircularProgress,
  Divider,
  FormControlLabel,
  IconButton,
  InputAdornment,
  Link,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import BusinessRoundedIcon from '@mui/icons-material/BusinessRounded'
import MailOutlineRoundedIcon from '@mui/icons-material/MailOutlineRounded'
import LockOutlinedIcon from '@mui/icons-material/LockOutlined'
import VerifiedUserRoundedIcon from '@mui/icons-material/VerifiedUserRounded'
import HubRoundedIcon from '@mui/icons-material/HubRounded'
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined'
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined'
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded'
import RadioButtonUncheckedRoundedIcon from '@mui/icons-material/RadioButtonUncheckedRounded'
import { api, SSO_REAUTH_KEY, ssoEntryUrl, type PasswordLinkStatus, type Session } from './api'
import { loginButtonSx, loginFieldSx } from './components/login/loginStyles'
import WorkspaceBootScreen from './components/login/WorkspaceBootScreen'
import { LogoMark } from './components/ui'
import { brand } from './theme'
import { emailError, visibleFieldError } from './validation'
import { takeSessionEndMessage } from './uiQuality/sessionReason'
import { setArborTitle } from './uiQuality/pageTitle'
import { rememberNextFromLocation } from './uiQuality/navUrl'

const REMEMBER_KEY = 'arbor_login_remember'
const TENANT_KEY = 'arbor_login_tenant'
const EMAIL_KEY = 'arbor_login_email'

const fieldIcon = { color: brand.muted, fontSize: 17 }

const visuallyHidden = {
  position: 'absolute',
  width: 1,
  height: 1,
  overflow: 'hidden',
  clip: 'rect(0 0 0 0)',
  whiteSpace: 'nowrap',
} as const

function invalidLink(): PasswordLinkStatus {
  return {
    state: 'INVALID',
    purpose: null,
    maskedEmail: null,
    workspaceId: null,
    organisation: null,
    expiresAt: null,
    passwordMinLength: 8,
    passwordMixedCase: false,
    passwordSpecialChar: false,
  }
}

function passwordRules(pw: string, status: PasswordLinkStatus | null) {
  const min = status?.passwordMinLength ?? 8
  const rules = [{ label: `At least ${min} characters`, met: pw.length >= min }]
  if (status?.passwordMixedCase) {
    rules.push(
      { label: 'An upper case letter', met: /[A-Z]/.test(pw) },
      { label: 'A lower case letter', met: /[a-z]/.test(pw) },
      { label: 'A number', met: /\d/.test(pw) },
    )
  }
  if (status?.passwordSpecialChar) {
    rules.push({ label: 'A special character, such as ! @ # $', met: /[^\p{L}\p{N}\s]/u.test(pw) })
  }
  return rules
}

function linkProblem(status: PasswordLinkStatus): { title: string; body: string } {
  const activation = status.purpose === 'ACTIVATION'
  switch (status.state) {
    case 'ACTIVATED':
      return {
        title: 'Account already activated',
        body: 'This account already has a password. Sign in with it, or use Forgot password if you do not remember it.',
      }
    case 'USED':
      return {
        title: 'Link already used',
        body: activation
          ? 'This activation link was already used. Sign in with your password.'
          : 'This reset link was already used. Sign in with your password, or use Forgot password to get a new link.',
      }
    case 'EXPIRED':
      return activation
        ? {
            title: 'Activation link expired',
            body: 'Activation links are valid for 24 hours, and only the newest one works. Ask your Arbor contact to resend the activation email.',
          }
        : {
            title: 'Reset link expired',
            body: 'Reset links are valid for 1 hour, and only the newest one works. Use Forgot password on the sign-in page to get a new link.',
          }
    default:
      return {
        title: 'Link not valid',
        body: 'We could not recognise this link. Open the full link from your newest Arbor GRC email, or use Forgot password on the sign-in page.',
      }
  }
}

function formatLinkExpiry(iso: string) {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString(undefined, { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function readRemembered() {
  return localStorage.getItem(REMEMBER_KEY) === '1'
}

function readStoredTenant() {
  if (readRemembered()) return localStorage.getItem(TENANT_KEY) ?? ''
  return sessionStorage.getItem(TENANT_KEY) ?? ''
}

function persistTenant(id: string, remember: boolean) {
  const trimmed = id.trim()
  if (!trimmed) return
  if (remember) {
    localStorage.setItem(TENANT_KEY, trimmed)
    sessionStorage.removeItem(TENANT_KEY)
  } else {
    sessionStorage.setItem(TENANT_KEY, trimmed)
    localStorage.removeItem(TENANT_KEY)
  }
}

function persistEmail(value: string, remember: boolean) {
  if (remember) localStorage.setItem(EMAIL_KEY, value)
  else localStorage.removeItem(EMAIL_KEY)
}

type LoginFieldProps = ComponentProps<typeof TextField> & {
  showPasswordToggle?: boolean
  passwordVisible?: boolean
  onTogglePassword?: () => void
}

function LoginField({
  showPasswordToggle = false,
  passwordVisible = false,
  onTogglePassword,
  InputProps,
  type,
  sx,
  ...props
}: LoginFieldProps) {
  const resolvedType = showPasswordToggle ? (passwordVisible ? 'text' : 'password') : type

  return (
    <TextField
      fullWidth
      type={resolvedType}
      autoComplete={resolvedType === 'password' ? 'current-password' : props.name}
      {...props}
      InputProps={{
        ...InputProps,
        endAdornment: showPasswordToggle ? (
          <>
            <InputAdornment position="end">
              <IconButton
                aria-label={passwordVisible ? 'Hide password' : 'Show password'}
                onClick={onTogglePassword}
                onMouseDown={(e) => e.preventDefault()}
                edge="end"
                size="small"
                tabIndex={-1}
              >
                {passwordVisible ? (
                  <VisibilityOffOutlinedIcon sx={{ fontSize: 18 }} />
                ) : (
                  <VisibilityOutlinedIcon sx={{ fontSize: 18 }} />
                )}
              </IconButton>
            </InputAdornment>
            {InputProps?.endAdornment}
          </>
        ) : (
          InputProps?.endAdornment
        ),
      }}
      sx={{ ...loginFieldSx, ...sx }}
    />
  )
}

function PasswordOptionsRow({
  rememberMe,
  onRememberChange,
  onForgotPassword,
  disabled,
  sendingReset,
  hideForgot = false,
}: {
  rememberMe: boolean
  onRememberChange: (checked: boolean) => void
  onForgotPassword: () => void
  disabled?: boolean
  sendingReset?: boolean
  hideForgot?: boolean
}) {
  return (
    <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mt: -0.25 }}>
      <FormControlLabel
        control={
          <Checkbox
            size="small"
            checked={rememberMe}
            disabled={disabled || sendingReset}
            onChange={(_, checked) => onRememberChange(checked)}
            inputProps={{ 'aria-label': 'Remember me on this device' }}
            sx={{
              color: brand.muted,
              '&.Mui-checked': { color: brand.gold },
            }}
          />
        }
        label={
          <Typography variant="body2" sx={{ fontSize: '0.8125rem', color: brand.inkSoft }}>
            Remember me
          </Typography>
        }
        sx={{ m: 0 }}
      />
      {!hideForgot && (
        <Link
          component="button"
          type="button"
          variant="body2"
          disabled={disabled || sendingReset}
          onClick={onForgotPassword}
          sx={{
            fontSize: '0.8125rem',
            fontWeight: 600,
            color: brand.goldDark,
            textDecoration: 'none',
            '&:hover': { textDecoration: 'underline' },
            '&:focus-visible': { outline: `2px solid ${brand.gold}`, outlineOffset: 2, borderRadius: 0.5 },
          }}
        >
          {sendingReset ? 'Sending…' : 'Forgot password?'}
        </Link>
      )}
    </Stack>
  )
}

export default function LoginView({
  onLogin,
  initialError = '',
  operator = false,
}: {
  onLogin: (s: Session) => void
  initialError?: string
  /** Super Admin page at `/ops/signin`. Tenant `/signin` never shows this form. */
  operator?: boolean
}) {
  const [error, setError] = useState(initialError || takeSessionEndMessage())
  const [busy, setBusy] = useState(false)
  const [resetBusy, setResetBusy] = useState(false)
  const [signingIn, setSigningIn] = useState(false)
  const [ssoEnabled, setSsoEnabled] = useState(false)

  const [rememberMe, setRememberMe] = useState(readRemembered)
  const [tenantId, setTenantId] = useState(readStoredTenant)
  const [email, setEmail] = useState(() => (readRemembered() ? localStorage.getItem(EMAIL_KEY) ?? '' : ''))
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showLocal, setShowLocal] = useState(false)
  const [ssoChecking, setSsoChecking] = useState(false)
  const [mfaToken, setMfaToken] = useState<string | null>(null)
  const [mfaCode, setMfaCode] = useState('')
  const [resetToken, setResetToken] = useState('')
  const [linkStatus, setLinkStatus] = useState<PasswordLinkStatus | null>(null)
  const [linkChecking, setLinkChecking] = useState(false)
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [previewLink, setPreviewLink] = useState<string | null>(null)
  const [resetNotice, setResetNotice] = useState('')
  const [resetNoticeTone, setResetNoticeTone] = useState<'success' | 'info'>('info')
  const passwordInputRef = useRef<HTMLInputElement | null>(null)
  const [focusPassword, setFocusPassword] = useState(false)

  const [saEmail, setSaEmail] = useState('')
  const [saPassword, setSaPassword] = useState('')
  const [showSaPassword, setShowSaPassword] = useState(false)
  const [emailTouched, setEmailTouched] = useState(false)
  const [saEmailTouched, setSaEmailTouched] = useState(false)

  const loginEmailError = emailError(email, true)
  const saLoginEmailError = emailError(saEmail, true)
  const visibleLoginEmailError = visibleFieldError(loginEmailError, emailTouched, false)
  const visibleSaEmailError = visibleFieldError(saLoginEmailError, saEmailTouched, false)

  const tenantTrimmed = tenantId.trim()
  const canClientSignIn = !!tenantTrimmed && !!email.trim() && !!password && !loginEmailError

  useEffect(() => {
    setArborTitle('Sign In')
    rememberNextFromLocation()
    const path = window.location.pathname.replace(/\/+$/, '') || '/'
    const params = new URLSearchParams(window.location.search)
    const token = params.get('token') ?? ''
    const tenant = params.get('tenant') ?? ''
    if (tenant) setTenantId(tenant)
    if (path.startsWith('/signin/reset')) {
      if (token && tenant) {
        setResetToken(token)
      } else {
        setResetToken('-')
        setLinkStatus(invalidLink())
      }
    }
  }, [])

  const linkTenant = new URLSearchParams(window.location.search).get('tenant') ?? ''

  useEffect(() => {
    if (!resetToken || resetToken === '-' || !linkTenant) return
    let cancelled = false
    setLinkChecking(true)
    api.passwordLinkStatus(linkTenant, resetToken)
      .then((s) => {
        if (cancelled) return
        setLinkStatus(s)
        if (s.state === 'VALID' && s.workspaceId) setTenantId(s.workspaceId)
      })
      .catch(() => {
        if (!cancelled) setLinkStatus(invalidLink())
      })
      .finally(() => {
        if (!cancelled) setLinkChecking(false)
      })
    return () => {
      cancelled = true
    }
  }, [resetToken, linkTenant])

  useEffect(() => {
    if (!resetToken) return
    if (linkChecking || !linkStatus) setArborTitle('Checking your link')
    else if (linkStatus.state === 'VALID')
      setArborTitle(linkStatus.purpose === 'ACTIVATION' ? 'Activate your account' : 'Reset your password')
    else setArborTitle('Sign In')
  }, [resetToken, linkChecking, linkStatus])

  useEffect(() => {
    if (focusPassword && !resetToken) {
      passwordInputRef.current?.focus()
      setFocusPassword(false)
    }
  }, [focusPassword, resetToken])

  useEffect(() => {
    if (initialError) setError(initialError)
  }, [initialError])

  useEffect(() => {
    if (tenantTrimmed) persistTenant(tenantTrimmed, rememberMe)
  }, [tenantTrimmed, rememberMe])

  useEffect(() => {
    const id = tenantTrimmed
    if (!id) {
      setSsoEnabled(false)
      setSsoChecking(false)
      return
    }
    setSsoChecking(true)
    const handle = window.setTimeout(() => {
      api.tenantSsoStatus(id)
        .then((s) => {
          setSsoEnabled(s.ssoEnabled)
          if (s.ssoEnabled) setShowLocal(false)
        })
        .catch(() => setSsoEnabled(false))
        .finally(() => setSsoChecking(false))
    }, 300)
    return () => window.clearTimeout(handle)
  }, [tenantTrimmed])

  const handleRememberChange = (checked: boolean) => {
    setRememberMe(checked)
    if (checked) {
      localStorage.setItem(REMEMBER_KEY, '1')
      persistTenant(tenantId, true)
      persistEmail(email, true)
    } else {
      localStorage.removeItem(REMEMBER_KEY)
      localStorage.removeItem(EMAIL_KEY)
      localStorage.removeItem(TENANT_KEY)
      if (tenantTrimmed) sessionStorage.setItem(TENANT_KEY, tenantTrimmed)
    }
  }

  const rememberSession = () => {
    if (rememberMe) {
      persistTenant(tenantId, true)
      persistEmail(email, true)
    }
  }

  const run = async (fn: () => Promise<Session>) => {
    setBusy(true)
    setSigningIn(true)
    setError('')
    try {
      onLogin(await fn())
    } catch (e) {
      setSigningIn(false)
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const doTenantLogin = async () => {
    if (!canClientSignIn) return
    setBusy(true)
    setSigningIn(true)
    setError('')
    try {
      const result = await api.login(tenantTrimmed, email.trim(), password, rememberMe)
      if ('mfaRequired' in result && result.mfaRequired) {
        setMfaToken(result.mfaToken)
        setMfaCode('')
        rememberSession()
        setSigningIn(false)
        return
      }
      rememberSession()
      onLogin(result as Session)
    } catch (e) {
      setSigningIn(false)
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const doMfaVerify = () =>
    run(async () => {
      if (!mfaToken) throw new Error('MFA challenge missing: sign in again')
      const session = await api.verifyMfaLogin(tenantTrimmed, mfaToken, mfaCode.trim(), rememberMe)
      setMfaToken(null)
      setMfaCode('')
      rememberSession()
      return session
    })

  const doSuperAdminLogin = () => run(() => api.superAdminLogin(saEmail.trim(), saPassword))
  const ssoReady = !!tenantTrimmed && ssoEnabled && !ssoChecking

  const rules = passwordRules(newPassword, linkStatus)
  const resetWhy = rules.some((r) => !r.met)
    ? 'Password must meet every rule below'
    : !confirmPassword
      ? 'Confirm your password'
      : newPassword !== confirmPassword
        ? 'Password and confirmation must match'
        : ''

  const leaveLink = () => {
    setResetToken('')
    setLinkStatus(null)
    setNewPassword('')
    setConfirmPassword('')
    setError('')
    if (window.location.pathname.includes('/signin/reset')) {
      window.history.replaceState(null, '', '/signin')
    }
    setArborTitle('Sign In')
  }

  const sendResetLink = async () => {
    if (!tenantTrimmed) return
    if (!email.trim() || loginEmailError) {
      setEmailTouched(true)
      setError('Enter your email to reset your password')
      return
    }
    setResetBusy(true)
    setError('')
    setResetNotice('')
    try {
      const result = await api.forgotPassword(tenantTrimmed, email.trim())
      const kind = result?.kind
      if (kind === 'SSO') {
        setPreviewLink(null)
        setResetNoticeTone('info')
        setResetNotice(
          'This account signs in with company SSO. Arbor does not store a password for it. Use Continue with company SSO, or ask your IT team to reset the Microsoft password.',
        )
        return
      }
      if (kind !== 'LOCAL') {
        setPreviewLink(null)
        setResetNoticeTone('info')
        setResetNotice(
          'This email does not have a local Arbor password on this workspace. Sign in with company SSO if that is how you access Arbor, or ask your administrator.',
        )
        return
      }
      setPreviewLink(result?.previewLink ?? null)
      setResetNoticeTone('info')
      setResetNotice(
        `We sent a password reset link to ${email.trim()}. It expires in 1 hour. Only the newest link works.`,
      )
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setResetBusy(false)
    }
  }

  const completeReset = async () => {
    if (resetWhy || !linkStatus || linkStatus.state !== 'VALID') return
    setResetBusy(true)
    setError('')
    try {
      const done = await api.resetPassword({
        tenantId: linkTenant,
        token: resetToken,
        newPassword,
      })
      leaveLink()
      setTenantId(done.workspaceId)
      setEmail(done.email)
      setPassword('')
      setPreviewLink(null)
      setShowLocal(true)
      setResetNoticeTone('success')
      setResetNotice(
        done.purpose === 'ACTIVATION'
          ? 'Account activated. Sign in with your new password.'
          : 'Password reset. Sign in with your new password.',
      )
      setFocusPassword(true)
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
      api.passwordLinkStatus(linkTenant, resetToken)
        .then((s) => {
          if (s.state !== 'VALID') {
            setLinkStatus(s)
            setError('')
          }
        })
        .catch(() => undefined)
    } finally {
      setResetBusy(false)
    }
  }

  const goSso = () => {
    if (!tenantTrimmed || !ssoReady) return
    persistTenant(tenantTrimmed, rememberMe)
    rememberNextFromLocation()
    setSigningIn(true)
    let reauth = false
    try {
      reauth = sessionStorage.getItem(SSO_REAUTH_KEY) === '1'
    } catch {
      reauth = false
    }
    window.location.href = ssoEntryUrl(tenantTrimmed, reauth)
  }

  const workspaceHelper = ssoChecking
    ? 'Checking SSO…'
    : ssoReady
      ? 'Company SSO is available for this workspace'
      : tenantTrimmed
        ? 'Signing in with your local workspace account.'
        : 'Enter your organisation workspace id'

  const renderClientPasswordFields = (opts?: { outlinedButton?: boolean }) => (
    <>
      <LoginField
        label="Email"
        name="email"
        type="email"
        autoComplete="username"
        placeholder="name@company.com"
        value={email}
        onChange={(e) => {
          setEmail(e.target.value)
          persistEmail(e.target.value, rememberMe)
        }}
        onBlur={() => setEmailTouched(true)}
        error={!!visibleLoginEmailError}
        helperText={visibleLoginEmailError}
        disabled={!tenantTrimmed}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <MailOutlineRoundedIcon sx={fieldIcon} />
            </InputAdornment>
          ),
        }}
      />
      <LoginField
        label="Password"
        name="password"
        autoComplete="current-password"
        placeholder="Enter password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        disabled={!tenantTrimmed}
        inputRef={passwordInputRef}
        showPasswordToggle
        passwordVisible={showPassword}
        onTogglePassword={() => setShowPassword((v) => !v)}
        onKeyDown={(e) => e.key === 'Enter' && !busy && !resetBusy && canClientSignIn && void doTenantLogin()}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <LockOutlinedIcon sx={fieldIcon} />
            </InputAdornment>
          ),
        }}
      />
      <PasswordOptionsRow
        rememberMe={rememberMe}
        onRememberChange={handleRememberChange}
        onForgotPassword={() => void sendResetLink()}
        disabled={!tenantTrimmed || busy}
        sendingReset={resetBusy}
        hideForgot={!!resetNotice && resetNoticeTone === 'success'}
      />
      {resetNotice && (
        <Alert
          severity={resetNoticeTone}
          variant="outlined"
          onClose={() => {
            setResetNotice('')
            setPreviewLink(null)
          }}
          role="status"
        >
          {resetNotice}
          {previewLink && (
            <Box component="span" sx={{ display: 'block', mt: 0.75 }}>
              Email is not sending on this host.{' '}
              <Link href={previewLink} sx={{ fontWeight: 600, color: brand.goldDark }}>
                Open the reset link
              </Link>
            </Box>
          )}
        </Alert>
      )}
      <Button
        size="large"
        variant={opts?.outlinedButton ? 'outlined' : 'contained'}
        fullWidth
        onClick={() => void doTenantLogin()}
        disabled={busy || resetBusy || !canClientSignIn}
        startIcon={busy ? <CircularProgress size={15} color="inherit" /> : undefined}
        sx={loginButtonSx}
      >
        {busy
          ? 'Signing in…'
          : opts?.outlinedButton
            ? 'Sign In with Password'
            : 'Sign In'}
      </Button>
    </>
  )

  const activation = linkStatus?.purpose === 'ACTIVATION'
  const linkValid = !linkChecking && linkStatus?.state === 'VALID'
  const linkHeading = !resetToken
    ? 'Sign In'
    : linkChecking || !linkStatus
      ? 'Checking your link'
      : linkValid
        ? activation
          ? 'Activate your account'
          : 'Reset your password'
        : linkProblem(linkStatus).title

  const linkSubtext = (() => {
    if (!linkStatus || linkChecking) return 'One moment while we confirm your link.'
    if (!linkValid) return linkProblem(linkStatus).body
    const who = linkStatus.maskedEmail ?? 'your account'
    const org = linkStatus.organisation ? `${linkStatus.organisation} (workspace ${linkStatus.workspaceId})` : `workspace ${linkStatus.workspaceId}`
    return activation
      ? `Create a password for ${who} to activate your Owner account on ${org}.`
      : `Choose a new password for ${who} on ${org}.`
  })()

  const renderLinkPanel = () => {
    if (linkChecking || !linkStatus) {
      return (
        <Stack alignItems="center" sx={{ py: 3 }} role="status" aria-live="polite">
          <CircularProgress size={26} />
        </Stack>
      )
    }
    if (!linkValid) {
      return (
        <Stack spacing={1.75}>
          <Button size="large" variant="contained" fullWidth onClick={leaveLink} sx={loginButtonSx}>
            Go to Sign In
          </Button>
        </Stack>
      )
    }
    return (
      <Stack spacing={1.75} id="login-panel-password-link">
      <LoginField
        label={activation ? 'Create password' : 'New password'}
        name="reset-password"
        autoComplete="new-password"
        placeholder={activation ? 'Create a password' : 'Enter a new password'}
        required
        value={newPassword}
        onChange={(e) => setNewPassword(e.target.value)}
        showPasswordToggle
        passwordVisible={showPassword}
        onTogglePassword={() => setShowPassword((v) => !v)}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <LockOutlinedIcon sx={fieldIcon} />
            </InputAdornment>
          ),
        }}
      />
      <Box component="ul" aria-label="Password rules" sx={{ m: 0, mt: -0.75, pl: 0, listStyle: 'none' }}>
        {rules.map((r) => (
          <Typography
            key={r.label}
            component="li"
            variant="caption"
            sx={{ display: 'flex', alignItems: 'center', gap: 0.75, color: r.met ? 'success.main' : 'text.secondary' }}
          >
            {r.met ? (
              <CheckCircleRoundedIcon sx={{ fontSize: 14 }} aria-hidden />
            ) : (
              <RadioButtonUncheckedRoundedIcon sx={{ fontSize: 14 }} aria-hidden />
            )}
            {r.label}
            <Box component="span" sx={visuallyHidden}>
              {r.met ? ' (met)' : ' (not met)'}
            </Box>
          </Typography>
        ))}
      </Box>
      <LoginField
        label={activation ? 'Confirm password' : 'Confirm new password'}
        name="reset-password-confirm"
        autoComplete="new-password"
        placeholder="Re-enter the password"
        required
        error={!!confirmPassword && confirmPassword !== newPassword}
        helperText={confirmPassword && confirmPassword !== newPassword ? 'Passwords do not match' : undefined}
        value={confirmPassword}
        onChange={(e) => setConfirmPassword(e.target.value)}
        showPasswordToggle
        passwordVisible={showSaPassword}
        onTogglePassword={() => setShowSaPassword((v) => !v)}
        onKeyDown={(e) => e.key === 'Enter' && !resetBusy && !resetWhy && void completeReset()}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <LockOutlinedIcon sx={fieldIcon} />
            </InputAdornment>
          ),
        }}
      />
      <Button
        size="large"
        variant="contained"
        fullWidth
        onClick={() => void completeReset()}
        disabled={resetBusy || !!resetWhy}
        startIcon={resetBusy ? <CircularProgress size={15} color="inherit" /> : undefined}
        sx={loginButtonSx}
      >
        {resetBusy
          ? activation
            ? 'Activating…'
            : 'Saving…'
          : activation
            ? 'Activate Account'
            : 'Reset Password'}
      </Button>
      {resetWhy && (
        <Typography variant="caption" color="text.secondary" textAlign="center">
          {resetWhy}
        </Typography>
      )}
      {linkStatus?.expiresAt && (
        <Typography variant="caption" color="text.secondary" textAlign="center">
          This link is valid until {formatLinkExpiry(linkStatus.expiresAt)}.
        </Typography>
      )}
      <Button size="small" disabled={resetBusy} onClick={leaveLink}>
        Back to Sign In
      </Button>
      </Stack>
    )
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#f9f9f9', display: 'flex', flexDirection: 'column' }}>
      {signingIn && (
        <WorkspaceBootScreen
          title="Signing you in"
          message="Verifying your credentials and opening a secure session. Stay on this page."
        />
      )}
      <Box
        component="header"
        sx={{
          height: 64,
          px: 2,
          display: 'flex',
          alignItems: 'center',
          bgcolor: '#fff',
          borderBottom: '1px solid #e0e0e0',
          boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
        }}
      >
        <Stack component="a" href="/" direction="row" spacing={1} alignItems="center" sx={{ textDecoration: 'none', color: brand.ink }}>
          <LogoMark size={32} />
          <Typography sx={{ fontWeight: 700, fontSize: '0.9375rem', letterSpacing: '-0.02em' }}>Arbor GRC</Typography>
        </Stack>
      </Box>

      <Box
        component="main"
        sx={{
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          px: { xs: 2.5, sm: 3 },
          py: { xs: 4, md: 6 },
        }}
      >
        <Box
          component="section"
          aria-label="Sign In"
          sx={{
            width: '100%',
            maxWidth: 420,
            p: { xs: 3, sm: 4 },
            borderRadius: '6px',
            bgcolor: '#fff',
            border: `1px solid ${brand.border}`,
          }}
        >
              <Typography component="h1" sx={{ fontWeight: 700, fontSize: '1.5rem', letterSpacing: '-0.03em', mb: 0.35 }}>
                {operator ? 'Sign In' : linkHeading}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2.5 }}>
                {operator
                  ? 'Platform operators: provision and administer client tenants.'
                  : resetToken
                    ? linkSubtext
                    : 'Enter your workspace ID, then sign in with company SSO or your local account.'}
              </Typography>

              {!operator && resetToken && renderLinkPanel()}

              {!operator && mfaToken && (
                <Stack spacing={1.75} id="login-panel-mfa" role="tabpanel" aria-labelledby="login-tab-client">
                  <Alert severity="info" variant="outlined">
                    Enter the 6-digit code from your authenticator app to finish signing in.
                  </Alert>
                  <LoginField
                    label="Authenticator code"
                    name="mfa-code"
                    value={mfaCode}
                    onChange={(e) => setMfaCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                    onKeyDown={(e) => e.key === 'Enter' && !busy && mfaCode.length === 6 && void doMfaVerify()}
                    inputProps={{ inputMode: 'numeric', 'aria-label': 'Authenticator code' }}
                    InputProps={{
                      startAdornment: (
                        <InputAdornment position="start">
                          <VerifiedUserRoundedIcon sx={fieldIcon} />
                        </InputAdornment>
                      ),
                    }}
                  />
                  <Button
                    size="large"
                    variant="contained"
                    fullWidth
                    onClick={() => void doMfaVerify()}
                    disabled={busy || mfaCode.length !== 6}
                    startIcon={busy ? <CircularProgress size={15} color="inherit" /> : undefined}
                    sx={loginButtonSx}
                  >
                    {busy ? 'Verifying…' : 'Verify and Sign In'}
                  </Button>
                  <Button
                    size="small"
                    onClick={() => {
                      setMfaToken(null)
                      setMfaCode('')
                      setError('')
                    }}
                  >
                    Back
                  </Button>
                </Stack>
              )}

              {!operator && !mfaToken && !resetToken && (
                <Stack id="login-panel-client" role="tabpanel" aria-labelledby="login-tab-client" spacing={1.75}>
                  <LoginField
                    label="Workspace ID"
                    name="tenant"
                    placeholder="Enter your workspace ID"
                    value={tenantId}
                    onChange={(e) => setTenantId(e.target.value)}
                    helperText={workspaceHelper}
                    onKeyDown={(e) => {
                      if (e.key !== 'Enter' || busy) return
                      if (ssoReady) goSso()
                    }}
                    inputProps={{ 'aria-describedby': 'workspace-helper' }}
                    FormHelperTextProps={{ id: 'workspace-helper' }}
                    InputProps={{
                      startAdornment: (
                        <InputAdornment position="start">
                          <BusinessRoundedIcon sx={fieldIcon} />
                        </InputAdornment>
                      ),
                    }}
                  />

                  {ssoReady ? (
                    <>
                      <Button
                        size="large"
                        variant="contained"
                        fullWidth
                        startIcon={signingIn ? <CircularProgress size={15} color="inherit" /> : <HubRoundedIcon />}
                        disabled={busy || signingIn || resetBusy}
                        onClick={goSso}
                        sx={loginButtonSx}
                      >
                        {signingIn ? 'Continuing…' : 'Continue with Company SSO'}
                      </Button>
                      <Typography variant="caption" color="text.secondary" textAlign="center">
                        You’ll sign in with your organisation’s Microsoft Entra account. No Arbor password required.
                      </Typography>

                      {!showLocal && (
                        <Button
                          size="small"
                          onClick={() => setShowLocal(true)}
                          sx={{
                            alignSelf: 'center',
                            color: brand.goldDark,
                            textTransform: 'none',
                            fontSize: '0.8125rem',
                            fontWeight: 500,
                            '&:hover': {
                              bgcolor: brand.sidebarHover,
                              color: brand.goldDark,
                            },
                            '&:focus-visible': {
                              outline: `2px solid ${brand.gold}`,
                              outlineOffset: 2,
                              bgcolor: brand.sidebarHover,
                            },
                          }}
                        >
                          Use Local Account Instead
                        </Button>
                      )}

                      {showLocal && (
                        <Stack spacing={1.75} sx={{ pt: 0.5 }}>
                          <Divider>
                            <Typography variant="caption" color="text.secondary">
                              Break-glass / local
                            </Typography>
                          </Divider>
                          {renderClientPasswordFields({ outlinedButton: true })}
                        </Stack>
                      )}
                    </>
                  ) : (
                    renderClientPasswordFields()
                  )}
                </Stack>
              )}

              {operator && (
                <Stack id="login-panel-arbor" role="tabpanel" aria-labelledby="login-tab-arbor" spacing={1.75}>
                  <Alert severity="info" variant="outlined" sx={{ py: 0.5 }}>
                    Platform operators: provision and administer client tenants.
                  </Alert>
                  <LoginField
                    label="Email"
                    name="sa-email"
                    type="email"
                    autoComplete="username"
                    placeholder="name@company.com"
                    value={saEmail}
                    onChange={(e) => setSaEmail(e.target.value)}
                    onBlur={() => setSaEmailTouched(true)}
                    error={!!visibleSaEmailError}
                    helperText={visibleSaEmailError}
                    InputProps={{
                      startAdornment: (
                        <InputAdornment position="start">
                          <MailOutlineRoundedIcon sx={fieldIcon} />
                        </InputAdornment>
                      ),
                    }}
                  />
                  <LoginField
                    label="Password"
                    name="sa-password"
                    autoComplete="current-password"
                    placeholder="Enter password"
                    value={saPassword}
                    onChange={(e) => setSaPassword(e.target.value)}
                    showPasswordToggle
                    passwordVisible={showSaPassword}
                    onTogglePassword={() => setShowSaPassword((v) => !v)}
                    onKeyDown={(e) =>
                      e.key === 'Enter' && !busy && saEmail && saPassword && !saLoginEmailError && void doSuperAdminLogin()
                    }
                    InputProps={{
                      startAdornment: (
                        <InputAdornment position="start">
                          <LockOutlinedIcon sx={fieldIcon} />
                        </InputAdornment>
                      ),
                    }}
                  />
                  <Button
                    size="large"
                    variant="contained"
                    fullWidth
                    onClick={() => void doSuperAdminLogin()}
                    disabled={busy || !saEmail || !saPassword || !!saLoginEmailError}
                    startIcon={busy ? <CircularProgress size={15} color="inherit" /> : undefined}
                    sx={loginButtonSx}
                  >
                    {busy ? 'Signing in…' : 'Sign In as Super Admin'}
                  </Button>
                </Stack>
              )}

              {error && (
                <Alert severity="error" sx={{ mt: 2 }} onClose={() => setError('')} role="alert">
                  {error}
                </Alert>
              )}
              <Typography sx={{ mt: 2.5, textAlign: 'center' }}>
                <Link
                  component="button"
                  type="button"
                  underline="hover"
                  onClick={() => window.location.assign('/')}
                  sx={{ fontSize: '0.8125rem', color: brand.muted, fontWeight: 600, border: 0, bgcolor: 'transparent', cursor: 'pointer' }}
                >
                  Back to home
                </Link>
              </Typography>
        </Box>
      </Box>
    </Box>
  )
}
