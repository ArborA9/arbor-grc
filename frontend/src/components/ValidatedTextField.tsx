import TextField, { type TextFieldProps } from '@mui/material/TextField'
import { visibleFieldError } from '../validation/apiErrors'

type Props = Omit<TextFieldProps, 'error' | 'helperText'> & {
  /** Pre-computed message from useFormFields().fieldError(): shown as-is. */
  validationMessage?: string
  /** Client-side validation message (shown after blur or submit when validationMessage omitted). */
  errorText?: string
  touched?: boolean
  submitAttempted?: boolean
  serverError?: string
  onBlurField?: () => void
}

/** MUI TextField that shows validation errors on blur or after submit attempt. */
export default function ValidatedTextField({
  validationMessage,
  errorText = '',
  touched = false,
  submitAttempted = false,
  serverError,
  onBlurField,
  onBlur,
  ...rest
}: Props) {
  const message =
    validationMessage ??
    visibleFieldError(errorText, touched, submitAttempted, serverError)
  return (
    <TextField
      {...rest}
      error={!!message}
      helperText={message}
      onBlur={(e) => {
        onBlurField?.()
        onBlur?.(e)
      }}
    />
  )
}
