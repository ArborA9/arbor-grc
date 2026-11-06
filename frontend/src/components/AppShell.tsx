import { useCallback, useEffect, useState, type ReactNode } from 'react'
import {
  AppBar,
  Avatar,
  Box,
  Chip,
  Collapse,
  Divider,
  IconButton,
  ListItemIcon,
  Menu,
  MenuItem,
  Stack,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material'
import { alpha } from '@mui/material/styles'
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded'
import KeyboardDoubleArrowLeftRoundedIcon from '@mui/icons-material/KeyboardDoubleArrowLeftRounded'
import KeyboardDoubleArrowRightRoundedIcon from '@mui/icons-material/KeyboardDoubleArrowRightRounded'
import LogoutIcon from '@mui/icons-material/Logout'
import LockResetRoundedIcon from '@mui/icons-material/LockResetRounded'
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded'
import ExtensionRoundedIcon from '@mui/icons-material/ExtensionRounded'
import { brand, layout } from '../theme'
import { api } from '../api'
import ChangePasswordDialog from './ChangePasswordDialog'
import {
  CollapsedCascadeParent,
  CollapsedIconButton,
  collapsedNavTipSlotProps,
} from './shell/CollapsedNav'

const NAV_COLLAPSED_KEY = 'arbor.shell.navCollapsed'
const NAV_LIST_ID = 'arbor-primary-nav'

function readNavCollapsed(): boolean {
  try {
    return localStorage.getItem(NAV_COLLAPSED_KEY) === '1'
  } catch {
    return false
  }
}

function writeNavCollapsed(collapsed: boolean) {
  try {
    localStorage.setItem(NAV_COLLAPSED_KEY, collapsed ? '1' : '0')
  } catch {
    /* private mode / quota */
  }
}

/** First letter of the first and last name; falls back to the email when no name is known. */
export function accountInitials(fullName: string | null | undefined, email: string): string {
  const words = (fullName ?? '').trim().split(/\s+/).filter(Boolean)
  if (words.length >= 2) return (words[0][0] + words[words.length - 1][0]).toUpperCase()
  if (words.length === 1) return words[0][0].toUpperCase()
  const parts = email.split('@')[0].split(/[._-]+/).filter(Boolean)
  if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
  return email.slice(0, 1).toUpperCase()
}

export interface NavItem {
  id: string
  label: string
  icon: ReactNode
  /** Roadmap placeholder: visible but not navigable. */
  soon?: boolean
  /** Commercial module code; omit for Platform always-on items. */
  module?: string
  /** Visible if any of these modules are licensed (e.g. Frameworks under ISMS/ITSM/…). */
  anyModules?: string[]
  /**
   * Nested children (req §3.6 parent nav, e.g. Business Continuity).
   * Parent toggles expand; only children call onNavChange.
   * `opensPage` also navigates to this item's id when the label is clicked.
   */
  children?: NavItem[]
  opensPage?: boolean
}

export interface LicensedModuleNavItem {
  code: string
  label: string
}

interface AppShellProps {
  productTag?: string
  nav: NavItem[]
  active: string
  onNavChange: (id: string) => void
  /**
   * When true, show the Licences → Modules sidebar (tenant Dashboard). Platform Console
   * omits this. Empty list still renders ("No modules licensed") so clients see licence state.
   */
  showLicensedModulesNav?: boolean
  /** Tenant Dashboard only: licensed modules under a collapsible Licences section. */
  licensedModules?: LicensedModuleNavItem[]
  /** Prefix for module detail routes, default `licensed-module:`. */
  licensedModuleNavPrefix?: string
  email: string
  accountMeta?: string
  contextLabel?: string
  roleChips?: ReactNode
  headerTitle: string
  /** When set, the page title is a button. Used when the title is the way back to a list. */
  onHeaderTitleClick?: () => void
  headerSubtitle?: string
  /** Kit-style breadcrumb under the page title (e.g. Risk Management › Information Security Risk). */
  headerBreadcrumb?: string
  /** Clickable breadcrumb prefix (e.g. Assessments) before `›` + `headerBreadcrumb` trail. */
  headerBreadcrumbRoot?: { label: string; onClick: () => void }
  /** Breadcrumb only. The page title lives in the content, not again in the top bar. */
  headerCompact?: boolean
  /** Optional tab row under breadcrumb (project SoA / Gap, etc.). */
  headerTabs?: ReactNode
  /** Compact context in the topbar (status, dates, assignee). */
  headerMeta?: ReactNode
  /** Primary page CTAs in the topbar (e.g. Matrices, New Risk). */
  pageActions?: ReactNode
  /** When set, page actions stay visible but cannot run (licence view-only). */
  actionsLockedReason?: string
  /** Optional notice strip above page content (licence expiry). */
  notice?: ReactNode
  /** Sidebar footer workspace label (e.g. tenant id). */
  workspaceName?: string
  /** Hide the primary module sidebar (immersive sub-workspaces). */
  hideSidebar?: boolean
  /** Omit default content padding. */
  flushContent?: boolean
  actions?: ReactNode
  onLogout: () => void
  /** Local accounts only. Hidden for SSO. Super Admin is always local. */
  canChangePassword?: boolean
  superAdmin?: boolean
  children: ReactNode
}

const SIDEBAR_WIDTH = layout.sidebarWidth
const SIDEBAR_COLLAPSED = layout.sidebarCollapsedWidth
const NAV_ROW = layout.sidebarNavRow
const NAV_MOTION = 'width 200ms cubic-bezier(0.2, 0, 0, 1)'

function LogoMark({ size = 30 }: { size?: number }) {
  return (
    <Box
      sx={{
        width: size,
        height: size,
        borderRadius: 1,
        display: 'grid',
        placeItems: 'center',
        bgcolor: brand.gold,
        color: '#fff',
        fontSize: size * 0.47,
        fontWeight: 800,
        letterSpacing: '-0.5px',
        flexShrink: 0,
      }}
    >
      A
    </Box>
  )
}

function navContainsActive(item: NavItem, active: string): boolean {
  if (item.id === active) return true
  return !!item.children?.some((c) => navContainsActive(c, active))
}

function NavButton({
  selected,
  disabled,
  indent = 0,
  icon,
  label,
  soon,
  onClick,
  collapsed = false,
}: {
  selected: boolean
  disabled?: boolean
  indent?: number
  icon: ReactNode
  label: string
  soon?: boolean
  onClick: () => void
  collapsed?: boolean
}) {
  const nested = indent > 0
  const button = (
    <Box
      role="listitem"
      component="button"
      type="button"
      disabled={disabled}
      aria-label={soon ? `${label} (Soon)` : label}
      aria-current={selected ? 'page' : undefined}
      aria-disabled={disabled || undefined}
      onClick={onClick}
      sx={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: collapsed ? 'center' : 'flex-start',
        gap: collapsed ? 0 : 1,
        width: '100%',
        pl: collapsed ? 0 : indent >= 2 ? 3.75 : indent === 1 ? 2.5 : 1.1,
        pr: collapsed ? 0 : 1.1,
        py: collapsed ? 0 : nested ? 0.55 : 0.75,
        minHeight: collapsed ? NAV_ROW : nested ? undefined : NAV_ROW,
        border: 0,
        borderRadius: 1,
        textAlign: 'left',
        font: 'inherit',
        position: 'relative',
        cursor: disabled ? 'not-allowed' : 'pointer',
        color: disabled ? brand.navMuted : selected ? brand.accentHover : brand.nav,
        opacity: disabled ? 0.55 : 1,
        bgcolor: selected ? brand.sidebarSelected : 'transparent',
        fontWeight: selected ? 650 : 500,
        transition: 'background-color .12s ease, color .12s ease',
        '&::before': selected
          ? {
              content: '""',
              position: 'absolute',
              left: 0,
              top: 6,
              bottom: 6,
              width: 2.5,
              borderRadius: 1,
              bgcolor: brand.accent,
            }
          : undefined,
        '&:hover': {
          bgcolor: disabled ? 'transparent' : selected ? brand.sidebarSelected : brand.sidebarHover,
          color: disabled ? undefined : brand.ink,
        },
        '&:focus-visible': {
          outline: `2px solid ${brand.accent}`,
          outlineOffset: 1,
        },
      }}
    >
      <Box
        sx={{
          display: 'flex',
          color: selected ? brand.accent : 'inherit',
          opacity: selected ? 1 : 0.72,
          '& .MuiSvgIcon-root': { fontSize: collapsed ? 20 : 16 },
        }}
      >
        {icon}
      </Box>
      {!collapsed && (
        <>
          <Typography
            variant="body2"
            sx={{
              fontWeight: selected ? 650 : 500,
              flexGrow: 1,
              fontSize: nested ? '0.75rem' : '0.8125rem',
              letterSpacing: '-0.01em',
              color: 'inherit',
            }}
          >
            {label}
          </Typography>
          {soon && (
            <Chip
              label="Soon"
              size="small"
              sx={{
                height: 20,
                fontSize: '0.6875rem',
                fontWeight: 650,
                letterSpacing: '0.04em',
                bgcolor: brand.accentMuted,
                color: brand.inkSoft,
                border: `1px solid ${brand.hairline}`,
                '& .MuiChip-label': { px: 0.75 },
              }}
            />
          )}
        </>
      )}
    </Box>
  )
  if (!collapsed) return button
  return (
    <Tooltip
      title={soon ? `${label} (Soon)` : label}
      placement="right"
      enterDelay={400}
      enterNextDelay={400}
      slotProps={collapsedNavTipSlotProps}
    >
      <Box component="span" sx={{ display: 'block', width: '100%' }}>
        {button}
      </Box>
    </Tooltip>
  )
}

