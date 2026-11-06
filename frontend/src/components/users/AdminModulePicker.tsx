import { Box, Checkbox, FormControlLabel, FormGroup, Typography } from '@mui/material'
import { moduleDescription, moduleShortLabel } from '../../moduleDisplay'

/** Licensed modules an Admin may be given. Hidden unless the Admin role is selected without Owner. */
export default function AdminModulePicker(props: {
  modules: string[]
  selected: string[]
  disabled?: boolean
  onChange: (next: string[]) => void
}) {
  const { modules, selected, disabled, onChange } = props
  return (
    <Box>
      <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.5 }}>
        Modules
      </Typography>
      <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.75 }}>
        Choose which licensed modules this admin can open. Users, organisation, and notifications stay available.
      </Typography>
      {modules.length === 0 ? (
        <Typography variant="caption" color="warning.main">
          No licensed modules are available to assign.
        </Typography>
      ) : (
        <FormGroup>
          {modules.map((code) => (
            <FormControlLabel
              key={code}
              sx={{ alignItems: 'flex-start', mx: 0, my: 0.25 }}
              control={
                <Checkbox
                  size="small"
                  disabled={disabled}
                  checked={selected.includes(code)}
                  onChange={() =>
                    onChange(selected.includes(code) ? selected.filter((c) => c !== code) : [...selected, code])
                  }
                  sx={{ pt: 0.25 }}
                />
              }
              label={
                <Box>
                  <Typography variant="body2" fontWeight={600} sx={{ lineHeight: 1.3 }}>
                    {moduleShortLabel(code)}
                  </Typography>
                  {moduleDescription(code) && (
                    <Typography variant="caption" color="text.secondary" display="block">
                      {moduleDescription(code)}
                    </Typography>
                  )}
                </Box>
              }
            />
          ))}
        </FormGroup>
      )}
    </Box>
  )
}
