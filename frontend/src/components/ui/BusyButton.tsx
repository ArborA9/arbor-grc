import { Button, CircularProgress, Tooltip, type ButtonProps } from '@mui/material'

/** Contained/outlined action that shows a spinner while busy (G-04). */
export default function BusyButton({
  busy,
  busyLabel,
  children,
  disabled,
  disabledReason,
  onClick,
  ...rest
}: ButtonProps & { busy?: boolean; busyLabel?: string; disabledReason?: string }) {
  // Stay at full color while busy. MUI disabled fades the control, so a slow click looks like nothing happened.
  const blocked = Boolean(disabled) && !busy
  const button = (
    <Button
      {...rest}
      disabled={blocked}
      aria-busy={busy ? true : undefined}
      onClick={busy ? (event) => event.preventDefault() : onClick}
      startIcon={busy ? <CircularProgress size={14} color="inherit" /> : rest.startIcon}
    >
      {busy && busyLabel ? busyLabel : children}
    </Button>
  )
  if (blocked && disabledReason) {
    return (
      <Tooltip title={disabledReason}>
        <span style={{ flexShrink: 0 }}>{button}</span>
      </Tooltip>
    )
  }
  return button
}
