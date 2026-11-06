import type {
  AssetCriticality,
  IncidentPriority,
  KriRag,
  PriorityLevel,
  RiskLevel as ApiRiskLevel,
  TprmCriticality,
} from '../../api'
import type { RiskLevel } from './DomainBadge'

/** CRITICAL / HIGH / MEDIUM / LOW scale (assets, vendors, BIA). */
export function scaleLevelToBadge(
  level: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW',
): RiskLevel {
  switch (level) {
    case 'CRITICAL':
      return 'critical'
    case 'HIGH':
      return 'high'
    case 'MEDIUM':
      return 'medium'
    case 'LOW':
    default:
      return 'low'
  }
}

export function apiRiskLevelToBadge(level: ApiRiskLevel): RiskLevel {
  switch (level) {
    case 'VERY_HIGH':
      return 'critical'
    case 'HIGH':
      return 'high'
    case 'MEDIUM':
      return 'medium'
    case 'LOW':
      return 'low'
    case 'VERY_LOW':
    default:
      return 'veryLow'
  }
}

export function priorityLevelToBadge(p: PriorityLevel): RiskLevel {
  switch (p) {
    case 'CRITICAL':
      return 'critical'
    case 'HIGH':
      return 'high'
    case 'MEDIUM':
      return 'medium'
    case 'LOW':
    default:
      return 'low'
  }
}

export function incidentPriorityToBadge(p: IncidentPriority): RiskLevel {
  switch (p) {
    case 'P1':
      return 'critical'
    case 'P2':
      return 'high'
    case 'P3':
      return 'medium'
    case 'P4':
    default:
      return 'low'
  }
}

export function kriRagToBadge(rag: KriRag): RiskLevel {
  switch (rag) {
    case 'RED':
      return 'critical'
    case 'AMBER':
      return 'medium'
    case 'GREEN':
    default:
      return 'low'
  }
}

/** UAE IA three-layer RAG labels (Gap assessment). */
export function uaeIaRagToBadge(rag: string): RiskLevel {
  switch (rag) {
    case 'CRITICAL':
      return 'critical'
    case 'LOW':
      return 'high'
    case 'MODERATE':
      return 'medium'
    case 'GOOD':
    case 'STRONG':
      return 'low'
    default:
      return 'veryLow'
  }
}

export function assetCriticalityToBadge(c: AssetCriticality): RiskLevel {
  return scaleLevelToBadge(c)
}

export function vendorCriticalityToBadge(c: TprmCriticality): RiskLevel {
  return c === 'CRITICAL' ? 'critical' : 'low'
}

export function biaCriticalityToBadge(label: string | null | undefined): RiskLevel {
  if (!label) return 'veryLow'
  if (label.includes('Tier 1')) return 'critical'
  if (label.includes('Tier 2')) return 'high'
  if (label.includes('Tier 3')) return 'medium'
  return 'low'
}
