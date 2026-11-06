/**
 * Collapsed primary nav: icon-only cascade.
 *
 * Gesture split (deterministic, no hover race):
 *  - Hover  = label tooltip only (fast). Never opens a rail.
 *  - Click  = toggles that item's nested icon rail (right of the icon).
 *  - A rail stays open until: click away, Escape, navigate, or its parent closes.
 * While an item's rail is open its own tip is suppressed (the rail is the label).
 */
import { useCallback, useEffect, useState, type ReactNode, type Ref } from 'react'
import { Box, ClickAwayListener, Popper, Stack, Tooltip } from '@mui/material'
import ExtensionRoundedIcon from '@mui/icons-material/ExtensionRounded'
import { brand, layout } from '../../theme'

const ROW = layout.sidebarNavRow
const RAIL_W = layout.sidebarCollapsedWidth
/** Hover label delay. Short: in a collapsed rail the label is the only text. */
const TIP_ENTER_MS = 150

/** Collapsed-nav tip only — light kit chip; global MuiTooltip stays ink/white. */
export const collapsedNavTipSlotProps = {
  tooltip: {
    sx: {
      bgcolor: brand.paper,
      color: brand.ink,
      border: `1px solid ${brand.hairlineStrong}`,
      boxShadow: brand.shadowDropdown,
      fontSize: '0.75rem',
      fontWeight: 500,
      px: 1,
      py: 0.5,
      maxWidth: 220,
    },
  },
} as const

export interface CascadeNavItem {
  id: string
  label: string
  icon: ReactNode
  soon?: boolean
  children?: CascadeNavItem[]
  opensPage?: boolean
}

export interface CascadeModuleItem {
  code: string
  label: string
}

function branchActive(item: CascadeNavItem, active: string): boolean {
  if (item.id === active) return true
  return !!item.children?.some((c) => branchActive(c, active))
}

const railPaperSx = {
  width: RAIL_W,
  bgcolor: brand.sidebar,
  border: `1px solid ${brand.hairlineStrong}`,
  borderRadius: 1,
  boxShadow: '0 8px 24px rgba(28, 25, 23, 0.12)',
  py: 0.5,
  px: 0.5,
  pointerEvents: 'auto',
} as const

const railPopperModifiers = [
  { name: 'offset', options: { offset: [0, 0] } },
  { name: 'preventOverflow', options: { padding: 8 } },
  { name: 'flip', enabled: false },
]

function IconHit({
  label,
  icon,
  selected,
  disabled,
  expanded,
  hasChildren,
  showTooltip,
  testId,
  onActivate,
  buttonRef,
}: {
  label: string
  icon: ReactNode
  selected?: boolean
  disabled?: boolean
  expanded?: boolean
  hasChildren?: boolean
  /** False while this item's own rail is open (the rail is the label then). */
  showTooltip: boolean
  testId?: string
  onActivate: () => void
  buttonRef?: Ref<HTMLButtonElement>
}) {
  // One DOM tree always (Tooltip never unmounts) so Popper keeps a live anchor.
  // Controlled open so the tip drops the instant showTooltip flips false.
  const [tipOpen, setTipOpen] = useState(false)
  const tipAllowed = showTooltip && !disabled
  return (
    <Tooltip
      title={label}
      placement="right"
      enterDelay={TIP_ENTER_MS}
      enterNextDelay={TIP_ENTER_MS}
      disableInteractive
      open={tipAllowed && tipOpen}
      onOpen={() => setTipOpen(true)}
      onClose={() => setTipOpen(false)}
      slotProps={collapsedNavTipSlotProps}
    >
      <Box component="span" sx={{ display: 'block', width: '100%' }}>
        <Box
          component="button"
          ref={buttonRef}
          type="button"
          role="listitem"
          aria-label={label}
          aria-haspopup={hasChildren ? 'menu' : undefined}
          aria-expanded={hasChildren ? !!expanded : undefined}
          aria-current={selected && !hasChildren ? 'page' : undefined}
          disabled={disabled}
          data-testid={testId}
          onClick={onActivate}
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: '100%',
            minHeight: ROW,
            border: 0,
            borderRadius: 1,
            font: 'inherit',
            cursor: disabled ? 'not-allowed' : 'pointer',
            position: 'relative',
            opacity: disabled ? 0.45 : 1,
            color: selected || expanded ? brand.accentHover : brand.nav,
            bgcolor: selected || expanded ? brand.sidebarSelected : 'transparent',
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
            '&:hover': disabled ? undefined : { bgcolor: brand.sidebarHover, color: brand.ink },
            '&:focus-visible': { outline: `2px solid ${brand.accent}`, outlineOffset: 1 },
            '& .MuiSvgIcon-root': { fontSize: 22 },
          }}
        >
          {icon}
        </Box>
      </Box>
    </Tooltip>
  )
}

