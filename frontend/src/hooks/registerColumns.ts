import type { ColumnDefinition } from './columnVisibility'

export const EVIDENCE_REGISTER_COLUMNS: ColumnDefinition[] = [
  { id: 'title', label: 'Title', width: 'minmax(10rem, 1fr)', required: true },
  { id: 'classification', label: 'Classification', width: 'max-content' },
  { id: 'status', label: 'Status', width: 'max-content' },
  { id: 'version', label: 'Ver', width: 'max-content' },
  { id: 'file', label: 'File', width: 'max-content' },
  { id: 'links', label: 'Links', width: 'max-content' },
  { id: 'expires', label: 'Expires', width: 'max-content' },
]

export const INCIDENT_REGISTER_COLUMNS: ColumnDefinition[] = [
  { id: 'id', label: 'ID', width: 'max-content', required: true },
  { id: 'title', label: 'Title', width: 'minmax(10rem, 1fr)', required: true },
  { id: 'priority', label: 'Priority', width: 'max-content' },
  { id: 'type', label: 'Type', width: 'max-content' },
  { id: 'status', label: 'Status', width: 'max-content' },
  { id: 'responseDue', label: 'Response due', width: 'max-content' },
]

export const ASSET_REGISTER_COLUMNS: ColumnDefinition[] = [
  { id: 'displayId', label: 'Asset ID', width: 'max-content' },
  { id: 'name', label: 'Asset Name', width: 'minmax(10rem, 1fr)', required: true },
  { id: 'type', label: 'Asset Type', width: 'max-content' },
  { id: 'cia', label: 'Asset Value', width: 'max-content' },
  { id: 'criticality', label: 'Criticality Tier', width: 'max-content' },
  { id: 'classification', label: 'Derived Classification', width: 'max-content' },
  { id: 'status', label: 'Status', width: 'max-content' },
]

export const TPRM_REGISTER_COLUMNS: ColumnDefinition[] = [
  { id: 'name', label: 'Vendor', width: 'minmax(10rem, 1fr)', required: true },
  { id: 'type', label: 'Type', width: 'max-content' },
  { id: 'inherent', label: 'Inherent', width: 'max-content' },
  { id: 'tier', label: 'Tier', width: 'max-content' },
  { id: 'criticality', label: 'Criticality', width: 'max-content' },
  { id: 'posture', label: 'Posture', width: 'max-content' },
  { id: 'compliance', label: 'Compliance', width: 'max-content' },
  { id: 'state', label: 'State', width: 'max-content' },
  { id: 'nextReview', label: 'Next review', width: 'max-content' },
]

export const KRI_REGISTER_COLUMNS: ColumnDefinition[] = [
  { id: 'id', label: 'ID', width: 'max-content', required: true },
  { id: 'name', label: 'Name', width: 'minmax(10rem, 1fr)', required: true },
  { id: 'rag', label: 'RAG', width: 'max-content' },
  { id: 'value', label: 'Value', width: 'max-content' },
  { id: 'trend', label: 'Trend', width: 'max-content' },
  { id: 'due', label: 'Next due', width: 'max-content' },
  { id: 'status', label: 'Status', width: 'max-content' },
]

export const POLICY_REGISTER_COLUMNS: ColumnDefinition[] = [
  { id: 'title', label: 'Title', width: 'minmax(10rem, 1fr)', required: true },
  { id: 'category', label: 'Category', width: 'max-content' },
  { id: 'version', label: 'Version', width: 'max-content' },
  { id: 'status', label: 'Status', width: 'max-content' },
  { id: 'ack', label: 'Ack', width: 'max-content' },
]

export const WORKFLOW_REGISTER_COLUMNS: ColumnDefinition[] = [
  { id: 'record', label: 'Record', width: 'minmax(10rem, 1fr)', required: true },
  { id: 'definition', label: 'Definition', width: 'max-content' },
  { id: 'state', label: 'State', width: 'max-content' },
  { id: 'status', label: 'Status', width: 'max-content' },
  { id: 'started', label: 'Started', width: 'max-content' },
]

export const PRIVACY_INVENTORY_COLUMNS: ColumnDefinition[] = [
  { id: 'id', label: 'ID', width: 'max-content', required: true },
  { id: 'dataType', label: 'Data type', width: 'minmax(8rem, 1fr)', required: true },
  { id: 'subjects', label: 'Subjects', width: 'max-content' },
  { id: 'location', label: 'Location', width: 'max-content' },
  { id: 'country', label: 'Country', width: 'max-content' },
  { id: 'retention', label: 'Retention', width: 'max-content' },
  { id: 'status', label: 'Status', width: 'max-content' },
]

