import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { CameraDeviceService } from '../../../../core/services/camera-device.service';
import { HomeMonitoringService } from '../../../../core/services/home-monitoring.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import {
  CameraDevice,
  CameraZone,
  DetectedPattern,
  MotionEvent,
  PatternType
} from '../../../../core/models/camera-device.model';

type State = 'loading' | 'ready' | 'forbidden' | 'error';

/**
 * Home monitoring (IoT) for one patient, doctor view:
 * alerts detected from ESP32 camera motion, the patient's cameras and 24 h activity per room.
 */
@Component({
  selector: 'app-home-monitoring-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home-monitoring-panel.component.html'
})
export class HomeMonitoringPanelComponent implements OnChanges {
  /** Keycloak user id of the patient. */
  @Input({ required: true }) patientUserId!: string;

  state: State = 'loading';
  patterns: DetectedPattern[] = [];
  cameras: CameraDevice[] = [];
  activity: { zone: CameraZone; count: number }[] = [];
  maxActivity = 1;
  busyId: string | null = null;

  private readonly zoneLabels: Record<CameraZone, string> = {
    BEDROOM: 'Bedroom',
    HALLWAY: 'Hallway',
    BATHROOM: 'Bathroom',
    FRONT_DOOR: 'Front door',
    KITCHEN: 'Kitchen',
    LIVING_ROOM: 'Living room'
  };

  private readonly patternLabels: Record<PatternType, string> = {
    WANDERING: 'Possible wandering',
    NIGHT_MOTION: 'Night activity',
    NIGHT_EXIT_ATTEMPT: 'Possible night exit'
  };

  constructor(
    private monitoring: HomeMonitoringService,
    private cameraService: CameraDeviceService,
    private toast: ToastService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['patientUserId'] && this.patientUserId) {
      this.load();
    }
  }

  get openAlerts(): number {
    return this.patterns.filter(p => p.status === 'OPEN').length;
  }

  load(): void {
    this.state = 'loading';
    forkJoin({
      patterns: this.monitoring.getPatterns(this.patientUserId, 30),
      cameras: this.cameraService.getCamerasByPatient(this.patientUserId),
      events: this.monitoring.getRecentEvents(this.patientUserId, 24)
    }).subscribe({
      next: ({ patterns, cameras, events }) => {
        this.patterns = patterns;
        this.cameras = cameras;
        this.activity = this.summarise(events);
        this.state = 'ready';
      },
      error: (err: HttpErrorResponse) => {
        this.state = err.status === 403 ? 'forbidden' : 'error';
      }
    });
  }

  acknowledge(pattern: DetectedPattern): void {
    this.busyId = pattern.id;
    this.monitoring.acknowledge(pattern.id).subscribe({
      next: updated => {
        this.patterns = this.patterns.map(p => (p.id === updated.id ? updated : p));
        this.busyId = null;
        this.toast.success('Alert acknowledged');
      },
      error: () => {
        this.busyId = null;
        this.toast.error('Could not acknowledge the alert');
      }
    });
  }

  /** Pausing stops recording for that room (e.g. during a family visit); resuming re-enables it. */
  togglePause(camera: CameraDevice): void {
    const next = camera.status === 'PAUSED' ? 'ACTIVE' : 'PAUSED';
    this.busyId = camera.id;
    this.cameraService.updateStatus(camera.id, next).subscribe({
      next: updated => {
        this.cameras = this.cameras.map(c => (c.id === updated.id ? updated : c));
        this.busyId = null;
        this.toast.success(next === 'PAUSED' ? `${this.zone(camera.zone)} camera paused` : `${this.zone(camera.zone)} camera resumed`);
      },
      error: () => {
        this.busyId = null;
        this.toast.error('Could not change the camera status');
      }
    });
  }

  zone(zone: CameraZone): string {
    return this.zoneLabels[zone] ?? zone;
  }

  zonesText(zones: CameraZone[]): string {
    return zones.map(z => this.zone(z)).join(', ');
  }

  patternLabel(type: PatternType): string {
    return this.patternLabels[type] ?? type;
  }

  durationMinutes(p: DetectedPattern): number {
    return Math.max(1, Math.round((new Date(p.endedAt).getTime() - new Date(p.startedAt).getTime()) / 60000));
  }

  statusClasses(status: CameraDevice['status']): string {
    switch (status) {
      case 'ACTIVE':
        return 'bg-emerald-100 text-emerald-700';
      case 'PAUSED':
        return 'bg-gray-200 text-gray-700';
      default:
        return 'bg-amber-100 text-amber-800';
    }
  }

  private summarise(events: MotionEvent[]): { zone: CameraZone; count: number }[] {
    const counts = new Map<CameraZone, number>();
    events.forEach(e => counts.set(e.zone, (counts.get(e.zone) ?? 0) + 1));
    const rows = [...counts.entries()].map(([zone, count]) => ({ zone, count })).sort((a, b) => b.count - a.count);
    this.maxActivity = Math.max(1, ...rows.map(r => r.count));
    return rows;
  }
}
