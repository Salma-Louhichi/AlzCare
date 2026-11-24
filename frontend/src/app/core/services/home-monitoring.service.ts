import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DetectedPattern, MotionEvent } from '../models/camera-device.model';

/**
 * Home monitoring data from event-ingestion: behaviours detected from camera motion
 * (wandering, night motion, night exit attempts) and the raw motion timeline.
 * DOCTOR (own patients) and ADMIN only.
 */
@Injectable({ providedIn: 'root' })
export class HomeMonitoringService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getPatterns(patientUserId: string, days = 30): Observable<DetectedPattern[]> {
    const params = new HttpParams().set('days', days);
    return this.http.get<DetectedPattern[]>(`${this.baseUrl}/patterns/patient/${patientUserId}`, { params });
  }

  acknowledge(patternId: string): Observable<DetectedPattern> {
    return this.http.put<DetectedPattern>(`${this.baseUrl}/patterns/${patternId}/acknowledge`, {});
  }

  getRecentEvents(patientUserId: string, hours = 24): Observable<MotionEvent[]> {
    const params = new HttpParams().set('hours', hours);
    return this.http.get<MotionEvent[]>(`${this.baseUrl}/events/patient/${patientUserId}`, { params });
  }
}
