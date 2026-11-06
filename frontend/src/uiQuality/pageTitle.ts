const ROOT = 'Arbor GRC'

export function setArborTitle(page?: string | null) {
  document.title = page?.trim() ? `${page.trim()} · ${ROOT}` : `${ROOT}: Governance, Risk & Compliance`
}

export function navIdToTitle(navId: string, fallback = 'Dashboard'): string {
  const labels: Record<string, string> = {
    overview: 'Overview',
    'contributor-dashboard': 'Dashboard',
    reports: 'Reports',
    frameworks: 'Frameworks',
    'audit-trail': 'Audit Trail',
    evidence: 'Evidence',
    policies: 'Policies',
    organisation: 'Organisation',
    notifications: 'Notifications',
    soa: 'Statement of Applicability',
    assessments: 'Gap Assessment',
    'bcms-ncema': 'NCEMA',
    assets: 'Assets',
    risk: 'Information Security Risk',
    'erm-risk': 'ERM Risk',
    tprm: 'TPRM',
    'tprm-projects': 'TPRM Projects',
    'tprm-vendors': 'TPRM Vendors',
    'tprm-questionnaires': 'TPRM Questionnaires',
    kri: 'KRI',
    incidents: 'Incidents',
    'privacy-inventory': 'Privacy Inventory',
    'privacy-ropa': 'RoPA',
    'privacy-dsr': 'DSR',
    'privacy-dpia': 'DPIA',
    'privacy-dpia-projects': 'DPIA Projects',
    'privacy-dpia-questionnaires': 'DPIA Questionnaire',
    'privacy-risk': 'Privacy Risk',
    'privacy-consent': 'Consent',
    'privacy-breach': 'Breach',
    'bcms-profiles': 'Department Profiles',
    'bcms-bia': 'BIA',
    'bcms-plans': 'Recovery Plans',
    'bcms-exercises': 'Exercises',
    users: 'Users',
    workflows: 'Workflows',
    'audit-register': 'Audit Projects',
    'audit-engagements': 'Audit Projects',
    'audit-checklists': 'Audit Checklist',
    'audit-capa': 'CAPA',
    'audit-register2': 'CAPA',
    'audit-register3': 'CAPA',
    'audit-grants': 'Access Grants',
    'audit-access-log': 'Access View Log',
    'audit-findings': 'Findings',
  }
  if (labels[navId]) return labels[navId]
  if (navId.startsWith('audit')) return 'Audit'
  return fallback
}