function CascadeRail({
  items,
  active,
  onNavigate,
  testId,
  modules,
  licensedModuleNavPrefix,
}: {
  items: CascadeNavItem[]
  active: string
  onNavigate: (id: string) => void
  testId?: string
  modules?: CascadeModuleItem[]
  licensedModuleNavPrefix?: string
}) {
  return (
    <Stack data-testid={testId} role="menu" spacing={0.15} sx={{ width: '100%' }}>
      {items.map((child) => (
        <CascadeNode key={child.id} item={child} active={active} onNavigate={onNavigate} />
      ))}
      {modules?.map((m) => {
        const id = `${licensedModuleNavPrefix ?? 'licensed-module:'}${m.code}`
        return (
          <IconHit
            key={m.code}
            label={m.label}
            icon={<ExtensionRoundedIcon />}
            selected={active === id}
            showTooltip
            testId={`shell-nav-icon-${id}`}
            onActivate={() => onNavigate(id)}
          />
        )
      })}
    </Stack>
  )
}

/** Item inside a rail. Leaves navigate; groups toggle a deeper icon rail on click. */
function CascadeNode({
  item,
  active,
  onNavigate,
}: {
  item: CascadeNavItem
  active: string
  onNavigate: (id: string) => void
}) {
  const [anchorEl, setAnchorEl] = useState<HTMLButtonElement | null>(null)
  const [open, setOpen] = useState(false)
  const hasKids = !!item.children?.length
  const selected = branchActive(item, active)

  const buttonRef = useCallback((el: HTMLButtonElement | null) => setAnchorEl(el), [])

  const activate = () => {
    if (item.soon) return
    if (hasKids) {
      setOpen((v) => !v)
      if (item.opensPage) onNavigate(item.id)
      return
    }
    onNavigate(item.id)
  }

  const hit = (
    <IconHit
      buttonRef={buttonRef}
      label={item.soon ? `${item.label} (Soon)` : item.label}
      icon={item.icon}
      selected={selected}
      disabled={!!item.soon}
      hasChildren={hasKids}
      expanded={open}
      showTooltip={!open}
      testId={`shell-nav-icon-${item.id}`}
      onActivate={activate}
    />
  )

  if (!hasKids) return hit

  return (
    <ClickAwayListener onClickAway={() => setOpen(false)} mouseEvent="onMouseDown">
      <Box>
        {hit}
        <Popper
          open={open && !!anchorEl}
          anchorEl={anchorEl}
          placement="right-start"
          // In the parent DOM: ClickAway treats the rail as inside, and it unmounts with the parent.
          disablePortal
          modifiers={railPopperModifiers}
          style={{ zIndex: 1400 }}
        >
          <Box sx={railPaperSx}>
            <CascadeRail
              items={item.children!}
              active={active}
              onNavigate={onNavigate}
              testId={`shell-nav-flyout-${item.id}`}
            />
          </Box>
        </Popper>
      </Box>
    </ClickAwayListener>
  )
}

/** Top-level collapsed group: icon on the main rail + cascading icon strips. */
export function CollapsedCascadeParent({
  item,
  selected,
  active,
  onNavigate,
  modules,
  licensedModuleNavPrefix,
}: {
  item: CascadeNavItem
  selected: boolean
  active: string
  onNavigate: (id: string) => void
  modules?: CascadeModuleItem[]
  licensedModuleNavPrefix?: string
}) {
  const [anchorEl, setAnchorEl] = useState<HTMLButtonElement | null>(null)
  const [open, setOpen] = useState(false)

  const buttonRef = useCallback((el: HTMLButtonElement | null) => setAnchorEl(el), [])

  const go = (id: string) => {
    setOpen(false)
    onNavigate(id)
  }

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open])

  return (
    <ClickAwayListener onClickAway={() => setOpen(false)} mouseEvent="onMouseDown">
      <Box>
        <IconHit
          buttonRef={buttonRef}
          label={item.label}
          icon={item.icon}
          selected={selected}
          hasChildren
          expanded={open}
          showTooltip={!open}
          testId={`shell-nav-group-${item.id}`}
          onActivate={() => setOpen((v) => !v)}
        />
        <Popper
          open={open && !!anchorEl}
          anchorEl={anchorEl}
          placement="right-start"
          modifiers={railPopperModifiers}
          style={{ zIndex: 1300 }}
        >
          <Box sx={railPaperSx}>
            <CascadeRail
              items={item.children ?? []}
              active={active}
              onNavigate={go}
              testId={`shell-nav-flyout-${item.id}`}
              modules={modules}
              licensedModuleNavPrefix={licensedModuleNavPrefix}
            />
          </Box>
        </Popper>
      </Box>
    </ClickAwayListener>
  )
}

export function CollapsedIconButton({
  label,
  icon,
  selected,
  disabled,
  testId,
  onActivate,
  buttonRef,
}: {
  label: string
  icon: ReactNode
  selected?: boolean
  disabled?: boolean
  testId?: string
  onActivate: () => void
  buttonRef?: Ref<HTMLButtonElement>
}) {
  return (
    <IconHit
      buttonRef={buttonRef}
      label={label}
      icon={icon}
      selected={selected}
      disabled={disabled}
      showTooltip
      testId={testId}
      onActivate={onActivate}
    />
  )
}
