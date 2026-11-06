export type TprmModuleSection = 'projects' | 'vendors' | 'questionnaires'

export const TPRM_NAV: { id: TprmModuleSection; navId: string; label: string }[] = [
  { id: 'projects', navId: 'tprm-projects', label: 'Projects' },
  { id: 'vendors', navId: 'tprm-vendors', label: 'Vendors' },
  { id: 'questionnaires', navId: 'tprm-questionnaires', label: 'Questionnaires' },
]
