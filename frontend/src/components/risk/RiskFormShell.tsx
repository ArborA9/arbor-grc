import { type ReactNode } from 'react'
import { Box, Typography } from '@mui/material'
import FormSectionShell, { type FormSection } from '../ui/FormSectionShell'
import { FORM_SECTION_SCROLL_MARGIN } from '../ui/formLayout'
import { brand } from '../../theme'

export type RiskFormSection = FormSection

/** Risk module wrapper: prefer `FormSectionShell` for new full-page forms. */
export default function RiskFormShell({
  sections,
  children,
  fill = true,
}: {
  sections: RiskFormSection[]
  children: ReactNode
  fill?: boolean
}) {
  return (
    <FormSectionShell sections={sections} fill={fill}>
      {children}
    </FormSectionShell>
  )
}

export function RiskFormSectionBlock({
  sectionId,
  title,
  subtitle,
  children,
}: {
  sectionId: string
  title: string
  subtitle?: string
  children: ReactNode
}) {
  return (
    <Box id={sectionId} sx={{ scrollMarginTop: FORM_SECTION_SCROLL_MARGIN, mb: 2.5 }}>
      <Typography
        variant="caption"
        sx={{
          display: 'block',
          fontWeight: 700,
          letterSpacing: '0.04em',
          textTransform: 'uppercase',
          color: brand.muted,
          mb: 1.25,
        }}
      >
        {title}
      </Typography>
      {subtitle ? (
        <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1.25, mt: -0.5 }}>
          {subtitle}
        </Typography>
      ) : null}
      {children}
    </Box>
  )
}
