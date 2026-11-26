#!/usr/bin/env python3
"""Apply narrowly-scoped Android Gradle compatibility fixes to Pub cache plugins.

metadata_god 1.1.0's Cargokit Gradle script assumes AGP returns compile SDK
strings like "android-37". AGP 9.3 returns "android-37.0", so parsing the full
suffix as an integer throws NumberFormatException. Keep this patch versioned
and fail loudly if the upstream script changes, so a package upgrade prompts
a deliberate compatibility review instead of silently breaking builds.
"""

from __future__ import annotations

import os
from pathlib import Path
import sys


OLD_EXPRESSION = (
    "plugin.project.android.compileSdkVersion.substring(8) as int"
)
NEW_EXPRESSION = (
    "plugin.project.android.compileSdkVersion.substring(8).tokenize('.')[0] as int"
)


def patch_metadata_god(pub_cache: Path) -> Path:
    matches = sorted(
        (pub_cache / "hosted").glob(
            "*/metadata_god-1.1.0/cargokit/gradle/plugin.gradle"
        )
    )
    if len(matches) != 1:
        raise RuntimeError(
            "Expected exactly one cached metadata_god 1.1.0 Cargokit "
            f"Gradle script under {pub_cache / 'hosted'}, found {len(matches)}"
        )

    script_path = matches[0]
    source = script_path.read_text(encoding="utf-8")

    if NEW_EXPRESSION in source and OLD_EXPRESSION not in source:
        print(f"metadata_god Cargokit compatibility patch already present: {script_path}")
        return script_path

    occurrences = source.count(OLD_EXPRESSION)
    if occurrences != 1:
        raise RuntimeError(
            "Unexpected metadata_god 1.1.0 Cargokit compileSdk parser. "
            f"Expected one occurrence of {OLD_EXPRESSION!r}, found {occurrences}. "
            "Review the upstream package before updating this patch."
        )

    patched = source.replace(OLD_EXPRESSION, NEW_EXPRESSION, 1)
    if OLD_EXPRESSION in patched or NEW_EXPRESSION not in patched:
        raise RuntimeError("metadata_god Cargokit patch verification failed")

    script_path.write_text(patched, encoding="utf-8")
    print(f"Patched metadata_god Cargokit compileSdk parser: {script_path}")
    return script_path


def main() -> int:
    pub_cache = Path(
        os.environ.get("PUB_CACHE", str(Path.home() / ".pub-cache"))
    ).expanduser()
    try:
        patch_metadata_god(pub_cache)
    except (OSError, RuntimeError) as exc:
        print(f"Android Pub-cache compatibility patch failed: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
