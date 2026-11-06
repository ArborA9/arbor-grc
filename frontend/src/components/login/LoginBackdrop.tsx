import { Box } from '@mui/material'
import { alpha } from '@mui/material/styles'
import { brand } from '../../theme'

/** Full-bleed warm sand & gold atmosphere: no hard panel split. */
export default function LoginBackdrop() {
  return (
    <Box aria-hidden sx={{ position: 'absolute', inset: 0, overflow: 'hidden', pointerEvents: 'none' }}>
      <Box
        sx={{
          position: 'absolute',
          inset: 0,
          background: `
            radial-gradient(ellipse 120% 80% at 0% 0%, ${brand.goldLight} 0%, transparent 52%),
            radial-gradient(ellipse 90% 70% at 100% 100%, ${alpha(brand.goldMid, 0.65)} 0%, transparent 48%),
            radial-gradient(ellipse 60% 50% at 72% 18%, ${alpha(brand.gold, 0.12)} 0%, transparent 55%),
            linear-gradient(155deg, ${brand.sidebar} 0%, ${brand.hover} 38%, ${brand.canvas} 72%, ${brand.paper} 100%)
          `,
        }}
      />

      {/* Subtle grid */}
      <Box
        sx={{
          position: 'absolute',
          inset: 0,
          opacity: 0.35,
          backgroundImage: `
            linear-gradient(${alpha(brand.gold, 0.07)} 1px, transparent 1px),
            linear-gradient(90deg, ${alpha(brand.gold, 0.07)} 1px, transparent 1px)
          `,
          backgroundSize: '48px 48px',
          maskImage: 'linear-gradient(180deg, rgba(0,0,0,0.5) 0%, transparent 85%)',
        }}
      />

      {/* Decorative rings: ambient background */}
      {[
        { top: '12%', right: '6%', size: 300, opacity: 0.09 },
        { bottom: '8%', left: '4%', size: 240, opacity: 0.065 },
        { top: '55%', left: '38%', size: 200, opacity: 0.055 },
      ].map((ring, i) => (
        <Box
          key={i}
          sx={{
            position: 'absolute',
            ...('top' in ring ? { top: ring.top } : {}),
            ...('bottom' in ring ? { bottom: ring.bottom } : {}),
            left: 'left' in ring ? ring.left : undefined,
            right: 'right' in ring ? ring.right : undefined,
            width: ring.size,
            height: ring.size,
            borderRadius: '50%',
            border: `1px solid ${alpha(brand.gold, ring.opacity)}`,
            boxShadow: `inset 0 0 60px ${alpha(brand.goldLight, 0.22)}`,
          }}
        />
      ))}
    </Box>
  )
}
