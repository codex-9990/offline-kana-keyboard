#!/usr/bin/env python3
"""Build an unsigned APK using known open-source Maven artifacts; no SDK install.

Downloads occur only with --fetch, into this project's ignored build cache.
No agreements, credentials, global installs, remote writes or device operations.
"""
import argparse
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile
from project_version import VERSION_CODE, VERSION_NAME

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / ".build-cache/aosp-tools"
TOOLS = {
    "android-all": (
        "https://repo.maven.apache.org/maven2/org/robolectric/android-all/15-robolectric-12650502/android-all-15-robolectric-12650502.jar",
        "6c0670454f6fcee9f1b17aa393e81d71bf26e7e6ae7c5777ed3924b062191d5b"),
    "aapt2": (
        "https://dl.google.com/android/maven2/com/android/tools/build/aapt2/8.7.3-12006047/aapt2-8.7.3-12006047-linux.jar",
        "028916a040b58f34c9c89984e1bc48f8da6581662014f2b66391860def8b9c36"),
    "r8": (
        "https://dl.google.com/android/maven2/com/android/tools/r8/8.7.18/r8-8.7.18.jar",
        "58366f77067207c39a17d469de7b05701d2877212a9c55201bcb0af43e59e903"),
}


def run(args, **kwargs):
    subprocess.run([str(arg) for arg in args], check=True, cwd=ROOT, **kwargs)


def jdk_tool(name):
    java_home = os.environ.get("JAVA_HOME")
    path = str(Path(java_home) / "bin" / name) if java_home else shutil.which(name)
    if not path or not Path(path).is_file():
        raise SystemExit("NOT RUN: a complete JDK is required (set JAVA_HOME)")
    return path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--fetch", action="store_true", help="fetch pinned public build dependencies into project cache")
    args = parser.parse_args()
    CACHE.mkdir(parents=True, exist_ok=True)
    for name, (url, checksum) in TOOLS.items():
        path = CACHE / (name + ".jar")
        if not path.is_file():
            if not args.fetch:
                raise SystemExit("NOT RUN: cached " + name + " missing; --fetch is explicit")
            temp = CACHE / (name + ".download")
            run(["curl", "--fail", "--silent", "--show-error", "--proto", "=https",
                 "--max-time", "180", "--output", temp, url])
            if hashlib.sha256(temp.read_bytes()).hexdigest() != checksum:
                temp.unlink()
                raise SystemExit("FAIL: downloaded tool checksum mismatch: " + name)
            temp.replace(path)
        if hashlib.sha256(path.read_bytes()).hexdigest() != checksum:
            raise SystemExit("FAIL: cached tool checksum mismatch: " + name)
    run(["python3", "scripts/prepare_dictionary.py", "--check"])
    run(["python3", "scripts/audit_source.py"])
    build = ROOT / "app/build/aosp-debug"
    # Deleted Java/resources must not survive a later build. Only this
    # project's generated output is removed; cached tools and dist are kept.
    if build.exists():
        shutil.rmtree(build)
    for folder in (build, build / "generated", build / "classes", build / "dex", ROOT / "dist"):
        folder.mkdir(parents=True, exist_ok=True)
    aapt = CACHE / "aapt2"
    with zipfile.ZipFile(CACHE / "aapt2.jar") as archive:
        aapt.write_bytes(archive.read("aapt2"))
        # Executable mode for this new, checksum-verified build tool only.
        aapt.chmod(0o700)
    android = CACHE / "android-all.jar"
    tree = ET.parse(ROOT / "app/src/main/AndroidManifest.xml")
    ET.register_namespace("android", "http://schemas.android.com/apk/res/android")
    tree.getroot().set("package", "dev.local.koto")
    staged = build / "AndroidManifest.xml"
    tree.write(staged, encoding="utf-8", xml_declaration=True)
    run([aapt, "compile", "--dir", ROOT / "app/src/main/res", "-o", build / "resources.zip"])
    run([aapt, "link", "-o", build / "base.apk", "--manifest", staged, "-I", android,
         "--java", build / "generated", "-A", ROOT / "app/src/main/assets",
         "--min-sdk-version", "26", "--target-sdk-version", "35", "--version-code", str(VERSION_CODE),
         "--version-name", VERSION_NAME, build / "resources.zip"])
    sources = sorted((ROOT / "app/src/main/java").rglob("*.java")) + sorted((build / "generated").rglob("*.java"))
    run([jdk_tool("javac"), "--release", "8", "-encoding", "UTF-8", "-cp", android,
         "-d", build / "classes", *sources])
    run([jdk_tool("jar"), "cf", build / "app-classes.jar", "-C", build / "classes", "."])
    run([jdk_tool("java"), "-cp", CACHE / "r8.jar", "com.android.tools.r8.D8", "--debug",
         "--min-api", "26", "--lib", android, "--output", build / "dex", build / "app-classes.jar"])
    apk = ROOT / ("dist/offline-kana-keyboard-" + VERSION_NAME + "-trial-unsigned.apk")
    shutil.copyfile(build / "base.apk", apk)
    with zipfile.ZipFile(apk, "a", compression=zipfile.ZIP_DEFLATED) as archive:
        for dex in sorted((build / "dex").glob("*.dex")):
            archive.write(dex, dex.name)
    run(["python3", "scripts/audit_apk.py", apk])
    print("PASS: unsigned non-debuggable trial APK:", apk)
    print("SHA-256:", hashlib.sha256(apk.read_bytes()).hexdigest())
    print("Signing and device runtime verification are separate; no signing identity was created.")


if __name__ == "__main__":
    main()
