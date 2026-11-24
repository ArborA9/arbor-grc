import { Box } from '@mui/material'

export default function LogoMark({ size = 30 }: { size?: number }) {
  return (
    <Box
      component="img"
      src="/brand/arbor-logo.svg"
      alt=""
      aria-hidden
      draggable={false}
      sx={{ width: size, height: size, display: 'block', flexShrink: 0 }}
    />
  )
}
