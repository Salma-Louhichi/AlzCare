/*
 * AlzCare home motion sensor - reference firmware (ESP32 + PIR sensor, Arduino core)
 *
 * NOT TESTED ON HARDWARE: written against the event-ingestion API and verified only by reading.
 * Compile with the ESP32 Arduino core (board "ESP32 Dev Module").
 *
 * Wiring: HC-SR501 PIR  OUT -> GPIO 13, VCC -> 5V, GND -> GND.
 *
 * Behaviour
 *  - A motion episode starts when the PIR output goes HIGH and ends after QUIET_MS without motion.
 *  - Intensity = share of 100 ms samples that were HIGH during the episode (0..1).
 *  - Each episode becomes one event: {timestamp, motionIntensity, durationSeconds}, sent to
 *    POST /api/events with X-Device-Id / X-Device-Key. Only metadata is sent: no image, no sound.
 *  - Events are queued in RAM and flushed when Wi-Fi is up, so episodes recorded during an outage
 *    are still delivered (the service accepts events up to 24 h old, in any order).
 *  - 4xx answers other than 408/429 are permanent (wrong key, paused camera...): the event is dropped.
 */
#include <WiFi.h>
#include <HTTPClient.h>
#include <time.h>
#include "secrets.h"

static const int PIR_PIN = 13;
static const uint32_t SAMPLE_MS = 100;
static const uint32_t QUIET_MS = 20000;          // end of episode after 20 s without motion
static const uint32_t MAX_EPISODE_MS = 3600000;  // the API accepts at most 3600 s per event
static const size_t QUEUE_SIZE = 64;

struct MotionEvent {
  time_t startedAt;          // epoch seconds (UTC)
  uint16_t durationSeconds;
  float intensity;
};

static MotionEvent queue[QUEUE_SIZE];
static size_t queueHead = 0, queueCount = 0;

static bool inEpisode = false;
static uint32_t episodeStartMs = 0, lastMotionMs = 0, samples = 0, highSamples = 0;
static time_t episodeStartEpoch = 0;
static uint32_t lastSampleMs = 0, lastFlushMs = 0;

static void enqueue(const MotionEvent &e) {
  if (queueCount == QUEUE_SIZE) {               // full: drop the oldest
    queueHead = (queueHead + 1) % QUEUE_SIZE;
    queueCount--;
  }
  queue[(queueHead + queueCount) % QUEUE_SIZE] = e;
  queueCount++;
}

static bool clockIsSet() {
  return time(nullptr) > 1700000000;            // NTP has synced (after Nov 2023)
}

static void isoTimestamp(time_t t, char *out, size_t len) {
  struct tm utc;
  gmtime_r(&t, &utc);
  strftime(out, len, "%Y-%m-%dT%H:%M:%SZ", &utc);
}

/** @return true if the event was delivered or must be dropped, false to retry later */
static bool sendEvent(const MotionEvent &e) {
  char ts[25];
  isoTimestamp(e.startedAt, ts, sizeof(ts));
  char body[128];
  snprintf(body, sizeof(body), "{\"timestamp\":\"%s\",\"motionIntensity\":%.2f,\"durationSeconds\":%u}",
           ts, e.intensity, e.durationSeconds);

  HTTPClient http;
  http.setTimeout(5000);
  if (!http.begin(ALZCARE_EVENTS_URL)) {
    return false;
  }
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Device-Id", DEVICE_ID);
  http.addHeader("X-Device-Key", DEVICE_KEY);
  int status = http.POST(body);
  http.end();

  Serial.printf("event %s %.2f %us -> HTTP %d\n", ts, e.intensity, e.durationSeconds, status);
  if (status == 202) return true;
  if (status >= 400 && status < 500 && status != 408 && status != 429) return true;   // permanent
  return false;                                                                       // retry later
}

static void flushQueue() {
  if (WiFi.status() != WL_CONNECTED || !clockIsSet()) return;
  while (queueCount > 0) {
    if (!sendEvent(queue[queueHead])) return;   // keep the rest for the next attempt
    queueHead = (queueHead + 1) % QUEUE_SIZE;
    queueCount--;
  }
}

static void endEpisode(uint32_t nowMs) {
  uint32_t durationMs = (lastMotionMs - episodeStartMs) + SAMPLE_MS;
  MotionEvent e;
  e.startedAt = episodeStartEpoch;
  e.durationSeconds = (uint16_t) max<uint32_t>(1, min<uint32_t>(3600, durationMs / 1000));
  e.intensity = samples ? (float) highSamples / (float) samples : 0.0f;
  if (clockIsSet()) enqueue(e);                 // without a valid clock the timestamp would be wrong
  inEpisode = false;
}

void setup() {
  Serial.begin(115200);
  pinMode(PIR_PIN, INPUT);
  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  configTime(0, 0, "pool.ntp.org", "time.google.com");   // UTC; the server applies the time zone
  Serial.println("AlzCare motion sensor started");
}

void loop() {
  uint32_t now = millis();
  if (now - lastSampleMs >= SAMPLE_MS) {
    lastSampleMs = now;
    bool motion = digitalRead(PIR_PIN) == HIGH;

    if (motion && !inEpisode) {
      inEpisode = true;
      episodeStartMs = now;
      episodeStartEpoch = time(nullptr);
      samples = highSamples = 0;
    }
    if (inEpisode) {
      samples++;
      if (motion) {
        highSamples++;
        lastMotionMs = now;
      }
      if (now - lastMotionMs >= QUIET_MS || now - episodeStartMs >= MAX_EPISODE_MS) {
        endEpisode(now);
      }
    }
  }

  if (now - lastFlushMs >= 2000) {
    lastFlushMs = now;
    flushQueue();
  }
}
