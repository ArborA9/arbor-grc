import { alpha, createTheme } from '@mui/material'

/**
 * Arbor GRC: Option A · Warm Sand & Gold (client UI kit).
 */
export const brand = {
  gold: '#B8860B',
  goldDark: '#92530A',
  goldLight: '#FEF3C7',
  goldMid: '#FDE68A',

  canvas: '#FAFAF8',
  paper: '#FFFFFF',
  sidebar: '#F5EDD8',
  hover: '#FAF7F2',
  tableHeader: '#FAF7F2',

  ink: '#0A0A0A',
  inkSoft: '#1C1917',
  muted: '#3F3F46',
  mutedLight: '#52525B',
  nav: '#1C1917',
  navMuted: '#3F3F46',

  /** Primary accent: maps to kit gold for MUI `primary` */
  accent: '#B8860B',
  accentHover: '#92530A',
  accentMuted: '#FEF3C7',
  accentSoft: '#FDE68A',

  sidebarHover: 'rgba(184, 134, 11, 0.08)',
  sidebarSelected: 'rgba(184, 134, 11, 0.12)',
  /**
   * Solid match for left-nav selected gold (12% gold over sidebar sand).
   * Use on white/paper so chips and section bars match the sidebar highlight.
   */
  sidebarSelectedFill: '#EEE1BF',

  border: '#EDE8DF',
  borderLight: '#F5F0E8',
  hairline: '#EDE8DF',
  hairlineStrong: '#EDE8DF',

  shadowCard: '0 1px 4px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)',
  shadowDropdown: '0 8px 24px rgba(0,0,0,0.12)',
}

/** Semantic palettes: use for badges/heatmaps, not brand chrome. */
export const semantic = {
  risk: {
    critical: '#DC2626',
    high: '#EA580C',
    medium: '#CA8A04',
    low: '#16A34A',
    veryLow: '#94A3B8',
  },
  /** Filled risk chips (register + scoring). */
  riskChip: {
    critical: { bg: '#DC2626', text: '#FFFFFF' },
    high: { bg: '#FFEDD5', text: '#C2410C' },
    medium: { bg: '#FEF3C7', text: '#854D0E' },
    low: { bg: '#DCFCE7', text: '#166534' },
    veryLow: { bg: '#F1F5F9', text: '#475569' },
  },
  heatmap: {
    critical: '#DC2626',
    veryHigh: '#EF4444',
    high: '#F97316',
    medHigh: '#FBBF24',
    medium: '#FDE68A',
    low: '#D1FAE5',
    veryLow: '#F1F5F9',
    empty: '#FAFAF8',
  },
  lifecycle: {
    draft: { bg: '#F3F4F6', text: '#374151' },
    open: { bg: '#FEF9C3', text: '#854D0E' },
    closed: { bg: '#DCFCE7', text: '#166534' },
    treat: { bg: '#EDE9FE', text: '#5B21B6' },
  },
  gap: {
    compliant: { bg: '#DCFCE7', text: '#166534' },
    partial: { bg: '#FEF9C3', text: '#854D0E' },
    nonCompliant: { bg: '#FEE2E2', text: '#991B1B' },
    planned: { bg: '#EDE9FE', text: '#5B21B6' },
    notAssessed: { bg: '#FAF7F2', text: '#A8A29E' },
    na: { bg: '#F3F4F6', text: '#6B7280' },
  },
  soa: {
    yes: { bg: '#DCFCE7', text: '#166534' },
    no: { bg: '#FEE2E2', text: '#991B1B' },
    na: { bg: '#F3F4F6', text: '#6B7280' },
  },
  impl: {
    full: { bg: '#DCFCE7', text: '#166534' },
    partial: { bg: '#FEF9C3', text: '#854D0E' },
    planned: { bg: '#EDE9FE', text: '#5B21B6' },
    none: { bg: '#FEE2E2', text: '#991B1B' },
  },
  /** Gap workflow pills. Same pairs as the kit badges (grey, amber, gold, purple, red, green). */
  workflow: {
    notStarted: { bg: '#F3F4F6', text: '#6B7280' },
    draft: { bg: '#FEF9C3', text: '#854D0E' },
    withContributor: { bg: '#EDE9FE', text: '#5B21B6' },
    submitted: { bg: '#FEF3C7', text: '#92530A' },
    sentBack: { bg: '#FEE2E2', text: '#991B1B' },
    final: { bg: '#DCFCE7', text: '#166534' },
  },
}

