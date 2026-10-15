# WAVE-TRACE

An Android Wi‑Fi RSSI meter for **autonomous equipment troubleshooting**.

**Copyright © 2026 Shawn Baird**

---

## What It Does

Autonomous machines live and die by their wireless link health. WAVE-TRACE gives
you a quick, handheld view of the Wi‑Fi environment around a machine — signal
strength, network names, and a rolling signal graph — so you can tell whether a
dropout is the radio, the environment, or somebody's new access point.

Built for **isolated/field networks** where you need to see "is this machine's
AP actually reachable, and how strong?" without dragging a laptop around.

## Features

- **Network scan** — lists nearby Wi‑Fi networks (SSID) with live RSSI in dBm
- **Signal graph** — rolling visualisation of signal strength over time
  (`SignalGraphView`)
- **Per-network detail** — tap a network for its reading
- **Tone feedback** — a rising chirp when the monitored signal improves and a
  falling chirp when it weakens, triggered once RSSI moves past a configurable
  threshold (dB, default 5). Off-switchable in Settings.
- **Settings** — configurable scan behaviour, calibration offset, tone feedback

## Requirements & Permissions

- Android **minSdk 26**, target/compile SDK 34
- Permissions: `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `ACCESS_NETWORK_STATE`,
  `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`
  (location is required by Android for Wi‑Fi scan results)

## Build

Requires JDK 17 + the Android SDK.

```bash
./gradlew assembleRelease
```

The release build is signed with the auto-generated debug keystore so the APK
sideloads to a device for testing without extra secrets:

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

## ⚠️ Scope & Safety Notice

WAVE-TRACE is a **personal project, shared as-is**. It is a **diagnostic aid —
not a safety device** — and is **not certified, validated, or intended for
safety-of-life, regulatory, or compliance-critical use**. Readings may be wrong
or misleading. Do not rely on it for any decision affecting the safety of people
or equipment. Provided with **no warranty** of any kind.

## License

Licensed under **Creative Commons Attribution-NonCommercial-NoDerivatives 4.0
International (CC BY-NC-ND 4.0)** — see [`LICENSE`](LICENSE).

You may **view, run, and share this work verbatim** for non-commercial purposes,
with credit. You may **not** modify it, rebrand it, or use it commercially.

Full terms: https://creativecommons.org/licenses/by-nc-nd/4.0/

Third-party components and their licenses: see
[`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md).
