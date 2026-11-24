import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../../../core/services/api.service';
import { AuthService } from '../../../../core/services/auth.service';
import { MlPredictionService } from '../../../../core/services/ml-prediction.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { AutonomyAssessment, AutonomyLevel } from '../../../../core/models/api.model';
import { RiskLevel, RiskPrediction } from '../../../../core/models/ml.model';

type RiskState = 'idle' | 'loading' | 'ready' | 'no-data' | 'forbidden' | 'unavailable';

/**
 * Autonomy assessments + ML care-risk estimate for one patient (doctor view).
 *
 * - Records autonomy assessments (identity-service).
 * - Shows the assessment history and the trend of the 1-10 overall score.
 * - Shows the ML prediction (ml-prediction-service): probability, level and the main factors.
 */
@Component({
  selector: 'app-care-risk-panel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './care-risk-panel.component.html'
})
export class CareRiskPanelComponent implements OnChanges {
  /** Keycloak user id of the patient (used by the ML service). */
  @Input({ required: true }) patientUserId!: string;
  /** identity-service patient profile id (used by the autonomy endpoints). */
  @Input() patientProfileId: string | null = null;

  readonly levels: AutonomyLevel[] = [AutonomyLevel.INDEPENDENT, AutonomyLevel.ASSISTED, AutonomyLevel.DEPENDENT];
  readonly areas: { key: 'hygieneLevel' | 'medicationLevel' | 'mobilityLevel' | 'feedingLevel'; label: string }[] = [
    { key: 'hygieneLevel', label: 'Hygiene' },
    { key: 'medicationLevel', label: 'Medication' },
    { key: 'mobilityLevel', label: 'Mobility' },
    { key: 'feedingLevel', label: 'Feeding' }
  ];

  history: AutonomyAssessment[] = [];
  historyLoading = false;
  risk: RiskPrediction | null = null;
  riskState: RiskState = 'idle';
  showForm = false;
  saving = false;
  form: FormGroup;

  constructor(
    fb: FormBuilder,
    private api: ApiService,
    private auth: AuthService,
    private ml: MlPredictionService,
    private toast: ToastService
  ) {
    this.form = fb.group({
      hygieneLevel: ['INDEPENDENT', Validators.required],
      medicationLevel: ['INDEPENDENT', Validators.required],
      mobilityLevel: ['INDEPENDENT', Validators.required],
      feedingLevel: ['INDEPENDENT', Validators.required],
      assessmentDate: [new Date().toISOString().slice(0, 10), Validators.required],
      notes: ['', Validators.maxLength(2000)]
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['patientProfileId'] || changes['patientUserId']) {
      this.refresh();
    }
  }

  refresh(): void {
    this.loadHistory();
    this.loadRisk();
  }

  loadHistory(): void {
    if (!this.patientProfileId) {
      this.history = [];
      return;
    }
    this.historyLoading = true;
    this.api.getAutonomyAssessmentHistory(this.patientProfileId).subscribe({
      next: list => {
        this.history = [...list].sort((a, b) => b.assessmentDate.localeCompare(a.assessmentDate));
        this.historyLoading = false;
      },
      error: () => {
        this.history = [];
        this.historyLoading = false;
      }
    });
  }

  loadRisk(): void {
    if (!this.patientUserId) {
      return;
    }
    this.riskState = 'loading';
    this.ml.getPatientRisk(this.patientUserId).subscribe({
      next: prediction => {
        this.risk = prediction;
        this.riskState = 'ready';
      },
      error: (err: HttpErrorResponse) => {
        this.risk = null;
        this.riskState = err.status === 422 ? 'no-data' : err.status === 403 ? 'forbidden' : 'unavailable';
      }
    });
  }

  submit(): void {
    const assessorId = this.auth.getCurrentUserId();
    if (this.form.invalid || !this.patientProfileId || !assessorId) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    const v = this.form.value;
    this.api.createAutonomyAssessment(this.patientProfileId, {
      hygieneLevel: v.hygieneLevel,
      medicationLevel: v.medicationLevel,
      mobilityLevel: v.mobilityLevel,
      feedingLevel: v.feedingLevel,
      assessmentDate: v.assessmentDate,
      notes: v.notes?.trim() || undefined,
      assessedBy: assessorId
    }).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.toast.success('Autonomy assessment recorded');
        this.refresh();
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.toast.error(err.error?.detail || 'Could not save the assessment');
      }
    });
  }

  /** Change of the overall score between an assessment and the one before it. */
  scoreChange(index: number): number | null {
    const next = this.history[index + 1];
    return next ? this.history[index].overallScore - next.overallScore : null;
  }

  percent(probability: number): number {
    return Math.round(probability * 100);
  }

  levelClasses(level: RiskLevel): string {
    switch (level) {
      case 'HIGH':
        return 'bg-rose-100 text-rose-700 border-rose-200';
      case 'MODERATE':
        return 'bg-amber-100 text-amber-800 border-amber-200';
      default:
        return 'bg-emerald-100 text-emerald-700 border-emerald-200';
    }
  }

  barColor(level: RiskLevel): string {
    return level === 'HIGH' ? 'bg-rose-500' : level === 'MODERATE' ? 'bg-amber-500' : 'bg-emerald-500';
  }

  levelChip(level: AutonomyLevel): string {
    return level === AutonomyLevel.DEPENDENT
      ? 'bg-rose-50 text-rose-700'
      : level === AutonomyLevel.ASSISTED
        ? 'bg-amber-50 text-amber-700'
        : 'bg-emerald-50 text-emerald-700';
  }
}
