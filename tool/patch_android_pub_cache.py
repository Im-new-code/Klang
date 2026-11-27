#!/usr/bin/env python3
"""Apply narrowly-scoped Gradle 9 compatibility fixes to metadata_god 1.1.0.

The package's Cargokit script has two incompatibilities with the Gradle 9
toolchain used by this project:
1. AGP 9 returns compileSdk strings such as "android-37.0", not "android-37".
2. Gradle 9 removed Project.exec. The build task must use injected
   ExecOperations instead.

The patch is deliberately version-specific and fails loudly if upstream code
changes, so dependency upgrades trigger a conscious compatibility review.
"""

from __future__ import annotations

import os
from pathlib import Path
import sys


OLD_COMPILE_SDK = (
    "plugin.project.android.compileSdkVersion.substring(8) as int"
)
NEW_COMPILE_SDK = (
    "plugin.project.android.compileSdkVersion.substring(8).tokenize('.')[0] as int"
)

OLD_IMPORT = "import org.apache.tools.ant.taskdefs.condition.Os"
NEW_IMPORT = """import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.process.ExecOperations
import javax.inject.Inject"""

OLD_TASK_CLASS = "abstract class CargoKitBuildTask extends DefaultTask {"
NEW_TASK_CLASS = """abstract class CargoKitBuildTask extends DefaultTask {

    @Inject
    abstract ExecOperations getExecOperations()"""

OLD_CHMOD_BLOCK = """            project.exec {
                commandLine 'chmod', '+x', path
            }"""
NEW_CHMOD_BLOCK = """            execOperations.exec {
                commandLine 'chmod', '+x', path.toString()
            }"""

OLD_BUILD_BLOCK = """        project.exec {
            executable path
            args "build-gradle"
            environment "CARGOKIT_ROOT_PROJECT_DIR", rootProjectDir
            environment "CARGOKIT_TOOL_TEMP_DIR", "${buildDir}/build_tool"
            environment "CARGOKIT_MANIFEST_DIR", manifestDir
            environment "CARGOKIT_CONFIGURATION", buildMode
            environment "CARGOKIT_TARGET_TEMP_DIR", buildDir
            environment "CARGOKIT_OUTPUT_DIR", outputDir
            environment "CARGOKIT_NDK_VERSION", ndkVersion
            environment "CARGOKIT_SDK_DIR", sdkDirectory
            environment "CARGOKIT_COMPILE_SDK_VERSION", compileSdkVersion
            environment "CARGOKIT_MIN_SDK_VERSION", minSdkVersion
            environment "CARGOKIT_TARGET_PLATFORMS", targetPlatforms.join(",")
            environment "CARGOKIT_JAVA_HOME", System.properties['java.home']
        }"""
NEW_BUILD_BLOCK = """        execOperations.exec {
            executable path.toString()
            args "build-gradle"
            environment "CARGOKIT_ROOT_PROJECT_DIR", rootProjectDir
            environment "CARGOKIT_TOOL_TEMP_DIR", "${buildDir}/build_tool"
            environment "CARGOKIT_MANIFEST_DIR", manifestDir
            environment "CARGOKIT_CONFIGURATION", buildMode
            environment "CARGOKIT_TARGET_TEMP_DIR", buildDir
            environment "CARGOKIT_OUTPUT_DIR", outputDir
            environment "CARGOKIT_NDK_VERSION", ndkVersion
            environment "CARGOKIT_SDK_DIR", sdkDirectory
            environment "CARGOKIT_COMPILE_SDK_VERSION", compileSdkVersion
            environment "CARGOKIT_MIN_SDK_VERSION", minSdkVersion
            environment "CARGOKIT_TARGET_PLATFORMS", targetPlatforms.join(",")
            environment "CARGOKIT_JAVA_HOME", System.properties['java.home']
        }"""


def replace_once_or_already(source: str, old: str, new: str, label: str) -> str:
    count = source.count(old)
    if count == 1:
        return source.replace(old, new, 1)
    if count == 0 and new in source:
        return source
    raise RuntimeError(
        f"Unexpected metadata_god 1.1.0 Cargokit {label}: "
        f"expected one old block or one already-patched block; "
        f"old occurrences={count}, new occurrences={source.count(new)}"
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
    patched = source

    patched = replace_once_or_already(
        patched, OLD_COMPILE_SDK, NEW_COMPILE_SDK, "compileSdk parser"
    )
    patched = replace_once_or_already(
        patched, OLD_IMPORT, NEW_IMPORT, "Gradle imports"
    )
    patched = replace_once_or_already(
        patched, OLD_TASK_CLASS, NEW_TASK_CLASS, "task service injection"
    )
    patched = replace_once_or_already(
        patched, OLD_CHMOD_BLOCK, NEW_CHMOD_BLOCK, "chmod process execution"
    )
    patched = replace_once_or_already(
        patched, OLD_BUILD_BLOCK, NEW_BUILD_BLOCK, "Cargo build process execution"
    )

    if "project.exec {" in patched:
        raise RuntimeError(
            "metadata_god Cargokit still contains removed Gradle Project.exec calls"
        )
    for required in (
        NEW_COMPILE_SDK,
        "abstract ExecOperations getExecOperations()",
        "execOperations.exec {",
    ):
        if required not in patched:
            raise RuntimeError(f"metadata_god Cargokit patch missing {required!r}")

    if patched != source:
        script_path.write_text(patched, encoding="utf-8")
        print(f"Patched metadata_god Cargokit for AGP/Gradle 9: {script_path}")
    else:
        print(f"metadata_god Cargokit compatibility patch already present: {script_path}")

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
