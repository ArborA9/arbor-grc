import { Stack, type StackProps } from '@mui/material'
import { labeledFieldRowSx } from './formLayout'

/** Horizontal row of caption-above fields plus an action, aligned on the control baseline. */
export default function FieldActionRow({ children, sx, ...rest }: StackProps) {
  return (
    <Stack
      {...rest}
      direction={rest.direction ?? { xs: 'column', sm: 'row' }}
      spacing={rest.spacing ?? 1}
      sx={[labeledFieldRowSx, sx] as StackProps['sx']}
    >
      {children}
    </Stack>
  )
}
