import { parseFieldErrorsFromProblem } from './validation/apiErrors'
import { markSessionEnded } from './uiQuality/sessionReason'

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(message: string, status: number, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export interface TenantLicenseSummary {
  licensedCount: number
  moduleCount: number
  writeLocked: boolean
  /** NONE | ACTIVE | EXPIRING | EXPIRED */
  status: string
  earliestExpiresOn: string | null
  daysRemaining: number | null
  mixedExpiry: boolean
  licensedCodes: string[]
}

export interface Tenant {
  id: number
  tenantId: string
  name: string
  status: string
  jdbcUrl: string
  ssoEnabled: boolean
  oidcIssuer: string | null
  oidcClientId: string | null
  jitEnabled: boolean
  jitDefaultRole: 'BLOCK' | 'CONTRIBUTOR'
  createdAt: string
  license?: TenantLicenseSummary | null
}

export interface SsoGroupRoleMapping {
  id: number
  idpGroupName: string
  role: string
  createdAt: string
}

export interface ProvisionResponse {
  tenant: Tenant
  initialAdminEmail: string
  initialAdminPassword: string | null
  passwordEmailed?: boolean
  ssoConfigured?: boolean
}

export interface PasswordLinkStatus {
  state: 'VALID' | 'EXPIRED' | 'USED' | 'ACTIVATED' | 'INVALID'
  purpose: 'ACTIVATION' | 'RESET' | null
  maskedEmail: string | null
  workspaceId: string | null
  organisation: string | null
  expiresAt: string | null
  passwordMinLength: number
  passwordMixedCase: boolean
  passwordSpecialChar: boolean
}

export interface Session {
  token: string
  email: string
  tenantId: string | null
  roles: string[]
  superAdmin: boolean
  refreshToken?: string | null
  authType?: 'LOCAL' | 'SSO'
}

export interface UserSummary {
  id: number
  email: string
  fullName: string
  authType: string
  localLoginEnabled?: boolean
  status: string
  roles: string[]
  orgUnitIds?: number[]
  /** Modules this Admin can open. Empty unless the person is an Admin and not an Owner. */
  adminModules?: string[]
  createdAt: string
}

export interface EmployeeDirectoryEntry {
  directoryId: string
  email: string | null
  displayName: string
  jobTitle: string | null
  department: string | null
  managerName: string | null
  managerEmail: string | null
  managerJobTitle: string | null
  managerDepartment: string | null
}

export interface EmployeeDirectorySearch {
  available: boolean
  reason: string | null
  results: EmployeeDirectoryEntry[]
}

export interface PingRecord {
  id: number
  note: string
  createdAt: string
}

export interface ModuleLicense {
  code: string
  enabled: boolean
  enabledAt: string | null
  expiresAt: string | null
  expiresOn?: string | null
  expired?: boolean
  daysRemaining?: number | null
  /** UNLICENSED | ACTIVE | EXPIRING | EXPIRED */
  status?: string
  entitledFrameworkIds?: number[]
}

export interface LicenseBanner {
  key: string
  tone: 'error' | 'warning' | 'info'
  title: string
  message: string
  dismissed: boolean
}

/** Commercial module codes (Platform always-on is not listed). */
export const MODULE_CODES = [
  'ISMS',
  'ERM',
  'BCMS',
  'PRIVACY',
  'ITSM',
  'AIMS',
  'AUDIT',
  'QMS',
  'OHSMS',
  'SAMA',
] as const

export const MODULE_LABELS: Record<string, string> = {
  ISMS: 'ISMS: Frameworks, SoA, Gap, Information Security Risk',
  ERM: 'ERM: Enterprise & third-party risk',
  BCMS: 'BCMS: Business continuity',
  PRIVACY: 'Privacy',
  ITSM: 'ITSM',
  AIMS: 'AIMS: AI governance',
  AUDIT: 'Audit: Internal/External audits + CAPA',
  QMS: 'QMS: Quality Management (ISO 9001), SoA, Gap',
  OHSMS: 'OHSMS: Occupational Health & Safety (ISO 45001), SoA, Gap',
  SAMA: 'SAMA: Cyber Security Framework, applicability (SoA), Gap',
}

/** Modules that own framework libraries (nav + grouping). */
export const FRAMEWORK_MODULES = [
  'ISMS',
  'ERM',
  'BCMS',
  'PRIVACY',
  'ITSM',
  'AIMS',
  'QMS',
  'OHSMS',
  'SAMA',
] as const

/** Modules that can create compliance projects. */
export const ASSESSMENT_MODULES = [
  'ISMS',
  'BCMS',
  'QMS',
  'OHSMS',
  'SAMA',
  'ITSM',
  'AIMS',
  'PRIVACY',
] as const

/** Modules with Statement of Applicability. BCMS stays Gap-only. */
export const SOA_MODULES = ['ISMS', 'QMS', 'OHSMS', 'SAMA', 'ITSM', 'AIMS', 'PRIVACY'] as const

/** Gap without SoA (BCMS only). */
export const GAP_ONLY_MODULES = ['BCMS'] as const

const TOKEN_KEY = 'arbor_session'
const REMEMBER_FLAG_KEY = 'arbor_login_remember'
const COOKIE_SESSION_HEADER = 'X-Arbor-Session'

export function isRememberedLogin(): boolean {
  try {
    return localStorage.getItem(REMEMBER_FLAG_KEY) === '1'
  } catch {
    return false
  }
}

function sessionFromAuth(data: {
  token: string
  email: string
  tenantId?: string | null
  roles?: string[]
  refreshToken?: string | null
  authType?: string | null
}, superAdmin = false): Session {
  return {
    token: data.token,
    email: data.email,
    tenantId: data.tenantId ?? null,
    roles: data.roles ?? [],
    superAdmin,
    refreshToken: data.refreshToken ?? null,
    authType: data.authType === 'SSO' ? 'SSO' : data.authType === 'LOCAL' ? 'LOCAL' : undefined,
  }
}

function persistSessionToStore(session: Session) {
  const json = JSON.stringify(session)
  const keep =
    session.superAdmin || isRememberedLogin()
  try {
    if (keep) {
      localStorage.setItem(TOKEN_KEY, json)
      sessionStorage.removeItem(TOKEN_KEY)
    } else {
      sessionStorage.setItem(TOKEN_KEY, json)
      localStorage.removeItem(TOKEN_KEY)
    }
  } catch {
    /* private mode */
  }
}

export function loadSession(): Session | null {
  try {
    const raw = localStorage.getItem(TOKEN_KEY) ?? sessionStorage.getItem(TOKEN_KEY)
    if (!raw) return null
    const session = JSON.parse(raw) as Session
    // Tokens used to live here. Drop that copy so a script cannot read them.
    if (session.token || session.refreshToken) {
      clearSession()
      return null
    }
    return session
  } catch {
    return null
  }
}

function saveSession(session: Session) {
  persistSessionToStore(session)
}

export function clearSession() {
  try {
    localStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(TOKEN_KEY)
  } catch {
    /* ignore */
  }
}

/** Persist a session created by any login path (local or SSO). */
export function persistSession(session: Session) {
  saveSession(session)
}

/**
 * Build a Session from a JWT the backend minted after SSO. Payload is decoded client-side
 * (signature already verified by the IdP handshake on the server).
 */
export function sessionFromJwt(token: string): Session {
  const parts = token.split('.')
  if (parts.length < 2) throw new Error('Invalid SSO token')
  const json = atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'))
  const payload = JSON.parse(json) as { sub?: string; tenant_id?: string; roles?: string[] }
  return {
    token,
    email: payload.sub ?? '',
    tenantId: payload.tenant_id ?? null,
    roles: payload.roles ?? [],
    superAdmin: false,
  }
}

export interface TenantSsoStatus {
  tenantId: string
  name: string
  ssoEnabled: boolean
}

/** Set after Sign out of an SSO session. The next Company SSO click sends prompt=login. */
export const SSO_REAUTH_KEY = 'arbor_reauth'

/** Same-origin SSO entry (Vite dev proxies /sso → :8080; bundled JAR serves both on :8080). */
export function ssoEntryUrl(tenantId: string, reauth = false): string {
  const path = `/sso/${encodeURIComponent(normalizeTenantId(tenantId))}`
  return reauth ? `${path}?reauth=1` : path
}

/**
 * Absolute Entra redirect URI for this workspace. Must match Spring OAuth2
 * `{baseUrl}/login/oauth2/code/{registrationId}` (registrationId = tenant id).
 * Share with the tenant's Entra admin when registering the app.
 */
export function ssoRedirectUri(tenantId: string): string {
  const id = encodeURIComponent(normalizeTenantId(tenantId))
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  return `${origin}/login/oauth2/code/${id}`
}

/** Operators may type mixed case; storage and API lookups use lowercase. */
export function normalizeTenantId(tenantId: string): string {
  return tenantId.trim().toLowerCase()
}

async function apiFetch(input: string, init?: RequestInit): Promise<Response> {
  try {
    const res = await fetch(input, { ...init, credentials: 'include' })
    if (res.status !== 401 || skipRefresh(input)) return res
    const refreshed = await refreshOnce()
    if (!refreshed) return res
    return await fetch(input, withRefreshedAuth(init))
  } catch {
    throw new Error('Arbor can’t reach the server. Try again in a moment.')
  }
}

function skipRefresh(input: string): boolean {
  const path = typeof input === 'string' ? input : ''
  return /\/api\/auth\/(login|refresh|logout|forgot-password|reset-password|password-link|mfa\/verify|sso\/exchange|super-admin\/login|tenant\/)/.test(
    path,
  )
}

let refreshInFlight: Promise<boolean> | null = null

async function refreshOnce(): Promise<boolean> {
  const session = loadSession()
  const tenantId = session?.tenantId
  if (!session || !tenantId || session.superAdmin) return false
  if (refreshInFlight) return refreshInFlight
  refreshInFlight = (async () => {
    try {
      const res = await fetch('/api/auth/refresh', {
        method: 'POST',
        credentials: 'include',
        headers: cookieHeaders({ 'Content-Type': 'application/json', 'X-Tenant-ID': tenantId }),
        body: '{}',
      })
      if (!res.ok) {
        clearSession()
        return false
      }
      const data = await res.json()
      adoptBrowserSession({ ...data, tenantId: data.tenantId ?? tenantId })
      return true
    } catch {
      return false
    } finally {
      refreshInFlight = null
    }
  })()
  return refreshInFlight
}

function withRefreshedAuth(init?: RequestInit): RequestInit {
  const headers = new Headers(init?.headers)
  const session = loadSession()
  if (session?.token) headers.set('Authorization', `Bearer ${session.token}`)
  return { ...init, headers, credentials: 'include' }
}

async function handle(res: Response) {
  if (res.status === 401) {
    const isAuthCall = res.url.includes('/api/auth/')
    if (!isAuthCall) {
      markSessionEnded('expired')
      const operator = !!loadSession()?.superAdmin
      clearSession()
      if (operator) {
        window.location.assign('/ops/signin')
        return Promise.reject(new ApiError('Your session expired. Please sign in again.', 401))
      }
      const here = `${window.location.pathname}${window.location.search}`
      const next =
        here.startsWith('/') && !here.startsWith('//') && !here.startsWith('/signin') && !here.startsWith('/login')
          ? `?next=${encodeURIComponent(here)}`
          : ''
      window.location.assign(`/signin${next}`)
      return Promise.reject(new ApiError('Your session expired. Please sign in again.', 401))
    }
    let detail = 'Invalid email or password'
    try {
      const problem = await res.json()
      if (problem?.detail) detail = problem.detail
    } catch {
      /* keep default */
    }
    throw new ApiError(detail, 401)
  }
    if (!res.ok) {
    const text = await res.text()
    try {
      const problem = JSON.parse(text)
      const detail = problem.detail || problem.message || text
      const fieldErrors = parseFieldErrorsFromProblem(problem, detail)
      throw new ApiError(detail, res.status, fieldErrors)
    } catch (e) {
      if (e instanceof ApiError) throw e
      throw new ApiError(text || `${res.status} ${res.statusText}`, res.status)
    }
  }
  if (res.status === 204 || res.status === 205) return null
  const text = await res.text()
  if (!text.trim()) return null
  return JSON.parse(text)
}

function authHeaders(): Record<string, string> {
  const session = loadSession()
  if (!session?.token) return {}
  return { Authorization: `Bearer ${session.token}` }
}

function cookieHeaders(extra?: Record<string, string>): Record<string, string> {
  return { [COOKIE_SESSION_HEADER]: 'cookie', ...extra }
}

function adoptBrowserSession(data: {
  email: string
  tenantId?: string | null
  roles?: string[]
  authType?: string | null
}, superAdmin = false): Session {
  const session = sessionFromAuth({ ...data, token: '', refreshToken: null }, superAdmin)
  saveSession(session)
  return session
}

export const api = {
  // --- auth: tenant users ---
  login: async (
    tenantId: string,
    email: string,
    password: string,
    rememberMe = false,
  ): Promise<Session | { mfaRequired: true; mfaToken: string; email: string; tenantId: string }> => {
    const tid = normalizeTenantId(tenantId)
    const data = await apiFetch('/api/auth/login', {
      method: 'POST',
      headers: cookieHeaders({ 'Content-Type': 'application/json', 'X-Tenant-ID': tid }),
      body: JSON.stringify({ email, password, rememberMe }),
    }).then(handle)
    if (data.mfaRequired && data.mfaToken) {
      return {
        mfaRequired: true,
        mfaToken: data.mfaToken,
        email: data.email,
        tenantId: data.tenantId ?? tid,
      }
    }
    return adoptBrowserSession({ ...data, tenantId: data.tenantId ?? tid })
  },

  verifyMfaLogin: async (
    tenantId: string,
    mfaToken: string,
    code: string,
    rememberMe = false,
  ): Promise<Session> => {
    const data = await apiFetch('/api/auth/mfa/verify', {
      method: 'POST',
      headers: cookieHeaders({ 'Content-Type': 'application/json', 'X-Tenant-ID': tenantId }),
      body: JSON.stringify({ mfaToken, code, rememberMe }),
    }).then(handle)
    return adoptBrowserSession(data)
  },

  /** Restore a cookie session after a full page load. Null when signed out. */
  currentSession: async (): Promise<Session | null> => {
    const res = await fetch('/api/auth/session', { credentials: 'include' })
    if (!res.ok) {
      if (res.status === 401) {
        await fetch('/api/auth/logout', {
          method: 'POST',
          credentials: 'include',
          headers: cookieHeaders({ 'Content-Type': 'application/json' }),
          body: '{}',
        }).catch(() => undefined)
      }
      clearSession()
      return null
    }
    const data = await res.json()
    return adoptBrowserSession({
      email: data.email ?? '',
      tenantId: data.tenantId ?? null,
      roles: data.roles ?? [],
      authType: data.authType,
    }, !!data.superAdmin)
  },

  getMfaStatus: (): Promise<{ enrolled: boolean; enabled: boolean }> =>
    apiFetch('/api/auth/mfa/status', { headers: authHeaders() }).then(handle),

  startMfaEnroll: (): Promise<{ secret: string; otpauthUri: string }> =>
    apiFetch('/api/auth/mfa/enroll/start', {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  confirmMfaEnroll: (code: string): Promise<{ enrolled: boolean; enabled: boolean }> =>
    apiFetch('/api/auth/mfa/enroll/confirm', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ code }),
    }).then(handle),

  disableMfa: (code: string): Promise<{ enrolled: boolean; enabled: boolean }> =>
    apiFetch('/api/auth/mfa', {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ code }),
    }).then(handle),

  tenantSsoStatus: (tenantId: string): Promise<TenantSsoStatus> =>
    apiFetch(`/api/auth/tenant/${encodeURIComponent(tenantId)}/sso`).then(handle),

  /** Redeem one-time SSO code (see backend SsoExchangeService: no JWT in URL fragment). */
  exchangeSsoCode: async (code: string): Promise<Session> => {
    const data = await apiFetch('/api/auth/sso/exchange', {
      method: 'POST',
      headers: cookieHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ code }),
    }).then(handle)
    return adoptBrowserSession(data)
  },

  forgotPassword: (
    tenantId: string,
    email: string,
  ): Promise<{ kind: 'LOCAL' | 'SSO' | 'NOT_LOCAL'; previewLink: string | null }> =>
    apiFetch('/api/auth/forgot-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Tenant-ID': normalizeTenantId(tenantId) },
      body: JSON.stringify({ email }),
    }).then(handle),

  passwordLinkStatus: (tenantId: string, token: string): Promise<PasswordLinkStatus> =>
    apiFetch('/api/auth/password-link', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Tenant-ID': normalizeTenantId(tenantId) },
      body: JSON.stringify({ token }),
    }).then(handle),

  resetPassword: (body: {
    tenantId: string
    token: string
    newPassword: string
  }): Promise<{ purpose: 'ACTIVATION' | 'RESET'; email: string; workspaceId: string }> =>
    apiFetch('/api/auth/reset-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Tenant-ID': normalizeTenantId(body.tenantId) },
      body: JSON.stringify({ token: body.token, newPassword: body.newPassword }),
    }).then(handle),

  changePassword: async (
    currentPassword: string,
    newPassword: string,
    superAdmin = false,
  ): Promise<Session> => {
    const path = superAdmin ? '/api/auth/super-admin/change-password' : '/api/auth/change-password'
    const data = await apiFetch(path, {
      method: 'POST',
      headers: cookieHeaders({ 'Content-Type': 'application/json', ...authHeaders() }),
      body: JSON.stringify({ currentPassword, newPassword }),
    }).then(handle)
    return adoptBrowserSession(data, superAdmin)
  },

  // --- auth: Arbor Super Admin (no tenant) ---
  superAdminLogin: async (email: string, password: string): Promise<Session> => {
    const data = await apiFetch('/api/auth/super-admin/login', {
      method: 'POST',
      headers: cookieHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ email, password }),
    }).then(handle)
    return adoptBrowserSession(data, true)
  },

  // --- catalog / provisioning (Super Admin only) ---
  listTenants: (): Promise<Tenant[]> =>
    apiFetch('/api/admin/tenants', { headers: authHeaders() }).then(handle),

  provisionTenant: (
    tenantId: string,
    name: string,
    owner?: {
      ownerEmail?: string
      ownerFullName?: string
      ssoEnabled?: boolean
      oidcIssuer?: string
      oidcClientId?: string
      oidcClientSecret?: string
    },
  ): Promise<ProvisionResponse> =>
    apiFetch('/api/admin/tenants', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({
        tenantId: normalizeTenantId(tenantId),
        name,
        ownerEmail: owner?.ownerEmail,
        ownerFullName: owner?.ownerFullName,
        ssoEnabled: owner?.ssoEnabled ?? false,
        oidcIssuer: owner?.oidcIssuer,
        oidcClientId: owner?.oidcClientId,
        oidcClientSecret: owner?.oidcClientSecret,
      }),
    }).then(handle),

  resendOwnerActivation: (tenantId: string): Promise<{ tenantId: string; sentTo: string[] }> =>
    apiFetch(`/api/admin/tenants/${encodeURIComponent(tenantId)}/owner-activation`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  configureTenantIdp: (
    tenantId: string,
    config: {
      ssoEnabled: boolean
      oidcIssuer: string
      oidcClientId: string
      oidcClientSecret: string
      jitEnabled?: boolean
      jitDefaultRole?: 'BLOCK' | 'CONTRIBUTOR'
    },
  ): Promise<Tenant> =>
    apiFetch(`/api/admin/tenants/${encodeURIComponent(tenantId)}/idp`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(config),
    }).then(handle),

  listTenantModules: (tenantId: string): Promise<{ tenantId: string; modules: ModuleLicense[] }> =>
    apiFetch(`/api/admin/tenants/${encodeURIComponent(tenantId)}/modules`, {
      headers: authHeaders(),
    }).then(handle),

  updateTenantModules: (
    tenantId: string,
    modules: { code: string; enabled: boolean; frameworkIds?: number[] | null; expiresOn?: string | null }[],
    expiresOn?: string | null,
  ): Promise<{ tenantId: string; modules: ModuleLicense[] }> =>
    apiFetch(`/api/admin/tenants/${encodeURIComponent(tenantId)}/modules`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ modules, ...(expiresOn ? { expiresOn } : {}) }),
    }).then(handle),

  extendTenantModuleExpiry: (
    tenantId: string,
    expiresOn: string,
    codes?: string[],
  ): Promise<{ tenantId: string; modules: ModuleLicense[] }> =>
    apiFetch(`/api/admin/tenants/${encodeURIComponent(tenantId)}/modules/expiry`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ expiresOn, ...(codes && codes.length ? { codes } : {}) }),
    }).then(handle),

  dismissLicenseBanner: (dismissKey: string): Promise<{ status: string }> =>
    apiFetch('/api/license/banner-dismiss', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ dismissKey }),
    }).then(handle),

  listSsoGroupRoleMappings: (): Promise<SsoGroupRoleMapping[]> =>
    apiFetch('/api/sso/group-role-mappings', { headers: authHeaders() }).then(handle),

  createSsoGroupRoleMapping: (body: {
    idpGroupName: string
    role: string
  }): Promise<SsoGroupRoleMapping> =>
    apiFetch('/api/sso/group-role-mappings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  deleteSsoGroupRoleMapping: (id: number): Promise<null> =>
    apiFetch(`/api/sso/group-role-mappings/${id}`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(handle),

  /** Licensed modules for the JWT tenant (nav filtering). */
  listMyModules: (): Promise<{
    tenantId: string
    modules: ModuleLicense[]
    enabled: string[]
    expiredCodes?: string[]
    writeLocked?: boolean
    banner?: LicenseBanner | null
  }> => apiFetch('/api/modules', { headers: authHeaders() }).then(handle),

  // --- users (tenant from token) ---
  authMe: (): Promise<UserSummary> =>
    apiFetch('/api/auth/me', { headers: authHeaders() }).then(handle),

  listUsers: (): Promise<UserSummary[]> =>
    apiFetch('/api/users', { headers: authHeaders() }).then(handle),

  /** Enabled tenant users (id, name, email). Champion may call this. */
  listUserDirectory: (): Promise<{ id: number; email: string; fullName: string }[]> =>
    apiFetch('/api/users/directory', { headers: authHeaders() }).then(handle),

  searchEmployees: (q: string): Promise<EmployeeDirectorySearch> =>
    apiFetch(`/api/directory/employees?q=${encodeURIComponent(q)}`, { headers: authHeaders() }).then(handle),

  listAuditPlanningUsers: (): Promise<UserSummary[]> =>
    apiFetch('/api/users/audit-planning', { headers: authHeaders() }).then(handle),

  listEligibleContributors: (orgUnitId: number): Promise<{ id: number; email: string; fullName: string }[]> =>
    apiFetch(`/api/users/eligible-contributors?orgUnitId=${orgUnitId}`, { headers: authHeaders() }).then(handle),

  // authType: 'SSO' (invite, AD-backed, no password) or 'LOCAL' (break-glass, password required)
  createUser: (
    email: string,
    fullName: string,
    roles: string[],
    authType: 'SSO' | 'LOCAL',
    password?: string,
    orgUnitIds?: number[],
    adminModules?: string[],
  ): Promise<UserSummary> =>
    apiFetch('/api/users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({
        email,
        fullName,
        roles,
        authType,
        password,
        orgUnitIds: orgUnitIds ?? [],
        adminModules: adminModules ?? [],
      }),
    }).then(handle),

  updateUserOrgUnits: (id: number, orgUnitIds: number[]): Promise<UserSummary> =>
    apiFetch(`/api/users/${id}/org-units`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ orgUnitIds }),
    }).then(handle),

  disableUser: (id: number): Promise<UserSummary> =>
    apiFetch(`/api/users/${id}/disable`, { method: 'POST', headers: authHeaders() }).then(handle),

  enableUser: (id: number): Promise<UserSummary> =>
    apiFetch(`/api/users/${id}/enable`, { method: 'POST', headers: authHeaders() }).then(handle),

  localLoginPolicy: (): Promise<{ scope: string; ssoEnabled: boolean; ownerOnly: boolean }> =>
    apiFetch('/api/users/local-login-policy', { headers: authHeaders() }).then(handle),

  disableLocalLogin: (id: number): Promise<UserSummary> =>
    apiFetch(`/api/users/${id}/local-login/disable`, { method: 'POST', headers: authHeaders() }).then(handle),

  enableLocalLogin: (id: number, password?: string): Promise<UserSummary> =>
    apiFetch(`/api/users/${id}/local-login/enable`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(password ? { password } : {}),
    }).then(handle),

  /** orgUnitIds omitted keeps the user's current departments. */
  updateUserRoles: (id: number, roles: string[], adminModules?: string[], orgUnitIds?: number[]): Promise<UserSummary> =>
    apiFetch(`/api/users/${id}/roles`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ roles, adminModules: adminModules ?? [], ...(orgUnitIds ? { orgUnitIds } : {}) }),
    }).then(handle),

  resetUserPassword: (id: number, password: string): Promise<UserSummary> =>
    apiFetch(`/api/users/${id}/reset-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ password }),
    }).then(handle),

  refreshSession: async (tenantId: string, refreshToken: string): Promise<Session> => {
    const data = await apiFetch('/api/auth/refresh', {
      method: 'POST',
      headers: cookieHeaders({ 'Content-Type': 'application/json', 'X-Tenant-ID': tenantId }),
      body: JSON.stringify({ refreshToken }),
    }).then(handle)
    return adoptBrowserSession({ ...data, tenantId: data.tenantId ?? tenantId })
  },

  logoutSession: async (endEntra = false): Promise<string | null> => {
    const session = loadSession()
    const headers = cookieHeaders({ 'Content-Type': 'application/json' })
    if (session?.tenantId) headers['X-Tenant-ID'] = session.tenantId
    let endSessionUrl: string | null = null
    let reauth = false
    try {
      const res = await apiFetch(`/api/auth/logout${endEntra ? '?endEntra=1' : ''}`, {
        method: 'POST',
        headers,
        body: '{}',
      })
      if (res.ok) {
        const data = await res.json().catch(() => null) as { endSessionUrl?: string | null; reauth?: boolean } | null
        endSessionUrl = data?.endSessionUrl || null
        reauth = Boolean(data?.reauth)
      }
    } catch {
      /* Cookie clear is best-effort. Local profile is removed either way. */
    }
    if (reauth) {
      try {
        sessionStorage.setItem(SSO_REAUTH_KEY, '1')
      } catch {
        /* private mode */
      }
    }
    clearSession()
    return endSessionUrl
  },

  listMyTasks: (): Promise<WorkTaskSummary[]> =>
    apiFetch('/api/my-tasks', { headers: authHeaders() }).then(handle),

  getMyTask: (id: number): Promise<WorkTaskDetail> =>
    apiFetch(`/api/my-tasks/${id}`, { headers: authHeaders() }).then(handle),

  submitGapTask: (
    taskId: number,
    body: {
      implementationStatus?: GapImplementationStatus | null
      maturity?: number | null
      maturityGates?: (boolean | null)[] | null
      observations?: string | null
      gapDescription?: string | null
      contributorComments?: string | null
    },
  ): Promise<WorkTaskDetail> =>
    apiFetch(`/api/my-tasks/${taskId}/submit-gap`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  saveGapDraft: (
    taskId: number,
    body: {
      implementationStatus?: GapImplementationStatus | null
      maturity?: number | null
      maturityGates?: (boolean | null)[] | null
      observations?: string | null
      gapDescription?: string | null
      contributorComments?: string | null
    },
  ): Promise<WorkTaskDetail> =>
    apiFetch(`/api/my-tasks/${taskId}/save-gap-draft`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  submitMilestoneTask: (
    taskId: number,
    body?: { progressNotes?: string | null },
  ): Promise<WorkTaskDetail> =>
    apiFetch(`/api/my-tasks/${taskId}/submit-milestone`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body ?? {}),
    }).then(handle),

  submitRiskTask: (taskId: number, body: SubmitRiskTaskBody): Promise<WorkTaskDetail> =>
    apiFetch(`/api/my-tasks/${taskId}/submit-risk`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  submitCapaTask: (
    taskId: number,
    body: {
      rootCause: string
      correctiveAction: string
      preventiveAction?: string | null
      progressNotes?: string | null
    },
  ): Promise<WorkTaskDetail> =>
    apiFetch(`/api/my-tasks/${taskId}/submit-capa`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  submitBiaTask: (taskId: number): Promise<WorkTaskDetail> =>
    apiFetch(`/api/my-tasks/${taskId}/submit-bia`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  acknowledgePolicyTask: (taskId: number): Promise<WorkTaskDetail> =>
    apiFetch(`/api/my-tasks/${taskId}/acknowledge-policy`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  submitKriTask: (
    taskId: number,
    body: { value: number; notes?: string | null },
  ): Promise<WorkTaskDetail> =>
    apiFetch(`/api/my-tasks/${taskId}/submit-kri`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  listKris: (): Promise<KriRecord[]> =>
    apiFetch('/api/kris', { headers: authHeaders() }).then(handle),

  getKri: (id: number): Promise<KriRecord> =>
    apiFetch(`/api/kris/${id}`, { headers: authHeaders() }).then(handle),

  createKri: (body: CreateKriBody): Promise<KriRecord> =>
    apiFetch('/api/kris', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updateKri: (id: number, body: UpdateKriBody): Promise<KriRecord> =>
    apiFetch(`/api/kris/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  recordKriValue: (
    id: number,
    body: { value: number; notes?: string | null },
  ): Promise<KriRecord> =>
    apiFetch(`/api/kris/${id}/values`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  acknowledgeKriRed: (id: number): Promise<KriRecord> =>
    apiFetch(`/api/kris/${id}/acknowledge-red`, { method: 'POST', headers: authHeaders() }).then(
      handle,
    ),

  runKriLifecycle: (asOf?: string): Promise<{ asOf: string; dueNotified: number; overdueNotified: number }> =>
    apiFetch(`/api/kris/jobs/lifecycle-run${asOf ? `?asOf=${encodeURIComponent(asOf)}` : ''}`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  listIncidents: (): Promise<IncidentRecord[]> =>
    apiFetch('/api/incidents', { headers: authHeaders() }).then(handle),

  getIncident: (id: number): Promise<IncidentRecord> =>
    apiFetch(`/api/incidents/${id}`, { headers: authHeaders() }).then(handle),

  reportIncident: (body: ReportIncidentBody): Promise<IncidentRecord> =>
    apiFetch('/api/incidents/report', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  createIncident: (body: CreateIncidentBody): Promise<IncidentRecord> =>
    apiFetch('/api/incidents', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updateIncident: (id: number, body: UpdateIncidentBody): Promise<IncidentRecord> =>
    apiFetch(`/api/incidents/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  assignIncident: (id: number, body: { userId: number }): Promise<IncidentRecord> =>
    apiFetch(`/api/incidents/${id}/assign`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  resolveIncident: (
    id: number,
    body?: { resolutionNotes?: string | null; rootCause?: string | null; containmentActions?: string | null },
  ): Promise<IncidentRecord> =>
    apiFetch(`/api/incidents/${id}/resolve`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body ?? {}),
    }).then(handle),

  closeIncident: (id: number): Promise<IncidentRecord> =>
    apiFetch(`/api/incidents/${id}/close`, { method: 'POST', headers: authHeaders() }).then(handle),

  runIncidentLifecycle: (
    asOf?: string,
  ): Promise<{ asOf: string; overdueNotified: number }> =>
    apiFetch(
      `/api/incidents/jobs/lifecycle-run${asOf ? `?asOf=${encodeURIComponent(asOf)}` : ''}`,
      { method: 'POST', headers: authHeaders() },
    ).then(handle),

  submitIncidentTask: (
    taskId: number,
    body: {
      progressNotes?: string | null
      containmentActions?: string | null
      rootCause?: string | null
      markResolved?: boolean | null
    },
  ): Promise<WorkTaskDetail> =>
    apiFetch(`/api/my-tasks/${taskId}/submit-incident`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  listAudits: (): Promise<GrcAuditSummary[]> =>
    apiFetch('/api/audits', { headers: authHeaders() }).then(handle),

  getMyAuditAccess: (): Promise<{ modules: EngagementScopeModule[]; grants: AccessGrant[] }> =>
    apiFetch('/api/audits/my-access', { headers: authHeaders() }).then(handle),

  getAudit: (id: number): Promise<GrcAuditDetail> =>
    apiFetch(`/api/audits/${id}`, { headers: authHeaders() }).then(handle),

  listAuditScopedEvidence: (auditId: number): Promise<AuditPortalEvidenceItem[]> =>
    apiFetch(`/api/audits/${auditId}/scoped-evidence`, { headers: authHeaders() }).then(handle),

  listAuditScopedGap: (auditId: number): Promise<AuditPortalGapItem[]> =>
    apiFetch(`/api/audits/${auditId}/scoped-gap`, { headers: authHeaders() }).then(handle),

  listAuditScopedSoa: (auditId: number): Promise<AuditPortalSoaItem[]> =>
    apiFetch(`/api/audits/${auditId}/scoped-soa`, { headers: authHeaders() }).then(handle),

  listAuditScopedRisks: (auditId: number): Promise<AuditPortalRiskItem[]> =>
    apiFetch(`/api/audits/${auditId}/scoped-risks`, { headers: authHeaders() }).then(handle),

  downloadAuditReport: async (auditId: number, format: 'pdf' | 'xlsx' = 'pdf'): Promise<void> => {
    const path = format === 'xlsx' ? `/api/audits/${auditId}/report.xlsx` : `/api/audits/${auditId}/report.pdf`
    const res = await apiFetch(path, { headers: authHeaders() })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const disp = res.headers.get('Content-Disposition') ?? ''
    const match = /filename="([^"]+)"/.exec(disp)
    const fallback = format === 'xlsx' ? `audit-${auditId}-report.xlsx` : `audit-${auditId}-report.pdf`
    const name = match?.[1] ?? fallback
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = name
    a.click()
    URL.revokeObjectURL(url)
  },

  createAudit: (body: CreateAuditBody): Promise<GrcAuditDetail> =>
    apiFetch('/api/audits', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  listAuditChecklistLibraries: (): Promise<AuditChecklistLibrary[]> =>
    apiFetch('/api/audits/checklist-library', { headers: authHeaders() }).then(handle),

  getAuditChecklistLibrary: (id: number): Promise<AuditChecklistLibrary> =>
    apiFetch(`/api/audits/checklist-library/${id}`, { headers: authHeaders() }).then(handle),

  createAuditChecklistLibrary: (name: string): Promise<AuditChecklistLibrary> =>
    apiFetch('/api/audits/checklist-library', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ name }),
    }).then(handle),

  saveAuditChecklistLibrary: (
    id: number,
    body: {
      name: string
      items?: {
        id?: number
        clauseRef: string
        section?: string
        title: string
        testFocus?: string
        soaApplicability?: string
        evidenceLives?: string
      }[]
    },
  ): Promise<AuditChecklistLibrary> =>
    apiFetch(`/api/audits/checklist-library/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  deleteAuditChecklistLibrary: (id: number): Promise<void> =>
    apiFetch(`/api/audits/checklist-library/${id}`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(handle),

  downloadAuditChecklistTemplate: async (): Promise<void> => {
    const res = await apiFetch('/api/audits/checklist-library/template.xlsx', { headers: authHeaders() })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'audit-checklist-template.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  },

  uploadAuditChecklistLibrary: (id: number, file: File): Promise<AuditChecklistLibrary> => {
    const fd = new FormData()
    fd.append('file', file)
    return apiFetch(`/api/audits/checklist-library/${id}/upload`, {
      method: 'POST',
      headers: authHeaders(),
      body: fd,
    }).then(handle)
  },

  listAuditGrants: (auditId: number): Promise<AccessGrant[]> =>
    apiFetch(`/api/audits/${auditId}/grants`, { headers: authHeaders() }).then(handle),

  listAllAuditGrants: (): Promise<AccessGrant[]> =>
    apiFetch('/api/audits/grants', { headers: authHeaders() }).then(handle),

  listAuditAccessLog: (): Promise<AccessViewLog[]> =>
    apiFetch('/api/audits/access-log', { headers: authHeaders() }).then(handle),

  createAuditGrant: (
    auditId: number,
    body: {
      userId: number
      granteeType: AuditGranteeType
      organisation?: string
      ndaReference?: string
      canDownloadEvidence?: boolean
    },
  ): Promise<AccessGrant> =>
    apiFetch(`/api/audits/${auditId}/grants`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  revokeAuditGrant: (auditId: number, grantId: number, reason: string): Promise<AccessGrant> =>
    apiFetch(`/api/audits/${auditId}/grants/${grantId}/revoke`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ reason }),
    }).then(handle),

  updateAuditEngagement: (id: number, body: UpdateAuditEngagementBody): Promise<GrcAuditDetail> =>
    apiFetch(`/api/audits/${id}/engagement`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  listAuditExternalInvites: (auditId: number): Promise<AuditExternalInvite[]> =>
    apiFetch(`/api/audits/${auditId}/external-invites`, { headers: authHeaders() }).then(handle),

  createAuditExternalInvite: (
    auditId: number,
    body: { name: string; email: string; expiryDays?: number },
  ): Promise<AuditExternalInvite> =>
    apiFetch(`/api/audits/${auditId}/external-invites`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  startAudit: (id: number): Promise<GrcAuditDetail> =>
    apiFetch(`/api/audits/${id}/start`, { method: 'POST', headers: authHeaders() }).then(handle),

  updateAuditChecklist: (
    auditId: number,
    itemId: number,
    body: {
      assessment?: string | null
      notes?: string | null
      inScope?: boolean
      evidenceExamined?: string | null
      personsInterviewed?: string | null
      reviewedDate?: string | null
      conclusion?: ChecklistConclusion | null
      findingDescription?: string | null
      clauseRef?: string | null
      section?: string | null
      title?: string | null
      testFocus?: string | null
      soaApplicability?: string | null
      evidenceModule?: string | null
    },
  ): Promise<AuditChecklistItem> =>
    apiFetch(`/api/audits/${auditId}/checklist/${itemId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  addAuditChecklist: (
    auditId: number,
    body: { templateKey?: ChecklistTemplateKey; name?: string },
  ): Promise<GrcAuditDetail> =>
    apiFetch(`/api/audits/${auditId}/checklists`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  deleteAuditChecklist: (auditId: number, checklistId: number): Promise<GrcAuditDetail> =>
    apiFetch(`/api/audits/${auditId}/checklists/${checklistId}`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(handle),

  uploadNewAuditChecklist: (auditId: number, name: string, file: File): Promise<GrcAuditDetail> => {
    const fd = new FormData()
    fd.append('name', name)
    fd.append('file', file)
    return apiFetch(`/api/audits/${auditId}/checklists/upload`, {
      method: 'POST',
      headers: authHeaders(),
      body: fd,
    }).then(handle)
  },

  uploadAuditChecklist: (auditId: number, checklistId: number, file: File): Promise<GrcAuditDetail> => {
    const fd = new FormData()
    fd.append('file', file)
    return apiFetch(`/api/audits/${auditId}/checklists/${checklistId}/upload`, {
      method: 'POST',
      headers: authHeaders(),
      body: fd,
    }).then(handle)
  },

  addAuditChecklistItem: (
    auditId: number,
    checklistId: number,
    body: {
      clauseRef: string
      section?: string
      title: string
      testFocus?: string
      soaApplicability?: string
      evidenceModule?: string
    },
  ): Promise<AuditChecklistItem> =>
    apiFetch(`/api/audits/${auditId}/checklists/${checklistId}/items`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  deleteAuditChecklistItem: (auditId: number, itemId: number): Promise<GrcAuditDetail> =>
    apiFetch(`/api/audits/${auditId}/checklist/${itemId}`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(handle),

  addAuditFinding: (auditId: number, body: CreateFindingBody): Promise<AuditFinding> =>
    apiFetch(`/api/audits/${auditId}/findings`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updateAuditSummary: (id: number, executiveSummary: string): Promise<GrcAuditDetail> =>
    apiFetch(`/api/audits/${id}/summary`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ executiveSummary }),
    }).then(handle),

  updateAuditScope: (
    id: number,
    body: { scopeNotes?: string | null; scopeModules: EngagementScopeModule[] },
  ): Promise<GrcAuditDetail> =>
    apiFetch(`/api/audits/${id}/scope`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  auditorSignAudit: (id: number): Promise<GrcAuditDetail> =>
    apiFetch(`/api/audits/${id}/auditor-sign`, { method: 'POST', headers: authHeaders() }).then(handle),

  closeAudit: (id: number): Promise<GrcAuditDetail> =>
    apiFetch(`/api/audits/${id}/close`, { method: 'POST', headers: authHeaders() }).then(handle),

  adminSignAudit: (id: number): Promise<GrcAuditDetail> =>
    apiFetch(`/api/audits/${id}/admin-sign`, { method: 'POST', headers: authHeaders() }).then(handle),

  syncAuditGapScores: (id: number): Promise<GrcAuditDetail> =>
    apiFetch(`/api/audits/${id}/sync-gap-scores`, { method: 'POST', headers: authHeaders() }).then(handle),

  updateAuditDomainNarratives: (
    id: number,
    narratives: { code: string; narrative: string }[],
  ): Promise<GrcAuditDetail> =>
    apiFetch(`/api/audits/${id}/domain-narratives`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ narratives }),
    }).then(handle),

  listAuditFindingsRegister: (severity?: FindingSeverity): Promise<FindingRegisterItem[]> => {
    const qs = severity ? `?severity=${severity}` : ''
    return apiFetch(`/api/audits/findings/register${qs}`, { headers: authHeaders() }).then(handle)
  },

  returnAudit: (id: number, reason: string): Promise<GrcAuditDetail> =>
    apiFetch(`/api/audits/${id}/return`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ reason }),
    }).then(handle),

  listCapa: (register?: string, capaRegisterId?: number): Promise<CapaRecord[]> => {
    const q = new URLSearchParams()
    if (register) q.set('register', register)
    if (capaRegisterId != null) q.set('capaRegisterId', String(capaRegisterId))
    const qs = q.toString()
    return apiFetch(`/api/capa${qs ? `?${qs}` : ''}`, {
      headers: authHeaders(),
    }).then(handle)
  },

  listCapaRegisters: (): Promise<CapaRegisterRecord[]> =>
    apiFetch('/api/capa/registers', { headers: authHeaders() }).then(handle),

  getCapaRegister: (id: number): Promise<CapaRegisterRecord> =>
    apiFetch(`/api/capa/registers/${id}`, { headers: authHeaders() }).then(handle),

  listCapaAvailableFindings: (): Promise<CapaAvailableFinding[]> =>
    apiFetch('/api/capa/registers/available-findings', { headers: authHeaders() }).then(handle),

  createCapaRegister: (body: {
    year: number
    name: string
  }): Promise<CapaRegisterRecord> =>
    apiFetch('/api/capa/registers', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  createCapa: (body: {
    registerCode: CapaRegisterCode
    title: string
    description?: string
    recommendedAction?: string
    controlRef?: string
    severity?: string
    priority: string
    targetDate?: string | null
    auditId?: number
    capaRegisterId?: number
    findingId?: number
    externalFindingCode?: string
    sourceOther?: string
    conductedBy?: string
    correction?: string
    rootCause?: string
    correctiveActionPlan?: string
    evidenceRequired?: string
    contributorDepartment?: string
    assignedContributorUserId?: number
  }): Promise<CapaRecord> =>
    apiFetch('/api/capa', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  getCapa: (id: number): Promise<CapaRecord> =>
    apiFetch(`/api/capa/${id}`, { headers: authHeaders() }).then(handle),

  assignCapa: (
    id: number,
    body: { ownerUserId: number; targetDate?: string | null; contributorDepartment?: string | null },
  ): Promise<CapaRecord> =>
    apiFetch(`/api/capa/${id}/assign`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  /** Admin updates CAPA Tracker fields (Excel E, G-N, Q-T) */
  updateCapaTracker: (
    id: number,
    body: {
      clauseRef?: string // E
      correction?: string // G
      rootCause?: string // H
      correctiveActionPlan?: string // I
      agreedTargetDate?: string | null // K
      assignedContributorUserId?: number | null // L
      contributorDepartment?: string // M
      evidenceRequired?: string // N
      reviewRemarks?: string // Q
      evidenceOfCompletion?: string // R
      verificationDate?: string | null // S
      verificationResult?: string // T
    },
  ): Promise<CapaRecord> =>
    apiFetch(`/api/capa/${id}/tracker`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  /** Contributor submits their response (Excel O-P) */
  submitCapaResponse: (
    id: number,
    body: {
      responseDescription?: string // O
      contributorEvidenceNote?: string // P
    },
  ): Promise<CapaRecord> =>
    apiFetch(`/api/capa/${id}/response`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  submitCapa: (id: number): Promise<CapaRecord> =>
    apiFetch(`/api/capa/${id}/submit`, { method: 'POST', headers: authHeaders() }).then(handle),

  closeCapa: (id: number): Promise<CapaRecord> =>
    apiFetch(`/api/capa/${id}/close`, { method: 'POST', headers: authHeaders() }).then(handle),

  rejectCapa: (id: number, reason: string): Promise<CapaRecord> =>
    apiFetch(`/api/capa/${id}/reject`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ reason }),
    }).then(handle),

  listAssets: (): Promise<AssetSummary[]> =>
    apiFetch('/api/assets', { headers: authHeaders() }).then(handle),

  getAssetMeta: (): Promise<AssetMeta> =>
    apiFetch('/api/assets/meta', { headers: authHeaders() }).then(handle),

  getAssetKpis: (): Promise<AssetKpis> =>
    apiFetch('/api/assets/kpis', { headers: authHeaders() }).then(handle),

  getAsset: (id: number): Promise<AssetSummary> =>
    apiFetch(`/api/assets/${id}`, { headers: authHeaders() }).then(handle),

  createAsset: (body: CreateAssetBody): Promise<AssetSummary> =>
    apiFetch('/api/assets', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updateAsset: (id: number, body: CreateAssetBody): Promise<AssetSummary> =>
    apiFetch(`/api/assets/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  disposeAsset: (id: number): Promise<AssetSummary> =>
    apiFetch(`/api/assets/${id}/dispose`, { method: 'POST', headers: authHeaders() }).then(handle),

  listRiskProjects: (projectType: RiskType): Promise<RiskProjectSummary[]> =>
    apiFetch(`/api/risk-projects?projectType=${projectType}`, { headers: authHeaders() }).then(handle),

  listRiskKpis: (riskType: RiskType): Promise<RiskKpis> =>
    apiFetch(`/api/risks/kpis?riskType=${riskType}`, { headers: authHeaders() }).then(handle),

  createRiskProject: (body: {
    projectType: RiskType
    name: string
    year: number
    frameworkId?: number | null
  }): Promise<RiskProjectSummary> =>
    apiFetch('/api/risk-projects', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  getRiskProject: (id: number): Promise<RiskProjectSummary> =>
    apiFetch(`/api/risk-projects/${id}`, { headers: authHeaders() }).then(handle),

  listAcceptanceRegister: (): Promise<RiskAcceptanceForm[]> =>
    apiFetch('/api/risks/acceptance-register', { headers: authHeaders() }).then(handle),

  postTreatmentRisk: (
    id: number,
    body: {
      controlEffectiveness?: RiskLevel | null
      residualScoringMode?: ResidualScoringMode | null
      residualScore?: number | null
      targetControlEffectiveness?: RiskLevel | null
      targetResidualScore?: number | null
      targetResidualManual?: boolean | null
      nextReviewDate?: string | null
      overrideAutoClose?: boolean | null
      autoCloseOverrideJustification?: string | null
    },
  ): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/post-treatment`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  decideRiskFinalStatus: (
    id: number,
    decision: 'APPROVE' | 'REJECT',
    reason?: string | null,
    opts?: {
      actionStatus?: RiskStatus | null
      nextReviewDate?: string | null
      sendBack?: boolean
      startAcceptance?: boolean
    },
  ): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/final-status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({
        decision,
        reason: reason ?? null,
        actionStatus: opts?.actionStatus ?? null,
        nextReviewDate: opts?.nextReviewDate ?? null,
        sendBack: opts?.sendBack ?? null,
        startAcceptance: opts?.startAcceptance ?? null,
      }),
    }).then(handle),

  listRisks: (
    riskType?: string,
    filters?: {
      probability?: string
      impact?: string
      residualRisk?: string
      controlEffectiveness?: string
      inherentRisk?: string
      residualProbability?: string
      residualImpact?: string
      openOnly?: boolean
      projectId?: number
      status?: string
    },
  ): Promise<RiskSummary[]> => {
    const q = new URLSearchParams()
    if (riskType) q.set('riskType', riskType)
    if (filters?.probability) q.set('probability', filters.probability)
    if (filters?.impact) q.set('impact', filters.impact)
    if (filters?.residualRisk) q.set('residualRisk', filters.residualRisk)
    if (filters?.controlEffectiveness) q.set('controlEffectiveness', filters.controlEffectiveness)
    if (filters?.inherentRisk) q.set('inherentRisk', filters.inherentRisk)
    if (filters?.residualProbability) q.set('residualProbability', filters.residualProbability)
    if (filters?.residualImpact) q.set('residualImpact', filters.residualImpact)
    if (filters?.openOnly != null) q.set('openOnly', String(filters.openOnly))
    if (filters?.projectId != null) q.set('projectId', String(filters.projectId))
    if (filters?.status) q.set('status', filters.status)
    const qs = q.toString()
    return apiFetch(`/api/risks${qs ? `?${qs}` : ''}`, { headers: authHeaders() }).then(handle)
  },

  getRiskHeatMap: (
    riskType: RiskType,
    scoreBasis: HeatMapScoreBasis = 'INHERENT',
    openOnly = true,
  ): Promise<RiskHeatMap> =>
    apiFetch(
      `/api/risks/heatmap?riskType=${riskType}&scoreBasis=${scoreBasis}&openOnly=${openOnly}`,
      { headers: authHeaders() },
    ).then(handle),

  getRisk: (id: number): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}`, { headers: authHeaders() }).then(handle),

  listRiskEligibleContributors: (
    orgUnitId: number,
  ): Promise<{ id: number; email: string; fullName: string }[]> =>
    apiFetch(`/api/risks/eligible-contributors?orgUnitId=${orgUnitId}`, { headers: authHeaders() }).then(
      handle,
    ),

  getRiskMatrices: (): Promise<RiskMatricesConfig> =>
    apiFetch('/api/risks/matrices', { headers: authHeaders() }).then(handle),

  updateRiskMatrices: (body: {
    gridSize: number
    inherent: RiskLevel[][]
    residual: RiskLevel[][]
    rowLevels: RiskLevel[]
    colLevels: RiskLevel[]
    changeReason: string
  }): Promise<RiskMatricesConfig> =>
    apiFetch('/api/risks/matrices', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  raiseRiskFromGap: (
    assessmentId: number,
    itemId: number,
    body: CreateRiskBody,
  ): Promise<RiskSummary> =>
    apiFetch(`/api/gap-assessments/${assessmentId}/items/${itemId}/raise-risk`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  createRisk: (body: CreateRiskBody): Promise<RiskSummary> =>
    apiFetch('/api/risks', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  listRiskDrafts: (riskType: RiskType, projectId: number): Promise<RiskFormDraft[]> =>
    apiFetch(`/api/risks/drafts?riskType=${riskType}&projectId=${projectId}`, {
      headers: authHeaders(),
    }).then(handle),

  getRiskDraft: (id: number): Promise<RiskFormDraft> =>
    apiFetch(`/api/risks/drafts/${id}`, { headers: authHeaders() }).then(handle),

  saveRiskDraft: (
    id: number | null,
    body: { riskType: RiskType; riskProjectId: number; payload: Record<string, unknown> },
  ): Promise<RiskFormDraft> =>
    apiFetch(id == null ? '/api/risks/drafts' : `/api/risks/drafts/${id}`, {
      method: id == null ? 'POST' : 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  deleteRiskDraft: (id: number): Promise<void> =>
    apiFetch(`/api/risks/drafts/${id}`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(handle),

  createRiskFromDraft: (draftId: number, body: CreateRiskBody): Promise<RiskSummary> =>
    apiFetch(`/api/risks/drafts/${draftId}/create`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  exportRisk: async (id: number, format: 'xlsx' | 'pdf' = 'xlsx'): Promise<void> => {
    const res = await apiFetch(`/api/risks/${id}/export?format=${format}`, { headers: authHeaders() })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const disp = res.headers.get('Content-Disposition') ?? ''
    const match = /filename="?([^"]+)"?/.exec(disp)
    const name = match?.[1] ?? `risk-${id}.${format}`
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = name
    a.click()
    URL.revokeObjectURL(url)
  },

  downloadRiskBulkTemplate: async (
    riskType: RiskType,
    projectId?: number | null,
  ): Promise<void> => {
    const qs = new URLSearchParams({ riskType })
    if (projectId != null) qs.set('projectId', String(projectId))
    const res = await apiFetch(`/api/risks/template.xlsx?${qs}`, {
      headers: authHeaders(),
    })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = riskType === 'ERM' ? 'erm-risk-bulk-template.xlsx' : 'is-risk-bulk-template.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  },

  importRiskBulk: async (
    riskType: RiskType,
    file: File,
    projectId?: number | null,
  ): Promise<RiskBulkImportResult> => {
    const form = new FormData()
    form.append('file', file)
    const qs = new URLSearchParams({ riskType })
    if (projectId != null) qs.set('projectId', String(projectId))
    const res = await apiFetch(`/api/risks/import.xlsx?${qs}`, {
      method: 'POST',
      headers: authHeaders(),
      body: form,
    })
    return handle(res)
  },

  updateRisk: (id: number, body: Partial<CreateRiskBody>): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  reassignRiskOwner: (id: number, userId: number): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/owner`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ userId }),
    }).then(handle),

  assignRisk: (
    id: number,
    body: { userId: number; dueDate?: string | null; treatmentOption?: TreatmentOption; treatmentPlan?: string | null },
  ): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/assign`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  cancelRisk: (id: number, reason?: string | null): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/cancel`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ reason: reason?.trim() || null }),
    }).then(handle),

  approveRisk: (id: number): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/approve`, { method: 'POST', headers: authHeaders() }).then(handle),

  startRiskValidation: (id: number): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/validate/start`, { method: 'POST', headers: authHeaders() }).then(handle),

  rejectRiskValidation: (id: number, reason: string): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/validate/reject`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ reason }),
    }).then(handle),

  approveRiskValidation: (
    id: number,
    validationNotes?: string | null,
    infosecStatusFinal?: string | null,
  ): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/validate/approve`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({
        validationNotes: validationNotes ?? null,
        infosecStatusFinal: infosecStatusFinal ?? null,
      }),
    }).then(handle),

  upsertRiskTreatmentPlan: (
    id: number,
    body: {
      actionDescription?: string | null
      actionOwnerId?: number | null
      custodianUserId?: number | null
      startDate?: string | null
      targetDate?: string | null
      resourcesRequired?: string | null
      costEstimateAed?: number | null
      evidenceRequired?: string | null
      markInProgress?: boolean
    },
  ): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/treatment-plan`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  addRiskMilestone: (
    id: number,
    body: { milestoneTitle: string; dueDate?: string | null },
  ): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/milestones`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  completeRiskMilestone: (
    id: number,
    milestoneId: number,
    body?: { progressNotes?: string | null },
  ): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/milestones/${milestoneId}/complete`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body ?? {}),
    }).then(handle),

  closeRisk: (
    id: number,
    body?: {
      probability?: RiskLevel | null
      impact?: RiskLevel | null
      controlEffectiveness?: RiskLevel | null
      closeNotes?: string | null
    } | null,
  ): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/close`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body ?? {}),
    }).then(handle),

  changeRiskTreatmentOption: (
    id: number,
    body: { treatmentOption: TreatmentOption; justification: string },
  ): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/treatment-option`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  ownerDecideRiskAcceptance: (
    id: number,
    body: { decision: 'APPROVED' | 'REJECTED'; comments?: string | null; declaration: boolean },
  ): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/acceptance/owner-decide`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  adminDecideRiskAcceptance: (
    id: number,
    body: {
      decision: 'ACCEPTED' | 'SENT_BACK'
      comments?: string | null
      declaration: boolean
      nextReviewDate?: string | null
    },
  ): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/acceptance/admin-decide`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  reopenRisk: (id: number, reason: string): Promise<RiskSummary> =>
    apiFetch(`/api/risks/${id}/reopen`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ reason }),
    }).then(handle),

  listRiskScoreHistory: (id: number): Promise<RiskScoreHistoryEntry[]> =>
    apiFetch(`/api/risks/${id}/score-history`, { headers: authHeaders() }).then(handle),

  listRiskNotes: (id: number): Promise<RiskStageNote[]> =>
    apiFetch(`/api/risks/${id}/notes`, { headers: authHeaders() }).then(handle),

  addRiskNote: (
    id: number,
    body: { body: string; internalOnly?: boolean | null },
  ): Promise<RiskStageNote> =>
    apiFetch(`/api/risks/${id}/notes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  deleteRiskNote: (id: number, noteId: number): Promise<void> =>
    apiFetch(`/api/risks/${id}/notes/${noteId}`, { method: 'DELETE', headers: authHeaders() }).then(
      handle,
    ),

  assignGapItem: (
    assessmentId: number,
    itemId: number,
    body: {
      userId: number
      implementationStatus?: GapImplementationStatus | null
      dueDate?: string | null
      assignAllQuestions?: boolean | null
      frameworkQuestionIds?: number[] | null
      customQuestionIds?: number[] | null
      /** False stores the contributor and waits for Send. */
      release?: boolean | null
    },
  ): Promise<GapItem> =>
    apiFetch(`/api/gap-assessments/${assessmentId}/items/${itemId}/assign`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  finalizeGapItem: (
    assessmentId: number,
    itemId: number,
    body: {
      finalImplementationStatus?: GapImplementationStatus | null
      finalMaturity?: number | null
      adminReviewNotes?: string | null
    },
  ): Promise<GapItem> =>
    apiFetch(`/api/gap-assessments/${assessmentId}/items/${itemId}/finalize`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  sendGapItems: (assessmentId: number, itemIds: number[]): Promise<GapItem[]> =>
    apiFetch(`/api/gap-assessments/${assessmentId}/items/send`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ itemIds }),
    }).then(handle),

  sendBackGapItems: (
    assessmentId: number,
    itemIds: number[],
    reason: string,
  ): Promise<GapItem[]> =>
    apiFetch(`/api/gap-assessments/${assessmentId}/items/send-back`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ itemIds, reason }),
    }).then(handle),

  setGapItemFlag: (assessmentId: number, itemId: number, flagged: boolean): Promise<GapItem> =>
    apiFetch(`/api/gap-assessments/${assessmentId}/items/${itemId}/flag`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ flagged }),
    }).then(handle),

  sendBackGapItem: (
    assessmentId: number,
    itemId: number,
    body: { reason: string; implementationStatus?: GapImplementationStatus | null },
  ): Promise<GapItem> =>
    apiFetch(`/api/gap-assessments/${assessmentId}/items/${itemId}/send-back`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  raiseGapCapa: (assessmentId: number, itemId: number): Promise<CapaRecord> =>
    apiFetch(`/api/gap-assessments/${assessmentId}/items/${itemId}/raise-capa`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  raiseRiskCapa: (id: number): Promise<CapaRecord> =>
    apiFetch(`/api/risks/${id}/raise-capa`, { method: 'POST', headers: authHeaders() }).then(handle),

  listGapItemSubmissions: (
    assessmentId: number,
    itemId: number,
  ): Promise<GapItemSubmission[]> =>
    apiFetch(`/api/gap-assessments/${assessmentId}/items/${itemId}/submissions`, {
      headers: authHeaders(),
    }).then(handle),

  // --- demo (tenant from token) ---
  listPings: (): Promise<{ records: PingRecord[] }> =>
    apiFetch('/api/demo/ping', { headers: authHeaders() }).then(handle),

  addPing: (note: string): Promise<unknown> =>
    apiFetch('/api/demo/ping', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ note }),
    }).then(handle),

  listAuditTrail: (page = 0, size = 50): Promise<AuditPage> =>
    apiFetch(`/api/audit-trail?page=${page}&size=${size}`, { headers: authHeaders() }).then(handle),

  downloadAuditTrailCsv: async (opts?: {
    module?: string
    actionType?: string
    limit?: number
  }): Promise<void> => {
    const qs = new URLSearchParams()
    if (opts?.module) qs.set('module', opts.module)
    if (opts?.actionType) qs.set('actionType', opts.actionType)
    if (opts?.limit != null) qs.set('limit', String(opts.limit))
    const q = qs.toString()
    const res = await apiFetch(`/api/audit-trail/export.csv${q ? `?${q}` : ''}`, {
      headers: authHeaders(),
    })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const disp = res.headers.get('Content-Disposition') ?? ''
    const match = /filename="([^"]+)"/.exec(disp)
    const name = match?.[1] ?? 'audit-trail.csv'
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = name
    a.click()
    URL.revokeObjectURL(url)
  },

  listEvidence: (params?: {
    q?: string
    classification?: string
    status?: string
    page?: number
    size?: number
  }): Promise<EvidencePage> => {
    const qs = new URLSearchParams()
    if (params?.q) qs.set('q', params.q)
    if (params?.classification) qs.set('classification', params.classification)
    if (params?.status) qs.set('status', params.status)
    qs.set('page', String(params?.page ?? 0))
    qs.set('size', String(params?.size ?? 50))
    return apiFetch(`/api/evidence?${qs}`, { headers: authHeaders() }).then(handle)
  },

  listEvidenceLinked: (
    moduleCode: string,
    recordType: string,
    recordId: string,
  ): Promise<EvidenceSummary[]> => {
    const qs = new URLSearchParams({ moduleCode, recordType, recordId })
    return apiFetch(`/api/evidence/linked?${qs}`, { headers: authHeaders() }).then(handle)
  },

  getEvidence: (id: number): Promise<EvidenceDetail> =>
    apiFetch(`/api/evidence/${id}`, { headers: authHeaders() }).then(handle),

  uploadEvidence: async (fields: {
    file: File
    title?: string
    classification: string
    expiresOn?: string
    moduleCode?: string
    recordType?: string
    recordId?: string
    workflowStage?: string
  }): Promise<EvidenceDetail> => {
    const body = new FormData()
    body.append('file', fields.file)
    if (fields.title) body.append('title', fields.title)
    body.append('classification', fields.classification)
    if (fields.expiresOn) body.append('expiresOn', fields.expiresOn)
    if (fields.moduleCode) body.append('moduleCode', fields.moduleCode)
    if (fields.recordType) body.append('recordType', fields.recordType)
    if (fields.recordId) body.append('recordId', fields.recordId)
    if (fields.workflowStage) body.append('workflowStage', fields.workflowStage)
    return apiFetch('/api/evidence', { method: 'POST', headers: authHeaders(), body }).then(handle)
  },

  addEvidenceVersion: async (id: number, file: File): Promise<EvidenceDetail> => {
    const body = new FormData()
    body.append('file', file)
    return apiFetch(`/api/evidence/${id}/versions`, {
      method: 'POST',
      headers: authHeaders(),
      body,
    }).then(handle)
  },

  addEvidenceLink: (
    id: number,
    link: { moduleCode: string; recordType: string; recordId: string; workflowStage?: string },
  ): Promise<EvidenceLink> =>
    apiFetch(`/api/evidence/${id}/links`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(link),
    }).then(handle),

  deleteEvidence: (id: number): Promise<null> =>
    apiFetch(`/api/evidence/${id}`, { method: 'DELETE', headers: authHeaders() }).then(handle),

  unlinkEvidenceFromRecord: (
    id: number,
    link: { moduleCode: string; recordType: string; recordId: string },
  ): Promise<null> => {
    const qs = new URLSearchParams(link)
    return apiFetch(`/api/evidence/${id}/record-link?${qs}`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(handle)
  },

  fetchEvidenceBlob: async (
    id: number,
    version?: number,
  ): Promise<{ blob: Blob; filename: string; contentType: string }> => {
    const qs = version != null ? `?version=${version}` : ''
    const res = await apiFetch(`/api/evidence/${id}/download${qs}`, { headers: authHeaders() })
    if (!res.ok) {
      await handle(res)
      throw new Error('Evidence download failed')
    }
    const blob = await res.blob()
    const disp = res.headers.get('Content-Disposition') ?? ''
    const match = /filename="([^"]+)"/.exec(disp)
    const filename = match?.[1] ?? `evidence-${id}`
    const contentType = res.headers.get('Content-Type') ?? blob.type
    return { blob, filename, contentType }
  },

  downloadEvidence: async (id: number, version?: number): Promise<void> => {
    const { blob, filename } = await api.fetchEvidenceBlob(id, version)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
  },

  listWorkflowDefinitions: (): Promise<WorkflowDefinition[]> =>
    apiFetch('/api/workflows/definitions', { headers: authHeaders() }).then(handle),

  listWorkflowInstances: (status?: string, page = 0, size = 50): Promise<WorkflowInstancePage> => {
    const qs = new URLSearchParams()
    if (status) qs.set('status', status)
    qs.set('page', String(page))
    qs.set('size', String(size))
    return apiFetch(`/api/workflows/instances?${qs}`, { headers: authHeaders() }).then(handle)
  },

  getWorkflowInstance: (id: number): Promise<WorkflowInstanceDetail> =>
    apiFetch(`/api/workflows/instances/${id}`, { headers: authHeaders() }).then(handle),

  startWorkflow: (body: {
    definitionCode: string
    moduleCode?: string
    recordType?: string
    recordId: string
    recordName?: string
  }): Promise<WorkflowInstanceDetail> =>
    apiFetch('/api/workflows/instances', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  transitionWorkflow: (
    id: number,
    body: { action: string; comment?: string },
  ): Promise<WorkflowInstanceDetail> =>
    apiFetch(`/api/workflows/instances/${id}/transitions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  getOrgProfile: (): Promise<OrgProfile> =>
    apiFetch('/api/org/profile', { headers: authHeaders() }).then(handle),

  getSessionPolicy: (): Promise<{ idleTimeoutMinutes: number; displayName?: string | null }> =>
    apiFetch('/api/org/session-policy', { headers: authHeaders() }).then(handle),

  updateOrgProfile: (body: {
    displayName?: string | null
    industry?: string | null
    sizeBand?: string | null
    timezone: string
    dateFormat: string
    idleTimeoutMinutes?: number | null
    rtoMaxCriticalHours?: number | null
    rtoMaxHighHours?: number | null
    rtoMaxMediumHours?: number | null
    rtoMaxLowHours?: number | null
    riskMatrixGridSize?: number | null
    riskAppetiteRating?: string | null
  }): Promise<OrgProfile> =>
    apiFetch('/api/org/profile', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  listOrgLevels: (): Promise<OrgLevel[]> =>
    apiFetch('/api/org/levels', { headers: authHeaders() }).then(handle),

  updateOrgLevels: (levels: { level: number; label: string }[]): Promise<OrgLevel[]> =>
    apiFetch('/api/org/levels', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ levels }),
    }).then(handle),

  listOrgUnits: (includeInactive = false): Promise<OrgUnitNode[]> =>
    apiFetch(`/api/org/units?includeInactive=${includeInactive}`, { headers: authHeaders() }).then(
      handle,
    ),

  createOrgUnit: (body: {
    name: string
    parentId?: number | null
    headUserId?: number | null
  }): Promise<OrgUnitNode> =>
    apiFetch('/api/org/units', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updateOrgUnit: (
    id: number,
    body: { name: string; headUserId?: number | null },
  ): Promise<OrgUnitNode> =>
    apiFetch(`/api/org/units/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  syncDepartments: (): Promise<{ available: boolean; reason: string | null; added: number; names: string[] }> =>
    apiFetch('/api/org/departments/sync', { method: 'POST', headers: authHeaders() }).then(handle),

  deactivateOrgUnit: (id: number): Promise<OrgUnitNode> =>
    apiFetch(`/api/org/units/${id}/deactivate`, { method: 'POST', headers: authHeaders() }).then(
      handle,
    ),

  reactivateOrgUnit: (id: number): Promise<OrgUnitNode> =>
    apiFetch(`/api/org/units/${id}/reactivate`, { method: 'POST', headers: authHeaders() }).then(
      handle,
    ),

  deleteOrgUnit: (id: number): Promise<null> =>
    apiFetch(`/api/org/units/${id}`, { method: 'DELETE', headers: authHeaders() }).then(handle),

  completeOrgWizard: (): Promise<OrgProfile> =>
    apiFetch('/api/org/wizard/complete', { method: 'POST', headers: authHeaders() }).then(handle),

  listSoas: (projectId?: number): Promise<SoaSummary[]> =>
    apiFetch(`/api/soa${projectId != null ? `?projectId=${projectId}` : ''}`, {
      headers: authHeaders(),
    }).then(handle),

  getSoa: (id: number): Promise<SoaDetail> =>
    apiFetch(`/api/soa/${id}`, { headers: authHeaders() }).then(handle),

  createSoa: (projectId: number, frameworkId: number): Promise<SoaDetail> =>
    apiFetch('/api/soa', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ projectId, frameworkId }),
    }).then(handle),

  updateSoaItem: (
    soaId: number,
    itemId: number,
    body: { applicability: SoaApplicability; justification: string; remarks?: string | null },
  ): Promise<SoaItem> =>
    apiFetch(`/api/soa/${soaId}/items/${itemId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  bulkUpdateSoaItems: (
    soaId: number,
    body: {
      applicability: SoaApplicability
      justification: string
      scope: 'UNANSWERED' | 'ALL' | 'SELECTED'
      itemIds?: number[]
    },
  ): Promise<SoaDetail> =>
    apiFetch(`/api/soa/${soaId}/items/bulk`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  completeSoa: (id: number): Promise<SoaDetail> =>
    apiFetch(`/api/soa/${id}/complete`, { method: 'POST', headers: authHeaders() }).then(handle),

  reopenSoa: (id: number): Promise<SoaDetail> =>
    apiFetch(`/api/soa/${id}/reopen`, { method: 'POST', headers: authHeaders() }).then(handle),

  downloadSoaExport: async (id: number): Promise<void> => {
    const res = await apiFetch(`/api/soa/${id}/export.xlsx`, { headers: authHeaders() })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const disp = res.headers.get('Content-Disposition') ?? ''
    const match = /filename="([^"]+)"/.exec(disp)
    const name = match?.[1] ?? `soa-${id}.xlsx`
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = name
    a.click()
    URL.revokeObjectURL(url)
  },

  listComplianceProjects: (): Promise<ComplianceProjectSummary[]> =>
    apiFetch('/api/compliance-projects', { headers: authHeaders() }).then(handle),

  getComplianceProject: (id: number): Promise<ComplianceProjectDetail> =>
    apiFetch(`/api/compliance-projects/${id}`, { headers: authHeaders() }).then(handle),

  createComplianceProject: (body: {
    name: string
    year: number
    frameworkIds: number[]
    includeCloud?: boolean
  }): Promise<ComplianceProjectDetail> =>
    apiFetch('/api/compliance-projects', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updateComplianceProjectStatus: (
    id: number,
    status: ComplianceProjectStatus,
  ): Promise<ComplianceProjectDetail> =>
    apiFetch(`/api/compliance-projects/${id}/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ status }),
    }).then(handle),

  listGapAssessments: (projectId?: number): Promise<GapAssessmentSummary[]> =>
    apiFetch(`/api/gap-assessments${projectId != null ? `?projectId=${projectId}` : ''}`, {
      headers: authHeaders(),
    }).then(handle),

  locateGapItem: (itemId: number): Promise<GapItemLocator> =>
    apiFetch(`/api/gap-assessments/by-item/${itemId}`, { headers: authHeaders() }).then(handle),

  getControlAssessmentStatus: (
    frameworkId: number,
    frameworkNodeId: number,
  ): Promise<ControlAssessmentStatus> =>
    apiFetch(
      `/api/gap-assessments/control-assessment-status?frameworkId=${frameworkId}&frameworkNodeId=${frameworkNodeId}`,
      { headers: authHeaders() },
    ).then(handle),

  getGapAssessment: (id: number): Promise<GapAssessmentDetail> =>
    apiFetch(`/api/gap-assessments/${id}`, { headers: authHeaders() }).then(handle),

  getGapAssessmentSummary: (id: number): Promise<GapAssessmentSummary> =>
    apiFetch(`/api/gap-assessments/${id}/summary`, { headers: authHeaders() }).then(handle),

  getGapItem: (assessmentId: number, itemId: number): Promise<GapItem> =>
    apiFetch(`/api/gap-assessments/${assessmentId}/items/${itemId}`, { headers: authHeaders() }).then(
      handle,
    ),

  createGapAssessment: (body: {
    projectId: number
    frameworkId: number
    name: string
    assessmentType: GapAssessmentType
    targetDate?: string | null
    scope?: string | null
    assessorUserId?: number | null
    /** UAE IA: 3 = L3 controls, 4 = L4 sub-controls. Ignored for other frameworks. */
    assessmentDepth?: 3 | 4 | null
  }): Promise<GapAssessmentDetail> =>
    apiFetch('/api/gap-assessments', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updateGapItem: (
    assessmentId: number,
    itemId: number,
    body: {
      applicability?: GapApplicability
      overrideJustification?: string | null
      implementationStatus?: GapImplementationStatus | null
      maturity?: number | null
      maturityGates?: (boolean | null)[] | null
      observations?: string | null
      gapDescription?: string | null
      whatItMeans?: string | null
      evidenceGuidance?: string | null
    },
  ): Promise<GapItem> =>
    apiFetch(`/api/gap-assessments/${assessmentId}/items/${itemId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  completeGapAssessment: (id: number): Promise<GapAssessmentDetail> =>
    apiFetch(`/api/gap-assessments/${id}/complete`, { method: 'POST', headers: authHeaders() }).then(
      handle,
    ),

  reopenGapAssessment: (id: number): Promise<GapAssessmentDetail> =>
    apiFetch(`/api/gap-assessments/${id}/reopen`, { method: 'POST', headers: authHeaders() }).then(
      handle,
    ),

  downloadGapExport: async (id: number): Promise<void> => {
    const res = await apiFetch(`/api/gap-assessments/${id}/export.xlsx`, { headers: authHeaders() })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const disp = res.headers.get('Content-Disposition') ?? ''
    const match = /filename="([^"]+)"/.exec(disp)
    const name = match?.[1] ?? `gap-${id}.xlsx`
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = name
    a.click()
    URL.revokeObjectURL(url)
  },

  listNotificationsLatest: (): Promise<{ notifications: NotificationItem[]; unreadCount: number }> =>
    apiFetch('/api/notifications?latest=true', { headers: authHeaders() }).then(handle),

  listNotificationsPage: (page = 0, size = 25): Promise<NotificationPage> =>
    apiFetch(`/api/notifications?page=${page}&size=${size}`, { headers: authHeaders() }).then(handle),

  listNotificationTemplates: (): Promise<NotificationTemplateSummary[]> =>
    apiFetch('/api/notifications/templates', { headers: authHeaders() }).then(handle),

  setNotificationTemplateActive: (
    id: number,
    active: boolean,
  ): Promise<NotificationTemplateSummary> =>
    apiFetch(`/api/notifications/templates/${id}/active`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ active }),
    }).then(handle),

  markNotificationRead: (id: number): Promise<unknown> =>
    apiFetch(`/api/notifications/${id}/read`, { method: 'POST', headers: authHeaders() }).then(handle),

  markAllNotificationsRead: (): Promise<unknown> =>
    apiFetch('/api/notifications/read-all', { method: 'POST', headers: authHeaders() }).then(handle),

  // --- frameworks (Super Admin catalog + tenant activate) ---
  listFrameworkTypes: (): Promise<FrameworkType[]> =>
    apiFetch('/api/admin/frameworks/types', { headers: authHeaders() }).then(handle),

  downloadFrameworkSample: async (typeCode: string): Promise<void> => {
    const res = await apiFetch(`/api/admin/frameworks/types/${encodeURIComponent(typeCode)}/sample`, {
      headers: authHeaders(),
    })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const cd = res.headers.get('Content-Disposition') ?? ''
    const match = /filename="?([^"]+)"?/.exec(cd)
    const filename = match?.[1] ?? `${typeCode}_sample.xlsx`
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
  },

  listAdminFrameworks: (): Promise<FrameworkSummary[]> =>
    apiFetch('/api/admin/frameworks', { headers: authHeaders() }).then(handle),

  getAdminFramework: (id: number): Promise<FrameworkDetail> =>
    apiFetch(`/api/admin/frameworks/${id}`, { headers: authHeaders() }).then(handle),

  updateAdminFrameworkNode: (
    frameworkId: number,
    nodeId: number,
    body: {
      code?: string
      title: string
      definition?: string | null
      controlType?: string | null
      priority?: string | null
      recordType?: string | null
      officialRef?: string | null
      inSoa?: boolean
      inGapAssessment?: boolean
      inAuditChecklist?: boolean
      inRiskRegister?: boolean
      attributes?: Record<string, string>
    },
  ): Promise<FrameworkNode> =>
    apiFetch(`/api/admin/frameworks/${frameworkId}/nodes/${nodeId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  createAdminFrameworkNode: (
    frameworkId: number,
    body: {
      parentId?: number | null
      code: string
      title: string
      definition?: string | null
      controlType?: string | null
      priority?: string | null
      recordType?: string | null
      officialRef?: string | null
      inSoa?: boolean
      inGapAssessment?: boolean
      inAuditChecklist?: boolean
      inRiskRegister?: boolean
      attributes?: Record<string, string>
    },
  ): Promise<FrameworkNode> =>
    apiFetch(`/api/admin/frameworks/${frameworkId}/nodes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  reorderAdminFrameworkNodes: (
    frameworkId: number,
    nodeIdsInOrder: number[],
  ): Promise<FrameworkDetail> =>
    apiFetch(`/api/admin/frameworks/${frameworkId}/nodes/reorder`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ nodeIdsInOrder }),
    }).then(handle),

  renameAdminFrameworkSheetTab: (
    frameworkId: number,
    fromLabel: string,
    toLabel: string,
  ): Promise<FrameworkDetail> =>
    apiFetch(`/api/admin/frameworks/${frameworkId}/sheet-tabs/rename`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ fromLabel, toLabel }),
    }).then(handle),

  retireAdminFrameworkNode: (frameworkId: number, nodeId: number): Promise<FrameworkNode> =>
    apiFetch(`/api/admin/frameworks/${frameworkId}/nodes/${nodeId}/retire`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  unretireAdminFrameworkNode: (frameworkId: number, nodeId: number): Promise<FrameworkNode> =>
    apiFetch(`/api/admin/frameworks/${frameworkId}/nodes/${nodeId}/unretire`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  deleteAdminFrameworkNode: (frameworkId: number, nodeId: number): Promise<FrameworkDetail> =>
    apiFetch(`/api/admin/frameworks/${frameworkId}/nodes/${nodeId}`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(handle),

  /** Upload a single framework pack Excel (auto-detect). Prefer the bundle import for many packs. */
  importFramework: async (
    file: File,
    opts?: { typeCode?: string; version?: string; dryRun?: boolean },
  ): Promise<FrameworkImportResult> => {
    const body = new FormData()
    if (opts?.typeCode) body.append('typeCode', opts.typeCode)
    if (opts?.version) body.append('version', opts.version)
    body.append('file', file)
    const dryRun = opts?.dryRun ? 'true' : 'false'
    const res = await apiFetch(`/api/admin/frameworks/import?dryRun=${dryRun}`, {
      method: 'POST',
      headers: authHeaders(),
      body,
    })
    if (!res.ok) {
      const text = await res.text()
      try {
        const problem = JSON.parse(text) as {
          detail?: string
          errors?: { row: number; field: string; message: string }[]
        }
        if (problem.errors?.length) {
          const lines = problem.errors
            .map((e) => `Row ${e.row} · ${e.field}: ${e.message}`)
            .join('\n')
          throw new Error((problem.detail ?? 'Import failed') + '\n' + lines)
        }
        throw new Error(problem.detail || text)
      } catch (e) {
        if (e instanceof Error && e.message && !e.message.startsWith('{')) throw e
        throw new Error(text || `${res.status} ${res.statusText}`)
      }
    }
    return res.json()
  },

  validateFramework: (id: number): Promise<FrameworkSummary> =>
    apiFetch(`/api/admin/frameworks/${id}/validate`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  publishFramework: (id: number): Promise<FrameworkSummary> =>
    apiFetch(`/api/admin/frameworks/${id}/publish`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  deleteFrameworkDraft: (id: number): Promise<null> =>
    apiFetch(`/api/admin/frameworks/${id}`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(handle),

  getFrameworkBundleStatus: (): Promise<FrameworkBundleStatus> =>
    apiFetch('/api/admin/frameworks/import-bundle', { headers: authHeaders() }).then(handle),

  importFrameworkBundle: (opts: {
    scope: 'ALL' | 'MODULE' | 'FRAMEWORK'
    moduleFolder?: string
    relativePath?: string
    dryRun?: boolean
  }): Promise<FrameworkBundleImportResult> => {
    const qs = new URLSearchParams({
      scope: opts.scope,
      dryRun: opts.dryRun ? 'true' : 'false',
    })
    if (opts.moduleFolder) qs.set('moduleFolder', opts.moduleFolder)
    if (opts.relativePath) qs.set('relativePath', opts.relativePath)
    return apiFetch(`/api/admin/frameworks/import-bundle?${qs}`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle)
  },

  listFrameworkTenantAssignments: (id: number): Promise<FrameworkTenantAssignment[]> =>
    apiFetch(`/api/admin/frameworks/${id}/tenants`, { headers: authHeaders() }).then(handle),

  updateFrameworkTenantAssignments: (
    id: number,
    tenants: { tenantId: string; assigned: boolean; licenseModule?: boolean; expiresOn?: string | null }[],
  ): Promise<FrameworkTenantAssignment[]> =>
    apiFetch(`/api/admin/frameworks/${id}/tenants`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ tenants }),
    }).then(handle),

  listFrameworkQuestions: (frameworkId: number): Promise<FrameworkQuestion[]> =>
    apiFetch(`/api/admin/frameworks/${frameworkId}/questions`, { headers: authHeaders() }).then(handle),

  downloadFrameworkQuestionsSample: async (frameworkId: number): Promise<void> => {
    const res = await apiFetch(`/api/admin/frameworks/${frameworkId}/questions/sample.xlsx`, {
      headers: authHeaders(),
    })
    if (!res.ok) {
      const text = await res.text()
      throw new Error(text || `${res.status} ${res.statusText}`)
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `framework-${frameworkId}-questions-sample.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  },

  importFrameworkQuestions: async (
    frameworkId: number,
    file: File,
  ): Promise<FrameworkQuestionImportResult> => {
    const body = new FormData()
    body.append('file', file)
    const res = await apiFetch(`/api/admin/frameworks/${frameworkId}/questions/import`, {
      method: 'POST',
      headers: authHeaders(),
      body,
    })
    if (!res.ok) {
      const text = await res.text()
      try {
        const problem = JSON.parse(text) as { detail?: string }
        throw new Error(problem.detail || text)
      } catch (e) {
        if (e instanceof Error && e.message && !e.message.startsWith('{')) throw e
        throw new Error(text || `${res.status} ${res.statusText}`)
      }
    }
    return res.json()
  },

  listFrameworkRiskLibrary: (frameworkId: number): Promise<RiskTemplate[]> =>
    apiFetch(`/api/admin/frameworks/${frameworkId}/risk-library`, { headers: authHeaders() }).then(handle),

  downloadFrameworkRiskLibrarySample: async (frameworkId: number): Promise<void> => {
    const res = await apiFetch(`/api/admin/frameworks/${frameworkId}/risk-library/sample.xlsx`, {
      headers: authHeaders(),
    })
    if (!res.ok) {
      const text = await res.text()
      throw new Error(text || `${res.status} ${res.statusText}`)
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `framework-${frameworkId}-risk-library-sample.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  },

  importFrameworkRiskLibrary: async (
    frameworkId: number,
    file: File,
  ): Promise<FrameworkQuestionImportResult> => {
    const body = new FormData()
    body.append('file', file)
    const res = await apiFetch(`/api/admin/frameworks/${frameworkId}/risk-library/import`, {
      method: 'POST',
      headers: authHeaders(),
      body,
    })
    if (!res.ok) {
      const text = await res.text()
      try {
        const problem = JSON.parse(text) as { detail?: string }
        throw new Error(problem.detail || text)
      } catch (e) {
        if (e instanceof Error && e.message && !e.message.startsWith('{')) throw e
        throw new Error(text || `${res.status} ${res.statusText}`)
      }
    }
    return res.json()
  },

  listGapItemQuestions: (
    assessmentId: number,
    itemId: number,
  ): Promise<FrameworkQuestion[]> =>
    apiFetch(`/api/gap-assessments/${assessmentId}/items/${itemId}/questions`, {
      headers: authHeaders(),
    }).then(handle),

  listGapItemAnswers: (
    assessmentId: number,
    itemId: number,
  ): Promise<GapQuestionAnswer[]> =>
    apiFetch(`/api/gap-assessments/${assessmentId}/items/${itemId}/answers`, {
      headers: authHeaders(),
    }).then(handle),

  upsertGapItemAnswers: (
    assessmentId: number,
    itemId: number,
    answers: {
      questionId?: number | null
      customQuestionId?: number | null
      answer?: string | null
      note?: string | null
    }[],
  ): Promise<GapQuestionAnswer[]> =>
    apiFetch(`/api/gap-assessments/${assessmentId}/items/${itemId}/answers`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ answers }),
    }).then(handle),

  listGapCustomQuestions: (
    assessmentId: number,
    itemId: number,
  ): Promise<GapCustomQuestion[]> =>
    apiFetch(`/api/gap-assessments/${assessmentId}/items/${itemId}/custom-questions`, {
      headers: authHeaders(),
    }).then(handle),

  addGapCustomQuestion: (
    assessmentId: number,
    itemId: number,
    questionText: string,
  ): Promise<GapCustomQuestion> =>
    apiFetch(`/api/gap-assessments/${assessmentId}/items/${itemId}/custom-questions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ questionText }),
    }).then(handle),

  listAvailableFrameworks: (): Promise<FrameworkSummary[]> =>
    apiFetch('/api/frameworks/available', { headers: authHeaders() }).then(handle),

  listActivatedFrameworks: (): Promise<FrameworkSummary[]> =>
    apiFetch('/api/frameworks/activated', { headers: authHeaders() }).then(handle),

  activateFramework: (id: number, opts?: { includeCloud?: boolean }): Promise<FrameworkSummary> =>
    apiFetch(`/api/frameworks/${id}/activate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ includeCloud: opts?.includeCloud === true }),
    }).then(handle),

  deactivateFramework: (id: number): Promise<null> =>
    apiFetch(`/api/frameworks/${id}/deactivate`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  /** Read-only question bank for one control: audit "what to test" guidance. */
  listFrameworkNodeQuestions: (
    frameworkId: number,
    nodeId: number,
  ): Promise<FrameworkQuestion[]> =>
    apiFetch(`/api/frameworks/${frameworkId}/nodes/${nodeId}/questions`, {
      headers: authHeaders(),
    }).then(handle),

  listCrossFrameworkMappingsForNode: (nodeId: number): Promise<CrossFrameworkMapping[]> =>
    apiFetch(`/api/frameworks/nodes/${nodeId}/cross-mappings`, { headers: authHeaders() }).then(handle),

  /**
   * @param routing narrows nodes to the ones the framework routes to that module. Omit it for the
   *   framework viewer, which should show the whole tree.
   */
  getTenantFramework: (id: number, routing?: ModuleRouting): Promise<FrameworkDetail> =>
    apiFetch(`/api/frameworks/${id}${routing ? `?routing=${routing}` : ''}`, {
      headers: authHeaders(),
    }).then(handle),

  // --- BCMS: department profiles ---
  listDeptProfiles: (): Promise<BcmsDeptProfile[]> =>
    apiFetch('/api/bcms/dept-profiles', { headers: authHeaders() }).then(handle),

  getDeptProfile: (id: number): Promise<BcmsDeptProfile> =>
    apiFetch(`/api/bcms/dept-profiles/${id}`, { headers: authHeaders() }).then(handle),

  getDeptProfileByOrgUnit: (orgUnitId: number): Promise<BcmsDeptProfile> =>
    apiFetch(`/api/bcms/dept-profiles/by-org-unit/${orgUnitId}`, { headers: authHeaders() }).then(handle),

  upsertDeptProfile: (body: UpsertDeptProfileBody): Promise<BcmsDeptProfile> =>
    apiFetch('/api/bcms/dept-profiles', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  // --- BCMS: business impact analyses (2026 pack) ---
  listBiaProjects: (): Promise<BiaProjectRecord[]> =>
    apiFetch('/api/bcms/bia/projects', { headers: authHeaders() }).then(handle),

  getBiaProject: (id: number): Promise<BiaProjectRecord> =>
    apiFetch(`/api/bcms/bia/projects/${id}`, { headers: authHeaders() }).then(handle),

  createBiaProject: (body: CreateBiaProjectBody): Promise<BiaProjectRecord> =>
    apiFetch('/api/bcms/bia/projects', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  listBiaCycles: (params?: { biaProjectId?: number; orgUnitId?: number; status?: BiaStatus }): Promise<BiaCycleRecord[]> => {
    const qs = new URLSearchParams()
    if (params?.biaProjectId != null) qs.set('biaProjectId', String(params.biaProjectId))
    if (params?.orgUnitId != null) qs.set('orgUnitId', String(params.orgUnitId))
    if (params?.status) qs.set('status', params.status)
    const q = qs.toString()
    return apiFetch(`/api/bcms/bia/cycles${q ? `?${q}` : ''}`, { headers: authHeaders() }).then(handle)
  },

  getBiaCycle: (id: number): Promise<BiaCycleRecord> =>
    apiFetch(`/api/bcms/bia/cycles/${id}`, { headers: authHeaders() }).then(handle),

  createBiaCycle: (body: CreateBiaCycleBody): Promise<BiaCycleRecord> =>
    apiFetch('/api/bcms/bia/cycles', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updateBiaCycle: (id: number, body: UpdateBiaCycleBody): Promise<BiaCycleRecord> =>
    apiFetch(`/api/bcms/bia/cycles/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  assignBiaCycle: (id: number, body: { userId: number; dueDate?: string | null }): Promise<BiaCycleRecord> =>
    apiFetch(`/api/bcms/bia/cycles/${id}/assign`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  submitBiaCycle: (id: number): Promise<BiaCycleRecord> =>
    apiFetch(`/api/bcms/bia/cycles/${id}/submit`, { method: 'POST', headers: authHeaders() }).then(handle),

  sendBiaToHead: (id: number, body: { bcHeadUserId: number; notes?: string | null }): Promise<BiaCycleRecord> =>
    apiFetch(`/api/bcms/bia/cycles/${id}/send-to-head`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  headVerifyBia: (id: number, processIds: number[]): Promise<BiaCycleRecord> =>
    apiFetch(`/api/bcms/bia/cycles/${id}/head-verify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ processIds }),
    }).then(handle),

  headApproveBia: (id: number, body?: { notes?: string | null }): Promise<BiaCycleRecord> =>
    apiFetch(`/api/bcms/bia/cycles/${id}/head-approve`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body ?? {}),
    }).then(handle),

  headReturnBia: (id: number, body: { notes: string }): Promise<BiaCycleRecord> =>
    apiFetch(`/api/bcms/bia/cycles/${id}/head-return`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  closeBiaCycle: (id: number): Promise<BiaCycleRecord> =>
    apiFetch(`/api/bcms/bia/cycles/${id}/close`, { method: 'POST', headers: authHeaders() }).then(handle),

  createBiaProcess: (cycleId: number, body: UpsertBiaProcessBody): Promise<BiaProcessRecord> =>
    apiFetch(`/api/bcms/bia/cycles/${cycleId}/processes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updateBiaProcess: (id: number, body: UpsertBiaProcessBody): Promise<BiaProcessRecord> =>
    apiFetch(`/api/bcms/bia/processes/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  deleteBiaProcess: (id: number): Promise<BiaCycleRecord> =>
    apiFetch(`/api/bcms/bia/processes/${id}`, { method: 'DELETE', headers: authHeaders() }).then(handle),

  saveBiaImpact: (id: number, cells: BiaImpactCell[]): Promise<BiaProcessRecord> =>
    apiFetch(`/api/bcms/bia/processes/${id}/impact`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ cells }),
    }).then(handle),

  saveBiaMor: (id: number, body: SaveBiaMorBody): Promise<BiaProcessRecord> =>
    apiFetch(`/api/bcms/bia/processes/${id}/mor`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  reviewBiaProcess: (id: number, body: { outcome: string; comment?: string | null }): Promise<BiaProcessRecord> =>
    apiFetch(`/api/bcms/bia/processes/${id}/review`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  listApprovedBiaProcesses: (): Promise<BiaProcessRecord[]> =>
    apiFetch('/api/bcms/bia/processes/approved', { headers: authHeaders() }).then(handle),

  biaLookups: (): Promise<BiaLookups> =>
    apiFetch('/api/bcms/bia/lookups', { headers: authHeaders() }).then(handle),

  downloadBiaTemplate: async (cycleId?: number): Promise<void> => {
    const path = cycleId != null
      ? `/api/bcms/bia/cycles/${cycleId}/template.xlsx`
      : '/api/bcms/bia/template.xlsx'
    const res = await apiFetch(path, { headers: authHeaders() })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const disp = res.headers.get('Content-Disposition')
    const match = disp?.match(/filename="?([^"]+)"?/i)
    a.download = match?.[1] ?? (cycleId != null ? `bia-${cycleId}-process-template.xlsx` : 'bia-process-template.xlsx')
    a.click()
    URL.revokeObjectURL(url)
  },

  downloadBiaExport: async (cycleId: number): Promise<void> => {
    const res = await apiFetch(`/api/bcms/bia/cycles/${cycleId}/export.xlsx`, { headers: authHeaders() })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `bia-${cycleId}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  },

  importBiaCycle: async (cycleId: number, file: File): Promise<BiaCycleRecord> => {
    const form = new FormData()
    form.append('file', file)
    const res = await apiFetch(`/api/bcms/bia/cycles/${cycleId}/import.xlsx`, {
      method: 'POST',
      headers: authHeaders(),
      body: form,
    })
    return handle(res)
  },

  importBiaProcesses: async (cycleId: number, file: File): Promise<BiaProcessImportResult> => {
    const form = new FormData()
    form.append('file', file)
    const res = await apiFetch(`/api/bcms/bia/cycles/${cycleId}/processes/import.xlsx`, {
      method: 'POST',
      headers: authHeaders(),
      body: form,
    })
    return handle(res)
  },

  // --- BCMS: recovery plans ---
  listRecoveryPlans: (projectId?: number): Promise<RecoveryPlan[]> =>
    apiFetch(`/api/bcms/recovery-plans${projectId != null ? `?projectId=${projectId}` : ''}`, {
      headers: authHeaders(),
    }).then(handle),

  getRecoveryPlan: (id: number): Promise<RecoveryPlan> =>
    apiFetch(`/api/bcms/recovery-plans/${id}`, { headers: authHeaders() }).then(handle),

  createRecoveryPlan: (body: CreateRecoveryPlanBody): Promise<RecoveryPlan> =>
    apiFetch('/api/bcms/recovery-plans', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updateRecoveryPlan: (id: number, body: UpdateRecoveryPlanBody): Promise<RecoveryPlan> =>
    apiFetch(`/api/bcms/recovery-plans/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  submitRecoveryPlan: (id: number): Promise<RecoveryPlan> =>
    apiFetch(`/api/bcms/recovery-plans/${id}/submit`, { method: 'POST', headers: authHeaders() }).then(handle),

  approveRecoveryPlan: (id: number): Promise<RecoveryPlan> =>
    apiFetch(`/api/bcms/recovery-plans/${id}/approve`, { method: 'POST', headers: authHeaders() }).then(handle),

  downloadRecoveryPlanPdf: async (id: number): Promise<void> => {
    const res = await apiFetch(`/api/bcms/recovery-plans/${id}/pdf`, { headers: authHeaders() })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const disp = res.headers.get('Content-Disposition') ?? ''
    const match = /filename="?([^"]+)"?/.exec(disp)
    const name = match?.[1] ?? `recovery-plan-${id}.pdf`
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = name
    a.click()
    URL.revokeObjectURL(url)
  },

  // --- BCMS: exercises ---
  listExercises: (projectId?: number): Promise<BcmsExercise[]> =>
    apiFetch(`/api/bcms/exercises${projectId != null ? `?projectId=${projectId}` : ''}`, {
      headers: authHeaders(),
    }).then(handle),

  getExercise: (id: number): Promise<BcmsExercise> =>
    apiFetch(`/api/bcms/exercises/${id}`, { headers: authHeaders() }).then(handle),

  createExercise: (body: CreateExerciseBody): Promise<BcmsExercise> =>
    apiFetch('/api/bcms/exercises', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updateExercise: (id: number, body: UpdateExerciseBody): Promise<BcmsExercise> =>
    apiFetch(`/api/bcms/exercises/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  completeExercise: (id: number, body: CompleteExerciseBody): Promise<BcmsExercise> =>
    apiFetch(`/api/bcms/exercises/${id}/complete`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  cancelExercise: (id: number): Promise<BcmsExercise> =>
    apiFetch(`/api/bcms/exercises/${id}/cancel`, { method: 'POST', headers: authHeaders() }).then(handle),

  // --- Policy repository (platform always-on) ---
  listPolicyCategories: (): Promise<PolicyCategory[]> =>
    apiFetch('/api/policies/categories', { headers: authHeaders() }).then(handle),

  listPolicies: (params?: { category?: PolicyCategory; status?: PolicyStatus }): Promise<PolicySummary[]> => {
    const qs = new URLSearchParams()
    if (params?.category) qs.set('category', params.category)
    if (params?.status) qs.set('status', params.status)
    const q = qs.toString()
    return apiFetch(`/api/policies${q ? `?${q}` : ''}`, { headers: authHeaders() }).then(handle)
  },

  getPolicy: (id: number): Promise<PolicyDetail> =>
    apiFetch(`/api/policies/${id}`, { headers: authHeaders() }).then(handle),

  createPolicy: (body: CreatePolicyBody): Promise<PolicyDetail> =>
    apiFetch('/api/policies', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updatePolicy: (id: number, body: UpdatePolicyBody): Promise<PolicyDetail> =>
    apiFetch(`/api/policies/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  uploadPolicyFile: async (id: number, file: File): Promise<PolicyDetail> => {
    const body = new FormData()
    body.append('file', file)
    return apiFetch(`/api/policies/${id}/file`, { method: 'POST', headers: authHeaders(), body }).then(handle)
  },

  downloadPolicyFile: async (id: number, filenameHint?: string): Promise<void> => {
    const res = await apiFetch(`/api/policies/${id}/file`, { headers: authHeaders() })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const disp = res.headers.get('Content-Disposition') ?? ''
    const match = /filename="?([^"]+)"?/.exec(disp)
    const name = match?.[1] ?? filenameHint ?? `policy-${id}`
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = name
    a.click()
    URL.revokeObjectURL(url)
  },

  submitPolicy: (id: number): Promise<PolicyDetail> =>
    apiFetch(`/api/policies/${id}/submit`, { method: 'POST', headers: authHeaders() }).then(handle),

  recommendPolicy: (id: number): Promise<PolicyDetail> =>
    apiFetch(`/api/policies/${id}/recommend`, { method: 'POST', headers: authHeaders() }).then(handle),

  rejectPolicy: (id: number, reason: string): Promise<PolicyDetail> =>
    apiFetch(`/api/policies/${id}/reject`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ reason }),
    }).then(handle),

  approvePolicy: (id: number): Promise<PolicyDetail> =>
    apiFetch(`/api/policies/${id}/approve`, { method: 'POST', headers: authHeaders() }).then(handle),

  acknowledgePolicy: (id: number): Promise<PolicyDetail> =>
    apiFetch(`/api/policies/${id}/acknowledge`, { method: 'POST', headers: authHeaders() }).then(handle),

  listPolicyVersions: (id: number): Promise<PolicyVersion[]> =>
    apiFetch(`/api/policies/${id}/versions`, { headers: authHeaders() }).then(handle),

  listPolicyAcknowledgements: (id: number): Promise<PolicyAcknowledgement[]> =>
    apiFetch(`/api/policies/${id}/acknowledgements`, { headers: authHeaders() }).then(handle),

  exportPolicyAcknowledgementsCsv: async (id: number): Promise<void> => {
    const res = await apiFetch(`/api/policies/${id}/acknowledgements/export`, { headers: authHeaders() })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `policy-${id}-acks.csv`
    a.click()
    URL.revokeObjectURL(url)
  },

  // --- Dashboard & Reports (Sprint 8, platform always-on) ---
  getDashboardSummary: (): Promise<DashboardSummary> =>
    apiFetch('/api/dashboard/summary', { headers: authHeaders() }).then(handle),

  getContributorDashboard: (): Promise<ContributorDashboard> =>
    apiFetch('/api/dashboard/contributor', { headers: authHeaders() }).then(handle),

  listReports: (): Promise<ReportDescriptor[]> =>
    apiFetch('/api/reports', { headers: authHeaders() }).then(handle),

  exportReport: async (
    id: string,
    format: string,
    params?: { soaId?: number; gapId?: number; auditId?: number; exerciseId?: number; variant?: string },
  ): Promise<void> => {
    const qs = new URLSearchParams({ format })
    if (params?.soaId != null) qs.set('soaId', String(params.soaId))
    if (params?.gapId != null) qs.set('gapId', String(params.gapId))
    if (params?.auditId != null) qs.set('auditId', String(params.auditId))
    if (params?.exerciseId != null) qs.set('exerciseId', String(params.exerciseId))
    if (params?.variant) qs.set('variant', params.variant)
    const res = await apiFetch(`/api/reports/${id}/export?${qs}`, { headers: authHeaders() })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const disp = res.headers.get('Content-Disposition') ?? ''
    const match = /filename="?([^"]+)"?/.exec(disp)
    const name = match?.[1] ?? `${id}.${format}`
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = name
    a.click()
    URL.revokeObjectURL(url)
  },

  // --- TPRM (Third Party Risk Management, included in ERM licence) ---
  listTprmLookups: (): Promise<TprmLookups> =>
    apiFetch('/api/tprm/lookups', { headers: authHeaders() }).then(handle),

  listTprmKpis: (): Promise<TprmKpis> =>
    apiFetch('/api/tprm/kpis', { headers: authHeaders() }).then(handle),

  listTprmVendors: (): Promise<TprmVendor[]> =>
    apiFetch('/api/tprm/vendors', { headers: authHeaders() }).then(handle),

  getTprmVendor: (id: number): Promise<TprmVendor> =>
    apiFetch(`/api/tprm/vendors/${id}`, { headers: authHeaders() }).then(handle),

  createTprmVendor: (body: UpsertVendorBody): Promise<TprmVendor> =>
    apiFetch('/api/tprm/vendors', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updateTprmVendor: (id: number, body: UpsertVendorBody): Promise<TprmVendor> =>
    apiFetch(`/api/tprm/vendors/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  generateTprmQuestionnaire: (vendorId: number): Promise<TprmAssessment> =>
    apiFetch(`/api/tprm/vendors/${vendorId}/questionnaire`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  issueTprmAssessment: (vendorId: number, body: CreateAssessmentBody): Promise<TprmAssessment> =>
    apiFetch(`/api/tprm/vendors/${vendorId}/assessments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  listTprmAssessments: (vendorId?: number): Promise<TprmAssessment[]> =>
    apiFetch(`/api/tprm/assessments${vendorId != null ? `?vendorId=${vendorId}` : ''}`, {
      headers: authHeaders(),
    }).then(handle),

  listTprmQuestionnaires: (): Promise<TprmQuestionnaire[]> =>
    apiFetch('/api/tprm/questionnaires', { headers: authHeaders() }).then(handle),

  getTprmQuestionnaire: (id: number): Promise<TprmQuestionnaire> =>
    apiFetch(`/api/tprm/questionnaires/${id}`, { headers: authHeaders() }).then(handle),

  createTprmQuestionnaire: (name: string): Promise<TprmQuestionnaire> =>
    apiFetch('/api/tprm/questionnaires', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ name }),
    }).then(handle),

  saveTprmQuestionnaire: (
    id: number,
    body: {
      name: string
      items?: {
        id?: number
        controlCode: string
        domain: string
        requirement: string
        requiredEvidence: string
        evidenceMandatory: boolean
      }[]
      draft?: boolean
    },
  ): Promise<TprmQuestionnaire> =>
    apiFetch(`/api/tprm/questionnaires/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  renameTprmQuestionnaire: (id: number, name: string): Promise<TprmQuestionnaire> =>
    apiFetch(`/api/tprm/questionnaires/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ name }),
    }).then(handle),

  downloadTprmQuestionnaireTemplate: async (id?: number): Promise<void> => {
    const path = id != null ? `/api/tprm/questionnaires/${id}/sample.xlsx` : '/api/tprm/questionnaires/sample.xlsx'
    const res = await apiFetch(path, { headers: authHeaders() })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = id != null ? `questionnaire-${id}-template.xlsx` : 'tprm-questionnaire-template.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  },

  downloadTprmQuestionnaireSample: async (id?: number): Promise<void> => {
    const path = id != null ? `/api/tprm/questionnaires/${id}/sample.xlsx` : '/api/tprm/questionnaires/sample.xlsx'
    const res = await apiFetch(path, { headers: authHeaders() })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = id != null ? `questionnaire-${id}-template.xlsx` : 'tprm-questionnaire-template.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  },

  uploadTprmQuestionnaireLibrary: (id: number, file: File): Promise<TprmQuestionnaire> => {
    const fd = new FormData()
    fd.append('file', file)
    return apiFetch(`/api/tprm/questionnaires/${id}/upload`, {
      method: 'POST',
      headers: authHeaders(),
      body: fd,
    }).then(handle)
  },

  listTprmProjects: (): Promise<TprmAssessment[]> =>
    apiFetch('/api/tprm/projects', { headers: authHeaders() }).then(handle),

  createTprmProject: (body: { name: string; description?: string | null; vendorId: number; questionnaireId: number }): Promise<TprmAssessment> =>
    apiFetch('/api/tprm/projects', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updateTprmProject: (id: number, body: { vendorId: number; questionnaireId: number }): Promise<TprmAssessment> =>
    apiFetch(`/api/tprm/projects/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  issueTprmProject: (id: number, body: CreateAssessmentBody): Promise<TprmAssessment> =>
    apiFetch(`/api/tprm/projects/${id}/issue`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  addTprmCollaborator: (id: number, body: { name?: string; email: string }): Promise<TprmAssessment> =>
    apiFetch(`/api/tprm/projects/${id}/collaborators`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  downloadTprmProjectExport: async (id: number, format: 'xlsx' | 'pdf'): Promise<void> => {
    const res = await apiFetch(`/api/tprm/projects/${id}/export.${format}`, { headers: authHeaders() })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `tprm-project-${id}.${format}`
    a.click()
    URL.revokeObjectURL(url)
  },

  getTprmAssessment: (id: number): Promise<TprmAssessment> =>
    apiFetch(`/api/tprm/assessments/${id}`, { headers: authHeaders() }).then(handle),

  downloadTprmQuestionnaire: async (id: number): Promise<void> => {
    const res = await apiFetch(`/api/tprm/assessments/${id}/questionnaire.xlsx`, { headers: authHeaders() })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `questionnaire-${id}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  },

  uploadTprmQuestionnaire: (id: number, file: File): Promise<TprmAssessment> => {
    const fd = new FormData()
    fd.append('file', file)
    return apiFetch(`/api/tprm/assessments/${id}/questionnaire.xlsx`, {
      method: 'POST',
      headers: authHeaders(),
      body: fd,
    }).then(handle)
  },

  reviewTprmAssessment: (id: number, body: ReviewAssessmentBody): Promise<TprmAssessment> =>
    apiFetch(`/api/tprm/assessments/${id}/review`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  raiseTprmErm: (id: number): Promise<RiskSummary> =>
    apiFetch(`/api/tprm/assessments/${id}/raise-erm`, { method: 'POST', headers: authHeaders() }).then(handle),

  listTprmFindings: (vendorId?: number): Promise<TprmFinding[]> =>
    apiFetch(`/api/tprm/findings${vendorId != null ? `?vendorId=${vendorId}` : ''}`, {
      headers: authHeaders(),
    }).then(handle),

  updateTprmFinding: (id: number, status: FindingStatus): Promise<TprmFinding> =>
    apiFetch(`/api/tprm/findings/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ status }),
    }).then(handle),

  // --- Privacy (Inventory / ROPA / DSR, ModuleCode.PRIVACY) ---

  listPrivacyInventory: (): Promise<PrivacyInventory[]> =>
    apiFetch('/api/privacy/inventory', { headers: authHeaders() }).then(handle),

  createPrivacyInventory: (body: CreatePrivacyInventoryBody): Promise<PrivacyInventory> =>
    apiFetch('/api/privacy/inventory', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updatePrivacyInventory: (id: number, body: CreatePrivacyInventoryBody): Promise<PrivacyInventory> =>
    apiFetch(`/api/privacy/inventory/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  archivePrivacyInventory: (id: number): Promise<PrivacyInventory> =>
    apiFetch(`/api/privacy/inventory/${id}/archive`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  listPrivacyRopa: (): Promise<PrivacyRopa[]> =>
    apiFetch('/api/privacy/ropa', { headers: authHeaders() }).then(handle),

  downloadPrivacyRopaTemplate: async (): Promise<void> => {
    const res = await apiFetch('/api/privacy/ropa/template.xlsx', { headers: authHeaders() })
    if (!res.ok) throw new Error(await res.text())
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'arbor-ropa-template.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  },

  listRopaProjects: (): Promise<RopaProject[]> =>
    apiFetch('/api/privacy/ropa-projects', { headers: authHeaders() }).then(handle),

  createRopaProject: (body: { name: string; year?: number | null }): Promise<RopaProject> =>
    apiFetch('/api/privacy/ropa-projects', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  getRopaProject: (id: number): Promise<RopaProject> =>
    apiFetch(`/api/privacy/ropa-projects/${id}`, { headers: authHeaders() }).then(handle),

  completeRopaProject: (id: number): Promise<RopaProject> =>
    apiFetch(`/api/privacy/ropa-projects/${id}/complete`, { method: 'POST', headers: authHeaders() }).then(handle),

  addRopaDepartment: (
    projectId: number,
    body: { orgUnitId: number; departmentHeadUserId: number; championUserId: number },
  ): Promise<RopaDepartment> =>
    apiFetch(`/api/privacy/ropa-projects/${projectId}/departments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  assignRopaDepartment: (
    departmentId: number,
    body: { departmentHeadUserId: number; championUserId: number },
  ): Promise<RopaDepartment> =>
    apiFetch(`/api/privacy/ropa-projects/departments/${departmentId}/assign`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  listRopaProcesses: (departmentId: number): Promise<PrivacyRopa[]> =>
    apiFetch(`/api/privacy/ropa-projects/departments/${departmentId}/processes`, { headers: authHeaders() }).then(handle),

  downloadRopaProcessTemplate: async (departmentId: number): Promise<void> => {
    const res = await apiFetch(
      `/api/privacy/ropa-projects/departments/${departmentId}/processes/template.xlsx`,
      { headers: authHeaders() },
    )
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const disp = res.headers.get('Content-Disposition')
    const match = disp?.match(/filename="?([^"]+)"?/i)
    a.download = match?.[1] ?? `ropa-${departmentId}-process-template.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  },

  importRopaProcesses: async (departmentId: number, file: File): Promise<RopaProcessImportResult> => {
    const form = new FormData()
    form.append('file', file)
    const res = await apiFetch(
      `/api/privacy/ropa-projects/departments/${departmentId}/processes/import.xlsx`,
      {
        method: 'POST',
        headers: authHeaders(),
        body: form,
      },
    )
    return handle(res)
  },

  createRopaProcess: (departmentId: number, body: CreatePrivacyRopaBody): Promise<PrivacyRopa> =>
    apiFetch(`/api/privacy/ropa-projects/departments/${departmentId}/processes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updateRopaProcess: (departmentId: number, processId: number, body: CreatePrivacyRopaBody): Promise<PrivacyRopa> =>
    apiFetch(`/api/privacy/ropa-projects/departments/${departmentId}/processes/${processId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  submitRopaDepartment: (departmentId: number): Promise<RopaDepartment> =>
    apiFetch(`/api/privacy/ropa-projects/departments/${departmentId}/submit`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  reviewRopaProcess: (
    departmentId: number,
    processId: number,
    body: {
      /** Ignored by API; DPIA required is derived from the four criteria. */
      dpiaRequired?: boolean
      dpiaComments?: string | null
      comments?: string | null
      outcome: 'REVIEWED' | 'RETURNED'
      dpiaCritLargeSensitive?: boolean | null
      dpiaCritSystematicAssessment?: boolean | null
      dpiaCritAutomatedProfiling?: boolean | null
      dpiaCritLegalSeriousImpact?: boolean | null
    },
  ): Promise<PrivacyRopa> =>
    apiFetch(`/api/privacy/ropa-projects/departments/${departmentId}/processes/${processId}/review`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  finishRopaReview: (departmentId: number): Promise<RopaDepartment> =>
    apiFetch(`/api/privacy/ropa-projects/departments/${departmentId}/finish-review`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  sendRopaToHead: (departmentId: number): Promise<RopaDepartment> =>
    apiFetch(`/api/privacy/ropa-projects/departments/${departmentId}/send-to-head`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  headReviewRopaProcess: (
    departmentId: number,
    processId: number,
    body: { outcome: 'APPROVED' | 'RETURNED'; comments?: string | null },
  ): Promise<PrivacyRopa> =>
    apiFetch(`/api/privacy/ropa-projects/departments/${departmentId}/processes/${processId}/head-review`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  headApproveRopa: (departmentId: number, notes?: string): Promise<RopaDepartment> =>
    apiFetch(`/api/privacy/ropa-projects/departments/${departmentId}/head-approve`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ notes: notes ?? null }),
    }).then(handle),

  headReturnRopa: (departmentId: number, notes?: string): Promise<RopaDepartment> =>
    apiFetch(`/api/privacy/ropa-projects/departments/${departmentId}/head-return`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ notes: notes ?? null }),
    }).then(handle),

  closeRopaDepartment: (departmentId: number): Promise<RopaDepartment> =>
    apiFetch(`/api/privacy/ropa-projects/departments/${departmentId}/close`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  listDpiaQuestionnaires: (): Promise<DpiaQuestionnaire[]> =>
    apiFetch('/api/privacy/dpia-questionnaires', { headers: authHeaders() }).then(handle),

  getDpiaQuestionnaire: (id: number): Promise<DpiaQuestionnaire> =>
    apiFetch(`/api/privacy/dpia-questionnaires/${id}`, { headers: authHeaders() }).then(handle),

  createDpiaQuestionnaire: (body: { name: string; description?: string | null }): Promise<DpiaQuestionnaire> =>
    apiFetch('/api/privacy/dpia-questionnaires', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updateDpiaQuestionnaire: (
    id: number,
    body: { name: string; description?: string | null; active?: boolean | null },
  ): Promise<DpiaQuestionnaire> =>
    apiFetch(`/api/privacy/dpia-questionnaires/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  addDpiaQuestionnaireQuestion: (id: number, body: UpsertDpiaQuestionBody): Promise<DpiaQuestionnaireQuestion> =>
    apiFetch(`/api/privacy/dpia-questionnaires/${id}/questions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updateDpiaQuestionnaireQuestion: (
    id: number,
    questionId: number,
    body: UpsertDpiaQuestionBody,
  ): Promise<DpiaQuestionnaireQuestion> =>
    apiFetch(`/api/privacy/dpia-questionnaires/${id}/questions/${questionId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  deleteDpiaQuestionnaireQuestion: (id: number, questionId: number): Promise<void> =>
    apiFetch(`/api/privacy/dpia-questionnaires/${id}/questions/${questionId}`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(async (res) => {
      if (!res.ok) await handle(res)
    }),

  deleteDpiaQuestionnaire: (id: number): Promise<void> =>
    apiFetch(`/api/privacy/dpia-questionnaires/${id}`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(async (res) => {
      if (!res.ok) await handle(res)
    }),

  listDpiaProjects: (): Promise<DpiaProject[]> =>
    apiFetch('/api/privacy/dpia-projects', { headers: authHeaders() }).then(handle),

  listDpiaRiskTitleCandidates: (): Promise<DpiaRiskTitleCandidate[]> =>
    apiFetch('/api/privacy/dpia-projects/risk-title-candidates', { headers: authHeaders() }).then(handle),

  getDpiaProject: (id: number): Promise<DpiaProject> =>
    apiFetch(`/api/privacy/dpia-projects/${id}`, { headers: authHeaders() }).then(handle),

  createDpiaProject: (body: {
    name: string
    year?: number | null
    linkedRopaProjectId?: number | null
  }): Promise<DpiaProject> =>
    apiFetch('/api/privacy/dpia-projects', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  completeDpiaProject: (id: number): Promise<DpiaProject> =>
    apiFetch(`/api/privacy/dpia-projects/${id}/complete`, { method: 'POST', headers: authHeaders() }).then(handle),

  addDpiaDepartment: (
    projectId: number,
    body: {
      orgUnitId: number
      reasonDescription?: string | null
      departmentHeadUserId?: number | null
      championUserId?: number | null
      dueDate?: string | null
      questionnaireId?: number | null
    },
  ): Promise<DpiaDepartment> =>
    apiFetch(`/api/privacy/dpia-projects/${projectId}/departments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  getDpiaDepartment: (departmentId: number): Promise<DpiaDepartment> =>
    apiFetch(`/api/privacy/dpia-projects/departments/${departmentId}`, { headers: authHeaders() }).then(handle),

  assignDpiaDepartment: (
    departmentId: number,
    body: {
      departmentHeadUserId?: number | null
      championUserId?: number | null
      dueDate?: string | null
      questionnaireId?: number | null
    },
  ): Promise<DpiaDepartment> =>
    apiFetch(`/api/privacy/dpia-projects/departments/${departmentId}/assign`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  saveDpiaAnswers: (
    departmentId: number,
    body: {
      answers: Array<{
        questionId: number
        answerText?: string | null
        answerOptions?: string[] | null
        detailsEvidence?: string | null
      }>
    },
  ): Promise<DpiaDepartment> =>
    apiFetch(`/api/privacy/dpia-projects/departments/${departmentId}/answers`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  submitDpiaDepartment: (departmentId: number): Promise<DpiaDepartment> =>
    apiFetch(`/api/privacy/dpia-projects/departments/${departmentId}/submit`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  reviewDpiaDepartment: (
    departmentId: number,
    body: { outcome: 'ACCEPT' | 'RETURN'; comments?: string | null },
  ): Promise<DpiaDepartment> =>
    apiFetch(`/api/privacy/dpia-projects/departments/${departmentId}/review`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  saveDpiaAssessment: (
    departmentId: number,
    body: {
      rows: Array<{
        id: number
        complianceStatus?: string | null
        remarks?: string | null
        recommendedMeasures?: string | null
        riskTitle?: string | null
      }>
    },
  ): Promise<DpiaDepartment> =>
    apiFetch(`/api/privacy/dpia-projects/departments/${departmentId}/assessment`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  finalizeDpiaAssessment: (departmentId: number): Promise<DpiaDepartment> =>
    apiFetch(`/api/privacy/dpia-projects/departments/${departmentId}/finalize-assessment`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  headApproveDpia: (departmentId: number, notes?: string): Promise<DpiaDepartment> =>
    apiFetch(`/api/privacy/dpia-projects/departments/${departmentId}/head-approve`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ notes: notes ?? null }),
    }).then(handle),

  headReturnDpia: (departmentId: number, notes?: string): Promise<DpiaDepartment> =>
    apiFetch(`/api/privacy/dpia-projects/departments/${departmentId}/head-return`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ notes: notes ?? null }),
    }).then(handle),

  closeDpiaDepartment: (departmentId: number): Promise<DpiaDepartment> =>
    apiFetch(`/api/privacy/dpia-projects/departments/${departmentId}/close`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  createPrivacyRopa: (body: CreatePrivacyRopaBody): Promise<PrivacyRopa> =>
    apiFetch('/api/privacy/ropa', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updatePrivacyRopa: (id: number, body: CreatePrivacyRopaBody): Promise<PrivacyRopa> =>
    apiFetch(`/api/privacy/ropa/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  assignPrivacyRopa: (
    id: number,
    body: { userId: number; dueDate?: string | null },
  ): Promise<PrivacyRopa> =>
    apiFetch(`/api/privacy/ropa/${id}/assign`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  submitPrivacyRopaReview: (id: number): Promise<PrivacyRopa> =>
    apiFetch(`/api/privacy/ropa/${id}/submit-review`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  approvePrivacyRopa: (id: number): Promise<PrivacyRopa> =>
    apiFetch(`/api/privacy/ropa/${id}/approve`, { method: 'POST', headers: authHeaders() }).then(handle),

  rejectPrivacyRopa: (id: number, notes: string): Promise<PrivacyRopa> =>
    apiFetch(`/api/privacy/ropa/${id}/reject`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ notes }),
    }).then(handle),

  submitRopaTask: (taskId: number): Promise<WorkTaskDetail> =>
    apiFetch(`/api/my-tasks/${taskId}/submit-ropa`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  listPrivacyDsr: (): Promise<PrivacyDsr[]> =>
    apiFetch('/api/privacy/dsr', { headers: authHeaders() }).then(handle),

  createPrivacyDsr: (body: CreatePrivacyDsrBody): Promise<PrivacyDsr> =>
    apiFetch('/api/privacy/dsr', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updatePrivacyDsr: (id: number, body: UpdatePrivacyDsrBody): Promise<PrivacyDsr> =>
    apiFetch(`/api/privacy/dsr/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  startPrivacyDsr: (id: number): Promise<PrivacyDsr> =>
    apiFetch(`/api/privacy/dsr/${id}/start`, { method: 'POST', headers: authHeaders() }).then(handle),

  completePrivacyDsr: (id: number): Promise<PrivacyDsr> =>
    apiFetch(`/api/privacy/dsr/${id}/complete`, { method: 'POST', headers: authHeaders() }).then(handle),

  rejectPrivacyDsr: (id: number, rejectionReason: string): Promise<PrivacyDsr> =>
    apiFetch(`/api/privacy/dsr/${id}/reject`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ rejectionReason }),
    }).then(handle),

  runPrivacyDsrLifecycle: (asOf?: string): Promise<PrivacyDsrLifecycleResult> =>
    apiFetch(`/api/privacy/dsr/lifecycle/run${asOf ? `?asOf=${encodeURIComponent(asOf)}` : ''}`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  listPrivacyDpia: (): Promise<PrivacyDpia[]> =>
    apiFetch('/api/privacy/dpia', { headers: authHeaders() }).then(handle),

  getPrivacyDpiaMethodologyBank: (): Promise<DpiaMethodologyBank> =>
    apiFetch('/api/privacy/dpia/methodology-bank', { headers: authHeaders() }).then(handle),

  downloadPrivacyDpiaTemplate: async (): Promise<void> => {
    const res = await apiFetch('/api/privacy/dpia/template.xlsx', { headers: authHeaders() })
    if (!res.ok) throw new Error(await res.text())
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'arbor-dpia-template.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  },

  listActivityTypes: (): Promise<ActivityTypeInfo[]> =>
    apiFetch('/api/activity-types', { headers: authHeaders() }).then(handle),

  createPrivacyDpia: (body: CreatePrivacyDpiaBody): Promise<PrivacyDpia> =>
    apiFetch('/api/privacy/dpia', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  updatePrivacyDpia: (id: number, body: CreatePrivacyDpiaBody): Promise<PrivacyDpia> =>
    apiFetch(`/api/privacy/dpia/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  assignPrivacyDpia: (
    id: number,
    body: { userId: number; dueDate?: string | null },
  ): Promise<PrivacyDpia> =>
    apiFetch(`/api/privacy/dpia/${id}/assign`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  submitPrivacyDpiaReview: (id: number): Promise<PrivacyDpia> =>
    apiFetch(`/api/privacy/dpia/${id}/submit-review`, { method: 'POST', headers: authHeaders() }).then(handle),

  approvePrivacyDpia: (id: number): Promise<PrivacyDpia> =>
    apiFetch(`/api/privacy/dpia/${id}/approve`, { method: 'POST', headers: authHeaders() }).then(handle),

  rejectPrivacyDpia: (id: number, notes: string): Promise<PrivacyDpia> =>
    apiFetch(`/api/privacy/dpia/${id}/reject`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ notes }),
    }).then(handle),

  submitDpiaTask: (taskId: number): Promise<WorkTaskDetail> =>
    apiFetch(`/api/my-tasks/${taskId}/submit-dpia`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),

  listPrivacyConsent: (): Promise<PrivacyConsent[]> =>
    apiFetch('/api/privacy/consent', { headers: authHeaders() }).then(handle),

  createPrivacyConsent: (body: CreatePrivacyConsentBody): Promise<PrivacyConsent> =>
    apiFetch('/api/privacy/consent', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  withdrawPrivacyConsent: (id: number): Promise<PrivacyConsent> =>
    apiFetch(`/api/privacy/consent/${id}/withdraw`, { method: 'POST', headers: authHeaders() }).then(handle),

  listPrivacyBreaches: (): Promise<PrivacyBreach[]> =>
    apiFetch('/api/privacy/breaches', { headers: authHeaders() }).then(handle),

  createPrivacyBreach: (body: CreatePrivacyBreachBody): Promise<PrivacyBreach> =>
    apiFetch('/api/privacy/breaches', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    }).then(handle),

  investigatePrivacyBreach: (id: number): Promise<PrivacyBreach> =>
    apiFetch(`/api/privacy/breaches/${id}/investigate`, { method: 'POST', headers: authHeaders() }).then(handle),

  reportPrivacyBreach: (id: number, authorityReference?: string): Promise<PrivacyBreach> =>
    apiFetch(`/api/privacy/breaches/${id}/report`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ authorityReference: authorityReference ?? null }),
    }).then(handle),

  closePrivacyBreach: (id: number): Promise<PrivacyBreach> =>
    apiFetch(`/api/privacy/breaches/${id}/close`, { method: 'POST', headers: authHeaders() }).then(handle),

  runPrivacyBreachLifecycle: (asOf?: string): Promise<PrivacyBreachLifecycleResult> =>
    apiFetch(`/api/privacy/breaches/lifecycle/run${asOf ? `?asOf=${encodeURIComponent(asOf)}` : ''}`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(handle),
}

/** Public vendor portal: the vendor's own scoped JWT is passed explicitly (no admin session involved). */
export const vendorPortalApi = {
  validate: (tenantId: string, token: string): Promise<ValidateVendorAccessResponse> =>
    apiFetch(
      `/api/tprm/vendor-access/validate?tenantId=${encodeURIComponent(tenantId)}&token=${encodeURIComponent(token)}`,
    ).then(handle),

  requestOtp: (tenantId: string, token: string, email: string): Promise<{ previewOtp?: string | null } | null> =>
    apiFetch('/api/tprm/vendor-access/request-otp', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ tenantId, token, email }),
    }).then(handle),

  redeem: (
    tenantId: string,
    token: string,
    otp: string,
  ): Promise<{ token: string; email: string; tenantId: string; roles: string[]; expiresInMinutes: number }> =>
    apiFetch('/api/tprm/vendor-access/redeem', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ tenantId, token, otp }),
    }).then(handle),

  getAssessment: (vendorToken: string): Promise<TprmAssessment> =>
    apiFetch('/api/tprm/vendor-access/assessment', {
      headers: { Authorization: `Bearer ${vendorToken}` },
    }).then(handle),

  saveAnswers: (
    vendorToken: string,
    answers: { itemId: number; vendorAnswer?: TprmAnswer | null; justification?: string | null; evidenceRef?: string | null }[],
  ): Promise<TprmAssessment> =>
    apiFetch('/api/tprm/vendor-access/answers', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${vendorToken}` },
      body: JSON.stringify({ answers }),
    }).then(handle),

  submit: (vendorToken: string): Promise<TprmAssessment> =>
    apiFetch('/api/tprm/vendor-access/submit', {
      method: 'POST',
      headers: { Authorization: `Bearer ${vendorToken}` },
    }).then(handle),

  uploadAnswerEvidence: (vendorToken: string, itemId: number, file: File): Promise<TprmAssessment> => {
    const fd = new FormData()
    fd.append('itemId', String(itemId))
    fd.append('file', file)
    return apiFetch('/api/tprm/vendor-access/answers/evidence', {
      method: 'POST',
      headers: { Authorization: `Bearer ${vendorToken}` },
      body: fd,
    }).then(handle)
  },

  removeAnswerEvidence: (vendorToken: string, itemId: number, evidenceId: number): Promise<TprmAssessment> =>
    apiFetch(
      `/api/tprm/vendor-access/answers/evidence?itemId=${itemId}&evidenceId=${evidenceId}`,
      {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${vendorToken}` },
      },
    ).then(handle),

  fetchAnswerEvidenceBlob: async (
    vendorToken: string,
    itemId: number,
    evidenceId: number,
  ): Promise<{ blob: Blob; filename: string; contentType: string }> => {
    const res = await apiFetch(
      `/api/tprm/vendor-access/answers/evidence/${evidenceId}/file?itemId=${itemId}`,
      { headers: { Authorization: `Bearer ${vendorToken}` } },
    )
    if (!res.ok) {
      await handle(res)
      throw new Error('Evidence download failed')
    }
    const blob = await res.blob()
    const disp = res.headers.get('Content-Disposition') ?? ''
    const match = /filename="([^"]+)"/.exec(disp)
    const filename = match?.[1] ?? `evidence-${evidenceId}`
    const contentType = res.headers.get('Content-Type') ?? blob.type
    return { blob, filename, contentType }
  },

  downloadAnswerEvidence: async (vendorToken: string, itemId: number, evidenceId: number): Promise<void> => {
    const { blob, filename } = await vendorPortalApi.fetchAnswerEvidenceBlob(vendorToken, itemId, evidenceId)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
  },

  downloadQuestionnaire: async (vendorToken: string): Promise<void> => {
    const res = await apiFetch('/api/tprm/vendor-access/questionnaire.xlsx', {
      headers: { Authorization: `Bearer ${vendorToken}` },
    })
    if (!res.ok) {
      await handle(res)
      return
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'questionnaire.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  },

  uploadQuestionnaire: (vendorToken: string, file: File): Promise<TprmAssessment> => {
    const fd = new FormData()
    fd.append('file', file)
    return apiFetch('/api/tprm/vendor-access/questionnaire.xlsx', {
      method: 'POST',
      headers: { Authorization: `Bearer ${vendorToken}` },
      body: fd,
    }).then(handle)
  },
}

export interface ValidateExternalAuditAccessResponse {
  valid: boolean
  status: string
  auditName: string
  inviteeName: string
  inviteeEmail: string
  expiresAt: string
  message: string
}

export interface AuditPortalSession {
  auditId: number
  auditName: string
  frameworkName: string
  frameworkCode: string
  accessExpiresAt: string
  scopeModules: EngagementScopeModule[]
  scopeNodeIds: number[]
  orgUnitIds: number[]
  audit: GrcAuditSummary
}

export interface AuditPortalGapItem {
  id: number
  frameworkNodeId: number
  nodeCode: string
  title: string
  status: string | null
  evidenceCount: number
}

export interface AuditPortalSoaItem {
  id: number
  frameworkNodeId: number
  nodeCode: string
  title: string
  applicability: string
  justification: string | null
}

export interface AuditPortalRiskItem {
  id: number
  displayId: string
  title: string
  nodeCode: string | null
  orgUnitId: number | null
  residualRisk: string | null
  status: string
}

export interface AuditPortalEvidenceItem {
  id: number
  title: string
  classification: string
  status: string
  linkedNodeCode: string | null
  linkedRecordType: string | null
  linkedRecordId: string | null
}

function portalHeaders(auditorToken: string, tenantId: string): Record<string, string> {
  return {
    Authorization: `Bearer ${auditorToken}`,
    'X-Tenant-ID': tenantId,
  }
}

/** Public external audit portal: scoped JWT from OTP redeem (no main app session). */
export const externalAuditPortalApi = {
  validate: (tenantId: string, token: string): Promise<ValidateExternalAuditAccessResponse> =>
    apiFetch(
      `/api/audits/external-access/validate?tenantId=${encodeURIComponent(tenantId)}&token=${encodeURIComponent(token)}`,
    ).then(handle),

  redeem: (
    tenantId: string,
    token: string,
    otp: string,
  ): Promise<{ token: string; email: string; tenantId: string; roles: string[]; expiresInMinutes: number }> =>
    apiFetch('/api/audits/external-access/redeem', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ tenantId, token, otp }),
    }).then(handle),

  session: (auditorToken: string, tenantId: string): Promise<AuditPortalSession> =>
    apiFetch('/api/audits/portal/session', {
      headers: portalHeaders(auditorToken, tenantId),
    }).then(handle),

  getAudit: (auditorToken: string, tenantId: string, auditId: number): Promise<GrcAuditDetail> =>
    apiFetch(`/api/audits/${auditId}`, {
      headers: portalHeaders(auditorToken, tenantId),
    }).then(handle),

  listGap: (auditorToken: string, tenantId: string, auditId: number): Promise<AuditPortalGapItem[]> =>
    apiFetch(`/api/audits/portal/${auditId}/gap`, {
      headers: portalHeaders(auditorToken, tenantId),
    }).then(handle),

  listSoa: (auditorToken: string, tenantId: string, auditId: number): Promise<AuditPortalSoaItem[]> =>
    apiFetch(`/api/audits/portal/${auditId}/soa`, {
      headers: portalHeaders(auditorToken, tenantId),
    }).then(handle),

  listRisks: (auditorToken: string, tenantId: string, auditId: number): Promise<AuditPortalRiskItem[]> =>
    apiFetch(`/api/audits/portal/${auditId}/risks`, {
      headers: portalHeaders(auditorToken, tenantId),
    }).then(handle),

  listEvidence: (
    auditorToken: string,
    tenantId: string,
    auditId: number,
  ): Promise<AuditPortalEvidenceItem[]> =>
    apiFetch(`/api/audits/portal/${auditId}/evidence`, {
      headers: portalHeaders(auditorToken, tenantId),
    }).then(handle),

  updateChecklistNotes: (
    auditorToken: string,
    tenantId: string,
    auditId: number,
    itemId: number,
    notes: string,
  ): Promise<AuditChecklistItem> =>
    apiFetch(`/api/audits/${auditId}/checklist/${itemId}`, {
      method: 'PUT',
      headers: { ...portalHeaders(auditorToken, tenantId), 'Content-Type': 'application/json' },
      body: JSON.stringify({ notes }),
    }).then(handle),

  addFinding: (
    auditorToken: string,
    tenantId: string,
    auditId: number,
    body: {
      severity: FindingSeverity
      title: string
      description: string
      controlRef?: string
      checklistItemId?: number
    },
  ): Promise<AuditFinding> =>
    apiFetch(`/api/audits/${auditId}/findings`, {
      method: 'POST',
      headers: { ...portalHeaders(auditorToken, tenantId), 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }).then(handle),
}

export interface FrameworkType {
  code: string
  name: string
  moduleCode: string
  defaultVersion: string
  dedicatedParserExists: boolean
  sampleAvailable: boolean
}

export interface FrameworkSummary {
  id: number
  code: string
  name: string
  version: string
  moduleCode: string
  status: string
  controlCount: number
  /** Doc 02 T-11: CLAUSE nodes (library + tenant Frameworks lists). */
  clauseCount?: number | null
  /** Doc 02 T-11: annex/control/cloud nodes (library + tenant Frameworks lists). */
  annexControlCount?: number | null
  /** Nodes routed to each module. Only the tenant framework lists populate it. */
  routingCounts?: FrameworkRoutingCounts | null
  importedBy: string | null
  importedAt: string | null
  validatedBy: string | null
  validatedAt: string | null
  publishedBy: string | null
  publishedAt: string | null
  sourceFilename: string | null
  activated?: boolean | null
  activatedAt?: string | null
  /** Tenant activation: include ISO 27017 / cloud sheet in SoA & Gap. */
  includeCloud?: boolean | null
  /** Catalog has cloud (CLOUD record type) nodes. */
  hasCloud?: boolean | null
}

export interface FrameworkRoutingCounts {
  soa: number
  gap: number
  audit: number
  risk: number
}

/** Which module's slice of a framework to fetch: see the `routing` query param on GET /api/frameworks/{id}. */
export type ModuleRouting = 'SOA' | 'GAP' | 'AUDIT' | 'RISK' | 'ANY'

export interface FrameworkNode {
  id: number
  parentId: number | null
  level: number
  code: string
  title: string
  definition: string | null
  controlType: string | null
  priority: string | null
  displayOrder: number
  recordType: string | null
  officialRef: string | null
  /** The client's import set decides which modules each node feeds. */
  inSoa: boolean
  inGapAssessment: boolean
  inAuditChecklist: boolean
  inRiskRegister: boolean
  /** Soft-retired: hidden from new SoA/Gap seeds. */
  retired?: boolean
  /** Free-form import attrs: sheet_tab, section_type, domain_objective, what_it_means, … */
  attributes?: Record<string, string>
}

export interface FrameworkDetail {
  framework: FrameworkSummary
  nodes: FrameworkNode[]
}

export interface CrossFrameworkMappingEndpoint {
  frameworkId: number
  frameworkCode: string
  frameworkName: string
  frameworkVersion: string
  nodeId: number
  nodeCode: string
  nodeTitle: string
}

export interface CrossFrameworkMapping {
  id: number
  mappingType: string
  justification: string | null
  peer: CrossFrameworkMappingEndpoint
  direction: 'OUTBOUND' | 'INBOUND'
}

export interface FrameworkImportReportLine {
  sheetName: string
  sourceFrameworkCode: string | null
  frameworkCode: string | null
  version: string | null
  status: string
  batchId: number | null
  frameworkId: number | null
  contentRows: number
  nodesCreated: number
  leafNodes: number
  questionsImported: number
  errorCount: number
  routing: {
    inSoa: number
    inGapAssessment: number
    inAuditChecklist: number
    inRiskRegister: number
  } | null
  merge: {
    refRows: number
    exactMatches: number
    ancestorAttachments: number
    unmatchedRefs: number
    questionsReplaced: number
    questionsKept: number
    attributesWritten: number
  } | null
  errors: { row: number; field: string; message: string }[]
  info: string[]
  message: string | null
}

export interface FrameworkImportResult {
  imported: FrameworkDetail[]
  report?: FrameworkImportReportLine[]
}

export interface FrameworkBundleStatus {
  directory: string
  directoryExists: boolean
  modules: string[]
  packs: {
    moduleFolder: string
    relativePath: string
    filename: string
    importable: boolean
  }[]
  skipped: string[]
}

export interface FrameworkBundleImportResult {
  scope: string
  moduleFolder: string | null
  relativePath: string | null
  dryRun: boolean
  importedFileCount: number
  skippedFileCount: number
  errorFileCount: number
  files: {
    step: number
    moduleFolder: string
    relativePath: string
    filename: string
    status: string
    message: string
    importResult: FrameworkImportResult | null
    errors: string[]
  }[]
}

export interface FrameworkTenantAssignment {
  tenantId: string
  tenantName: string
  moduleLicensed: boolean
  assigned: boolean
  assignedAt: string | null
}

export interface FrameworkQuestion {
  id: number
  frameworkId: number
  frameworkNodeId: number
  questionCode: string
  /** The node this hangs off. */
  controlCode: string | null
  /** The ref the client authored it against, when deeper than `controlCode`: pack imports only. */
  sourceRef: string | null
  questionType: string | null
  questionText: string
  /** Client-authored option list, e.g. `Yes/No/Partial/NA`. Null falls back to the standard four. */
  answerOptions: string | null
  isMandatory: boolean
  evidenceGuidance: string | null
  displayOrder: number
  versionNumber: number
}

export interface FrameworkQuestionImportResult {
  versionNumber: number
  importedCount: number
  skippedRows: number
}

export interface GapQuestionAnswer {
  questionId: number | null
  customQuestionId: number | null
  custom: boolean
  questionCode: string | null
  questionType: string | null
  questionText: string | null
  evidenceGuidance: string | null
  versionNumber: number
  answer: string | null
  note: string | null
}

export interface GapCustomQuestion {
  id: number
  questionText: string
  sortOrder: number
}

export interface NotificationItem {
  id: number
  triggerType: string
  title: string
  body: string | null
  recordType: string | null
  recordId: string | null
  link: string | null
  unread: boolean
  readAt: string | null
  createdAt: string
}

export interface NotificationPage {
  content: NotificationItem[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface NotificationTemplateSummary {
  id: number
  triggerType: string
  subjectTemplate: string
  channelEmail: boolean
  channelInApp: boolean
  version: number
  active: boolean
}

export interface AuditLogEntry {
  id: number
  tenantId: string
  userId: number | null
  userRole: string | null
  actionType: string
  module: string
  recordType: string | null
  recordId: string | null
  recordName: string | null
  fieldChanged: string | null
  oldValue: string | null
  newValue: string | null
  ipAddress: string | null
  authMethod: string | null
  createdAt: string
}

export interface AuditPage {
  content: AuditLogEntry[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export type EvidenceClassification = 'SECRET' | 'SENSITIVE' | 'CONFIDENTIAL' | 'OPEN'
export type EvidenceStatus = 'ACTIVE' | 'EXPIRED'

export interface EvidenceSummary {
  id: number
  title: string
  classification: EvidenceClassification
  status: EvidenceStatus
  expiresOn: string | null
  currentVersion: number
  latestFilename: string | null
  latestSizeBytes: number
  linkCount: number
  updatedAt: string
  workflowStage?: string | null
  createdByUserId?: number
}

export interface EvidenceVersion {
  id: number
  versionNumber: number
  originalFilename: string
  contentType: string
  sizeBytes: number
  virusScanStatus: string
  /** Human label: never implies clean when SKIPPED/PENDING. */
  virusScanLabel: string
  uploadedByUserId: number
  uploadedAt: string
}

export interface EvidenceLink {
  id: number
  moduleCode: string
  recordType: string
  recordId: string
  workflowStage: string | null
  linkedByUserId: number
  linkedAt: string
}

export interface EvidenceDetail {
  id: number
  title: string
  classification: EvidenceClassification
  status: EvidenceStatus
  expiresOn: string | null
  currentVersion: number
  createdByUserId: number
  createdAt: string
  updatedAt: string
  versions: EvidenceVersion[]
  links: EvidenceLink[]
}

export interface EvidencePage {
  content: EvidenceSummary[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export const EVIDENCE_CLASSIFICATIONS: EvidenceClassification[] = [
  'SECRET',
  'SENSITIVE',
  'CONFIDENTIAL',
  'OPEN',
]

export const EVIDENCE_CLASSIFICATION_LABELS: Record<EvidenceClassification, string> = {
  SECRET: 'Secret',
  SENSITIVE: 'Sensitive',
  CONFIDENTIAL: 'Confidential',
  OPEN: 'Open',
}

/** RISK evidence link stages (§22 / J3). Required when moduleCode=RISK. */
export const RISK_EVIDENCE_STAGES = [
  'IDENTIFICATION',
  'ASSIGNMENT',
  'REVIEW',
  'TREATMENT',
  'TOLERANCE',
  'TRANSFER',
] as const

export type RiskEvidenceStage = (typeof RISK_EVIDENCE_STAGES)[number]

export interface WorkflowTransitionDef {
  id: number
  fromState: string
  toState: string
  actionCode: string
  allowedRoles: string
  requiresComment: boolean
}

export interface WorkflowDefinition {
  id: number
  code: string
  name: string
  moduleCode: string
  recordType: string
  initialState: string
  version: number
  transitions: WorkflowTransitionDef[]
}

export interface WorkflowLogEntry {
  id: number
  fromState: string
  toState: string
  actionCode: string
  actorUserId: number
  commentText: string | null
  createdAt: string
}

export interface WorkflowInstanceDetail {
  id: number
  definitionId: number
  definitionCode: string
  definitionName: string
  moduleCode: string
  recordType: string
  recordId: string
  recordName: string | null
  currentState: string
  status: string
  startedByUserId: number
  startedAt: string
  completedAt: string | null
  availableActions: WorkflowTransitionDef[]
  history: WorkflowLogEntry[]
}

export interface WorkflowInstancePage {
  content: WorkflowInstanceDetail[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface OrgProfile {
  id: number
  displayName: string | null
  industry: string | null
  sizeBand: string | null
  timezone: string
  dateFormat: string
  wizardCompleted: boolean
  wizardCompletedAt: string | null
  riskMatrixGridSize: number
  riskMatrixLocked: boolean
  riskAppetiteRating: string
  idleTimeoutMinutes: number
  rtoMaxCriticalHours: number
  rtoMaxHighHours: number
  rtoMaxMediumHours: number
  rtoMaxLowHours: number
  updatedAt: string
}

export interface OrgLevel {
  level: number
  label: string
  updatedAt: string
}

export interface OrgUnitNode {
  id: number
  name: string
  level: number
  parentId: number | null
  headUserId: number | null
  active: boolean
  source?: string
  createdAt: string
  updatedAt: string
  children: OrgUnitNode[]
}

export const ORG_SIZE_BANDS = ['1–50', '51–200', '201–1000', '1000+', 'Prefer not to say'] as const

export const ORG_DATE_FORMATS = ['DD/MM/YYYY', 'MM/DD/YYYY', 'YYYY-MM-DD'] as const

export const ORG_TIMEZONES: { value: string; label: string }[] = [
  { value: 'Asia/Dubai', label: 'UAE: Asia/Dubai (GST)' },
  { value: 'Asia/Riyadh', label: 'Saudi Arabia: Asia/Riyadh' },
  { value: 'Asia/Kolkata', label: 'India: Asia/Kolkata (IST)' },
  { value: 'Asia/Singapore', label: 'Singapore: Asia/Singapore' },
  { value: 'Europe/London', label: 'UK: Europe/London' },
  { value: 'Europe/Berlin', label: 'Central Europe: Europe/Berlin' },
  { value: 'Europe/Paris', label: 'France: Europe/Paris' },
  { value: 'America/New_York', label: 'US Eastern: America/New_York' },
  { value: 'America/Chicago', label: 'US Central: America/Chicago' },
  { value: 'America/Denver', label: 'US Mountain: America/Denver' },
  { value: 'America/Los_Angeles', label: 'US Pacific: America/Los_Angeles' },
  { value: 'UTC', label: 'UTC' },
]

export type SoaStatus = 'DRAFT' | 'COMPLETE'
export type SoaApplicability = 'APPLICABLE' | 'NOT_APPLICABLE'

export type ComplianceProjectStatus = 'ACTIVE' | 'COMPLETE' | 'ARCHIVED'

export interface ProjectFramework {
  frameworkId: number
  frameworkCode: string
  frameworkName: string
  frameworkVersion: string
}

export interface ComplianceProjectSummary {
  id: number
  name: string
  year: number
  status: ComplianceProjectStatus
  frameworks: ProjectFramework[]
  includeCloud?: boolean
  createdAt: string
  updatedAt: string
  /** Doc 02 G-14 */
  soaCount?: number
  gapAssessmentCount?: number
  /** Contributor: Gap controls assigned to you. */
  assignedControlCount?: number
  /** Contributor: assigned controls still waiting on you. */
  needsActionCount?: number
}

export interface ComplianceProjectDetail {
  project: ComplianceProjectSummary
  soas: SoaSummary[]
  gapAssessments: GapAssessmentSummary[]
  seededIsRiskCount?: number | null
}

export interface RiskTemplate {
  id: number
  frameworkId: number
  frameworkNodeId: number | null
  sourceRiskId: string
  title: string
  description: string | null
  category: string
  typicalThreats: string | null
  suggestedProbability: string
  suggestedImpact: string
  suggestedControlEffectiveness: string
  treatmentOption: string | null
  treatmentRecommendation: string | null
  displayOrder: number
  versionNumber: number
}

export interface SoaSummary {
  id: number
  projectId: number
  frameworkId: number
  frameworkCode: string
  frameworkName: string
  frameworkVersion: string
  status: SoaStatus
  totalControls: number
  applicableCount: number
  notApplicableCount: number
  unansweredCount: number
  completedAt: string | null
  updatedAt: string
}

export interface SoaItem {
  id: number
  frameworkNodeId: number
  nodeCode: string
  title: string
  priority: string | null
  recordType?: string | null
  applicabilityLocked?: boolean
  applicability: SoaApplicability | null
  justification: string | null
  remarks: string | null
  updatedByUserId: number | null
  updatedAt: string
}

export interface SoaDetail {
  soa: SoaSummary
  items: SoaItem[]
}

export type GapAssessmentStatus = 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED'
export type GapAssessmentType = 'INTERNAL' | 'EXTERNAL' | 'SELF'
export type GapApplicability = 'APPLICABLE' | 'NOT_APPLICABLE'
export type GapImplementationStatus =
  | 'FULLY_IMPLEMENTED'
  | 'PARTIALLY_IMPLEMENTED'
  | 'PLANNED'
  | 'NOT_IMPLEMENTED'

export interface GapLayerScore {
  score: number | null
  rag: string | null
  applicableCount: number
  scoredCount: number
}

export interface GapDomainScore extends GapLayerScore {
  code: string
  name: string
}

export interface GapLayerScoreSummary {
  management: GapLayerScore | null
  technical: GapLayerScore | null
  domains: GapDomainScore[]
  overallUaeIa: GapLayerScore | null
}

export interface GapItemLocator {
  itemId: number
  assessmentId: number
  projectId: number
}

export interface GapAssessmentSummary {
  id: number
  projectId: number
  frameworkId: number
  frameworkCode: string
  frameworkName: string
  frameworkVersion: string
  soaId: number | null
  name: string
  assessmentType: GapAssessmentType
  status: GapAssessmentStatus
  targetDate: string | null
  scope: string | null
  assessorUserId: number | null
  overallScore: number | null
  ragStatus: string | null
  applicableCount: number
  scoredCount: number
  applicabilityOverrideCount: number
  layerScores: GapLayerScoreSummary | null
  /** UAE IA: 3 or 4. Other frameworks always 4. */
  assessmentDepth?: number | null
  completedAt: string | null
  updatedAt: string
}

/** Doc 02 R-09: Gap status for a framework control on the risk form. */
export interface ControlAssessmentStatus {
  found: boolean
  gapAssessmentId: number | null
  gapAssessmentName: string | null
  assessmentStatus: GapAssessmentStatus | null
  gapItemId: number | null
  nodeCode: string | null
  applicability: string | null
  reviewStatus: string | null
  implementationStatus: GapImplementationStatus | null
  finalImplementationStatus: GapImplementationStatus | null
  effectiveStatus: GapImplementationStatus | null
}

export interface GapItem {
  id: number
  frameworkNodeId: number
  nodeCode: string
  title: string
  definitionText: string | null
  priority: string | null
  /** Level-1 ancestor in the catalog; null for frameworks imported without a hierarchy. */
  domainCode: string | null
  domainLabel: string | null
  applicability: GapApplicability
  applicabilityOverride: boolean
  overrideJustification: string | null
  implementationStatus: GapImplementationStatus | null
  /** SAMA CSF maturity 0–5 (target 3). Null for ISO / UAE IA. */
  maturity: number | null
  /** SAMA: five Yes/No evidence-gate answers; drives maturity when present. */
  maturityGates: (boolean | null)[] | null
  observations: string | null
  gapDescription: string | null
  contributorComments: string | null
  score: number | null
  reviewStatus: GapItemReviewStatus
  finalImplementationStatus: GapImplementationStatus | null
  finalMaturity: number | null
  adminReviewNotes: string | null
  sendBackReason: string | null
  contributorSubmittedAt: string | null
  adminReviewedAt: string | null
  adminReviewedByUserId: number | null
  assignedToUserId: number | null
  workTaskId: number | null
  /** ALL = every question; SELECTED = only assignedFramework/CustomQuestionIds (may be empty). */
  questionAssignmentMode: 'ALL' | 'SELECTED'
  assignedFrameworkQuestionIds: number[]
  assignedCustomQuestionIds: number[]
  updatedAt: string
  /** Present on the open control. List rows omit it. */
  dueDate?: string | null
  whatItMeans?: string | null
  evidenceGuidance?: string | null
  /** False while the decision is a draft the contributor cannot see. */
  releasedToContributor?: boolean
  /** Admin mark for later work. Independent of Workflow. */
  flaggedForLater?: boolean
}

export type GapItemReviewStatus = 'OPEN' | 'SUBMITTED' | 'SENT_BACK' | 'FINALIZED'

export interface GapItemSubmission {
  id: number
  submissionNumber: number
  implementationStatus: GapImplementationStatus | null
  maturity: number | null
  maturityGates: (boolean | null)[] | null
  observations: string | null
  gapDescription: string | null
  contributorComments: string | null
  score: number | null
  submittedByUserId: number
  submittedAt: string
}

export type WorkTaskStatus = 'PENDING' | 'IN_PROGRESS' | 'SUBMITTED' | 'CANCELLED'

export type RiskType = 'IS_RISK' | 'ERM' | 'PRIVACY_RISK'

export interface WorkTaskSummary {
  id: number
  sourceModule: string
  sourceRecordType: string
  sourceRecordId: number
  title: string
  detail: string | null
  status: WorkTaskStatus
  dueDate: string | null
  assignedByUserId: number
  assignedByName: string | null
  assignedByEmail: string | null
  submittedAt: string | null
  updatedAt: string
  returned?: boolean
  returnReason?: string | null
  /** Present on RISK_RECORD / RISK_MILESTONE / GAP_ITEM when the row sits on a project. */
  projectId?: number | null
  projectName?: string | null
  /** Present on risk / acceptance / milestone tasks so Contributor nav can hide unused registers. */
  riskType?: RiskType | null
  /** Risk record status when the task is RISK_RECORD / RISK_ACCEPTANCE / RISK_MILESTONE. */
  riskStatus?: string | null
}

export interface WorkTaskDetail {
  task: WorkTaskSummary
  gapItem: GapItem | null
  gapAssessmentId: number | null
  assessmentName: string | null
  frameworkCode: string | null
  riskRecord: RiskSummary | null
  capa: CapaRecord | null
  bia: BiaCycleRecord | null
  policy: PolicyTaskResponse | null
  kri: KriRecord | null
  incident: IncidentRecord | null
  treatmentMilestone: TreatmentMilestone | null
  privacyRopa: PrivacyRopa | null
  privacyDpia: PrivacyDpia | null
}

export type KriFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'ANNUAL'
export type KriDirection = 'HIGHER_IS_WORSE' | 'LOWER_IS_WORSE'
export type KriRag = 'GREEN' | 'AMBER' | 'RED'
export type KriTrend = 'IMPROVING' | 'STABLE' | 'DETERIORATING'
export type KriStatus = 'ACTIVE' | 'INACTIVE'

export interface KriReading {
  id: number
  value: number
  rag: KriRag
  notes: string | null
  recordedByUserId: number
  recordedAt: string
}

export interface KriRecord {
  id: number
  displayId: string
  name: string
  linkedRiskId: number | null
  metricDescription: string | null
  unit: string | null
  frequency: KriFrequency
  ownerUserId: number
  direction: KriDirection
  greenThreshold: number
  amberThreshold: number
  redThreshold: number
  currentValue: number | null
  previousValue: number | null
  rag: KriRag | null
  trend: KriTrend | null
  nextDueDate: string | null
  lastMeasuredAt: string | null
  redAcknowledgedAt: string | null
  redAcknowledgedBy: number | null
  status: KriStatus
  workTaskId: number | null
  createdByUserId: number
  createdAt: string
  updatedAt: string
  recentReadings?: KriReading[]
}

export interface CreateKriBody {
  name: string
  linkedRiskId?: number | null
  metricDescription?: string | null
  unit?: string | null
  frequency: KriFrequency
  ownerUserId: number
  direction: KriDirection
  greenThreshold: number
  amberThreshold: number
  redThreshold: number
  nextDueDate?: string | null
}

export interface UpdateKriBody {
  name?: string | null
  linkedRiskId?: number | null
  metricDescription?: string | null
  unit?: string | null
  frequency?: KriFrequency | null
  ownerUserId?: number | null
  direction?: KriDirection | null
  greenThreshold?: number | null
  amberThreshold?: number | null
  redThreshold?: number | null
  nextDueDate?: string | null
  status?: KriStatus | null
}

export type IncidentPriority = 'P1' | 'P2' | 'P3' | 'P4'
export type IncidentStatus = 'REPORTED' | 'ASSIGNED' | 'RESOLVED' | 'CLOSED'
export type IncidentType =
  | 'SECURITY'
  | 'OPERATIONAL'
  | 'DATA_BREACH'
  | 'PRIVACY'
  | 'PHYSICAL'
  | 'OTHER'

export interface IncidentRecord {
  id: number
  displayId: string
  title: string
  incidentType: IncidentType
  description: string | null
  occurredAt: string
  affectedArea: string | null
  priority: IncidentPriority
  status: IncidentStatus
  linkedRiskId: number | null
  linkedGapAssessmentId?: number | null
  orgUnitId: number | null
  reporterUserId: number
  ownerUserId: number | null
  rootCause: string | null
  containmentActions: string | null
  resolutionNotes: string | null
  responseDueAt: string
  resolutionDueAt: string | null
  respondedAt: string | null
  resolvedAt: string | null
  resolvedByUserId: number | null
  closedAt: string | null
  closedByUserId: number | null
  workTaskId: number | null
  createdByUserId: number
  createdAt: string
  updatedAt: string
}

export interface ReportIncidentBody {
  title: string
  incidentType: IncidentType
  description?: string | null
  occurredAt?: string | null
  affectedArea?: string | null
}

export interface CreateIncidentBody {
  title: string
  incidentType: IncidentType
  description?: string | null
  occurredAt?: string | null
  affectedArea?: string | null
  priority: IncidentPriority
  linkedRiskId?: number | null
  linkedGapAssessmentId?: number | null
  orgUnitId?: number | null
  ownerUserId?: number | null
  rootCause?: string | null
  containmentActions?: string | null
}

export interface UpdateIncidentBody {
  title?: string | null
  incidentType?: IncidentType | null
  description?: string | null
  occurredAt?: string | null
  affectedArea?: string | null
  priority?: IncidentPriority | null
  linkedRiskId?: number | null
  linkedGapAssessmentId?: number | null
  orgUnitId?: number | null
  rootCause?: string | null
  containmentActions?: string | null
  resolutionNotes?: string | null
}

export type AuditStandard = 'ISO_27001' | 'ISO_20000' | 'UAE_IA' | 'ISO_27701' | 'MULTIPLE'
export type ChecklistTemplateKey = 'ISO_27001' | 'ISO_20000' | 'UAE_IA' | 'CUSTOM'
export type GrcAuditType = 'INTERNAL' | 'EXTERNAL' | 'EXTERNAL_ASSESSMENT' | 'FOLLOW_UP'
export type GrcAuditStatus =
  | 'DRAFT'
  | 'ACCESS_GRANTED'
  | 'IN_PROGRESS'
  | 'REPORT_ISSUED'
  | 'CLOSED'
  | 'CANCELLED'
export type AuditGranteeType = 'INTERNAL_AUDITOR' | 'EXTERNAL_AUDITOR' | 'TECHNICAL_EXPERT' | 'OBSERVER'
export type ChecklistConclusion =
  | 'CONFORMS'
  | 'MAJOR_NC'
  | 'MINOR_NC'
  | 'OBSERVATION'
  | 'OFI'
  | 'NOT_APPLICABLE'
  | 'NOT_TESTED'
export type FindingSeverity = 'MAJOR_NC' | 'MINOR_NC' | 'OBSERVATION' | 'OFI'
export type CapaRegisterCode = 'GAP' | 'INTERNAL_AUDIT' | 'EXTERNAL_AUDIT' | 'BCMS' | 'OTHER'
export type CapaStatus = 'OPEN' | 'IN_PROGRESS' | 'PENDING_VERIFICATION' | 'CLOSED' | 'OVERDUE'

export type EngagementScopeModule =
  | 'DASHBOARD'
  | 'GAP'
  | 'SOA'
  | 'RISK'
  | 'ERM'
  | 'EVIDENCE'
  | 'BCMS'
  | 'PRIVACY'
  | 'ASSET'
export type EngagementTeamRole = 'LEAD' | 'MEMBER'

export interface AuditTeamMember {
  userId: number
  teamRole: EngagementTeamRole
}

export interface AuditExternalInvite {
  id: number
  auditId: number
  inviteeName: string
  inviteeEmail: string
  status: string
  expiresAt: string
  createdAt: string
  debugAccessLink?: string | null
  debugOtp?: string | null
  debugToken?: string | null
}

export interface GrcAuditSummary {
  id: number
  displayId?: string | null
  name: string
  auditType: GrcAuditType
  auditStandard?: AuditStandard | null
  auditMode: string
  frameworkId: number | null
  frameworkCode: string
  frameworkName: string
  frameworkVersion: string
  status: GrcAuditStatus
  startDate: string | null
  endDate: string | null
  auditorOrganisation?: string | null
  auditeeOrgUnitId?: number | null
  leadAuditorUserId: number
  leadAuditorName?: string | null
  scopeModules?: EngagementScopeModule[]
  locked: boolean
  gapAssessmentId: number | null
  auditorSignedAt: string | null
  adminSignedAt: string | null
  updatedAt: string
  findingCount?: number
}

export interface AuditChecklistItem {
  id: number
  checklistId?: number
  frameworkNodeId: number | null
  nodeCode: string
  title: string
  section?: string | null
  testFocus?: string | null
  soaApplicability?: string | null
  evidenceModule?: string | null
  libraryLocked?: boolean
  inScope?: boolean
  assessment: string | null
  notes: string | null
  evidenceExamined?: string | null
  personsInterviewed?: string | null
  reviewedDate?: string | null
  conclusion?: ChecklistConclusion | null
  findingDescription?: string | null
  findingId?: number | null
  findingDisplayCode?: string | null
  updatedAt: string
}

export interface AuditChecklistSheet {
  id: number
  templateKey: ChecklistTemplateKey
  name: string
  sortOrder: number
  items: AuditChecklistItem[]
}

export interface AuditChecklistLibraryItem {
  id: number
  sortOrder: number
  clauseRef: string
  section: string | null
  title: string
  testFocus: string | null
  soaApplicability: string | null
  evidenceLives: string | null
}

export interface AuditChecklistLibrary {
  id: number
  name: string
  templateKey: ChecklistTemplateKey
  systemSeeded: boolean
  lineCount: number
  createdByUserId: number
  createdAt: string
  updatedAt: string
  items: AuditChecklistLibraryItem[]
}

export interface AuditFinding {
  id: number
  checklistItemId: number | null
  displayCode: string
  severity: FindingSeverity
  title: string
  description: string
  recommendedAction: string | null
  controlRef: string | null
  capaId: number | null
  createdAt: string
}

export interface AuditDomainNarrative {
  code: string
  name: string
  narrative: string
}

export interface FindingRegisterItem {
  findingId: number
  displayCode: string
  severity: FindingSeverity
  title: string
  controlRef: string | null
  auditId: number
  auditName: string
  auditType: GrcAuditType
  auditLocked: boolean
  capaId: number | null
  createdAt: string
}

export interface GrcAuditDetail {
  audit: GrcAuditSummary
  scopeNotes: string | null
  executiveSummary: string | null
  returnReason: string | null
  orgUnitIds: number[]
  teamMembers: AuditTeamMember[]
  scopeModules: EngagementScopeModule[]
  scopeNodeIds: number[]
  checklists?: AuditChecklistSheet[]
  checklist: AuditChecklistItem[]
  findings: AuditFinding[]
  gapAssessmentId: number | null
  gapLayerScores: GapLayerScoreSummary | null
  domainNarratives: AuditDomainNarrative[]
  accessGrants?: AccessGrant[]
}

export interface AccessGrant {
  id: number
  displayId: string
  auditId: number
  userId: number
  granteeType: AuditGranteeType
  organisation?: string | null
  ndaReference?: string | null
  modules: EngagementScopeModule[]
  scopeBoundary?: string | null
  canPreviewEvidence: boolean
  canExportNotes: boolean
  canDownloadEvidence: boolean
  validFrom: string
  validTo: string
  graceDays: number
  autoExpiry: string
  revoked: boolean
  accessStatus: string
  independenceCheck: string
  recordsViewed: number
  createdAt: string
}

export interface AccessViewLog {
  id: number
  grantId: number
  grantDisplayId: string
  auditId: number | null
  auditDisplayId: string
  auditName: string
  userId: number
  userName: string
  viewedAt: string
  moduleCode: string
  recordRef: string
  action: string
  documentName?: string | null
  classification?: string | null
  withinScope: string
}

export interface CreateAuditBody {
  name: string
  auditType: GrcAuditType
  auditMode?: string
  standard?: AuditStandard
  checklistTemplate?: ChecklistTemplateKey
  checklistLibraryId?: number
  checklistLibraryIds?: number[]
  frameworkId?: number
  leadAuditorUserId?: number
  externalLeadEmail?: string
  externalLeadName?: string
  startDate?: string | null
  endDate?: string | null
  scopeNotes?: string | null
  orgUnitIds?: number[]
  gapAssessmentId?: number | null
  teamMemberUserIds?: number[]
  scopeModules?: EngagementScopeModule[]
  scopeNodeIds?: number[]
  auditorOrganisation?: string | null
  auditeeOrgUnitId?: number | null
}

export interface UpdateAuditEngagementBody {
  name: string
  leadAuditorUserId: number
  startDate?: string | null
  endDate?: string | null
  scopeNotes?: string | null
  orgUnitIds?: number[]
  teamMemberUserIds?: number[]
  scopeModules?: EngagementScopeModule[]
  scopeNodeIds?: number[]
}

export interface CreateFindingBody {
  checklistItemId?: number | null
  severity: FindingSeverity
  title: string
  description: string
  recommendedAction?: string | null
  controlRef?: string | null
}

/**
 * CAPA record - fields ordered per Excel Audit_Module_MVP_v2.xlsx CAPA Tracker (A-T)
 */
export interface CapaRecord {
  // A: CAPA ID
  id: number
  // B: Finding ID (source reference)
  sourceFindingDisplayCode: string | null
  // C: Finding Title
  title: string
  // D: Classification (severity)
  severity: string | null
  // E: Clause Ref
  controlRef: string | null
  // F: Date Raised
  createdAt: string
  // G: Correction (immediate fix)
  correction: string | null
  // H: Root Cause
  rootCause: string | null
  // I: Corrective Action Plan
  correctiveAction: string | null
  // J: Date Assigned
  assignedAt: string | null
  // K: Agreed Target Date
  targetDate: string | null
  // L: Assigned Contributor (owner)
  ownerUserId: number | null
  // M: Contributor Department
  contributorDepartment: string | null
  // N: Evidence Required
  evidenceRequired: string | null
  // O: Response Description (Contributor)
  responseDescription: string | null
  // P: Upload Evidence note (Contributor)
  contributorEvidenceNote: string | null
  // Q: Review Remarks (Admin)
  reviewRemarks: string | null
  // R: Evidence of Completion (Admin)
  evidenceOfCompletion: string | null
  // S: Verification Date (Admin)
  verificationDate: string | null
  // T: Verification Result (Admin)
  verificationResult: string | null
  // Additional fields not in Excel tracker
  registerCode: CapaRegisterCode
  sourceType: string
  sourceRecordId: number | null
  description: string | null
  recommendedAction: string | null
  priority: string
  status: CapaStatus
  preventiveAction: string | null
  progressNotes: string | null
  rejectionReason: string | null
  returnHistory?: CapaReturnHistoryItem[]
  workTaskId: number | null
  closedAt: string | null
  updatedAt: string
  sourceAuditId: number | null
  sourceAuditName: string | null
  conductedBy: string | null
  capaRegisterId: number | null
  sourceOther: string | null
}

export interface CapaReturnHistoryItem {
  id: number
  rejectedAt: string
  rejectedByUserId: number
  rejectedByName: string | null
  reason: string
}

export interface CapaAvailableFinding {
  id: number
  displayCode: string
  title: string
  severity: string | null
  controlRef: string | null
  auditId: number
  auditName: string | null
}

export interface CapaRegisterRecord {
  id: number
  year: number
  name: string
  auditId: number | null
  auditName: string | null
  auditDisplayId: string | null
  conductedBy: string
  description: string
  capaCount: number
  createdAt: string
  updatedAt: string
}

export type AssetType =
  | 'HARDWARE'
  | 'APPLICATION'
  | 'SOFTWARE'
  | 'DATA'
  | 'NETWORK'
  | 'CLOUD'
  | 'PHYSICAL'
  | 'PEOPLE'
  | 'SERVICE'
  | 'FACILITY'
  | 'INTANGIBLE'
  | 'OT_ICS'
  | 'OTHER'
export type AssetStatus = 'DRAFT' | 'ACTIVE' | 'DISPOSED'
export type AssetCriticality = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type AssetClassification = 'SECRET' | 'SENSITIVE' | 'CONFIDENTIAL' | 'OPEN'
export type AssetTriState = 'YES' | 'NO' | 'NOT_APPLICABLE'

export interface AssetHandlingGuidance {
  confidentiality: number
  sheetLabel: string
  labelling: string
  storage: string
  transfer: string
  access: string
  disposal: string
}

export interface AssetSummary {
  id: number
  displayId: string | null
  name: string
  assetType: AssetType
  assetTypeLabel?: string | null
  assetSubType: string | null
  dependencyText?: string | null
  businessProcessService?: string | null
  environment?: string | null
  status: AssetStatus
  description: string | null
  ownerUserId: number | null
  ownerJobTitle?: string | null
  ownerDepartment?: string | null
  orgUnitId: number | null
  custodianUserId?: number | null
  custodianName?: string | null
  parentId: number | null
  country?: string | null
  siteBuilding?: string | null
  hostingModel?: string | null
  cloudProviderRegion?: string | null
  dataResidency?: string | null
  dataResidencyOther?: string | null
  hostname?: string | null
  ipAddress?: string | null
  assetTagSerial?: string | null
  platformOsVersion?: string | null
  confidentiality: number | null
  confidentialityJustification?: string | null
  integrityRating: number | null
  integrityJustification?: string | null
  availability: number | null
  availabilityJustification?: string | null
  ciaScore: number | null
  aggregateScore?: number | null
  weightedScore?: number | null
  criticality: AssetCriticality | null
  criticalityLabel?: string | null
  derivedClassification?: AssetClassification | null
  appliedClassification?: AssetClassification | null
  classification: AssetClassification | null
  classRank?: number | null
  classificationCheck?: string | null
  recommendedRtoHours?: number | null
  recommendedRpoHours?: number | null
  agreedRtoHours?: number | null
  agreedRpoHours?: number | null
  recoveryObjectiveCheck?: string | null
  reviewCycleMonths?: number | null
  requiredControlBaseline?: string | null
  baselineGaps?: string | null
  encryptedAtRest?: AssetTriState | null
  encryptedInTransit?: AssetTriState | null
  mfaEnforced?: AssetTriState | null
  backedUp?: AssetTriState | null
  backupFrequency?: string | null
  lastRestoreTestDate?: string | null
  siemMonitored?: AssetTriState | null
  vulnScanned?: AssetTriState | null
  lastPatchDate?: string | null
  accessReviewFrequency?: string | null
  lifecycleStatus?: string | null
  acquisitionGoLiveDate?: string | null
  warrantyEndDate?: string | null
  endOfLifeDate?: string | null
  eolRiskFlag?: string | null
  disposalMethod?: string | null
  thirdPartyInvolved?: AssetTriState | null
  vendorName?: string | null
  contractSlaRef?: string | null
  tprmAssessmentDone?: AssetTriState | null
  lastReviewedDate?: string | null
  nextReviewDue?: string | null
  reviewStatus?: string | null
  reviewedBy?: string | null
  changeTicketRef?: string | null
  notes?: string | null
  handlingGuidance?: AssetHandlingGuidance | null
  createdByUserId: number
  disposedAt: string | null
  disposedByUserId: number | null
  createdAt: string
  updatedAt: string
}

export interface AssetKpis {
  total: number
  active: number
  disposed: number
  tier1Or2: number
  secretOrSensitive: number
}

export interface AssetMeta {
  classifications: string[]
  assetTypes: { value: string; label: string }[]
  assetSubTypes: string[]
  hostingModels: string[]
  dataResidencies: string[]
  environments: string[]
  lifecycleStatuses: string[]
  triStates: string[]
  impactScale: {
    level: number
    confidentiality: string
    integrity: string
    availability: string
    classification: string
  }[]
  handlingByConfidentiality: AssetHandlingGuidance[]
}

export interface CreateAssetBody {
  name: string
  assetType: AssetType
  assetSubType?: string | null
  dependencyText?: string | null
  businessProcessService?: string | null
  environment?: string | null
  description?: string | null
  ownerUserId?: number | null
  ownerJobTitle?: string | null
  ownerDepartment?: string | null
  orgUnitId?: number | null
  custodianUserId?: number | null
  custodianName?: string | null
  parentId?: number | null
  country?: string | null
  siteBuilding?: string | null
  hostingModel?: string | null
  cloudProviderRegion?: string | null
  dataResidency?: string | null
  dataResidencyOther?: string | null
  hostname?: string | null
  ipAddress?: string | null
  assetTagSerial?: string | null
  platformOsVersion?: string | null
  confidentiality?: number | null
  confidentialityJustification?: string | null
  integrityRating?: number | null
  integrityJustification?: string | null
  availability?: number | null
  availabilityJustification?: string | null
  appliedClassification?: AssetClassification | null
  classification?: AssetClassification | null
  clearAppliedClassification?: boolean | null
  agreedRtoHours?: number | null
  agreedRpoHours?: number | null
  encryptedAtRest?: AssetTriState | null
  encryptedInTransit?: AssetTriState | null
  mfaEnforced?: AssetTriState | null
  backedUp?: AssetTriState | null
  backupFrequency?: string | null
  lastRestoreTestDate?: string | null
  siemMonitored?: AssetTriState | null
  vulnScanned?: AssetTriState | null
  lastPatchDate?: string | null
  accessReviewFrequency?: string | null
  lifecycleStatus?: string | null
  acquisitionGoLiveDate?: string | null
  warrantyEndDate?: string | null
  endOfLifeDate?: string | null
  disposalMethod?: string | null
  thirdPartyInvolved?: AssetTriState | null
  vendorName?: string | null
  contractSlaRef?: string | null
  tprmAssessmentDone?: AssetTriState | null
  lastReviewedDate?: string | null
  reviewedBy?: string | null
  changeTicketRef?: string | null
  notes?: string | null
  /** Persist incomplete work as DRAFT (name required). Full Save omits or sets false → ACTIVE. */
  asDraft?: boolean | null
}

export type RiskLevel = 'VERY_LOW' | 'LOW' | 'MEDIUM' | 'HIGH' | 'VERY_HIGH'
export type ResidualScoringMode = 'CONTROL_EFFECTIVENESS' | 'MANUAL_PROBABILITY_IMPACT'
export type RiskCategory =
  | 'STRATEGIC'
  | 'OPERATIONAL'
  | 'FINANCIAL'
  | 'COMPLIANCE'
  | 'IT'
  | 'REPUTATIONAL'
  | 'LEGAL'
  | 'ENVIRONMENTAL'
  | 'OTHER'
export type PriorityLevel = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'
export type RiskStatus =
  | 'DRAFT'
  | 'ASSIGNED'
  | 'SUBMITTED'
  | 'UNDER_VALIDATION'
  | 'MITIGATING'
  | 'PENDING_ACCEPTANCE'
  | 'PENDING_ADMIN_REVIEW'
  | 'AVOIDED'
  | 'TRANSFERRED'
  | 'TOLERATED'
  | 'ACCEPTED'
  | 'CLOSED'
  | 'AUTO_CLOSED'
  | 'OVERDUE'
  | 'RE_OPENED'
  | 'CANCELLED'
export type TreatmentOption = 'TREAT' | 'TOLERATE' | 'TERMINATE' | 'TRANSFER'
export type TransferMethod = 'INSURANCE' | 'OUTSOURCE' | 'CONTRACT' | 'OTHER'
export type TreatmentPlanStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE'

export interface TreatmentMilestone {
  id: number
  treatmentPlanId: number
  milestoneTitle: string
  dueDate: string | null
  completed: boolean
  completedDate: string | null
  completedByUserId: number | null
  progressNotes: string | null
}

export interface TreatmentPlanDetail {
  id: number
  riskId: number
  actionDescription: string | null
  actionOwnerId: number | null
  custodianUserId?: number | null
  startDate: string | null
  targetDate: string | null
  resourcesRequired: string | null
  costEstimateAed: number | null
  evidenceRequired: string | null
  status: TreatmentPlanStatus
  completionDate: string | null
  milestones: TreatmentMilestone[]
}

export interface RiskAcceptanceForm {
  id: number
  riskId: number
  justification: string | null
  businessRationale: string | null
  conditionsOfAcceptance: string | null
  compensatingControls: string | null
  compControlOwnerId: number | null
  compControlEffectiveness: RiskLevel | null
  reviewDate: string | null
  contributorDeclaration: boolean
  riskOwnerDecision: string | null
  riskOwnerComments: string | null
  adminDecision: string | null
  adminComments: string | null
  status: string
  formKind?: string | null
  displayId?: string | null
  cycleNo?: number
  approver1UserId?: number | null
  approver1Name?: string | null
  approver2UserId?: number | null
  approver2Name?: string | null
  approver2Decision?: string | null
  approver2Comments?: string | null
  approver3UserId?: number | null
  approver3Name?: string | null
  approver3Decision?: string | null
  approver3Comments?: string | null
}

export interface RiskScoreHistoryEntry {
  id: number
  riskId: number
  probability: RiskLevel
  impact: RiskLevel
  controlEffectiveness: RiskLevel
  inherentRisk: RiskLevel
  residualRisk: RiskLevel
  reason: string | null
  changedByUserId: number
  recordedAt: string
}

export interface RiskStageNote {
  id: number
  riskId: number
  workflowStage: RiskStatus
  body: string
  internalOnly: boolean
  createdByUserId: number
  createdAt: string
}

export interface RiskMatricesConfig {
  gridSize: number
  inherent: RiskLevel[][]
  residual: RiskLevel[][]
  rowLevels: RiskLevel[]
  colLevels: RiskLevel[]
  version: number
  changeReason: string | null
  updatedByUserId: number | null
  updatedAt: string | null
  usingDefaults: boolean
  gridLocked?: boolean
  appetiteRating?: string | null
  appetiteMaxResidual?: number | null
}

export type HeatMapScoreBasis = 'INHERENT' | 'RESIDUAL'

/** Unfinished IS / ERM create form. Not a risk: no Risk ID or status until Create. */
export interface RiskFormDraft {
  id: number
  riskType: RiskType
  riskProjectId: number
  title: string | null
  payload: Record<string, unknown>
  attachmentCount: number
  createdAt: string
  updatedAt: string
}

export interface RiskBulkImportResult {
  createdCount: number
  failedCount: number
  skippedBlankCount: number
  ignoredCount: number
  rows: {
    excelRow: number
    success: boolean
    ignored: boolean
    riskId: number | null
    displayId: string | null
    error: string | null
  }[]
}

export interface RiskHeatMapCell {
  rowLevel: RiskLevel
  colLevel: RiskLevel
  row: number
  col: number
  count: number
  band: RiskLevel
}

export interface RiskHeatMap {
  riskType: RiskType
  scoreBasis: HeatMapScoreBasis
  openOnly: boolean
  gridSize: number
  rowAxis: string
  colAxis: string
  rows: RiskLevel[]
  cols: RiskLevel[]
  cells: RiskHeatMapCell[]
  total: number
  closedExcluded: number
}

export interface RiskSummary {
  id: number
  riskType: RiskType
  displayId: string
  displayIdOverridden?: boolean
  title: string
  description: string | null
  category: RiskCategory
  categoryOther?: string | null
  status: RiskStatus
  priorityLevel: PriorityLevel
  uaeIaPriority?: 'P1' | 'P2' | 'P3' | 'P4' | null
  orgUnitId: number | null
  orgUnitName?: string | null
  assetId: number | null
  frameworkId: number | null
  frameworkCode: string | null
  frameworkNodeId: number | null
  nodeCode: string | null
  observedGap: string | null
  threats: string | null
  probability: RiskLevel
  impact: RiskLevel
  inherentRisk: RiskLevel
  existingControls: string | null
  controlEffectiveness: RiskLevel
  residualScoringMode?: ResidualScoringMode
  residualProbability?: RiskLevel | null
  residualImpact?: RiskLevel | null
  residualRisk: RiskLevel
  inherentScore?: number | null
  inherentRating?: string | null
  inherentAction?: string | null
  residualScore?: number | null
  residualRating?: string | null
  residualAction?: string | null
  autoCloseOverride?: boolean
  autoCloseOverrideJustification?: string | null
  treatmentOption: TreatmentOption | null
  treatmentPlan: string | null
  treatmentChangeJustification: string | null
  eliminationJustification: string | null
  eliminationHow: string | null
  activityStopped: boolean | null
  transferMethod: TransferMethod | null
  thirdPartyName: string | null
  transferDescription: string | null
  residualAfterTransfer: string | null
  transferCompensatingControls: string | null
  validationNotes: string | null
  rejectionReason: string | null
  nextReviewDate: string | null
  riskOwnerUserId: number | null
  riskOwnerName?: string | null
  assignedContributorId: number | null
  assignedContributorName?: string | null
  workTaskId: number | null
  adminNotes: string | null
  strategicObjective: string | null
  financialImpactAed: number | null
  tprmVendorId: number | null
  sourceGapAssessmentId?: number | null
  sourceGapItemId?: number | null
  sourceDpiaId?: number | null
  sourceDpiaAssessmentRowId?: number | null
  sourceRopaId?: number | null
  sourceComplianceProjectId?: number | null
  sourceRiskTemplateId?: number | null
  createdByUserId: number
  assignedAt: string | null
  submittedAt: string | null
  closedAt: string | null
  createdAt: string
  updatedAt: string
  activeTreatmentPlan: TreatmentPlanDetail | null
  acceptanceForm: RiskAcceptanceForm | null
  additionalFrameworks?: {
    id: number
    frameworkId: number
    frameworkCode: string | null
    frameworkNodeId: number | null
    controlRef: string | null
    customRef: string | null
  }[]
  infosecStatusInitial?: string | null
  infosecStatusContributor?: string | null
  infosecStatusFinal?: string | null
  riskProjectId?: number | null
  dateIdentified?: string | null
  cause?: string | null
  riskReductionPct?: number | null
  withinAppetite?: boolean | null
  evaluationDecision?: string | null
  acceptanceRequired?: boolean
  acceptanceRequiredLabel?: string | null
  targetControlEffectiveness?: RiskLevel | null
  targetResidualScore?: number | null
  targetWithinAppetite?: boolean | null
  targetResidualManual?: boolean
  treatedRiskScore?: string | null
  finalRiskStatus?: 'APPROVE' | 'REJECT' | null
  custodianComments?: string | null
}

export interface SubmitRiskTaskBody {
  confirmTreatment: boolean
  treatmentOption?: TreatmentOption | null
  justification?: string | null
  comments?: string | null
  actionDescription?: string | null
  actionOwnerId?: number | null
  startDate?: string | null
  targetDate?: string | null
  resourcesRequired?: string | null
  costEstimateAed?: number | null
  evidenceRequired?: string | null
  acceptanceJustification?: string | null
  businessRationale?: string | null
  conditionsOfAcceptance?: string | null
  compensatingControls?: string | null
  compControlOwnerId?: number | null
  compControlEffectiveness?: RiskLevel | null
  reviewDate?: string | null
  contributorDeclaration?: boolean | null
  eliminationJustification?: string | null
  eliminationHow?: string | null
  activityStopped?: boolean | null
  transferMethod?: TransferMethod | null
  thirdPartyName?: string | null
  transferDescription?: string | null
  residualAfterTransfer?: string | null
  transferCompensatingControls?: string | null
  infosecStatusContributor?: string | null
  approver2UserId?: number | null
  approver3UserId?: number | null
  riskOwnerUserId?: number | null
}

export interface CreateRiskBody {
  riskType?: RiskType
  displayIdOverride?: string | null
  title: string
  description?: string | null
  category: RiskCategory
  categoryOther?: string | null
  orgUnitId?: number | null
  assetId?: number | null
  frameworkId?: number | null
  frameworkCode?: string | null
  frameworkNodeId?: number | null
  nodeCode?: string | null
  uaeIaPriority?: 'P1' | 'P2' | 'P3' | 'P4' | null
  observedGap?: string | null
  threats?: string | null
  probability: RiskLevel
  impact: RiskLevel
  existingControls?: string | null
  controlEffectiveness: RiskLevel
  residualScoringMode?: ResidualScoringMode | null
  residualProbability?: RiskLevel | null
  residualImpact?: RiskLevel | null
  treatmentOption?: TreatmentOption | null
  treatmentPlan?: string | null
  treatmentPlanDetail?: {
    actionDescription?: string | null
    actionOwnerId?: number | null
    custodianUserId?: number | null
    startDate?: string | null
    targetDate?: string | null
    resourcesRequired?: string | null
    costEstimateAed?: number | null
    evidenceRequired?: string | null
  } | null
  riskOwnerUserId?: number | null
  adminNotes?: string | null
  strategicObjective?: string | null
  financialImpactAed?: number | null
  tprmVendorId?: number | null
  overrideAutoClose?: boolean | null
  autoCloseOverrideJustification?: string | null
  additionalFrameworks?: {
    frameworkId: number
    frameworkCode?: string | null
    frameworkNodeId?: number | null
    controlRef?: string | null
    customRef?: string | null
  }[] | null
  sourceGapAssessmentId?: number | null
  sourceGapItemId?: number | null
  sourceDpiaId?: number | null
  sourceRopaId?: number | null
  sourceDpiaAssessmentRowId?: number | null
  infosecStatusInitial?: string | null
  riskProjectId?: number | null
  dateIdentified?: string | null
  cause?: string | null
  residualScore?: number | null
  targetControlEffectiveness?: RiskLevel | null
  assignedContributorId?: number | null
  nextReviewDate?: string | null
  thirdPartyName?: string | null
  treatmentJustification?: string | null
  compensatingControls?: string | null
}

export interface RiskKpis {
  veryHigh: number
  high: number
  medium: number
  low: number
  veryLow: number
  totalOpen: number
}

export interface RiskProjectSummary {
  id: number
  projectType: RiskType
  name: string
  year: number
  status: string
  frameworkId: number | null
  createdByUserId: number
  riskCount: number
  /** Contributor: assigned rows still needing input. Admin: 0. */
  needsActionCount?: number
  seedTotal?: number
  seedStatus?: 'RUNNING' | 'DONE' | 'FAILED'
  createdAt: string
  updatedAt: string
}

export interface GapAssessmentDetail {
  assessment: GapAssessmentSummary
  items: GapItem[]
}

export const ASSIGNABLE_ROLES = [
  'OWNER',
  'ADMIN',
  'CONTRIBUTOR',
  'INTERNAL_AUDITOR',
  'GLOBAL_READER',
]

// --- BCMS ---

export interface BcmsDeptProfile {
  id: number
  orgUnitId: number
  totalStaff: number | null
  criticalStaffRoles: string | null
  primaryFunctions: string | null
  internalDependencies: string | null
  externalDependencies: string | null
  physicalLocation: string | null
  alternateWorkLocation: string | null
  assetIds: number[]
  createdAt: string
  updatedAt: string
}

export interface UpsertDeptProfileBody {
  orgUnitId: number
  totalStaff?: number | null
  criticalStaffRoles?: string | null
  primaryFunctions?: string | null
  internalDependencies?: string | null
  externalDependencies?: string | null
  physicalLocation?: string | null
  alternateWorkLocation?: string | null
  assetIds?: number[]
}

export type BiaStatus =
  | 'DRAFT'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'SUBMITTED'
  | 'RETURNED'
  | 'APPROVED'
  | 'PENDING_BC_HEAD'
  | 'RETURNED_TO_ADMIN'
  | 'READY_TO_CLOSE'
  | 'CLOSED'

export type BiaProjectStatus = 'OPEN' | 'CLOSED'

export interface BiaProjectRecord {
  id: number
  year: number
  name: string
  status: BiaProjectStatus
  createdByUserId: number
  createdAt: string
  updatedAt: string
  departmentCount: number
}

export interface CreateBiaProjectBody {
  year: number
  name: string
}
export type BiaReviewOutcome =
  | 'PENDING_REVIEW'
  | 'VERIFIED'
  | 'RETURNED_FOR_REVISION'
  | 'APPROVED'
  | 'REJECTED'

export interface BiaKpi {
  inScope: number
  criticalCount: number
  normalCount: number
  tier1: number
  tier2: number
  tier3: number
  tier4: number
  tier5: number
  completeCount: number
}

export interface BiaRankedRow {
  recoveryOrder: number
  processId: number
  processName: string
  ownerOrgUnitId: number | null
  categoryLabel: string | null
  rtoHours: number | null
  maoHours: number | null
  capacityPct: number | null
  minStaff: number | null
  recoveryStrategy: string | null
}

export interface BiaImpactCell {
  category: string
  band: string
  rating: number | null
  remarks: string | null
}

export interface BiaPersonRef {
  source: 'ENTRA' | 'ARBOR'
  directoryId?: string | null
  userId?: number | null
  email?: string | null
  displayName: string
  jobTitle?: string | null
}

export interface BiaAssetDependency {
  type: string
  detail: string
}

export interface BiaThirdPartyContact {
  name: string
  email: string
  phone?: string | null
}

export interface BiaAssetTypeLookup {
  code: string
  label: string
  detailKind: 'STRING' | 'INTEGER' | string
}

export interface BiaMorRecord {
  id: number
  processId: number
  capacityPct: number | null
  minStaff: number | null
  people: string | null
  technology: string | null
  premises: string | null
  equipment: string | null
  recordsText: string | null
  thirdPartySupport: string | null
  recoveryStrategy: string | null
  workaround: string | null
  minStaffPeople: BiaPersonRef[]
  recoveryStrategies: string[]
  internetConnectivity: string | null
  intranetConnectivity: string | null
  vpnAccess: string | null
  siteType: string | null
  softwareApplications: string[]
  laptops: number | null
  m365: string | null
  thirdPartyContacts: BiaThirdPartyContact[]
}

export interface BiaProcessRecord {
  id: number
  cycleId: number
  sortOrder: number
  processCode: string
  processName: string
  description: string | null
  flowTrigger: string | null
  flowSteps: string | null
  flowOutput: string | null
  thirdPartyActivities: string | null
  supplierName: string | null
  ownerOrgUnitId: number | null
  ownerUserId: number | null
  frequency: string | null
  peakPeriod: string | null
  depStaff: string | null
  depPremises: string | null
  depIt: string | null
  depEquipment: string | null
  depRecords: string | null
  depThirdParty: string | null
  relatedRiskCode: string | null
  championNotes: string | null
  depStaffPeople: BiaPersonRef[]
  depAssets: BiaAssetDependency[]
  peakScore: number | null
  criticalityTier: number | null
  categoryLabel: string | null
  rtoHours: number | null
  maoHours: number | null
  recoveryPriority: number | null
  criticalRank: number | null
  morRequired: boolean
  rtoCaption: string | null
  completenessOk: boolean
  completenessLabel: string
  completenessMissing: string[]
  reviewOutcome: BiaReviewOutcome
  reviewOutcomeLabel: string | null
  submittedByUserId: number | null
  submittedAt: string | null
  reviewerUserId: number | null
  reviewerName: string | null
  reviewDate: string | null
  reviewComment: string | null
  createdByUserId: number | null
  createdByAdmin: boolean
  headVerified: boolean
  impact: BiaImpactCell[]
  mor: BiaMorRecord | null
}

export interface BiaProcessImportResult {
  createdCount: number
  failedCount: number
  ignoredCount: number
  skippedBlankCount: number
  rows: {
    excelRow: number
    processCode: string | null
    success: boolean
    processId: number | null
    completenessLabel: string | null
    error: string | null
    ignored: boolean
  }[]
}

export interface RopaProcessImportResult {
  createdCount: number
  failedCount: number
  ignoredCount: number
  skippedBlankCount: number
  rows: {
    excelRow: number
    activityName: string | null
    success: boolean
    processId: number | null
    completenessLabel: string | null
    error: string | null
    ignored: boolean
  }[]
}

export interface BiaCycleRecord {
  id: number
  biaProjectId: number
  orgUnitId: number
  displayId: string
  issueDate: string | null
  nextReviewDate: string | null
  versionLabel: string
  objective: string | null
  scopeText: string | null
  mbco: string | null
  status: BiaStatus
  assignedToUserId: number | null
  dueDate: string | null
  workTaskId: number | null
  returnReason: string | null
  approvedAt: string | null
  approvedByUserId: number | null
  bcHeadUserId: number | null
  bcHeadNotes: string | null
  bcHeadDecidedAt: string | null
  adminForwardNotes: string | null
  closedAt: string | null
  closedByUserId: number | null
  bcHeadWorkTaskId: number | null
  createdByUserId: number
  createdAt: string
  updatedAt: string
  kpis: BiaKpi
  rankedCritical: BiaRankedRow[]
  processes: BiaProcessRecord[]
}

export interface CreateBiaCycleBody {
  biaProjectId: number
  orgUnitId: number
  displayId?: string | null
  issueDate?: string | null
  nextReviewDate?: string | null
  versionLabel?: string | null
  objective?: string | null
  scopeText?: string | null
  mbco?: string | null
}

export interface UpdateBiaCycleBody {
  displayId?: string | null
  issueDate?: string | null
  nextReviewDate?: string | null
  versionLabel?: string | null
  objective?: string | null
  scopeText?: string | null
  mbco?: string | null
}

export interface UpsertBiaProcessBody {
  processCode: string
  processName: string
  description?: string | null
  flowTrigger?: string | null
  flowSteps?: string | null
  flowOutput?: string | null
  thirdPartyActivities?: string | null
  supplierName?: string | null
  ownerOrgUnitId?: number | null
  ownerUserId?: number | null
  frequency?: string | null
  peakPeriod?: string | null
  depStaff?: string | null
  depPremises?: string | null
  depIt?: string | null
  depEquipment?: string | null
  depRecords?: string | null
  depThirdParty?: string | null
  relatedRiskCode?: string | null
  championNotes?: string | null
  depStaffPeople?: BiaPersonRef[] | null
  depAssets?: BiaAssetDependency[] | null
}

export interface SaveBiaMorBody {
  capacityPct?: number | null
  minStaff?: number | null
  people?: string | null
  technology?: string | null
  premises?: string | null
  equipment?: string | null
  recordsText?: string | null
  thirdPartySupport?: string | null
  recoveryStrategy?: string | null
  workaround?: string | null
  minStaffPeople?: BiaPersonRef[] | null
  recoveryStrategies?: string[] | null
  internetConnectivity?: string | null
  intranetConnectivity?: string | null
  vpnAccess?: string | null
  siteType?: string | null
  softwareApplications?: string[] | null
  laptops?: number | null
  m365?: string | null
  thirdPartyContacts?: BiaThirdPartyContact[] | null
  enforceRequired?: boolean | null
}

export interface BiaLookups {
  frequencies: string[]
  recoveryStrategies: string[]
  reviewOutcomes: string[]
  impactScale: { rating: number; level: string; definition: string }[]
  bands: { code: string; label: string; maoHours: number; rtoHours: number }[]
  categories: string[]
  assetTypes?: BiaAssetTypeLookup[]
  orgUnits: { id: number; name: string }[]
  users: { id: number; name: string }[]
}

export type RecoveryPlanType = 'BCP' | 'DRP' | 'IT_RECOVERY' | 'CRISIS_COMMS'
export type RecoveryPlanStatus = 'DRAFT' | 'PENDING_OWNER' | 'APPROVED' | 'EXPIRED'

export interface RecoveryPlanProcedure {
  id: number
  biaId: number | null
  processName: string
  procedureText: string | null
  sortOrder: number
}

export interface RecoveryPlanProcedureBody {
  biaId?: number | null
  processName: string
  procedureText?: string | null
  sortOrder: number
}

export interface RecoveryPlan {
  id: number
  projectId: number | null
  name: string
  planType: RecoveryPlanType
  scope: string | null
  versionNum: number
  status: RecoveryPlanStatus
  crisisTeam: string | null
  escalationContacts: string | null
  communicationTree: string | null
  resourceRequirements: string | null
  alternateSiteDetails: string | null
  ownerUserId: number | null
  createdByUserId: number
  submittedAt: string | null
  approvedAt: string | null
  approvedByUserId: number | null
  reviewDate: string | null
  biaIds: number[]
  procedures: RecoveryPlanProcedure[]
  createdAt: string
  updatedAt: string
}

export interface CreateRecoveryPlanBody {
  projectId?: number | null
  name: string
  planType: RecoveryPlanType
  scope?: string | null
  crisisTeam?: string | null
  escalationContacts?: string | null
  communicationTree?: string | null
  resourceRequirements?: string | null
  alternateSiteDetails?: string | null
  ownerUserId?: number | null
  biaIds?: number[]
  procedures?: RecoveryPlanProcedureBody[]
}

export interface UpdateRecoveryPlanBody extends CreateRecoveryPlanBody {
  reviewDate?: string | null
}

export type ExerciseType = 'TABLETOP' | 'WALKTHROUGH' | 'SIMULATION' | 'FULL_TEST'
export type ExerciseStatus = 'PLANNED' | 'COMPLETED' | 'CANCELLED'

export interface BcmsExerciseParticipant {
  id: number
  userId: number | null
  participantName: string | null
}

export interface ExerciseParticipantBody {
  userId?: number | null
  participantName?: string | null
}

export interface BcmsExercise {
  id: number
  projectId: number | null
  name: string
  exerciseType: ExerciseType
  scope: string | null
  facilitatorUserId: number | null
  objectives: string | null
  scenariosTested: string | null
  results: string | null
  gapsIdentified: string | null
  lessonsLearned: string | null
  scheduledDate: string | null
  status: ExerciseStatus
  completedAt: string | null
  createdByUserId: number
  planIds: number[]
  participants: BcmsExerciseParticipant[]
  createdAt: string
  updatedAt: string
}

export interface CreateExerciseBody {
  projectId?: number | null
  name: string
  exerciseType: ExerciseType
  scope?: string | null
  facilitatorUserId?: number | null
  objectives?: string | null
  scheduledDate?: string | null
  planIds?: number[]
  participants?: ExerciseParticipantBody[]
}

export type UpdateExerciseBody = CreateExerciseBody

export interface CompleteExerciseBody {
  scenariosTested?: string | null
  results?: string | null
  gapsIdentified?: string | null
  lessonsLearned?: string | null
}

// --- Policy repository (platform always-on) ---

export type PolicyCategory = 'ISMS' | 'BCMS' | 'ITSM' | 'PRIVACY' | 'ERM' | 'GENERAL_HR'
export type PolicyStatus = 'DRAFT' | 'UNDER_REVIEW' | 'PENDING_OWNER' | 'APPROVED' | 'DUE_FOR_REVIEW'
export type PolicyAckScopeType = 'ALL_ACTIVE_USERS' | 'ORG_UNITS' | 'SELECTED_USERS'

export const POLICY_CATEGORY_LABELS: Record<PolicyCategory, string> = {
  ISMS: 'ISMS',
  BCMS: 'BCMS',
  ITSM: 'ITSM',
  PRIVACY: 'Privacy',
  ERM: 'ERM',
  GENERAL_HR: 'General / HR',
}

export interface PolicySummary {
  id: number
  title: string
  category: PolicyCategory
  status: PolicyStatus
  orgUnitId: number | null
  majorVersion: number
  minorVersion: number
  versionLabel: string
  reviewDate: string | null
  ackRequired: boolean
  ackScopeType: PolicyAckScopeType
  ackDeadline: string | null
  ownerUserId: number | null
  createdByUserId: number | null
  currentVersionId: number | null
  ackCount: number
  ackExpected: number
  updatedAt: string
}

export interface PolicyFramework {
  frameworkId: number
  frameworkCode: string
  frameworkName: string
}

export interface PolicyVersion {
  id: number
  policyId: number
  majorVersion: number
  minorVersion: number
  versionLabel: string
  bodyText: string | null
  hasFile: boolean
  originalFilename: string | null
  contentType: string | null
  sizeBytes: number | null
  approvedAt: string | null
  approvedByUserId: number | null
  createdAt: string
}

export interface PolicyAcknowledgement {
  id: number
  policyVersionId: number
  userId: number
  userEmail: string | null
  userFullName: string | null
  acknowledgedAt: string
}

export interface PolicyDetail {
  policy: PolicySummary
  bodyDraft: string | null
  hasDraftFile: boolean
  draftFilename: string | null
  rejectReason: string | null
  reviewNotes: string | null
  frameworks: PolicyFramework[]
  ackOrgUnitIds: number[]
  ackUserIds: number[]
  currentVersion: PolicyVersion | null
  currentUserAcknowledged: boolean
}

export interface CreatePolicyBody {
  title: string
  category: PolicyCategory
  orgUnitId?: number | null
  bodyText?: string | null
  reviewDate?: string | null
  ackRequired?: boolean | null
  ackScopeType?: PolicyAckScopeType | null
  ackDeadline?: string | null
  ownerUserId?: number | null
  frameworkIds?: number[]
  ackOrgUnitIds?: number[]
  ackUserIds?: number[]
}

export type UpdatePolicyBody = CreatePolicyBody

export interface PolicyTaskResponse {
  policyId: number
  title: string
  category: PolicyCategory
  versionLabel: string
  bodyText: string | null
  hasFile: boolean
  originalFilename: string | null
  ackDeadline: string | null
  alreadyAcknowledged: boolean
}

// --- Dashboard & Reports (Sprint 8, platform always-on) ---

export type WidgetTone = 'warning' | 'success' | 'info' | 'neutral' | 'danger'

export interface DashboardWidget {
  id: string
  title: string
  module: string
  value: string
  subtitle: string | null
  tone: WidgetTone
  navTarget: string | null
  emptyMessage: string | null
}

export interface RecentActivityItem {
  id: number
  actionType: string | null
  module: string | null
  recordName: string | null
  actorEmail: string | null
  at: string
}

export interface DashboardSummary {
  generatedAt: string
  widgets: DashboardWidget[]
  recentActivity: RecentActivityItem[]
}

/** Assigned-to-me workload. Separate from Admin DashboardSummary. */
export interface ContributorDashboard {
  generatedAt: string
  openCount: number
  overdueCount: number
  dueThisWeekCount: number
  returnedCount: number
  submittedCount: number
  byModule: ContributorDashboardModuleCount[]
  aging: ContributorDashboardAgingBucket[]
  queue: ContributorDashboardQueueItem[]
  weeklySubmitted: ContributorDashboardWeekPoint[]
}

export interface ContributorDashboardModuleCount {
  moduleCode: string
  label: string
  navTarget: string
  openCount: number
  overdueCount: number
}

export interface ContributorDashboardAgingBucket {
  id: string
  label: string
  count: number
}

export interface ContributorDashboardQueueItem {
  taskId: number
  title: string
  moduleLabel: string
  navTarget: string
  projectName: string
  assignedByName: string
  dueDate: string | null
  status: string
  returned: boolean
  overdue: boolean
  dueThisWeek: boolean
  waitingOnAdmin: boolean
  finished?: boolean
  waitingOn?: string | null
  agingLabel: string
}

export interface ContributorDashboardWeekPoint {
  weekStart: string
  submittedCount: number
}

export interface ReportDescriptor {
  id: string
  name: string
  formats: string[]
  module: string
  requiresProject: boolean
}

// --- TPRM (Third Party Risk Management, included in ERM licence) ---

export type VendorType =
  | 'IT_SERVICES'
  | 'NON_IT_SERVICES'
  | 'CLOUD_SERVICE_PROVIDER'
  | 'AI_SERVICE_PROVIDER'
  | 'MSSP'
  | 'SOFTWARE_PRODUCT'
  | 'PROFESSIONAL_SERVICES'
export type TprmCriticality = 'CRITICAL' | 'NON_CRITICAL'
export type TprmCompliance = 'UNKNOWN' | 'COMPLIANT' | 'NOT_COMPLIANT'
export type VendorStatus = 'ACTIVE' | 'INACTIVE'
export type VendorLifecycleState =
  | 'PROFILE_INCOMPLETE'
  | 'CATEGORISED'
  | 'QUESTIONNAIRE_READY'
  | 'ISSUED'
  | 'SUBMITTED'
  | 'IN_REVIEW'
  | 'RETURNED'
  | 'COMPLETE'
export type TprmAnswer = 'YES' | 'PARTIAL' | 'NO' | 'NA'
export type TprmVerdict = 'ACCEPTED' | 'ACCEPTED_WITH_OBSERVATION' | 'DOWNGRADED' | 'RETURNED'
export type TprmCertType = 'NONE' | 'ISO_27001' | 'ISO_27017_27018' | 'ISO_42001' | 'SOC2_TYPE_II' | 'UAE_IA' | 'OTHER'
export type ContactRole = 'PRIMARY_SPOC' | 'CONTRIBUTOR'
export type FindingStatus = 'OPEN' | 'CLOSED'
export type TprmAssessmentStatus =
  | 'DRAFT'
  | 'ISSUED'
  | 'IN_PROGRESS'
  | 'SUBMITTED'
  | 'IN_REVIEW'
  | 'RETURNED'
  | 'COMPLETE'
export type VendorTokenStatus = 'PENDING' | 'ACTIVE' | 'EXPIRED' | 'REVOKED' | 'CONSUMED'

/** @deprecated pack rebuild uses TprmCriticality */
export type VendorCriticality = TprmCriticality
export type VendorCategory = VendorType

export interface TprmLookups {
  vendorTypes: { value: string; label: string }[]
  factors: Record<string, string[]>
  certs: { value: string; label: string }[]
  answers: string[]
  verdicts: string[]
}

export interface TprmKpis {
  total: number
  tier1?: number
  tier2?: number
  tier3?: number
  tier4?: number
  critical: number
  nonCritical: number
  compliant: number
  notCompliant: number
  issued: number
  submitted: number
  reassessmentDue: number
}

export interface TprmContact {
  id?: number
  role: ContactRole
  name: string | null
  email: string
  mobile: string | null
}

export interface TprmCert {
  id?: number
  certType: TprmCertType
  expiryDate: string | null
  otherLabel?: string | null
}

export interface TprmVendor {
  id: number
  name: string
  vendorType: VendorType | null
  website: string | null
  servicesInScope: string | null
  projectDescription: string | null
  orgOwner: string | null
  contractCode: string | null
  contractStart: string | null
  contractEnd: string | null
  f1: string | null
  f2: string | null
  f3: string | null
  f4: string | null
  f5: string | null
  f6: string | null
  f1Score: number | null
  f2Score: number | null
  f3Score: number | null
  f4Score: number | null
  f5Score: number | null
  f6Score: number | null
  inherentScore: number | null
  assuranceCredit: number | null
  adjustedScore: number | null
  tier: string | null
  calculatedTier: string | null
  criticality: TprmCriticality | null
  postureTarget: number | null
  reassessmentMonths: number | null
  overrideTier: string | null
  overrideJustification: string | null
  elevationFlag: boolean
  profileComplete: boolean
  missingProfileFields: string[]
  lifecycleState: VendorLifecycleState
  complianceStatus: TprmCompliance
  posturePercent: number | null
  nextReviewDate: string | null
  linkedErmRiskId: number | null
  currentAssessmentId: number | null
  raiseErmPrompted: boolean
  /** True when the vendor record is locked (current project assigned or closed). */
  categorisationFrozen?: boolean
  status: VendorStatus
  contacts: TprmContact[]
  certs: TprmCert[]
  createdAt: string
  updatedAt: string
}

export interface UpsertVendorBody {
  name: string
  vendorType?: VendorType | null
  website?: string | null
  servicesInScope?: string | null
  projectDescription?: string | null
  orgOwner?: string | null
  contractCode?: string | null
  contractStart?: string | null
  contractEnd?: string | null
  f1?: string | null
  f2?: string | null
  f3?: string | null
  f4?: string | null
  f5?: string | null
  f6?: string | null
  overrideTier?: string | null
  overrideJustification?: string | null
  contacts?: TprmContact[]
  certs?: { certType: TprmCertType; expiryDate?: string | null; otherLabel?: string | null }[]
}

export interface TprmEvidenceFile {
  id: number
  filename: string
}

export interface TprmAssessmentItem {
  id: number
  controlId: number
  controlCode: string
  domain: string
  requirement: string
  description: string
  requiredEvidence: string
  evidenceMandatory: boolean
  weight: number
  weightLabel: string
  mandatory: boolean
  module: string
  sortOrder: number
  vendorAnswer: TprmAnswer | null
  justification: string | null
  evidenceId: number | null
  evidenceFilename: string | null
  evidenceRef: string | null
  reviewVerdict: TprmVerdict | null
  reviewComment: string | null
  returned: boolean
  /** True when Admin returned this control and content still matches the returned snapshot. */
  unchangedSinceReturn?: boolean
  evidenceFiles?: TprmEvidenceFile[]
}

export interface TprmAssessment {
  id: number
  vendorId: number
  vendorName: string
  displayId: string
  title: string | null
  description?: string | null
  status: TprmAssessmentStatus
  questionnaireId?: number | null
  questionnaireName?: string | null
  snapshotVendorType: string | null
  snapshotTier: string | null
  snapshotCriticality: string | null
  snapshotTarget: number | null
  inherentScore: number | null
  assuranceCredit: number | null
  adjustedScore: number | null
  posturePercent: number | null
  compliant: boolean | null
  dueDate: string | null
  expiresAt: string | null
  sentAt: string | null
  startedAt?: string | null
  submittedAt: string | null
  reviewedAt: string | null
  raiseErmPrompted: boolean
  linkedErmRiskId: number | null
  createdByUserId: number
  createdAt: string
  updatedAt: string
  items: TprmAssessmentItem[]
  accessToken?: string | null
  otpCode?: string | null
  canSubmit?: boolean | null
  portalInvites?: { email: string; accessToken: string; otpCode?: string | null }[] | null
}

export interface TprmQuestionnaireItem {
  id: number
  controlId: number | null
  controlCode: string
  domain: string
  requirement: string
  requiredEvidence: string
  evidenceMandatory: boolean
  sortOrder: number
}

export interface TprmQuestionnaire {
  id: number
  name: string
  controlCount: number
  createdByUserId: number
  createdAt: string
  updatedAt: string
  items: TprmQuestionnaireItem[]
}

export interface CreateAssessmentBody {
  vendorId?: number | null
  spocEmail?: string | null
  dueDate?: string | null
  expiryDays?: number | null
  title?: string | null
  extraEmails?: string[] | null
}

export interface ReviewAssessmentBody {
  items: { itemId: number; verdict: TprmVerdict; comment?: string | null }[]
  complete: boolean
  /** Hand off Returned controls to the vendor. Save alone does not. */
  returnToVendor?: boolean
}

export interface TprmFinding {
  id: number
  vendorId: number
  assessmentId: number
  itemId: number | null
  controlCode: string
  domain: string
  title: string
  ownerUserId: number | null
  dueDate: string | null
  status: FindingStatus
  createdAt: string
  closedAt: string | null
}

export interface ValidateVendorAccessResponse {
  valid: boolean
  status: VendorTokenStatus
  vendorName: string
  assessmentDisplayId: string | null
  expiresAt: string
  message: string
  previewOtp?: string | null
}

// --- Privacy (ModuleCode.PRIVACY) ---

export type PrivacyInventoryStatus = 'ACTIVE' | 'ARCHIVED'
export type RopaLegalBasis =
  | 'CONSENT'
  | 'PROTECT_PUBLIC_INTEREST'
  | 'DATA_SUBJECT_ACTIONS'
  | 'LEGAL_CLAIMS_DEFENSE'
  | 'JUDICIAL_SECURITY_PROCEDURES'
  | 'OCCUPATIONAL_HEALTH_ASSESSMENTS'
  | 'PUBLIC_HEALTH_PROTECTION'
  | 'ARCHIVAL_SCIENTIFIC_STUDIES'
  | 'PROTECTING_DATA_SUBJECT_INTERESTS'
  | 'EMPLOYMENT_SOCIAL_SECURITY'
  | 'CONTRACT_PERFORMANCE'
  | 'COMPLIANCE_WITH_OTHER_LAWS'
  | 'OTHER_CASES_AS_PER_REGULATIONS'
export type RopaDesignation = 'CONTROLLER' | 'PROCESSOR' | 'JOINT_CONTROLLER'
export type RopaStatus = 'DRAFT' | 'ASSIGNED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED'
export type RopaDpiaStatus = 'REQUIRED' | 'RECOMMENDED' | 'NOT_REQUIRED' | 'NOT_APPLICABLE'
export type RopaIntakeMode = 'FORM' | 'WORKBOOK'
export type DsrRequestType =
  | 'ACCESS'
  | 'DELETION'
  | 'CORRECTION'
  | 'PORTABILITY'
  | 'OBJECTION'
  | 'RESTRICTION'
export type DsrStatus = 'RECEIVED' | 'IN_PROGRESS' | 'COMPLETE' | 'REJECTED' | 'OVERDUE'

export const ROPA_LEGAL_BASES: RopaLegalBasis[] = [
  'CONSENT',
  'PROTECT_PUBLIC_INTEREST',
  'DATA_SUBJECT_ACTIONS',
  'LEGAL_CLAIMS_DEFENSE',
  'JUDICIAL_SECURITY_PROCEDURES',
  'OCCUPATIONAL_HEALTH_ASSESSMENTS',
  'PUBLIC_HEALTH_PROTECTION',
  'ARCHIVAL_SCIENTIFIC_STUDIES',
  'PROTECTING_DATA_SUBJECT_INTERESTS',
  'EMPLOYMENT_SOCIAL_SECURITY',
  'CONTRACT_PERFORMANCE',
  'COMPLIANCE_WITH_OTHER_LAWS',
  'OTHER_CASES_AS_PER_REGULATIONS',
]
export const ROPA_DPIA_STATUSES: RopaDpiaStatus[] = [
  'REQUIRED',
  'RECOMMENDED',
  'NOT_REQUIRED',
  'NOT_APPLICABLE',
]
export const ROPA_VOLUME_BUCKETS = ['0-1000', '1000-100000', '100000-1M', 'Over 1M'] as const
export const ROPA_FREQUENCIES = [
  'Daily',
  'Weekly',
  'Monthly',
  'Quarterly',
  'Annually',
  'Ad hoc',
  'One off',
] as const
export const PRIVACY_ROPA_EVIDENCE_STAGES = [
  'WORKBOOK_WORKING',
  'WORKBOOK_SUBMITTED',
  'APPROVAL_EVIDENCE',
] as const
export const ROPA_DESIGNATIONS: RopaDesignation[] = ['CONTROLLER', 'PROCESSOR', 'JOINT_CONTROLLER']
export const DSR_REQUEST_TYPES: DsrRequestType[] = [
  'ACCESS',
  'DELETION',
  'CORRECTION',
  'PORTABILITY',
  'OBJECTION',
  'RESTRICTION',
]

export interface PrivacyInventory {
  id: number
  displayId: string
  dataType: string
  dataSubjects: string | null
  volume: string | null
  format: string | null
  storageAssetId: number | null
  storageLocation: string | null
  storageCountry: string | null
  retentionPeriod: string | null
  deletionMethod: string | null
  orgUnitId: number | null
  status: PrivacyInventoryStatus
  notes: string | null
  createdByUserId: number
  createdAt: string
  updatedAt: string
}

export interface CreatePrivacyInventoryBody {
  dataType: string
  dataSubjects?: string | null
  volume?: string | null
  format?: string | null
  storageAssetId?: number | null
  storageLocation?: string | null
  storageCountry?: string | null
  retentionPeriod?: string | null
  deletionMethod?: string | null
  orgUnitId?: number | null
  notes?: string | null
}

export type RopaProjectStatus = 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED'
export type RopaDepartmentStatus =
  | 'NOT_STARTED'
  | 'IN_PROGRESS'
  | 'UNDER_REVIEW'
  | 'READY_FOR_HEAD_SIGN_OFF'
  | 'PENDING_HEAD_SIGN_OFF'
  | 'APPROVED'
  | 'CLOSED'

export interface RopaDepartment {
  id: number
  ropaProjectId: number
  orgUnitId: number
  orgUnitName: string | null
  departmentHeadUserId: number | null
  departmentHeadName: string | null
  championUserId: number | null
  championName: string | null
  dueDate: string | null
  status: RopaDepartmentStatus
  returnFrom: string | null
  returnReason: string | null
  workTaskId: number | null
  headWorkTaskId: number | null
  headNotes: string | null
  headDecidedAt: string | null
  closedAt: string | null
  processCount: number
  createdAt: string
  updatedAt: string
}

export interface RopaProject {
  id: number
  name: string
  year: number | null
  status: RopaProjectStatus
  departments: RopaDepartment[]
  createdByUserId: number
  createdAt: string
  updatedAt: string
}

export type DpiaResponseType =
  | 'FREE_TEXT'
  | 'FREE_TEXT_ATTACHMENT'
  | 'YES_NO'
  | 'YES_NO_PARTLY'
  | 'MULTI_SELECT'
  | 'CONFIRMATION'

export interface DpiaQuestionnaireQuestion {
  id: number
  questionnaireId: number
  code: string
  sectionName: string
  principle: string
  questionText: string
  guidance: string | null
  responseType: DpiaResponseType
  options: string[]
  sortOrder: number
  required: boolean
}

export interface DpiaQuestionnaire {
  id: number
  name: string
  description: string | null
  systemDefault: boolean
  active: boolean
  questionCount: number
  createdByUserId: number
  createdAt: string
  updatedAt: string
  questions?: DpiaQuestionnaireQuestion[] | null
}

export interface UpsertDpiaQuestionBody {
  code: string
  sectionName: string
  principle: string
  questionText: string
  guidance?: string | null
  responseType: DpiaResponseType
  options?: string[] | null
  sortOrder?: number | null
  required?: boolean | null
}

export type DpiaProjectStatus = 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED'
export type DpiaDepartmentStatus =
  | 'NOT_STARTED'
  | 'IN_PROGRESS'
  | 'UNDER_REVIEW'
  | 'DPO_ASSESSMENT'
  | 'PENDING_HEAD_SIGN_OFF'
  | 'APPROVED'
  | 'CLOSED'
export type DpiaHeadOutcome = 'PENDING' | 'APPROVED' | 'RETURNED'

export interface DpiaInstanceQuestion {
  id: number
  code: string
  sectionName: string
  principle: string
  questionText: string
  guidance: string | null
  responseType: DpiaResponseType
  options: string[]
  sortOrder: number
  required: boolean
  answerText: string | null
  answerOptions: string[]
  detailsEvidence: string | null
}

export interface DpiaAssessmentRow {
  id: number
  sortOrder: number
  principle: string
  descriptionText: string
  complianceStatus: string | null
  remarks: string | null
  recommendedMeasures: string | null
  riskTitle: string | null
  raisedRiskId: number | null
  raisedRiskDisplayId: string | null
}

export interface DpiaRiskTitleCandidate {
  assessmentRowId: number
  riskTitle: string
  principle: string
  departmentId: number
  departmentName: string | null
  projectId: number | null
  projectName: string | null
  orgUnitId: number
  raisedRiskId: number | null
  raisedRiskDisplayId: string | null
}

export interface DpiaDepartment {
  id: number
  dpiaProjectId: number
  orgUnitId: number
  orgUnitName: string | null
  reasonDescription: string | null
  questionnaireId: number | null
  questionnaireName: string | null
  departmentHeadUserId: number | null
  departmentHeadName: string | null
  championUserId: number | null
  championName: string | null
  dueDate: string | null
  status: DpiaDepartmentStatus
  returnFrom: string | null
  returnReason: string | null
  workTaskId: number | null
  headWorkTaskId: number | null
  headNotes: string | null
  headDecidedAt: string | null
  headOutcome: DpiaHeadOutcome | null
  assessmentComplete: boolean
  closedAt: string | null
  createdAt: string
  updatedAt: string
  questions?: DpiaInstanceQuestion[] | null
  assessmentRows?: DpiaAssessmentRow[] | null
}

export interface DpiaProject {
  id: number
  name: string
  year: number | null
  status: DpiaProjectStatus
  linkedRopaProjectId: number | null
  linkedRopaProjectName: string | null
  departments: DpiaDepartment[]
  createdByUserId: number
  createdAt: string
  updatedAt: string
}

export interface PrivacyRopa {
  id: number
  displayId: string
  activityName: string
  entityName: string | null
  sectorName: string | null
  divisionName: string | null
  sectionName: string | null
  processingDescription: string | null
  purpose: string | null
  legalBasis: RopaLegalBasis
  dataCategories: string | null
  sensitiveDataCategories: string | null
  dataSubjects: string | null
  dataSubjectVolume: string | null
  dataSubjectVolumeNote: string | null
  processingFrequency: string | null
  processingFrequencyNote: string | null
  sourceOfPersonalData: string | null
  methodOfReceiving: string | null
  recipients: string | null
  shareInternal: boolean
  shareInternalDetails: string | null
  shareOperator: boolean
  shareOperatorDetails: string | null
  shareExternal: boolean
  shareExternalDetails: string | null
  externalEntityLocation: string | null
  thirdCountryTransfers: boolean
  adequateProtection: boolean | null
  crossBorderTransferBasis: string | null
  safeguardsImplemented: string | null
  dpiaStatus: RopaDpiaStatus
  dpiaRequired: boolean
  linkedDpiaId: number | null
  thirdCountryTransferDetails: string | null
  retentionPeriod: string | null
  securityMeasures: string | null
  storageSystems: string | null
  representativeName: string | null
  representativeEmployeeId: string | null
  comments: string | null
  dpiaComments?: string | null
  dpiaCritLargeSensitive?: boolean | null
  dpiaCritSystematicAssessment?: boolean | null
  dpiaCritAutomatedProfiling?: boolean | null
  dpiaCritLegalSeriousImpact?: boolean | null
  designation: RopaDesignation
  intakeMode: RopaIntakeMode
  ropaDepartmentId: number | null
  draft?: boolean
  reviewOutcome: 'OPEN' | 'REVIEWED' | 'RETURNED' | null
  headOutcome?: 'PENDING' | 'APPROVED' | 'RETURNED' | null
  headComments?: string | null
  headDecidedByUserId?: number | null
  headDecidedAt?: string | null
  legalBasesJson: string | null
  adequacyNote: string | null
  orgUnitId: number | null
  assignedContributorId: number | null
  workTaskId: number | null
  assignedAt: string | null
  status: RopaStatus
  reviewNotes: string | null
  reviewedByUserId: number | null
  reviewedAt: string | null
  createdByUserId: number
  createdAt: string
  updatedAt: string
}

export interface CreatePrivacyRopaBody {
  activityName: string
  entityName?: string | null
  sectorName?: string | null
  divisionName?: string | null
  sectionName?: string | null
  processingDescription?: string | null
  purpose?: string | null
  legalBasis?: RopaLegalBasis | null
  dataCategories?: string | null
  sensitiveDataCategories?: string | null
  dataSubjects?: string | null
  dataSubjectVolume?: string | null
  dataSubjectVolumeNote?: string | null
  processingFrequency?: string | null
  processingFrequencyNote?: string | null
  sourceOfPersonalData?: string | null
  methodOfReceiving?: string | null
  recipients?: string | null
  shareInternal?: boolean | null
  shareInternalDetails?: string | null
  shareOperator?: boolean | null
  shareOperatorDetails?: string | null
  shareExternal?: boolean | null
  shareExternalDetails?: string | null
  externalEntityLocation?: string | null
  thirdCountryTransfers?: boolean | null
  adequateProtection?: boolean | null
  crossBorderTransferBasis?: string | null
  safeguardsImplemented?: string | null
  dpiaStatus?: RopaDpiaStatus | null
  dpiaRequired?: boolean | null
  thirdCountryTransferDetails?: string | null
  retentionPeriod?: string | null
  securityMeasures?: string | null
  storageSystems?: string | null
  representativeName?: string | null
  representativeEmployeeId?: string | null
  comments?: string | null
  designation: RopaDesignation
  intakeMode?: RopaIntakeMode | null
  orgUnitId?: number | null
  ropaDepartmentId?: number | null
  legalBasesJson?: string | null
  adequacyNote?: string | null
  draft?: boolean | null
}

export interface ActivityTypeInfo {
  code: string
  label: string
  moduleCode: string
  description: string
  implemented: boolean
  navPath: string | null
}

export interface PrivacyDsr {
  id: number
  displayId: string
  requestType: DsrRequestType
  subjectName: string
  subjectEmail: string | null
  subjectReference: string | null
  description: string | null
  status: DsrStatus
  assignedUserId: number | null
  receivedAt: string
  dueAt: string
  closedAt: string | null
  closeOutcome: 'COMPLETE' | 'REJECTED' | null
  rejectionReason: string | null
  day20NotifiedAt: string | null
  day28NotifiedAt: string | null
  overdueNotifiedAt: string | null
  createdByUserId: number
  createdAt: string
  updatedAt: string
}

export interface CreatePrivacyDsrBody {
  requestType: DsrRequestType
  subjectName: string
  subjectEmail?: string | null
  subjectReference?: string | null
  description?: string | null
  assignedUserId?: number | null
}

export interface UpdatePrivacyDsrBody {
  assignedUserId?: number | null
  description?: string | null
  subjectName?: string | null
  subjectEmail?: string | null
  subjectReference?: string | null
}

export interface PrivacyDsrLifecycleResult {
  asOf: string
  day20: number
  day28: number
  overdue: number
}

export type DpiaStatus = 'DRAFT' | 'IN_PROGRESS' | 'CANCELLED' | 'ASSIGNED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED'
export type DpiaIntakeMode = 'FORM' | 'WORKBOOK'
export type DpiaResidualRisk = 'LOW' | 'MEDIUM' | 'HIGH'
export type ConsentStatus = 'ACTIVE' | 'WITHDRAWN'
export type ConsentMethod = 'WEB_FORM' | 'WRITTEN' | 'VERBAL_RECORDED' | 'OTHER'
export type BreachSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type BreachStatus = 'OPEN' | 'INVESTIGATING' | 'OVERDUE' | 'REPORTED' | 'CLOSED'

export const CONSENT_METHODS: ConsentMethod[] = ['WEB_FORM', 'WRITTEN', 'VERBAL_RECORDED', 'OTHER']

export const DPIA_SCREENING_Q4_TO_Q7 = [
  'Will the processing involve a large volume of Sensitive Personal Data?',
  'Will the processing involve a systematic and comprehensive assessment of the personal aspects of the Data Subject?',
  'Will the processing involve automated processing of personal data, including profiling of the Data Subject?',
  'Will the processing have legal consequences or serious impact on the Data Subject?',
] as const

export interface DpiaMethodologyBank {
  screeningQ4toQ7: string[]
  questions: { number: number; principle: string; question: string }[]
  principles: { number: number; name: string; description: string }[]
  likelihood: string[]
  impact: string[]
  treatment: string[]
}

export interface PrivacyDpia {
  id: number
  displayId: string
  title: string
  ropaId: number | null
  intakeMode: DpiaIntakeMode
  assignedContributorId: number | null
  workTaskId: number | null
  assignedAt: string | null
  processingDescription: string | null
  necessityAssessment: string | null
  risksIdentified: string | null
  mitigationMeasures: string | null
  residualRisk: DpiaResidualRisk | null
  preScreeningRequired: boolean
  preScreeningOutcome: string | null
  preScreeningNotes: string | null
  screeningQ1: string | null
  screeningQ2: string | null
  screeningQ3: boolean | null
  screeningQ4: boolean | null
  screeningQ5: boolean | null
  screeningQ6: boolean | null
  screeningQ7: boolean | null
  fullDpiaMandatory: boolean
  questionsJson: string | null
  assessmentJson: string | null
  overviewRisksJson: string | null
  status: DpiaStatus
  reviewNotes: string | null
  reviewedByUserId: number | null
  reviewedAt: string | null
  createdByUserId: number
  createdAt: string
  updatedAt: string
}

export interface CreatePrivacyDpiaBody {
  title?: string
  ropaId?: number | null
  intakeMode?: DpiaIntakeMode | null
  processingDescription?: string | null
  necessityAssessment?: string | null
  risksIdentified?: string | null
  mitigationMeasures?: string | null
  residualRisk?: DpiaResidualRisk | null
  preScreeningRequired?: boolean | null
  preScreeningOutcome?: string | null
  preScreeningNotes?: string | null
  screeningQ1?: string | null
  screeningQ2?: string | null
  screeningQ3?: boolean | null
  screeningQ4?: boolean | null
  screeningQ5?: boolean | null
  screeningQ6?: boolean | null
  screeningQ7?: boolean | null
  questionsJson?: string | null
  assessmentJson?: string | null
  overviewRisksJson?: string | null
}

export interface PrivacyConsent {
  id: number
  displayId: string
  subjectName: string
  subjectEmail: string | null
  subjectReference: string | null
  purpose: string
  ropaId: number | null
  consentMethod: ConsentMethod
  consentedAt: string
  withdrawnAt: string | null
  status: ConsentStatus
  evidenceNotes: string | null
  createdByUserId: number
  createdAt: string
  updatedAt: string
}

export interface CreatePrivacyConsentBody {
  subjectName: string
  subjectEmail?: string | null
  subjectReference?: string | null
  purpose: string
  ropaId?: number | null
  consentMethod: ConsentMethod
  consentedAt?: string | null
  evidenceNotes?: string | null
}

export interface PrivacyBreach {
  id: number
  displayId: string
  title: string
  description: string | null
  severity: BreachSeverity
  discoveredAt: string
  reportDeadlineAt: string
  reportedAt: string | null
  authorityReference: string | null
  affectedSubjectsEstimate: string | null
  status: BreachStatus
  hour48NotifiedAt: string | null
  overdueNotifiedAt: string | null
  closedAt: string | null
  createdByUserId: number
  createdAt: string
  updatedAt: string
}

export interface CreatePrivacyBreachBody {
  title: string
  description?: string | null
  severity?: BreachSeverity | null
  discoveredAt: string
  affectedSubjectsEstimate?: string | null
}

export interface PrivacyBreachLifecycleResult {
  asOf: string
  hour48: number
  overdue: number
}
