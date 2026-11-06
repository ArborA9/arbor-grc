import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { api } from '../api'
import {
  DEFAULT_TENANT_LOCALE,
  formatAed,
  formatTenantDate,
  formatTenantDateTime,
  setTenantLocale,
  type TenantLocale,
} from './tenantLocale'

const TenantLocaleContext = createContext<TenantLocale>(DEFAULT_TENANT_LOCALE)

export function TenantLocaleProvider({ children }: { children: ReactNode }) {
  const [locale, setLocale] = useState<TenantLocale>(DEFAULT_TENANT_LOCALE)

  useEffect(() => {
    let cancelled = false
    api
      .getOrgProfile()
      .then((p) => {
        if (cancelled) return
        const next: TenantLocale = {
          dateFormat: p.dateFormat || DEFAULT_TENANT_LOCALE.dateFormat,
          timezone: p.timezone || DEFAULT_TENANT_LOCALE.timezone,
          displayName: p.displayName,
        }
        setTenantLocale(next)
        setLocale(next)
      })
      .catch(() => {
        api
          .getSessionPolicy()
          .then((policy) => {
            if (cancelled || !policy.displayName) return
            const next: TenantLocale = {
              ...DEFAULT_TENANT_LOCALE,
              displayName: policy.displayName,
            }
            setTenantLocale(next)
            setLocale(next)
          })
          .catch(() => {
            /* keep defaults */
          })
      })
    return () => {
      cancelled = true
    }
  }, [])

  return <TenantLocaleContext.Provider value={locale}>{children}</TenantLocaleContext.Provider>
}

export function useTenantLocale() {
  const locale = useContext(TenantLocaleContext)
  return {
    locale,
    formatDate: (v: string | null | undefined) => formatTenantDate(v, locale),
    formatDateTime: (v: string | null | undefined) => formatTenantDateTime(v, locale),
    formatAed,
  }
}