/* Collapsed rail: ./shell/CollapsedNav */

function NestedNavGroup({
  item,
  indent,
  active,
  openGroups,
  onToggle,
  onNavChange,
}: {
  item: NavItem
  indent: number
  active: string
  openGroups: Record<string, boolean>
  onToggle: (id: string, currentlyOpen: boolean) => void
  onNavChange: (id: string) => void
}) {
  const childActive = !!item.children?.some((c) => navContainsActive(c, active))
  const selfActive = !!(item.opensPage && item.id === active)
  const open = openGroups[item.id] ?? (childActive || selfActive)
  return (
    <Box>
      <Box
        component="button"
        type="button"
        aria-expanded={open}
        onClick={() => {
          if (item.opensPage) {
            onNavChange(item.id)
            if (!open) onToggle(item.id, open)
            return
          }
          onToggle(item.id, open)
        }}
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1,
          width: '100%',
          pl: indent >= 2 ? 3.75 : 2.5,
          pr: 1.1,
          py: 0.55,
          border: 0,
          borderRadius: 1,
          textAlign: 'left',
          font: 'inherit',
          cursor: 'pointer',
          color: selfActive || childActive ? brand.accentHover : brand.nav,
          bgcolor: (selfActive || childActive) && !open ? brand.sidebarSelected : 'transparent',
          '&:hover': { bgcolor: brand.sidebarHover, color: brand.ink },
        }}
      >
        <Box sx={{ display: 'flex', color: selfActive || childActive ? brand.accent : 'inherit', opacity: 0.85, '& .MuiSvgIcon-root': { fontSize: 14 } }}>
          {item.icon}
        </Box>
        <Typography
          variant="body2"
          sx={{ fontWeight: 650, flexGrow: 1, fontSize: '0.75rem', letterSpacing: '-0.01em' }}
        >
          {item.label}
        </Typography>
        <Box
          component="span"
          role="presentation"
          onClick={(e) => {
            e.stopPropagation()
            onToggle(item.id, open)
          }}
          sx={{ display: 'flex', alignItems: 'center' }}
        >
          <ExpandMoreRoundedIcon
            sx={{
              fontSize: 16,
              color: brand.muted,
              transform: open ? 'rotate(0deg)' : 'rotate(-90deg)',
              transition: 'transform .15s ease',
            }}
          />
        </Box>
      </Box>
      <Collapse in={open} timeout="auto" unmountOnExit={false}>
        <Stack spacing={0.15} sx={{ mt: 0.15 }}>
          {(item.children ?? []).map((child) => (
            <NavButton
              key={child.id}
              indent={indent + 1}
              selected={child.id === active && !child.soon}
              disabled={!!child.soon}
              icon={child.icon}
              label={child.label}
              soon={child.soon}
              onClick={() => !child.soon && onNavChange(child.id)}
            />
          ))}
        </Stack>
      </Collapse>
    </Box>
  )
}

