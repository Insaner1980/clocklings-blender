"""Compare the same release APK journeys on the dedicated 1080x1920 test emulator.

The emulator is intentionally required: these are controlled local comparisons,
not performance claims about physical phones. No device other than --serial is used.
"""

import argparse
import json
import re
import statistics
import subprocess
import time
from pathlib import Path


PACKAGE = "fi.clocklings.blender"
ACTIVITY = f"{PACKAGE}/fi.clocklings.MainActivity"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--label", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if not args.serial.startswith("emulator-"):
        raise ValueError("A dedicated emulator is required")

    def adb(*command: str, binary: bool = False):
        return subprocess.check_output(
            ["adb", "-s", args.serial, *command], text=not binary, timeout=120
        )

    if "ClocklingsOptimization" not in adb("emu", "avd", "name"):
        raise ValueError("Refusing to change an unrelated emulator")
    package_info = adb("shell", "dumpsys", "package", PACKAGE)
    if "DEBUGGABLE" in package_info:
        raise ValueError("Performance measurements require a non-debuggable release APK")
    if "1080x1920" not in adb("shell", "wm", "size"):
        raise ValueError("The verified journey coordinates require 1080x1920")
    if adb("shell", "settings", "get", "system", "font_scale").strip() != "1.0":
        raise ValueError("Measure at font scale 1.0; accessibility checks run separately")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    result = {
        "label": args.label,
        "device": adb("shell", "getprop", "ro.build.fingerprint").strip(),
        "avd": adb("emu", "avd", "name").splitlines()[0],
        "display": "1080x1920, 420 dpi, font scale 1.0",
        "rendering": "Emulator SwiftShader; not representative of physical-device speed",
        "compilation": "cmd package compile -m speed -f",
        "startup_metric": "am start -W TotalTime: process-cold first displayed activity, filesystem caches warm",
        "frame_metric": "FrameCompleted - IntendedVsync; nonzero Flags excluded; 60 Hz budget",
        "startup_ms": [],
        "journeys": {},
    }

    def save() -> None:
        args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")

    def require_foreground() -> None:
        windows = adb("shell", "dumpsys", "window")
        focus = next((line for line in windows.splitlines() if "mCurrentFocus=" in line), "")
        if ACTIVITY not in focus:
            raise RuntimeError(f"Measurement blocked by a dialog or another window: {focus}")

    def memory() -> dict:
        raw = adb("shell", "dumpsys", "meminfo", PACKAGE)
        values = {}
        for label in ("TOTAL PSS", "TOTAL RSS", "Java Heap", "Native Heap", "Graphics"):
            match = re.search(rf"{label}:\s+(\d+)", raw)
            if match:
                values[label + " KiB"] = int(match.group(1))
        if "TOTAL PSS KiB" not in values:
            raise RuntimeError(f"App memory unavailable: {raw}")
        return values

    def frames(start_ns: int) -> dict:
        raw = adb("shell", "dumpsys", "gfxinfo", PACKAGE, "framestats")
        end_ns = int(float(adb("shell", "cat", "/proc/uptime").split()[0]) * 1_000_000_000)
        header = None
        durations = []
        for line in raw.splitlines():
            if line.startswith("Flags,"):
                header = line.rstrip(",").split(",")
            elif header and re.match(r"^\d+,", line):
                values = dict(zip(header, map(int, line.rstrip(",").split(","))))
                duration = (values["FrameCompleted"] - values["IntendedVsync"]) / 1_000_000
                if (
                    values["Flags"] == 0
                    and values["IntendedVsync"] >= start_ns
                    and 0 < values["FrameCompleted"] <= end_ns
                    and duration > 0
                ):
                    durations.append(duration)
        if not durations:
            raise RuntimeError("No valid frame timing samples")
        ordered = sorted(durations)
        return {
            "frames": len(durations),
            "median_ms": statistics.median(durations),
            "p95_ms": ordered[min(len(ordered) - 1, int(len(ordered) * 0.95))],
            "over_16_67ms_percent": 100 * sum(value > 1000 / 60 for value in durations) / len(durations),
            "frame_ms": durations,
        }

    def tap(x: int, y: int) -> None:
        adb("shell", "input", "tap", str(x), str(y))

    def snapshot(name: str) -> None:
        path = args.output.parent / f"{args.label}-{name}.png"
        path.write_bytes(adb("exec-out", "screencap", "-p", binary=True))

    def sample(name: str, action, settle: float = 4) -> None:
        require_foreground()
        adb("shell", "dumpsys", "gfxinfo", PACKAGE, "reset")
        start_ns = int(float(adb("shell", "cat", "/proc/uptime").split()[0]) * 1_000_000_000)
        action()
        time.sleep(settle)
        entry = {"start_uptime_ns": start_ns, "timing": frames(start_ns), "memory": memory()}
        require_foreground()
        result["journeys"].setdefault(name, []).append(entry)
        save()
        print(f"{name}: p95={entry['timing']['p95_ms']:.2f} ms, PSS={entry['memory']['TOTAL PSS KiB']} KiB", flush=True)

    adb("shell", "pm", "clear", PACKAGE)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)
    time.sleep(8)  # Exclude the first-launch fixture/intro from repeated startup samples.
    require_foreground()
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "cmd", "package", "compile", "-m", "speed", "-f", PACKAGE)
    for index in range(7):
        adb("shell", "am", "force-stop", PACKAGE)
        launched = adb("shell", "am", "start", "-W", "-n", ACTIVITY)
        match = re.search(r"TotalTime:\s+(\d+)", launched)
        if not match or "Status: ok" not in launched:
            raise RuntimeError(launched)
        result["startup_ms"].append(int(match.group(1)))
        save()
        print(f"Startup {index + 1}: {match.group(1)} ms", flush=True)
        time.sleep(4)
        require_foreground()
    sample("home_idle", lambda: None, 6)
    snapshot("home")

    # Coordinates are checked against the captured release UI at the stated display settings.
    for _ in range(3):
        sample("open_companions", lambda: tap(285, 341), 5)
        tap(105, 150)
        time.sleep(2)
    tap(285, 341)
    time.sleep(4)
    snapshot("companions")
    for _ in range(3):
        sample("select_cat", lambda: tap(235, 1465), 3)
        sample("select_doll", lambda: tap(100, 1465), 3)
    for _ in range(3):
        sample("open_wardrobe", lambda: tap(540, 1660), 5)
        snapshot("wardrobe")
        tap(105, 150)
        time.sleep(2)
    result["startup_median_ms"] = statistics.median(result["startup_ms"])
    save()
    print(f"Completed {args.label}: median startup {result['startup_median_ms']} ms", flush=True)


if __name__ == "__main__":
    main()
