/**
 * CAPA Tracker row 4 labels from docs/audit/Audit_Module_MVP_v2.xlsx.
 * Asterisks and sheet hints ((auto), (Manual Entry), (Org)) are stripped for UI.
 * Journey STEP 4 names Audit Title, Conducted by, and CAPA Description.
 */
export const CAPA_EXCEL = {
  capaId: 'CAPA ID',
  findingId: 'Finding ID',
  findingTitle: 'Finding Title',
  classification: 'Classification',
  clauseRef: 'Clause Ref',
  dateRaised: 'Date Raised',
  correction: 'Correction (the immediate fix)',
  rootCause: 'Root Cause',
  correctiveActionPlan: 'Corrective Action Plan',
  dateAssigned: 'Date Assigned',
  agreedTargetDate: 'Agreed Target Date',
  assignedContributor: 'Assigned Contributor',
  contributorDepartment: 'Contributor Department',
  evidenceRequired: 'Evidence Required',
  responseDescription: 'Response Description',
  uploadEvidence: 'Upload Evidence',
  reviewRemarks: 'Corrective Action plan review remarks',
  evidenceOfCompletion: 'Evidence of Completion',
  verificationDate: 'Verification Date',
  verificationResult: 'Verification Result',
  auditTitle: 'Audit Title',
  conductedBy: 'Conducted by',
  capaDescription: 'CAPA Description',
} as const