export default function AppShell({
  productTag,
  nav,
  active,
  onNavChange,
  showLicensedModulesNav = false,
  licensedModules = [],
  licensedModuleNavPrefix = 'licensed-module:',
  email,
  accountMeta = 'Signed in',
  contextLabel,
  roleChips,
  headerTitle,
  onHeaderTitleClick,
  headerSubtitle,
  headerBreadcrumb,
  headerBreadcrumbRoot,
  headerCompact = false,
  headerTabs,
  headerMeta,
  pageActions,
  actionsLockedReason,
  notice,
  workspaceName,
  hideSidebar = false,
  flushContent = false,
  actions,
  onLogout,
  canChangePassword = false,
  superAdmin = false,
  children,
}: AppShellProps) {
  const [menuEl, setMenuEl] = useState<null | HTMLElement>(null)
  const [changePasswordOpen, setChangePasswordOpen] = useState(false)
  const [modulesOpen, setModulesOpen] = useState(false)
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({})
  const [navCollapsed, setNavCollapsed] = useState(readNavCollapsed)
  const [fullName, setFullName] = useState<string | null>(null)
  useEffect(() => {
    if (superAdmin) return
    let live = true
    api
      .authMe()
      .then((u) => live && setFullName(u.fullName?.trim() || null))
      .catch(() => undefined)
    return () => {
      live = false
    }
  }, [superAdmin, email])
  const initials = accountInitials(fullName, email)
  const moduleActive = active.startsWith(licensedModuleNavPrefix)

  const setCollapsed = useCallback((next: boolean) => {
    setNavCollapsed(next)
    writeNavCollapsed(next)
  }, [])

  const toggleCollapsed = useCallback(() => {
    setCollapsed(!navCollapsed)
  }, [navCollapsed, setCollapsed])

  useEffect(() => {
    if (hideSidebar) return
    const onKey = (e: KeyboardEvent) => {
      if (!(e.ctrlKey || e.metaKey) || e.key !== '\\') return
      const t = e.target as HTMLElement | null
      if (t && (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA' || t.isContentEditable)) return
      e.preventDefault()
      setNavCollapsed((prev) => {
        const next = !prev
        writeNavCollapsed(next)
        return next
      })
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [hideSidebar])

  const isGroupOpen = (item: NavItem) => {
    if (openGroups[item.id] !== undefined) return openGroups[item.id]
    // Doc 02 R-13: open Settings when a licensed module page is active so Modules stays reachable.
    if (item.id === 'settings' && moduleActive) return true
    return !!item.children?.some((c) => navContainsActive(c, active))
  }

  const toggleGroup = (id: string, currentlyOpen: boolean) => {
    setOpenGroups((prev) => ({ ...prev, [id]: !currentlyOpen }))
  }

  const railWidth = navCollapsed ? SIDEBAR_COLLAPSED : SIDEBAR_WIDTH

  return (
    <Box sx={{ display: 'flex', height: '100vh', overflow: 'hidden', bgcolor: 'background.default' }}>
      <Box
        component="a"
        href="#arbor-main"
        sx={{
          position: 'absolute',
          left: -9999,
          zIndex: 2000,
          '&:focus': {
            left: 8,
            top: 8,
            px: 1.5,
            py: 0.75,
            bgcolor: brand.paper,
            color: brand.ink,
            border: `1px solid ${brand.gold}`,
            borderRadius: 1,
            fontSize: '0.75rem',
            fontWeight: 600,
          },
        }}
      >
        Skip to content
      </Box>
      {!hideSidebar && (
      <Box
        component="nav"
        aria-label="Primary"
        data-testid="shell-nav"
        data-collapsed={navCollapsed ? 'true' : 'false'}
        sx={{
          width: railWidth,
          flexShrink: 0,
          display: { xs: 'none', md: 'flex' },
          flexDirection: 'column',
          color: brand.ink,
          bgcolor: brand.sidebar,
          height: '100%',
          borderRight: `1px solid ${brand.hairlineStrong}`,
          transition: NAV_MOTION,
          overflow: 'hidden',
          '@media (prefers-reduced-motion: reduce)': { transition: 'none' },
        }}
      >
        {/* Header: logo + title with the collapse toggle at the right (Pattern 1).
            Collapsed: logo above, toggle below, so the control stays top-anchored in both states. */}
        <Stack
          direction={navCollapsed ? 'column' : 'row'}
          spacing={navCollapsed ? 0.75 : 1.25}
          alignItems="center"
          justifyContent={navCollapsed ? 'center' : 'flex-start'}
          sx={{ px: navCollapsed ? 0.75 : 1.5, pt: 2, pb: navCollapsed ? 1.25 : 1.5, minHeight: 52 }}
        >
          <LogoMark size={navCollapsed ? 28 : 30} />
          {!navCollapsed && (
            <Box sx={{ minWidth: 0, flexGrow: 1 }}>
              <Typography
                sx={{
                  fontWeight: 700,
                  fontSize: '0.8125rem',
                  lineHeight: 1.2,
                  letterSpacing: '-0.02em',
                  color: brand.ink,
                }}
              >
                Arbor GRC
              </Typography>
              {productTag ? (
                <Typography
                  variant="caption"
                  sx={{
                    color: brand.navMuted,
                    display: 'block',
                    fontSize: '0.5625rem',
                    letterSpacing: '0.05em',
                    textTransform: 'uppercase',
                    fontWeight: 500,
                  }}
                >
                  {productTag}
                </Typography>
              ) : null}
            </Box>
          )}
          <Tooltip
            title={
              navCollapsed
                ? 'Expand navigation (Ctrl+\\)'
                : 'Collapse navigation (Ctrl+\\)'
            }
            placement="right"
            enterDelay={400}
            slotProps={collapsedNavTipSlotProps}
          >
            <Box
              component="button"
              type="button"
              data-testid="shell-nav-collapse"
              aria-label={navCollapsed ? 'Expand navigation' : 'Collapse navigation'}
              aria-expanded={!navCollapsed}
              aria-controls={NAV_LIST_ID}
              onClick={toggleCollapsed}
              sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
                width: 32,
                height: 32,
                border: 0,
                borderRadius: 1,
                font: 'inherit',
                cursor: 'pointer',
                color: brand.nav,
                bgcolor: 'transparent',
                '&:hover': { bgcolor: brand.sidebarHover, color: brand.ink },
                '&:focus-visible': { outline: `2px solid ${brand.accent}`, outlineOffset: 1 },
              }}
            >
              {navCollapsed ? (
                <KeyboardDoubleArrowRightRoundedIcon sx={{ fontSize: 20 }} />
              ) : (
                <KeyboardDoubleArrowLeftRoundedIcon sx={{ fontSize: 20 }} />
              )}
            </Box>
          </Tooltip>
        </Stack>

        <Divider sx={{ borderColor: brand.hairline }} />

        <Stack
          id={NAV_LIST_ID}
          spacing={0.2}
          sx={{ p: navCollapsed ? 0.5 : 1, flexGrow: 1, overflow: 'auto', overflowX: 'hidden' }}
          role="list"
        >
          {nav.map((item) => {
            if (item.children?.length) {
              const open = isGroupOpen(item)
              const childActive = item.children.some((c) => navContainsActive(c, active))
              const nestModulesHere = item.id === 'settings' && showLicensedModulesNav
              const settingsChildActive = childActive || (nestModulesHere && moduleActive)
              if (navCollapsed) {
                return (
                  <CollapsedCascadeParent
                    key={item.id}
                    item={item}
                    selected={settingsChildActive}
                    active={active}
                    onNavigate={onNavChange}
                    modules={nestModulesHere ? licensedModules : undefined}
                    licensedModuleNavPrefix={licensedModuleNavPrefix}
                  />
                )
              }
              return (
                <Box key={item.id}>
                  <Box
                    component="button"
                    type="button"
                    aria-expanded={open}
                    aria-label={item.label}
                    onClick={() => toggleGroup(item.id, open)}
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 1,
                      width: '100%',
                      px: 1.1,
                      py: 0.75,
                      border: 0,
                      borderRadius: 1,
                      textAlign: 'left',
                      font: 'inherit',
                      cursor: 'pointer',
                      color: settingsChildActive ? brand.accentHover : brand.nav,
                      bgcolor: settingsChildActive && !open ? brand.sidebarSelected : 'transparent',
                      '&:hover': { bgcolor: brand.sidebarHover, color: brand.ink },
                      '&:focus-visible': { outline: `2px solid ${brand.accent}`, outlineOffset: 1 },
                    }}
                  >
                    <Box sx={{ display: 'flex', color: settingsChildActive ? brand.accent : 'inherit', opacity: 0.85 }}>
                      {item.icon}
                    </Box>
                    <Typography
                      variant="body2"
                      sx={{ fontWeight: 650, flexGrow: 1, fontSize: '0.8125rem', letterSpacing: '-0.01em' }}
                    >
                      {item.label}
                    </Typography>
                    <ExpandMoreRoundedIcon
                      sx={{
                        fontSize: 16,
                        color: brand.muted,
                        transform: open ? 'rotate(0deg)' : 'rotate(-90deg)',
                        transition: 'transform .15s ease',
                      }}
                    />
                  </Box>
                  <Collapse in={open} timeout="auto" unmountOnExit={false}>
                    <Stack spacing={0.15} sx={{ mt: 0.15 }}>
                      {item.children.map((child) =>
                        child.children?.length ? (
                          <NestedNavGroup
                            key={child.id}
                            item={child}
                            indent={1}
                            active={active}
                            openGroups={openGroups}
                            onToggle={toggleGroup}
                            onNavChange={onNavChange}
                          />
                        ) : (
                          <NavButton
                            key={child.id}
                            indent={1}
                            selected={child.id === active && !child.soon}
                            disabled={!!child.soon}
                            icon={child.icon}
                            label={child.label}
                            soon={child.soon}
                            onClick={() => !child.soon && onNavChange(child.id)}
                          />
                        ),
                      )}
                      {nestModulesHere && (
                        <>
                          <Box
                            component="button"
                            type="button"
                            aria-expanded={modulesOpen}
                            onClick={() => setModulesOpen((o) => !o)}
                            sx={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: 1,
                              width: '100%',
                              pl: 2.75,
                              pr: 1.1,
                              py: 0.65,
                              border: 0,
                              borderRadius: 1,
                              textAlign: 'left',
                              font: 'inherit',
                              cursor: 'pointer',
                              color: moduleActive ? brand.accentHover : brand.nav,
                              bgcolor: moduleActive && !modulesOpen ? brand.sidebarSelected : 'transparent',
                              '&:hover': { bgcolor: brand.sidebarHover, color: brand.ink },
                            }}
                          >
                            <Box sx={{ display: 'flex', color: moduleActive ? brand.accent : 'inherit', opacity: 0.85 }}>
                              <ExtensionRoundedIcon sx={{ fontSize: 16 }} />
                            </Box>
                            <Typography
                              variant="body2"
                              sx={{ fontWeight: 650, flexGrow: 1, fontSize: '0.75rem', letterSpacing: '-0.01em' }}
                            >
                              Modules
                            </Typography>
                            <Typography variant="caption" sx={{ color: brand.muted, mr: 0.25 }}>
                              {licensedModules.length}
                            </Typography>
                            <ExpandMoreRoundedIcon
                              sx={{
                                fontSize: 16,
                                color: brand.muted,
                                transform: modulesOpen ? 'rotate(0deg)' : 'rotate(-90deg)',
                                transition: 'transform .15s ease',
                              }}
                            />
                          </Box>
                          <Collapse in={modulesOpen} timeout="auto" unmountOnExit={false}>
                            <Stack spacing={0.15} sx={{ mt: 0.15, pl: 1 }}>
                              {licensedModules.length === 0 ? (
                                <Typography
                                  variant="caption"
                                  sx={{ px: 2.5, py: 0.75, color: brand.muted, display: 'block' }}
                                >
                                  No modules licensed
                                </Typography>
                              ) : (
                                licensedModules.map((m) => {
                                  const id = `${licensedModuleNavPrefix}${m.code}`
                                  return (
                                    <NavButton
                                      key={m.code}
                                      indent={1}
                                      selected={active === id}
                                      icon={<ExtensionRoundedIcon />}
                                      label={m.label}
                                      onClick={() => onNavChange(id)}
                                    />
                                  )
                                })
                              )}
                            </Stack>
                          </Collapse>
                        </>
                      )}
                    </Stack>
                  </Collapse>
                </Box>
              )
            }
            if (navCollapsed) {
              return (
                <CollapsedIconButton
                  key={item.id}
                  label={item.soon ? `${item.label} (Soon)` : item.label}
                  icon={item.icon}
                  selected={item.id === active && !item.soon}
                  disabled={!!item.soon}
                  testId={`shell-nav-leaf-${item.id}`}
                  onActivate={() => !item.soon && onNavChange(item.id)}
                />
              )
            }
            return (
              <NavButton
                key={item.id}
                selected={item.id === active && !item.soon}
                disabled={!!item.soon}
                icon={item.icon}
                label={item.label}
                soon={item.soon}
                onClick={() => !item.soon && onNavChange(item.id)}
              />
            )
          })}
        </Stack>

        <Box
          sx={{
            px: navCollapsed ? 0.5 : 1,
            pt: 1,
            pb: 1.5,
            borderTop: `1px solid ${brand.hairline}`,
            display: 'flex',
            flexDirection: 'column',
            gap: 0.75,
            alignItems: navCollapsed ? 'center' : 'stretch',
          }}
        >
          {workspaceName ? (
            <Tooltip
              title={navCollapsed ? workspaceName : ''}
              placement="right"
              enterDelay={400}
              slotProps={navCollapsed ? collapsedNavTipSlotProps : undefined}
            >
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: navCollapsed ? 'center' : 'flex-start',
                  gap: 1,
                  px: navCollapsed ? 0 : 1.25,
                  py: 1,
                  borderRadius: 0.75,
                  bgcolor: 'rgba(184, 134, 11, 0.06)',
                  width: navCollapsed ? 40 : 'auto',
                  minHeight: 36,
                }}
              >
                <Box
                  sx={{
                    width: 7,
                    height: 7,
                    borderRadius: '50%',
                    bgcolor: brand.gold,
                    flexShrink: 0,
                  }}
                />
                {!navCollapsed && (
                  <Typography
                    sx={{
                      fontSize: '0.6875rem',
                      fontWeight: 600,
                      color: brand.ink,
                      lineHeight: 1.2,
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {workspaceName}
                  </Typography>
                )}
              </Box>
            </Tooltip>
          ) : null}
        </Box>
      </Box>
      )}

      <Box sx={{ flexGrow: 1, minWidth: 0, display: 'flex', flexDirection: 'column', height: '100%' }}>
        <AppBar position="sticky" sx={{ flexShrink: 0, overflow: 'visible' }}>
          <Toolbar
            sx={{
              gap: 1,
              px: { xs: 1.5, md: 2 },
              minHeight: headerBreadcrumb ? 64 : layout.topbarHeight,
              '@media (min-width: 600px)': {
                minHeight: headerBreadcrumb ? 64 : layout.topbarHeight,
              },
              alignItems: 'center',
              flexWrap: 'nowrap',
              overflow: 'visible',
            }}
          >
            {hideSidebar ? (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mr: 0.5, flexShrink: 0 }}>
                <LogoMark size={30} />
                <Box sx={{ display: { xs: 'none', sm: 'block' }, minWidth: 0 }}>
                  <Typography sx={{ fontWeight: 700, fontSize: '0.8125rem', lineHeight: 1.2, letterSpacing: '-0.02em' }}>
                    Arbor GRC
                  </Typography>
                  {productTag ? (
                    <Typography
                      variant="caption"
                      sx={{ color: 'text.secondary', fontSize: '0.5625rem', letterSpacing: '0.05em', textTransform: 'uppercase' }}
                    >
                      {productTag}
                    </Typography>
                  ) : null}
                </Box>
              </Box>
            ) : (
              <Box sx={{ display: { md: 'none' } }}>
                <LogoMark size={26} />
              </Box>
            )}
            {!hideSidebar ? (
            <Box
              sx={{
                minWidth: 0,
                flex: headerTabs || headerMeta ? '0 1 18rem' : '1 1 13.5rem',
                maxWidth: headerTabs || headerMeta ? { lg: '26rem' } : undefined,
                overflow: 'hidden',
              }}
            >
              {headerBreadcrumb || headerBreadcrumbRoot ? (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, minWidth: 0 }}>
                {headerBreadcrumbRoot ? (
                  <Tooltip
                    title={`Back to ${headerBreadcrumbRoot.label}`}
                    enterDelay={200}
                    slotProps={{
                      tooltip: {
                        sx: {
                          bgcolor: brand.paper,
                          color: brand.ink,
                          border: `1px solid ${brand.border}`,
                          boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
                          fontSize: '0.75rem',
                          fontWeight: 500,
                        },
                      },
                    }}
                  >
                    <IconButton
                      data-testid="shell-back-button"
                      aria-label={`Back to ${headerBreadcrumbRoot.label}`}
                      onClick={headerBreadcrumbRoot.onClick}
                      sx={{
                        width: 32,
                        height: 32,
                        flexShrink: 0,
                        borderRadius: '6px',
                        border: `1px solid ${brand.accent}`,
                        color: brand.accentHover,
                        bgcolor: 'transparent',
                        '&:hover': { borderColor: brand.accentHover, bgcolor: alpha(brand.gold, 0.08) },
                      }}
                    >
                      <ArrowBackRoundedIcon sx={{ fontSize: 18 }} />
                    </IconButton>
                  </Tooltip>
                ) : null}
                <Box sx={{ minWidth: 0, flex: 1 }}>
                  {headerCompact ? null : onHeaderTitleClick ? (
                  <Box
                    component="button"
                    type="button"
                    onClick={onHeaderTitleClick}
                    sx={{
                      display: 'block',
                      width: '100%',
                      border: 0,
                      p: 0,
                      m: 0,
                      bgcolor: 'transparent',
                      textAlign: 'left',
                      font: 'inherit',
                      fontWeight: 700,
                      fontSize: { xs: '1rem', sm: '0.9375rem' },
                      lineHeight: 1.25,
                      letterSpacing: '-0.025em',
                      color: brand.ink,
                      cursor: 'pointer',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                      '&:hover': { textDecoration: 'underline' },
                    }}
                  >
                    {headerTitle}
                  </Box>
                  ) : (
                  <Typography
                    sx={{
                      fontWeight: 700,
                      fontSize: { xs: '1rem', sm: '0.9375rem' },
                      lineHeight: 1.25,
                      letterSpacing: '-0.025em',
                      color: brand.ink,
                    }}
                    noWrap
                  >
                    {headerTitle}
                  </Typography>
                  )}
                  <Box
                    sx={{
                      display: 'flex',
                      alignItems: 'baseline',
                      gap: 0.5,
                      minWidth: 0,
                      mt: 0.25,
                    }}
                  >
                    {headerBreadcrumbRoot ? (
                      <Box
                        component="button"
                        type="button"
                        data-testid="shell-breadcrumb-back"
                        onClick={headerBreadcrumbRoot.onClick}
                        sx={{
                          border: 0,
                          p: 0,
                          m: 0,
                          bgcolor: 'transparent',
                          font: 'inherit',
                          fontSize: '0.75rem',
                          lineHeight: 1.3,
                          color: brand.goldDark,
                          fontWeight: 600,
                          cursor: 'pointer',
                          flexShrink: 0,
                          whiteSpace: 'nowrap',
                          textDecoration: 'underline',
                          textUnderlineOffset: '2px',
                          '&:hover': { color: brand.accentHover },
                        }}
                      >
                        {headerBreadcrumbRoot.label}
                      </Box>
                    ) : null}
                    {headerBreadcrumb ? (
                    <Tooltip title={headerBreadcrumb} enterDelay={400}>
                    <Typography
                      sx={{
                        fontSize: '0.75rem',
                        color: brand.muted,
                        lineHeight: 1.3,
                        fontWeight: 500,
                        minWidth: 0,
                        flex: 1,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                      }}
                      noWrap
                    >
                      {headerBreadcrumbRoot ? `${headerCompact ? '/ ' : '› '}${headerBreadcrumb}` : headerBreadcrumb}
                    </Typography>
                    </Tooltip>
                    ) : null}
                  </Box>
                </Box>
                </Box>
              ) : (
              <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={{ xs: 0.15, sm: 0.75 }}
                alignItems={{ sm: 'center' }}
                sx={{ minWidth: 0 }}
              >
                <Typography
                  sx={{
                    fontWeight: 700,
                    fontSize: { xs: '1rem', sm: '0.95rem' },
                    lineHeight: 1.2,
                    letterSpacing: '-0.025em',
                    flexShrink: 0,
                  }}
                  noWrap
                >
                  {headerTitle}
                </Typography>
                {(headerSubtitle || contextLabel) && (
                  <Stack
                    direction="row"
                    spacing={0.5}
                    alignItems="center"
                    sx={{ minWidth: 0, display: { xs: 'none', sm: 'flex' } }}
                  >
                    <Typography variant="caption" color="text.secondary" sx={{ opacity: 0.45 }}>
                      ·
                    </Typography>
                    {headerSubtitle && (
                      <Typography variant="caption" color="text.secondary" noWrap>
                        {headerSubtitle}
                      </Typography>
                    )}
                    {headerSubtitle && contextLabel && (
                      <Typography variant="caption" color="text.secondary" sx={{ opacity: 0.45 }}>
                        ·
                      </Typography>
                    )}
                    {contextLabel && (
                      <Typography
                        variant="caption"
                        color="text.secondary"
                        noWrap
                        sx={{ fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace' }}
                      >
                        {contextLabel}
                      </Typography>
                    )}
                  </Stack>
                )}
              </Stack>
              )}
              {!headerBreadcrumb && (headerSubtitle || contextLabel) && (
                <Typography
                  variant="caption"
                  color="text.secondary"
                  noWrap
                  sx={{ display: { xs: 'block', sm: 'none' }, mt: 0.1 }}
                >
                  {[headerSubtitle, contextLabel].filter(Boolean).join(' · ')}
                </Typography>
              )}
            </Box>
            ) : (
              <Box sx={{ flexGrow: 1 }} />
            )}
            {headerMeta && (
              <Box
                sx={{
                  display: { xs: 'none', md: 'flex' },
                  alignItems: 'center',
                  minWidth: 0,
                  flex: '1 1 auto',
                  overflow: 'hidden',
                  px: 1.5,
                }}
              >
                {headerMeta}
              </Box>
            )}
            {headerTabs && (
              <Stack
                direction="row"
                spacing={0.5}
                alignItems="center"
                sx={{
                  display: { xs: 'none', lg: 'flex' },
                  mr: 0.5,
                  minWidth: 0,
                  flex: '1 1 auto',
                  overflowX: 'auto',
                  scrollbarWidth: 'none',
                  '&::-webkit-scrollbar': { display: 'none' },
                }}
              >
                {headerTabs}
              </Stack>
            )}
            {pageActions && (
              <Tooltip title={actionsLockedReason ?? ''} disableHoverListener={!actionsLockedReason}>
                <Stack
                  direction="row"
                  spacing={1}
                  alignItems="center"
                  flexWrap="nowrap"
                  useFlexGap
                  sx={{
                    mr: 1,
                    flexShrink: 0,
                    justifyContent: { xs: 'flex-end', sm: 'flex-start' },
                    ...(actionsLockedReason
                      ? { opacity: 0.45, pointerEvents: 'none', filter: 'grayscale(0.2)' }
                      : {}),
                  }}
                >
                  {pageActions}
                </Stack>
              </Tooltip>
            )}
            <Stack
              direction="row"
              spacing={0.5}
              alignItems="center"
              sx={{ display: { xs: 'none', sm: 'flex' }, mr: 0.5 }}
            >
              {roleChips}
            </Stack>
            <Stack direction="row" alignItems="center" spacing={1} sx={{ flexShrink: 0 }}>
              {actions}
              <Divider
                orientation="vertical"
                flexItem
                sx={{
                  mx: 0.25,
                  my: 1.1,
                  borderColor: brand.border,
                  display: { xs: 'none', sm: 'block' },
                }}
              />
              <Tooltip title={fullName ?? email}>
                <IconButton onClick={(e) => setMenuEl(e.currentTarget)} size="small" aria-label="Account menu">
                  <Avatar
                    sx={{
                      width: 30,
                      height: 30,
                      bgcolor: brand.accent,
                      fontSize: '0.6875rem',
                      fontWeight: 700,
                    }}
                  >
                    {initials}
                  </Avatar>
                </IconButton>
              </Tooltip>
            </Stack>
            <Menu
              anchorEl={menuEl}
              open={!!menuEl}
              onClose={() => setMenuEl(null)}
              transformOrigin={{ horizontal: 'right', vertical: 'top' }}
              anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
            >
              <Box sx={{ px: 2, py: 1.1, minWidth: 220 }}>
                <Typography variant="body2" fontWeight={650}>
                  {fullName ?? email}
                </Typography>
                {fullName && (
                  <Typography variant="body2" color="text.secondary" sx={{ fontSize: '0.75rem' }}>
                    {email}
                  </Typography>
                )}
                <Typography variant="caption" color="text.secondary">
                  {accountMeta}
                </Typography>
              </Box>
              <Divider />
              {canChangePassword && (
                <MenuItem
                  onClick={() => {
                    setMenuEl(null)
                    setChangePasswordOpen(true)
                  }}
                >
                  <ListItemIcon>
                    <LockResetRoundedIcon fontSize="small" />
                  </ListItemIcon>
                  Change Password
                </MenuItem>
              )}
              <MenuItem
                onClick={() => {
                  setMenuEl(null)
                  onLogout()
                }}
              >
                <ListItemIcon>
                  <LogoutIcon fontSize="small" />
                </ListItemIcon>
                Sign Out
              </MenuItem>
            </Menu>
            <ChangePasswordDialog
              open={changePasswordOpen}
              superAdmin={superAdmin}
              onClose={() => setChangePasswordOpen(false)}
            />
          </Toolbar>
        </AppBar>

        <Box
          id="arbor-main"
          component="main"
          key={active}
          sx={{
            p: flushContent ? 0 : layout.contentPadding,
            flexGrow: 1,
            minHeight: 0,
            display: 'flex',
            flexDirection: 'column',
            overflow: flushContent ? 'hidden' : 'auto',
            animation: 'arborPageIn .18s ease',
            '@keyframes arborPageIn': {
              from: { opacity: 0 },
              to: { opacity: 1 },
            },
          }}
        >
          {notice ? <Box sx={{ flexShrink: 0, mb: flushContent ? 0 : 1.25 }}>{notice}</Box> : null}
          {children}
        </Box>
      </Box>
    </Box>
  )
}