export const PRIVACY_ROPA_COLUMNS: ColumnDefinition[] = [
  { id: 'id', label: 'ID', width: 'max-content', required: true },
  { id: 'activity', label: 'Activity', width: 'minmax(10rem, 1fr)', required: true },
  { id: 'basis', label: 'Legal basis', width: 'max-content' },
  { id: 'designation', label: 'Designation', width: 'max-content' },
  { id: 'transfers', label: 'Transfers', width: 'max-content' },
  { id: 'status', label: 'Status', width: 'max-content' },
]

export const PRIVACY_DSR_COLUMNS: ColumnDefinition[] = [
  { id: 'id', label: 'ID', width: 'max-content', required: true },
  { id: 'type', label: 'Type', width: 'max-content' },
  { id: 'subject', label: 'Subject', width: 'minmax(8rem, 1fr)' },
  { id: 'due', label: 'Due', width: 'max-content' },
  { id: 'status', label: 'Status', width: 'max-content' },
]

export const BCMS_PROFILE_COLUMNS: ColumnDefinition[] = [
  { id: 'dept', label: 'Org unit', width: 'minmax(10rem, 1fr)', required: true },
  { id: 'staff', label: 'Total staff', width: 'max-content' },
  { id: 'physical', label: 'Physical location', width: 'max-content' },
  { id: 'alternate', label: 'Alternate location', width: 'max-content' },
  { id: 'assets', label: 'Linked assets', width: 'max-content' },
  { id: 'updated', label: 'Updated', width: 'max-content' },
]

export const BCMS_BIA_COLUMNS: ColumnDefinition[] = [
  { id: 'reference', label: 'Reference', width: 'minmax(10rem, 1fr)', required: true },
  { id: 'dept', label: 'Org unit', width: 'max-content' },
  { id: 'processes', label: 'Processes', width: 'max-content' },
  { id: 'critical', label: 'Critical', width: 'max-content' },
  { id: 'status', label: 'Status', width: 'minmax(14rem, 1.2fr)' },
]

export const BCMS_PLAN_COLUMNS: ColumnDefinition[] = [
  { id: 'name', label: 'Name', width: 'minmax(10rem, 1fr)', required: true },
  { id: 'type', label: 'Type', width: 'max-content' },
  { id: 'version', label: 'Version', width: 'max-content' },
  { id: 'status', label: 'Status', width: 'max-content' },
  { id: 'owner', label: 'Owner', width: 'max-content' },
]

export const BCMS_EXERCISE_COLUMNS: ColumnDefinition[] = [
  { id: 'name', label: 'Name', width: 'minmax(10rem, 1fr)', required: true },
  { id: 'type', label: 'Type', width: 'max-content' },
  { id: 'scheduled', label: 'Scheduled', width: 'max-content' },
  { id: 'facilitator', label: 'Facilitator', width: 'max-content' },
  { id: 'status', label: 'Status', width: 'max-content' },
]

export const ASSESSMENT_PROJECT_COLUMNS: ColumnDefinition[] = [
  { id: 'project', label: 'Project', width: 'minmax(10rem, 1fr)', required: true },
  { id: 'year', label: 'Year', width: 'max-content' },
  { id: 'frameworks', label: 'Frameworks', width: 'max-content' },
  { id: 'assessments', label: 'Assessments', width: 'max-content' },
  { id: 'status', label: 'Status', width: 'max-content' },
]

export const SOA_HOME_COLUMNS: ColumnDefinition[] = [
  { id: 'project', label: 'Project', width: 'minmax(10rem, 1fr)', required: true },
  { id: 'year', label: 'Year', width: 'max-content' },
  { id: 'framework', label: 'Framework', width: 'minmax(8rem, 1fr)' },
  { id: 'status', label: 'Status', width: 'max-content' },
  { id: 'completion', label: 'Completion', width: 'max-content' },
]

export const GAP_HOME_COLUMNS: ColumnDefinition[] = [
  { id: 'assessment', label: 'Assessment', width: 'minmax(10rem, 1fr)', required: true },
  { id: 'project', label: 'Project', width: 'minmax(8rem, 1fr)' },
  { id: 'year', label: 'Year', width: 'max-content' },
  { id: 'framework', label: 'Framework', width: 'max-content' },
  { id: 'type', label: 'Type', width: 'max-content' },
  { id: 'status', label: 'Status', width: 'max-content' },
  { id: 'score', label: 'Score', width: 'max-content' },
  { id: 'due', label: 'Due', width: 'max-content' },
]

export const AUDIT_REGISTER_COLUMNS: ColumnDefinition[] = [
  { id: 'name', label: 'Name', width: 'minmax(10rem, 1fr)', required: true },
  { id: 'type', label: 'Type', width: 'max-content' },
  { id: 'assigned', label: 'Assigned to', width: 'max-content' },
  { id: 'modules', label: 'Modules in scope', width: 'minmax(8rem, 1fr)' },
  { id: 'dates', label: 'Dates', width: 'max-content' },
  { id: 'framework', label: 'Framework', width: 'max-content' },
  { id: 'status', label: 'Status', width: 'max-content' },
]
