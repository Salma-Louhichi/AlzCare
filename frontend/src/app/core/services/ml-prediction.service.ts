import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RiskPrediction } from '../models/ml.model';

/**
 * Client for the ML prediction service (routed by the gateway to ml-prediction:8094).
 * Only DOCTOR (for their own patients) and ADMIN may call it.
 */
@Injectable({ providedIn: 'root' })
export class MlPredictionService {
  private readonly baseUrl = `${environment.apiUrl}/v1/ml`;

  constructor(private http: HttpClient) {}

  /** Care-risk estimate computed from the patient's stored autonomy assessments. */
  getPatientRisk(patientUserId: string): Observable<RiskPrediction> {
    return this.http.get<RiskPrediction>(`${this.baseUrl}/risk/patients/${patientUserId}`);
  }
}
