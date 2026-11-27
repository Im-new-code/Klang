#!/usr/bin/env python3
"""Patch metadata_god 1.1.0 Cargokit for the project's AGP/Gradle 9 toolchain.

Uses java.lang.ProcessBuilder instead of Gradle's removed Project.exec API.
No ExecOperations injection is used, avoiding Groovy duplicate-accessor
problems when the script is compiled on clean Gradle 9.5 runners.
"""
from __future__ import annotations

import os
from pathlib import Path
import re
import sys

OLD_COMPILE_SDK = "plugin.project.android.compileSdkVersion.substring(8) as int"
NEW_COMPILE_SDK = "plugin.project.android.compileSdkVersion.substring(8).tokenize('.')[0] as int"

OLD_CHMOD_BLOCK = """            project.exec {
                commandLine 'chmod', '+x', path
            }"""
LEGACY_CHMOD_BLOCK = """            execOperations.exec {
                commandLine 'chmod', '+x', path.toString()
            }"""
NEW_CHMOD_BLOCK = """            def chmodProcess = new ProcessBuilder("chmod", "+x", path.toAbsolutePath().toString()).inheritIO().start()
            if (chmodProcess.waitFor() != 0) {
                throw new GradleException("Failed to make CargoKit build script executable")
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
LEGACY_BUILD_BLOCK = """        execOperations.exec {
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
NEW_BUILD_BLOCK = """        def cargoProcessBuilder = new ProcessBuilder(path.toAbsolutePath().toString(), "build-gradle")
        cargoProcessBuilder.directory(project.projectDir)
        def cargoEnvironment = cargoProcessBuilder.environment()
        cargoEnvironment.put("CARGOKIT_ROOT_PROJECT_DIR", rootProjectDir.toString())
        cargoEnvironment.put("CARGOKIT_TOOL_TEMP_DIR", Paths.get(buildDir, "build_tool").toString())
        cargoEnvironment.put("CARGOKIT_MANIFEST_DIR", manifestDir.toString())
        cargoEnvironment.put("CARGOKIT_CONFIGURATION", buildMode)
        cargoEnvironment.put("CARGOKIT_TARGET_TEMP_DIR", buildDir)
        cargoEnvironment.put("CARGOKIT_OUTPUT_DIR", outputDir)
        cargoEnvironment.put("CARGOKIT_NDK_VERSION", ndkVersion)
        cargoEnvironment.put("CARGOKIT_SDK_DIR", sdkDirectory.toString())
        cargoEnvironment.put("CARGOKIT_COMPILE_SDK_VERSION", compileSdkVersion.toString())
        cargoEnvironment.put("CARGOKIT_MIN_SDK_VERSION", minSdkVersion.toString())
        cargoEnvironment.put("CARGOKIT_TARGET_PLATFORMS", targetPlatforms.join(","))
        cargoEnvironment.put("CARGOKIT_JAVA_HOME", System.properties['java.home'].toString())
        cargoProcessBuilder.inheritIO()
        def cargoProcess = cargoProcessBuilder.start()
        def cargoExitCode = cargoProcess.waitFor()
        if (cargoExitCode != 0) {
            throw new GradleException("CargoKit build-gradle exited with code ${cargoExitCode}")
        }"""

def replace_once_or_already(source: str, old: str, new: str, label: str) -> str:
    count = source.count(old)
    if count == 1:
        return source.replace(old, new, 1)
    if count == 0 and new in source:
        return source
    raise RuntimeError(
        f"Unexpected metadata_god 1.1.0 Cargokit {label}: "
        f"expected one original block or one already-patched block; "
        f"original occurrences={count}, patched occurrences={source.count(new)}"
    )

def patch_metadata_god(pub_cache: Path) -> Path:
    matches = sorted((pub_cache / "hosted").glob("*/metadata_god-1.1.0/cargokit/gradle/plugin.gradle"))
    if len(matches) != 1:
        raise RuntimeError(
            "Expected exactly one cached metadata_god 1.1.0 Cargokit script, "
            f"found {len(matches)}"
        )
    script_path = matches[0]
    source = script_path.read_text(encoding="utf-8")
    patched = source

    patched = replace_once_or_already(patched, OLD_COMPILE_SDK, NEW_COMPILE_SDK, "compileSdk parser")

    # Remove the previous compatibility patch's service injection. The
    # original 1.1.0 Cargokit script has no ExecOperations accessor.
    patched = patched.replace(
        "import org.apache.tools.ant.taskdefs.condition.Os\nimport org.gradle.process.ExecOperations\nimport javax.inject.Inject",
        "import org.apache.tools.ant.taskdefs.condition.Os",
    )
    patched = re.sub(
        r"\n\s*@Inject\s*\n\s*abstract\s+ExecOperations\s+getExecOperations\(\)\s*",
        "\n",
        patched,
    )
    if "ExecOperations" in patched or "javax.inject.Inject" in patched:
        raise RuntimeError("Old ExecOperations injection remains in Cargokit script")

    for old, legacy, new, label in (
        (OLD_CHMOD_BLOCK, LEGACY_CHMOD_BLOCK, NEW_CHMOD_BLOCK, "chmod process"),
        (OLD_BUILD_BLOCK, LEGACY_BUILD_BLOCK, NEW_BUILD_BLOCK, "Cargo build process"),
    ):
        if new in patched:
            patched = patched.replace(old, "").replace(legacy, "")
        else:
            count = patched.count(old) + patched.count(legacy)
            if count != 1:
                raise RuntimeError(
                    f"Unexpected metadata_god 1.1.0 Cargokit {label} block; "
                    f"original/legacy occurrences={count}"
                )
            patched = patched.replace(old, new, 1) if old in patched else patched.replace(legacy, new, 1)

    if "project.exec {" in patched or "execOperations.exec {" in patched:
        raise RuntimeError("Cargokit still contains Gradle exec service calls")
    if "getExecOperations()" in patched or "ExecOperations" in patched:
        raise RuntimeError("Cargokit still contains duplicate-prone ExecOperations injection")
    if NEW_COMPILE_SDK not in patched or "new ProcessBuilder(" not in patched:
        raise RuntimeError("Cargokit Gradle 9 compatibility patch is incomplete")

    if patched != source:
        script_path.write_text(patched, encoding="utf-8")
        print(f"Patched metadata_god Cargokit with ProcessBuilder for Gradle 9: {script_path}")
    else:
        print(f"metadata_god Cargokit ProcessBuilder patch already present: {script_path}")
    return script_path

def main() -> int:
    pub_cache = Path(os.environ.get("PUB_CACHE", str(Path.home() / ".pub-cache"))).expanduser()
    try:
        patch_metadata_god(pub_cache)
    except (OSError, RuntimeError) as exc:
        print(f"Android Pub-cache compatibility patch failed: {exc}", file=sys.stderr)
        return 1
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
