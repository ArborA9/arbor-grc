const LABELS: Record<string, string> = {
  approve: 'Approve',
  reject: 'Reject',
  submit: 'Submit',
  return: 'Return',
  send_back: 'Send Back',
  cancel: 'Cancel',
  complete: 'Complete',
  start: 'Start',
}

export function workflowActionLabel(code: string): string {
  if (LABELS[code]) return LABELS[code]
  return code.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase())
}
