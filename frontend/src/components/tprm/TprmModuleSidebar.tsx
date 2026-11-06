import { Box, List, ListItemButton, ListItemText, Typography } from '@mui/material'
import { brand } from '../../theme'
import { TPRM_NAV, type TprmModuleSection } from './tprmNav'

export default function TprmModuleSidebar({
  section,
  onSectionChange,
}: {
  section: TprmModuleSection
  onSectionChange: (s: TprmModuleSection) => void
}) {
  return (
    <Box
      component="nav"
      aria-label="Third-Party Risk"
      data-testid="tprm-module-sidebar"
      sx={{
        width: 200,
        flexShrink: 0,
        bgcolor: brand.sidebar,
        borderRight: `1px solid ${brand.border}`,
        py: 1.5,
        px: 1,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <Typography
        variant="caption"
        sx={{
          px: 1,
          py: 0.5,
          fontWeight: 700,
          letterSpacing: '0.1em',
          textTransform: 'uppercase',
          color: 'text.disabled',
          fontSize: 9,
        }}
      >
        Third-Party Risk
      </Typography>
      <List dense disablePadding>
        {TPRM_NAV.map((item) => {
          const active = section === item.id
          return (
            <ListItemButton
              key={item.id}
              selected={active}
              onClick={() => onSectionChange(item.id)}
              data-testid={`tprm-nav-${item.id}`}
              sx={{
                borderRadius: 1,
                mb: 0.25,
                py: 0.75,
                pl: 1.25,
                position: 'relative',
                '&.Mui-selected': {
                  bgcolor: brand.sidebarSelected,
                  color: brand.goldDark,
                  '&::before': {
                    content: '""',
                    position: 'absolute',
                    left: 0,
                    top: 6,
                    bottom: 6,
                    width: 3,
                    borderRadius: '0 2px 2px 0',
                    bgcolor: brand.gold,
                  },
                },
              }}
            >
              <ListItemText
                primary={item.label}
                primaryTypographyProps={{ fontSize: '0.8125rem', fontWeight: active ? 700 : 500 }}
              />
            </ListItemButton>
          )
        })}
      </List>
    </Box>
  )
}
