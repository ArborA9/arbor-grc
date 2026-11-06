import { alpha } from '@mui/material/styles'
import { brand } from '../../theme'

export const loginFadeIn = {
  animation: 'loginFadeIn 0.45s ease both',
  '@keyframes loginFadeIn': {
    from: { opacity: 0, transform: 'translateY(10px)' },
    to: { opacity: 1, transform: 'none' },
  },
} as const

/** Subtle vertical drift: enterprise-grade, barely perceptible. */
export const loginFloat = (delay = '0s', duration = '12s') =>
  ({
    animation: `loginFloat ${duration} ease-in-out ${delay} infinite`,
    willChange: 'transform',
    '@keyframes loginFloat': {
      '0%, 100%': { transform: 'translateY(0)' },
      '50%': { transform: 'translateY(-5px)' },
    },
  }) as const

/** Consistent decorative card scale: large hero metric vs medium widgets. */
export const loginDecorSizes = {
  large: { width: 216, p: 1.5, minHeight: 118 },
  medium: { width: 172, p: 1.25, minHeight: 96 },
} as const

export const loginFieldSx = {
  '& .MuiOutlinedInput-root': {
    backgroundColor: brand.paper,
    transition: 'box-shadow 0.2s ease, border-color 0.2s ease',
    '&.Mui-focused': {
      boxShadow: `0 0 0 3px ${alpha(brand.gold, 0.14)}`,
    },
    '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
      borderColor: brand.gold,
    },
  },
  '& .MuiInputLabel-root.Mui-focused': {
    color: brand.goldDark,
  },
} as const

export const loginButtonSx = {
  minHeight: 48,
  py: 1.25,
  transition: 'transform 0.15s ease, box-shadow 0.2s ease, background-color 0.2s ease',
  '&:hover:not(:disabled)': {
    transform: 'translateY(-1px)',
    boxShadow: `0 6px 20px ${alpha(brand.gold, 0.28)}`,
  },
  '&:focus-visible': {
    outline: `2px solid ${brand.gold}`,
    outlineOffset: 2,
  },
} as const

export const loginChipSx = {
  height: 26,
  fontSize: '0.65rem',
  fontWeight: 700,
  cursor: 'default',
  transition: 'transform 175ms ease, box-shadow 175ms ease, border-color 175ms ease, filter 175ms ease',
  '&:hover': {
    transform: 'translateY(-2px)',
    boxShadow: '0 3px 10px rgba(28, 25, 23, 0.06)',
    filter: 'brightness(1.04)',
  },
} as const

/** Decorative GRC preview cards: always visually secondary to the login panel. */
export const loginDecorCardSx = {
  borderRadius: 1.5,
  bgcolor: alpha(brand.paper, 0.9),
  border: `1px solid ${alpha(brand.border, 0.75)}`,
  boxShadow: '0 6px 20px rgba(28, 25, 23, 0.035), 0 1px 3px rgba(28, 25, 23, 0.025)',
  backdropFilter: 'blur(6px)',
  opacity: 0.92,
} as const

export const loginDecorLayerSx = {
  position: 'absolute' as const,
  pointerEvents: 'none' as const,
  zIndex: 0,
  overflow: 'visible' as const,
} as const

export const loginCardElevatedSx = {
  position: 'relative' as const,
  zIndex: 2,
  boxShadow: '0 14px 36px rgba(28, 25, 23, 0.055), 0 2px 10px rgba(184, 134, 11, 0.035)',
} as const

export const loginPillarIconSx = {
  mt: '2px',
  width: 36,
  height: 36,
  borderRadius: 1.25,
  display: 'grid',
  placeItems: 'center',
  border: `1px solid ${brand.goldMid}`,
  bgcolor: 'rgba(255,255,255,0.78)',
  color: brand.goldDark,
  flexShrink: 0,
  boxShadow: '0 1px 5px rgba(184, 134, 11, 0.07)',
  '& .MuiSvgIcon-root': {
    fontSize: 19,
  },
} as const
