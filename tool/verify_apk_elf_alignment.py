#!/usr/bin/env python3
"""Verify 16 KB ELF PT_LOAD alignment in Android APK native libraries."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path


MIN_ALIGNMENT = 16 * 1024
TARGET_ABIS = ("arm64-v8a", "x86_64")
LOAD_LINE = re.compile(r"^\s*LOAD\s+.*?\s+(0x[0-9a-fA-F]+)\s*$")


def inspect_shared_object(readelf: str, path: Path) -> list[int]:
    result = subprocess.run(
        [readelf, "-lW", str(path)],
        check=True,
        capture_output=True,
        text=True,
    )
    alignments: list[int] = []
    for line in result.stdout.splitlines():
        match = LOAD_LINE.match(line)
        if match:
            alignments.append(int(match.group(1), 16))
    if not alignments:
        raise RuntimeError(f"No ELF PT_LOAD program headers found in {path.name}")
    return alignments


def verify_apk(readelf: str, apk_path: Path) -> tuple[int, int]:
    checked = 0
    violations = 0
    with zipfile.ZipFile(apk_path) as apk, tempfile.TemporaryDirectory(
        prefix="klang-elf-check-"
    ) as temp_dir:
        temp_root = Path(temp_dir)
        names = set(apk.namelist())
        for abi in TARGET_ABIS:
            entries = sorted(
                name
                for name in names
                if name.startswith(f"lib/{abi}/") and name.endswith(".so")
            )
            if not entries:
                continue
            for entry in entries:
                local_path = temp_root / abi / Path(entry).name
                local_path.parent.mkdir(parents=True, exist_ok=True)
                local_path.write_bytes(apk.read(entry))
                alignments = inspect_shared_object(readelf, local_path)
                checked += 1
                bad = [
                    value
                    for value in alignments
                    if value < MIN_ALIGNMENT or value % MIN_ALIGNMENT != 0
                ]
                if bad:
                    violations += 1
                    print(
                        f"FAIL {apk_path.name}: {entry}: PT_LOAD alignments "
                        + ", ".join(hex(value) for value in bad),
                        file=sys.stderr,
                    )
                else:
                    rendered = ", ".join(hex(value) for value in alignments)
                    print(f"PASS {apk_path.name}: {entry}: {rendered}")
    return checked, violations


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--readelf", required=True)
    parser.add_argument("apks", nargs="+", type=Path)
    args = parser.parse_args()

    total_checked = 0
    total_violations = 0
    for apk_path in args.apks:
        if not apk_path.is_file():
            print(f"APK does not exist: {apk_path}", file=sys.stderr)
            return 2
        checked, violations = verify_apk(args.readelf, apk_path)
        total_checked += checked
        total_violations += violations

    if total_checked == 0:
        print("No 64-bit shared libraries were inspected.", file=sys.stderr)
        return 2
    print(
        f"Checked {total_checked} 64-bit shared libraries across "
        f"{len(args.apks)} APK(s); violations: {total_violations}"
    )
    return 1 if total_violations else 0


if __name__ == "__main__":
    raise SystemExit(main())
