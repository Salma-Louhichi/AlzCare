# ESP32 motion sensor — reference firmware

> **Not tested on hardware.** This sketch was written against the event-ingestion API and
> reviewed, but never compiled for or flashed onto a board. Use the
> [camera simulator](../camera-simulator) to test the platform end to end without hardware.

An ESP32 with a PIR motion sensor plays the role of an AlzCare "camera": it reports **motion
metadata only** (start time, duration, intensity), never images or sound.

## Hardware
- ESP32 DevKit (any ESP32 board with Wi-Fi)
- HC-SR501 PIR sensor: `OUT → GPIO 13`, `VCC → 5V`, `GND → GND`

## Setup
1. Pair the device in AlzCare (**Admin › Medical › Cameras › Pair camera**) with its MAC address
   and the room it is in. Copy the **device id** and **device key** — the key is shown once.
2. `cp alzcare_motion_sensor/secrets.h.example alzcare_motion_sensor/secrets.h` and fill in
   Wi-Fi, the gateway URL, the device id and the key (`secrets.h` is git-ignored).
3. Open the sketch in the Arduino IDE (ESP32 core installed, board "ESP32 Dev Module") and upload.

## How it reports
| Step | Detail |
|---|---|
| Episode | starts when the PIR goes HIGH, ends after 20 s without motion (max 1 h) |
| Intensity | share of 100 ms samples that were HIGH during the episode |
| Request | `POST /api/events` with `X-Device-Id`, `X-Device-Key`, JSON `{timestamp, motionIntensity, durationSeconds}` |
| Time | UTC from NTP; episodes are not recorded before the clock is set |
| Outages | up to 64 events are queued and sent when Wi-Fi returns (the server accepts 24 h old events in any order) |
| Errors | 202 = delivered; other 4xx (except 408/429) = dropped (wrong key, paused camera); 5xx/timeouts = retried |

## Production notes
Use HTTPS with the gateway's root CA (`WiFiClientSecure`), store the key in NVS rather than in
the sketch, and add a key-rotation endpoint on the server.
