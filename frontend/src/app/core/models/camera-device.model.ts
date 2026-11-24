/**
 * Camera Device Model
 * 
 * Defines types and interfaces for camera device management in the admin dashboard.
 * Cameras are paired with patients for monitoring and event ingestion.
 */

/**
 * Camera status enum - represents the operational state of a camera device
 */
export type CameraStatus = 'ACTIVE' | 'OFFLINE' | 'PAUSED';

/**
 * Camera zone enum - represents the physical location where the camera is installed
 */
export type CameraZone = 'BEDROOM' | 'HALLWAY' | 'BATHROOM' | 'FRONT_DOOR' | 'KITCHEN' | 'LIVING_ROOM';

/**
 * Camera Device model - represents a paired camera in the system
 */
export interface CameraDevice {
  id: string;
  patientId: string;
  macAddress: string;
  zone: CameraZone;
  status: CameraStatus;
  lastSeen?: string;  // ISO timestamp
  pairedAt: string;   // ISO timestamp
  pairedBy: string;   // UUID of user who paired
}

/**
 * Request to pair a new camera device to a patient.
 * patientId is the patient's Keycloak user id; the server records the caller as pairedBy.
 */
export interface CameraDeviceRequest {
  patientId: string;
  macAddress: string;
  zone: CameraZone;
}

/**
 * Pairing response: the camera plus its device key. The key is returned only once and must be
 * flashed on the ESP32 (it authenticates its events with X-Device-Id / X-Device-Key).
 */
export interface CameraPairingResponse extends CameraDevice {
  deviceKey: string;
}

/** A behaviour detected by event-ingestion from camera motion. */
export type PatternType = 'WANDERING' | 'NIGHT_MOTION' | 'NIGHT_EXIT_ATTEMPT';

export interface DetectedPattern {
  id: string;
  patientId: string;
  type: PatternType;
  severity: 'MEDIUM' | 'HIGH';
  status: 'OPEN' | 'ACKNOWLEDGED';
  startedAt: string;
  endedAt: string;
  eventCount: number;
  zones: CameraZone[];
  detectedAt: string;
  notified: boolean;
  acknowledgedBy?: string;
  acknowledgedAt?: string;
}

/** A single motion event reported by a camera (metadata only). */
export interface MotionEvent {
  id: string;
  cameraId: string;
  zone: CameraZone;
  occurredAt: string;
  motionIntensity: number;
  durationSeconds: number;
}

/**
 * Status update request - for changing camera operational status
 */
export interface CameraStatusUpdateRequest {
  status: CameraStatus;
}
