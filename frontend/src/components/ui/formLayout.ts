import type { SxProps, Theme } from '@mui/material'

/**
 * Locked form layout rules: UI Kit Phase 14.
 */

/** More than this many inputs → full-page form (not dialog). */
export const FULL_PAGE_FIELD_THRESHOLD = 8

/** Simple CRUD (Assets, KRI create, dept profile): centered column. */
export const SIMPLE_FORM_MAX_WIDTH = 640

/** Medium forms (incident detail, evidence upload) when not full complex layout. */
export const MEDIUM_FORM_MAX_WIDTH = 720

/** Enum / status / date selects: content width, not the full card. */
export const COMPACT_SELECT_MIN_WIDTH = 168
export const COMPACT_SELECT_MAX_WIDTH = 220

/** Native date / datetime: wide enough for `dd/mm/yyyy` + calendar, not a half-card. */
export const DATE_FIELD_WIDTH = 148
export const DATETIME_FIELD_WIDTH = 232

/** Observations, comments, gap description: wide enough to write, not full-bleed. */
export const NARRATIVE_FIELD_MAX_WIDTH = 560

/** Long narratives stop growing after this many lines; extra text scrolls inside. */
export const NARRATIVE_MAX_ROWS = 6

/** Read-only long text in tables / wells: wrap, then scroll after NARRATIVE_MAX_ROWS. */
export const narrativeReadScrollSx: SxProps<Theme> = {
  maxHeight: `calc(0.75rem * 1.35 * ${NARRATIVE_MAX_ROWS})`,
  overflowY: 'auto',
  overflowX: 'hidden',
  whiteSpace: 'pre-wrap',
  wordBreak: 'break-word',
  minWidth: 0,
  pointerEvents: 'auto',
}

/** Scroll offset for section-tab anchors inside FormSectionShell. */
export const FORM_SECTION_SCROLL_MARGIN = 96

export const sectionAnchorSx: SxProps<Theme> = {
  scrollMarginTop: FORM_SECTION_SCROLL_MARGIN,
  minWidth: 0,
}

/** Complex multi-section forms use full workspace width + responsive column grids. */
export const complexFormContainerSx: SxProps<Theme> = {
  width: '100%',
}

/** Default create/edit field grid: 3 columns on desktop. Do not mix multiline with single-line in the same row. */
export const formFieldGridSx: SxProps<Theme> = {
  display: 'grid',
  gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))', md: 'repeat(3, minmax(0, 1fr))' },
  gap: 1,
  alignItems: 'start',
}

/** Dense create / dossier: four compact fields on desktop. */
export const fourColFieldSx: SxProps<Theme> = {
  display: 'grid',
  gridTemplateColumns: {
    xs: '1fr',
    sm: 'repeat(2, minmax(0, 1fr))',
    md: 'repeat(4, minmax(0, 1fr))',
  },
  gap: 1,
  alignItems: 'start',
}

/** Side-by-side narratives or paired actions. */
export const twoColFieldSx: SxProps<Theme> = {
  display: 'grid',
  gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))' },
  gap: 1,
  alignItems: 'start',
}

/**
 * Caption-above field + button on one row.
 * Align to the 32px control, not the label: otherwise Add/Save sits in the caption band.
 */
export const labeledFieldRowSx: SxProps<Theme> = {
  alignItems: { xs: 'stretch', sm: 'flex-end' },
}

/** Match kit control height when a button sits beside CompactTextField / FormSelect. */
export const controlActionSx: SxProps<Theme> = {
  height: 32,
  minHeight: 32,
  flexShrink: 0,
}

export function shouldUseFullPageForm(fieldCount: number): boolean {
  return fieldCount > FULL_PAGE_FIELD_THRESHOLD
}

/** Dialog is appropriate only for short flows (assign, reject, confirm, score). */
export function shouldUseDialogForm(fieldCount: number): boolean {
  return fieldCount <= FULL_PAGE_FIELD_THRESHOLD
}
