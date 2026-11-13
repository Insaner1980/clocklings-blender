"""Publish lossless Android atlases and verify every RGBA byte after decoding.

Run with: uv run --with pillow==13.0.0 python tools/blender/optimize_atlases.py
The PNG masters stay outside Android resources so they are never packaged twice.
"""

import argparse
import hashlib
import io
import json
from pathlib import Path

from PIL import Image, features


ROOT = Path(__file__).resolve().parents[2]


def publish(source: Path, output: Path, check: bool) -> list[dict]:
    sources = sorted(source.glob("*blender*.png"))
    if not sources:
        raise ValueError(f"No Blender PNG masters in {source}")
    if not check:
        output.mkdir(parents=True, exist_ok=True)
    results = []
    for path in sources:
        destination = output / f"{path.stem}.webp"
        with Image.open(path) as original:
            if original.mode != "RGBA":
                raise ValueError(f"Expected 8-bit RGBA: {path}")
            pixels = original.tobytes()
            if check:
                encoded = destination.read_bytes()
            else:
                buffer = io.BytesIO()
                original.save(
                    buffer,
                    format="WEBP",
                    lossless=True,
                    exact=True,
                    quality=100,
                    method=6,
                    icc_profile=original.info.get("icc_profile", b""),
                )
                encoded = buffer.getvalue()
            with Image.open(io.BytesIO(encoded)) as decoded:
                if decoded.size != original.size or decoded.convert("RGBA").tobytes() != pixels:
                    raise ValueError(f"RGBA pixels changed: {path.name}")
            if not check:
                destination.write_bytes(encoded)
            results.append(
                {
                    "name": path.stem,
                    "width": original.width,
                    "height": original.height,
                    "png_bytes": path.stat().st_size,
                    "webp_bytes": len(encoded),
                    "rgba_sha256": hashlib.sha256(pixels).hexdigest(),
                    "png_sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
                    "webp_sha256": hashlib.sha256(encoded).hexdigest(),
                    "identical_rgba": True,
                }
            )
        print(f"Verified {path.name}: {len(encoded)} bytes", flush=True)
    return results


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=ROOT / "art/blender/atlases")
    parser.add_argument("--output", type=Path, default=ROOT / "app/src/main/res/drawable-nodpi")
    parser.add_argument("--report", type=Path)
    parser.add_argument("--check", action="store_true", help="Verify existing WebP files without rewriting them")
    args = parser.parse_args()
    rows = publish(args.source, args.output, args.check)
    result = {
        "pillow": Image.__version__,
        "libwebp": features.version("webp"),
        "encoding": "lossless, exact RGBA, original dimensions",
        "png_bytes": sum(row["png_bytes"] for row in rows),
        "webp_bytes": sum(row["webp_bytes"] for row in rows),
        "assets": rows,
    }
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(f"Total: {result['png_bytes']} -> {result['webp_bytes']} bytes; {len(rows)} identical atlases")


if __name__ == "__main__":
    main()
