import { forwardRef } from 'react'
import { Stack, type StackProps } from '@mui/material'
import { labeledFieldRowSx } from './formLayout'

/** Horizontal row of caption-above fields plus an action, aligned on the control baseline. */
const FieldActionRow = forwardRef<HTMLDivElement, StackProps>(function FieldActionRow(
  { children, sx, ...rest },
  ref,
) {
  return (
    <Stack
      ref={ref}
      {...rest}
      direction={rest.direction ?? { xs: 'column', sm: 'row' }}
      spacing={rest.spacing ?? 1}
      sx={[labeledFieldRowSx, sx] as StackProps['sx']}
    >
      {children}
    </Stack>
  )
})

export default FieldActionRow
