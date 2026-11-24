"""AlzCare home-camera simulator.

Pairs simulated ESP32 cameras with a patient, then replays motion scenarios through the API
gateway exactly as real devices would (X-Device-Id / X-Device-Key, JSON body).

  python simulate.py setup --patient <patient-user-id>     # as the demo doctor
  python simulate.py run wandering
  python simulate.py run night-exit
  python simulate.py patterns                               # what the service detected
"""
from __future__ import annotations

import argparse
import json
import random
import sys
from datetime import datetime, timezone
from pathlib import Path

import httpx

from scenarios import SCENARIOS

DEFAULT_ZONES = ["HALLWAY", "KITCHEN", "LIVING_ROOM", "BATHROOM", "BEDROOM", "FRONT_DOOR"]


def login(keycloak: str, username: str, password: str) -> str:
    response = httpx.post(f"{keycloak}/realms/alzcare/protocol/openid-connect/token", data={
        "grant_type": "password", "client_id": "alzcare-webapp", "username": username, "password": password,
    }, timeout=10)
    response.raise_for_status()
    return response.json()["access_token"]


def random_mac() -> str:
    # 0x02 in the first byte: a locally administered address, never a real vendor's
    return ":".join(["02"] + [f"{random.randint(0, 255):02X}" for _ in range(5)])


def cmd_setup(args) -> int:
    token = login(args.keycloak, args.username, args.password)
    cameras = []
    with httpx.Client(base_url=args.gateway, headers={"Authorization": f"Bearer {token}"}, timeout=10) as client:
        for zone in args.zones.split(","):
            r = client.post("/api/cameras/pair", json={"patientId": args.patient, "macAddress": random_mac(), "zone": zone})
            if r.status_code != 201:
                print(f"  ✗ {zone}: {r.status_code} {r.text}")
                return 1
            body = r.json()
            cameras.append({"id": body["id"], "key": body["deviceKey"], "zone": zone, "mac": body["macAddress"]})
            print(f"  ✓ paired {zone:<12} {body['macAddress']}  id={body['id']}")
    Path(args.config).write_text(json.dumps({"patientId": args.patient, "cameras": cameras}, indent=2))
    print(f"Saved {len(cameras)} camera credentials to {args.config} (keep it private)")
    return 0


def load_config(path: str) -> dict:
    if not Path(path).exists():
        raise SystemExit(f"{path} not found: run 'python simulate.py setup --patient <id>' first")
    return json.loads(Path(path).read_text())


def cmd_run(args) -> int:
    config = load_config(args.config)
    by_zone = {c["zone"]: c for c in config["cameras"]}
    events = SCENARIOS[args.scenario](datetime.now(timezone.utc))
    detected: list[str] = []
    with httpx.Client(base_url=args.gateway, timeout=10) as client:
        for e in events:
            camera = by_zone.get(e.zone)
            if camera is None:
                print(f"  - skipped {e.zone}: no camera paired in this zone")
                continue
            r = client.post("/api/events",
                            headers={"X-Device-Id": camera["id"], "X-Device-Key": camera["key"]},
                            json={"timestamp": e.at.astimezone(timezone.utc).isoformat().replace("+00:00", "Z"),
                                  "motionIntensity": e.intensity, "durationSeconds": e.duration_seconds})
            local = e.at.strftime("%d/%m %H:%M")
            if r.status_code != 202:
                print(f"  ✗ {local} {e.zone:<12} -> {r.status_code} {r.text}")
                continue
            new = r.json().get("patternsDetected", [])
            detected += new
            print(f"  · {local} {e.zone:<12} -> 202" + (f"   ⚠ {', '.join(new)}" if new else ""))
    print(f"\nScenario '{args.scenario}': {len(events)} events sent, new patterns: {detected or 'none'}")
    return 0


def cmd_patterns(args) -> int:
    config = load_config(args.config)
    token = login(args.keycloak, args.username, args.password)
    r = httpx.get(f"{args.gateway}/api/patterns/patient/{config['patientId']}",
                  headers={"Authorization": f"Bearer {token}"}, timeout=10)
    r.raise_for_status()
    for p in r.json():
        print(f"  {p['severity']:<6} {p['type']:<19} {p['status']:<12} {p['startedAt']} → {p['endedAt']}  "
              f"{p['eventCount']} events  {', '.join(p['zones'])}")
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="AlzCare home-camera simulator")
    parser.add_argument("--gateway", default="http://localhost:8080")
    parser.add_argument("--keycloak", default="http://localhost:8090")
    parser.add_argument("--config", default="cameras.json")
    parser.add_argument("--username", default="doctor@example.com")
    parser.add_argument("--password", default="Password123!")
    sub = parser.add_subparsers(dest="command", required=True)

    setup = sub.add_parser("setup", help="pair one simulated camera per zone with a patient")
    setup.add_argument("--patient", required=True, help="patient's Keycloak user id")
    setup.add_argument("--zones", default=",".join(DEFAULT_ZONES))
    setup.set_defaults(func=cmd_setup)

    run = sub.add_parser("run", help="replay a motion scenario")
    run.add_argument("scenario", choices=sorted(SCENARIOS))
    run.set_defaults(func=cmd_run)

    sub.add_parser("patterns", help="list the patterns detected for the patient").set_defaults(func=cmd_patterns)

    args = parser.parse_args(argv)
    try:
        return args.func(args)
    except httpx.HTTPError as exc:
        print(f"HTTP error: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
