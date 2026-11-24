/**
 * ML prediction service models (Python/FastAPI, /api/v1/ml).
 * The care-risk model is a demonstration trained on synthetic data - see its MODEL_CARD.md.
 */
export type RiskLevel = 'LOW' | 'MODERATE' | 'HIGH';

export interface RiskFactor {
  feature: string;
  label: string;
  impact: number; // contribution to the log-odds; > 0 increases risk
  direction: 'increases risk' | 'decreases risk';
}

export interface RiskPrediction {
  patientUserId?: string;
  probability: number; // 0..1
  riskLevel: RiskLevel;
  overallScore: number; // 1..10
  topFactors: RiskFactor[];
  basedOnAssessmentDate?: string;
  assessmentsUsed: number;
  modelVersion: string;
  disclaimer: string;
}
