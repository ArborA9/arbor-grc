const NEXT_KEY = 'arbor_post_login'

const NAV_TO_PATH: Record<string, string> = {
  overview: '/dashboard',
  assets: '/asset-register',
}

const PATH_TO_NAV: Record<string, string> = {
  '/dashboard': 'overview',
  '/asset-register': 'assets',
  '/tprm': 'tprm-projects',
  '/audit': 'audit-register',
}

const AUTH_PATHS = new Set(['/signin', '/login', '/ops/signin'])

const RESERVED_SEGMENTS = new Set([
  'api',
  'sso',
  'oauth2',
  'actuator',
  'assets',
  'login',
  'signin',
  'ops',
  'vendor-portal',
  'external-audit',
])

function normalizePath(pathname: string): string {
  return pathname.replace(/\/+$/, '') || '/'
}

export function navIdToPath(id: string): string {
  if (!id || id === 'overview') return '/dashboard'
  if (id.startsWith('licensed-module:')) {
    return `/modules/${encodeURIComponent(id.slice('licensed-module:'.length))}`
  }
  return NAV_TO_PATH[id] ?? `/${id}`
}

export function pathToNavId(pathname: string): string | null {
  const path = normalizePath(pathname)
  if (AUTH_PATHS.has(path)) return null
  if (path === '/vendor-portal' || path.startsWith('/vendor-portal/')) return null
  if (path === '/external-audit' || path.startsWith('/external-audit/')) return null
  if (PATH_TO_NAV[path]) return PATH_TO_NAV[path]
  if (path.startsWith('/modules/')) {
    const code = decodeURIComponent(path.slice('/modules/'.length).split('/')[0] ?? '')
    return code ? `licensed-module:${code}` : null
  }
  if (path.startsWith('/') && path.indexOf('/', 1) < 0) {
    const seg = path.slice(1)
    if (seg && /^[a-z0-9-]+$/i.test(seg) && !RESERVED_SEGMENTS.has(seg)) return seg
  }
  return null
}

/** Current page id: `?nav=` (legacy E2E / bookmarks) or the path. */
export function readNavParam(): string | null {
  const q = new URLSearchParams(window.location.search).get('nav')
  if (q) return q
  return pathToNavId(window.location.pathname)
}

export function writeNavParam(id: string, mode: 'push' | 'replace' = 'push') {
  const url = new URL(window.location.href)
  url.pathname = navIdToPath(id)
  url.searchParams.delete('nav')
  url.searchParams.delete('next')
  url.searchParams.delete('projectId')
  url.hash = ''
  const href = `${url.pathname}${url.search}`
  if (mode === 'replace') window.history.replaceState({ nav: id }, '', href)
  else window.history.pushState({ nav: id }, '', href)
}

export function isAppShellPath(pathname: string): boolean {
  return pathToNavId(pathname) != null
}

export function isSafeNext(next: string): boolean {
  if (!next.startsWith('/') || next.startsWith('//')) return false
  const path = next.split('?')[0]?.split('#')[0] ?? ''
  if (path.startsWith('/api') || path.startsWith('/sso') || path.startsWith('/oauth2')) return false
  if (path.startsWith('/login/oauth2')) return false
  return isAppShellPath(path)
}

export function rememberNext(next: string) {
  if (isSafeNext(next)) sessionStorage.setItem(NEXT_KEY, next)
}

export function rememberNextFromLocation() {
  const url = new URL(window.location.href)
  const fromQuery = url.searchParams.get('next')
  if (fromQuery && isSafeNext(fromQuery)) {
    rememberNext(fromQuery)
    return
  }
  const here = `${normalizePath(url.pathname)}${url.search}`
  if (isSafeNext(here)) rememberNext(here)
}

export function takeRememberedNext(): string | null {
  const q = new URLSearchParams(window.location.search).get('next')
  if (q && isSafeNext(q)) {
    sessionStorage.removeItem(NEXT_KEY)
    return q
  }
  const stored = sessionStorage.getItem(NEXT_KEY)
  sessionStorage.removeItem(NEXT_KEY)
  return stored && isSafeNext(stored) ? stored : null
}

/**
 * After password / MFA / SSO: leave `/signin` and drop legacy `?nav=`.
 * Preserves extra query (e.g. `auditId`). Honours `next` / sessionStorage for SSO return.
 */
export function enterAuthenticatedLocation() {
  const url = new URL(window.location.href)
  const next = takeRememberedNext()
  const fromQuery = url.searchParams.get('nav')
  const fromNext = next ? pathToNavId(next.split('?')[0] ?? '') : null
  const nav = fromQuery || pathToNavId(url.pathname) || fromNext || 'overview'

  const dest = new URL(navIdToPath(nav), window.location.origin)
  url.searchParams.forEach((value, key) => {
    if (key !== 'nav' && key !== 'next') dest.searchParams.set(key, value)
  })
  if (next?.includes('?')) {
    new URLSearchParams(next.slice(next.indexOf('?') + 1)).forEach((value, key) => {
      if (key !== 'nav' && key !== 'next') dest.searchParams.set(key, value)
    })
  }
  const href = `${dest.pathname}${dest.search}`
  const current = `${url.pathname}${url.search}`
  if (current !== href) {
    window.history.replaceState({ nav }, '', href)
  }
}