const successMain = semantic.risk.low
const successDark = '#166534'

const INPUT_HEIGHT = 32
const SIDEBAR_WIDTH_PX = 232
const SIDEBAR_COLLAPSED_WIDTH_PX = 64
const SIDEBAR_NAV_ROW_PX = 40
const TOPBAR_HEIGHT_PX = 56

export const layout = {
  sidebarWidth: SIDEBAR_WIDTH_PX,
  /** Icon-only primary rail (collapsible nav). */
  sidebarCollapsedWidth: SIDEBAR_COLLAPSED_WIDTH_PX,
  /** Primary nav row height (expanded + collapsed). */
  sidebarNavRow: SIDEBAR_NAV_ROW_PX,
  topbarHeight: TOPBAR_HEIGHT_PX,
  contentPadding: '20px 16px',
  /** Overview, contributor home, and the external audit workspace. Narrower windows fill the pane. */
  wideCanvasMax: 2400,
  /** Policy, CAPA, KRI, incident, RoPA, DPIA, and treatment-milestone task forms. */
  contributorFormMax: 720,
}

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: brand.accent,
      dark: brand.accentHover,
      light: brand.accentSoft,
      contrastText: '#fff',
    },
    secondary: { main: brand.ink, contrastText: '#fff' },
    success: { main: successMain, dark: successDark, contrastText: '#fff' },
    warning: { main: '#CA8A04' },
    error: { main: '#DC2626' },
    info: { main: brand.mutedLight, dark: brand.muted, light: brand.hover },
    background: { default: brand.canvas, paper: brand.paper },
    text: { primary: brand.ink, secondary: brand.muted },
    divider: brand.hairline,
    action: {
      hover: alpha(brand.gold, 0.06),
      selected: brand.sidebarSelected,
      disabled: alpha(brand.ink, 0.5),
      disabledBackground: alpha(brand.ink, 0.05),
    },
  },
  shape: { borderRadius: 6 },
  spacing: 8,
  typography: {
    fontFamily: '"Poppins", system-ui, -apple-system, Segoe UI, sans-serif',
    h4: {
      fontWeight: 700,
      letterSpacing: '-0.02em',
      fontSize: '1.75rem',
      lineHeight: 1.15,
    },
    h5: {
      fontWeight: 700,
      letterSpacing: '-0.02em',
      fontSize: '1.35rem',
      lineHeight: 1.2,
    },
    h6: {
      fontWeight: 600,
      letterSpacing: '-0.01em',
      fontSize: '1rem',
    },
    subtitle1: { fontWeight: 600, fontSize: '0.9375rem' },
    subtitle2: { fontWeight: 600, fontSize: '0.8125rem' },
    button: { fontWeight: 600, letterSpacing: 0, textTransform: 'none' },
    overline: {
      fontWeight: 600,
      letterSpacing: '0.08em',
      fontSize: '0.6875rem',
      color: brand.navMuted,
    },
    body1: { fontSize: '0.875rem', lineHeight: 1.45, color: brand.ink, fontWeight: 500 },
    body2: { fontSize: '0.8125rem', lineHeight: 1.4, color: brand.ink, fontWeight: 500 },
        caption: { fontSize: '0.6875rem', lineHeight: 1.35, color: brand.muted, fontWeight: 500 },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          backgroundColor: brand.canvas,
          WebkitFontSmoothing: 'antialiased',
        },
        'input:-webkit-autofill, input:-webkit-autofill:hover, input:-webkit-autofill:focus, textarea:-webkit-autofill, select:-webkit-autofill':
          {
            WebkitBoxShadow: `0 0 0 1000px ${brand.paper} inset`,
            WebkitTextFillColor: brand.ink,
            caretColor: brand.ink,
            transition: 'background-color 99999s ease-in-out 0s',
          },
        '::selection': {
          backgroundColor: alpha(brand.gold, 0.22),
        },
      },
    },
    MuiAppBar: {
      defaultProps: { elevation: 0, color: 'inherit' },
      styleOverrides: {
        root: {
          backgroundColor: brand.paper,
          color: brand.ink,
          borderBottom: `1px solid ${brand.border}`,
          boxShadow: 'none',
        },
      },
    },
    MuiToolbar: {
      styleOverrides: {
        root: {
          minHeight: TOPBAR_HEIGHT_PX,
          '@media (min-width: 600px)': { minHeight: TOPBAR_HEIGHT_PX },
        },
      },
    },
    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          border: `1px solid ${brand.border}`,
          borderRadius: 9,
          boxShadow: brand.shadowCard,
          backgroundImage: 'none',
        },
      },
    },
    MuiPaper: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: { backgroundImage: 'none' },
        outlined: { borderColor: brand.border },
        rounded: { borderRadius: 9 },
      },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          borderRadius: 6,
          textTransform: 'none',
          fontWeight: 600,
          minHeight: INPUT_HEIGHT,
          paddingInline: 14,
          fontSize: '0.75rem',
          lineHeight: 1.25,
          transition: 'background-color .12s ease, border-color .12s ease, color .12s ease',
          '&.Mui-disabled': { opacity: 0.62 },
        },
        sizeLarge: { minHeight: 40, paddingInline: 16, fontSize: '0.8125rem' },
        sizeSmall: { minHeight: INPUT_HEIGHT, paddingInline: 12, fontSize: '0.75rem' },
        containedPrimary: {
          backgroundColor: brand.accent,
          '&:hover': { backgroundColor: brand.accentHover },
          '&.Mui-disabled': {
            backgroundColor: alpha(brand.accent, 0.35),
            color: '#fff',
            opacity: 1,
          },
        },
        containedSecondary: {
          backgroundColor: brand.ink,
          '&:hover': { backgroundColor: brand.inkSoft },
        },
        outlined: {
          borderColor: brand.border,
          backgroundColor: brand.paper,
          color: brand.ink,
          '&:hover': {
            borderColor: brand.goldMid,
            backgroundColor: brand.hover,
          },
        },
        outlinedPrimary: {
          borderColor: brand.accent,
          color: brand.accentHover,
          backgroundColor: 'transparent',
          '&:hover': {
            borderColor: brand.accentHover,
            backgroundColor: alpha(brand.gold, 0.08),
            color: brand.accentHover,
          },
          '&.Mui-disabled': {
            borderColor: brand.border,
            color: brand.muted,
          },
        },
        text: {
          color: brand.muted,
          '&:hover': { backgroundColor: brand.hover, color: brand.ink },
        },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        root: {
          borderRadius: 6,
          '&:hover': { backgroundColor: brand.hover },
        },
      },
    },
    MuiTextField: { defaultProps: { size: 'small', variant: 'outlined' } },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 6,
          backgroundColor: brand.paper,
          minHeight: INPUT_HEIGHT,
          '&:not(.MuiInputBase-multiline)': {
            height: INPUT_HEIGHT,
            maxHeight: INPUT_HEIGHT,
            boxSizing: 'border-box',
          },
          transition: 'box-shadow .12s ease, border-color .12s ease',
          '& .MuiOutlinedInput-notchedOutline': { borderColor: brand.border },
          '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: alpha(brand.ink, 0.25) },
          '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
            borderColor: brand.accent,
            borderWidth: 1,
          },
          '&.Mui-focused': {
            boxShadow: `0 0 0 3px ${alpha(brand.gold, 0.18)}`,
          },
          '&.Mui-disabled': { backgroundColor: brand.hover },
        },
        input: {
          paddingTop: 0,
          paddingBottom: 0,
          fontSize: '0.75rem',
          fontWeight: 500,
          lineHeight: 1.25,
          height: INPUT_HEIGHT - 2,
          boxSizing: 'border-box',
        },
        inputSizeSmall: {
          paddingTop: 0,
          paddingBottom: 0,
          height: INPUT_HEIGHT - 2,
        },
        adornedStart: { paddingLeft: 10 },
      },
    },
    MuiSelect: {
      defaultProps: { size: 'small' },
      styleOverrides: {
        select: {
          fontSize: '0.75rem',
          fontWeight: 500,
          lineHeight: 1.25,
          minHeight: 'unset',
          display: 'flex',
          alignItems: 'center',
          paddingTop: 0,
          paddingBottom: 0,
          height: INPUT_HEIGHT - 2,
          boxSizing: 'border-box',
        },
        icon: {
          '.Mui-disabled &': { display: 'none' },
        },
      },
    },
    MuiMenuItem: {
      styleOverrides: {
        root: { fontSize: '0.75rem', minHeight: INPUT_HEIGHT },
      },
    },
    MuiFormHelperText: {
      styleOverrides: { root: { marginLeft: 2, marginTop: 3, fontSize: '0.6875rem', color: brand.muted } },
    },
    MuiInputLabel: {
      styleOverrides: {
        root: { fontSize: '0.75rem', '&.Mui-focused': { color: brand.accentHover } },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 600, borderRadius: 20, height: 22 },
        sizeSmall: { height: 22, fontSize: '0.6875rem' },
        label: { paddingInline: 8 },
        outlined: {
          borderColor: brand.border,
          backgroundColor: brand.paper,
          color: brand.ink,
        },
        colorSuccess: {
          backgroundColor: semantic.gap.compliant.bg,
          color: semantic.gap.compliant.text,
        },
        colorWarning: {
          backgroundColor: semantic.gap.partial.bg,
          color: semantic.gap.partial.text,
        },
        colorPrimary: {
          backgroundColor: brand.accentMuted,
          color: brand.accentHover,
        },
        colorSecondary: {
          backgroundColor: brand.hover,
          color: brand.ink,
        },
        colorInfo: {
          backgroundColor: brand.hover,
          color: brand.muted,
        },
      },
    },
    MuiToggleButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
          borderRadius: 6,
          borderColor: brand.border,
          py: 0.75,
          '&.Mui-selected': {
            backgroundColor: brand.accentMuted,
            color: brand.accentHover,
            borderColor: brand.goldMid,
            '&:hover': { backgroundColor: alpha(brand.gold, 0.16) },
          },
        },
      },
    },
    MuiTabs: {
      styleOverrides: {
        indicator: { height: 2, borderRadius: 1, backgroundColor: brand.accent },
      },
    },
    MuiTab: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
          minHeight: 40,
          fontSize: '0.8125rem',
          color: brand.muted,
          '&.Mui-selected': { color: brand.ink },
          '&.Mui-focusVisible': {
            outline: `2px solid ${alpha(brand.gold, 0.45)}`,
            outlineOffset: 2,
          },
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        head: {
          fontSize: '0.6875rem',
          fontWeight: 700,
          letterSpacing: '0.08em',
          textTransform: 'uppercase',
          color: brand.muted,
          background: brand.sidebar,
          borderBottom: `1.5px solid ${brand.border}`,
          py: 0.55,
          px: 1.25,
          zIndex: 2,
          verticalAlign: 'middle',
          '&:last-of-type': { pr: 1 },
        },
        root: {
          borderBottom: `1px solid ${brand.border}`,
          py: 0.55,
          px: 1.25,
          fontSize: '0.75rem',
          verticalAlign: 'middle',
          '&:last-of-type': { pr: 1 },
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          '&.MuiTableRow-hover:hover': {
            backgroundColor: brand.sidebarHover,
          },
          '&.Mui-selected': {
            backgroundColor: brand.sidebarSelected,
          },
          '&.Mui-selected:hover': {
            backgroundColor: alpha(brand.gold, 0.16),
          },
          '&:nth-of-type(even):not(.Mui-selected)': {
            backgroundColor: alpha(brand.ink, 0.018),
          },
        },
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: 9,
          fontSize: '0.75rem',
          alignItems: 'center',
        },
        icon: {
          marginRight: 8,
          padding: 0,
          alignItems: 'center',
        },
        message: {
          padding: 0,
          display: 'flex',
          alignItems: 'center',
        },
        outlinedInfo: { backgroundColor: brand.accentMuted, borderColor: brand.goldMid },
      },
    },
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          fontSize: '0.65rem',
          borderRadius: 4,
          backgroundColor: brand.ink,
          padding: '5px 8px',
        },
      },
    },
    MuiMenu: {
      styleOverrides: {
        paper: {
          border: `1px solid ${brand.border}`,
          boxShadow: brand.shadowDropdown,
          borderRadius: 8,
          marginTop: 4,
        },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: 12,
          border: `1px solid ${brand.border}`,
          boxShadow: '0 16px 40px rgba(28, 25, 23, 0.14)',
        },
      },
    },
    MuiDivider: {
      styleOverrides: {
        root: { borderColor: brand.border },
      },
    },
    MuiSwitch: {
      styleOverrides: {
        switchBase: {
          '&.Mui-checked': {
            color: brand.accent,
            '& + .MuiSwitch-track': { backgroundColor: brand.accent },
          },
        },
      },
    },
    MuiLinearProgress: {
      styleOverrides: {
        root: { borderRadius: 4, backgroundColor: brand.border },
        bar: { borderRadius: 4, backgroundColor: brand.accent },
      },
    },
  },
})

export default theme
