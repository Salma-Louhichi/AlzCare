import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateManualBehaviorLogRequest,
  UpdateBehaviorLogRequest,
  ValidateBehaviorRequest,
  AcknowledgeAlertRequest,
  ResolveAlertRequest,
  BehaviorLogResponse,
  AlertResponse,
  AlertHistoryResponse,
  BehaviorSeverity
} from '../models/safety-alert.model';

/**
 * Safety Alert Service
 * 
 * Handles communication with the Safety Alert Engine for behavior logs and alerts.
 * All endpoints are proxied to the safety-alert-engine service.
 */
@Injectable({
  providedIn: 'root'
})
export class SafetyAlertService {
  private baseUrl = '/api'; // Proxied to safety-alert-engine

  constructor(private http: HttpClient) { }

  // ==================== BEHAVIOR LOGS ====================

  /**
   * Convert numeric severity (1-5) to enum string (ONE-FIVE)
   */
  private numberToSeverityEnum(severity: number): BehaviorSeverity {
    const map: Record<number, BehaviorSeverity> = {
      1: 'ONE',
      2: 'TWO',
      3: 'THREE',
      4: 'FOUR',
      5: 'FIVE'
    };
    return map[severity] || 'THREE';
  }

  /**
   * Create a manual behavior log entry
   */
  createManualBehaviorLog(request: CreateManualBehaviorLogRequest): Observable<BehaviorLogResponse> {
    // Convert numeric severity to enum string for backend
    const backendRequest = {
      ...request,
      severity: this.numberToSeverityEnum(request.severity)
    };
    return this.http.post<BehaviorLogResponse>(`${this.baseUrl}/behavior-logs/manual`, backendRequest);
  }

  /**
   * Get all behavior logs for a specific patient
   */
  getBehaviorLogsByPatient(patientId: string): Observable<BehaviorLogResponse[]> {
    return this.http.get<BehaviorLogResponse[]>(`${this.baseUrl}/behavior-logs/patient/${patientId}`);
  }

  /**
   * Get all behavior logs pending validation
   */
  getPendingValidations(): Observable<BehaviorLogResponse[]> {
    return this.http.get<BehaviorLogResponse[]>(`${this.baseUrl}/behavior-logs/pending`);
  }

  /**
   * Validate a behavior log (confirm or mark as false alarm)
   */
  validateBehavior(behaviorLogId: string, request: ValidateBehaviorRequest): Observable<BehaviorLogResponse> {
    return this.http.put<BehaviorLogResponse>(`${this.baseUrl}/behavior-logs/${behaviorLogId}/validate`, request);
  }

  /**
   * Get a single behavior log by ID
   */
  getBehaviorLogById(id: string): Observable<BehaviorLogResponse> {
    return this.http.get<BehaviorLogResponse>(`${this.baseUrl}/behavior-logs/${id}`);
  }

  /**
   * Update a behavior log (for editing manual logs)
   */
  updateBehaviorLog(id: string, request: UpdateBehaviorLogRequest): Observable<BehaviorLogResponse> {
    // Convert numeric severity to enum string for backend (similar to createManualBehaviorLog)
    const backendRequest = {
      ...request,
      severity: this.numberToSeverityEnum(request.severity)
    };
    return this.http.put<BehaviorLogResponse>(`${this.baseUrl}/behavior-logs/${id}`, backendRequest);
  }

  /**
   * Delete a behavior log permanently
   */
  deleteBehaviorLog(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/behavior-logs/${id}`);
  }

  // ==================== ALERTS ====================

  /**
   * Get all active alerts
   */
  getActiveAlerts(): Observable<AlertResponse[]> {
    return this.http.get<AlertResponse[]>(`${this.baseUrl}/alerts/active`);
  }

  /**
   * Get all overdue alerts (escalation deadline passed)
   */
  getOverdueAlerts(): Observable<AlertResponse[]> {
    return this.http.get<AlertResponse[]>(`${this.baseUrl}/alerts/overdue`);
  }

  /**
   * Get all alerts for a specific patient
   */
  getAlertsByPatient(patientId: string): Observable<AlertResponse[]> {
    return this.http.get<AlertResponse[]>(`${this.baseUrl}/alerts/patient/${patientId}`);
  }

  /**
   * Get a single alert by ID
   */
  getAlertById(id: string): Observable<AlertResponse> {
    return this.http.get<AlertResponse>(`${this.baseUrl}/alerts/${id}`);
  }

  /**
   * Acknowledge an alert
   */
  acknowledgeAlert(alertId: string, request: AcknowledgeAlertRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/alerts/${alertId}/acknowledge`, request);
  }

  /**
   * Resolve an alert
   */
  resolveAlert(alertId: string, request: ResolveAlertRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/alerts/${alertId}/resolve`, request);
  }

  /**
   * Escalate an alert to the next level
   */
  escalateAlert(alertId: string, userId: string, notes?: string): Observable<void> {
    const request = { userId, notes };
    return this.http.post<void>(`${this.baseUrl}/alerts/${alertId}/escalate`, request);
  }

  /**
   * Get history/actions for a specific alert
   */
  getAlertHistory(alertId: string): Observable<AlertHistoryResponse[]> {
    return this.http.get<AlertHistoryResponse[]>(`${this.baseUrl}/alerts/${alertId}/history`);
  }
}
