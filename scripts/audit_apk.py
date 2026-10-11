#!/usr/bin/env python3
"""Inspect the packaged manifest, resources, DEX and asset contents, offline."""
import hashlib
from pathlib import Path
import re
import struct
import subprocess
import sys
import zipfile
import xml.etree.ElementTree as ET
from axml import decode
from project_version import VERSION_CODE, VERSION_NAME

ROOT = Path(__file__).resolve().parents[1]
APK = Path(sys.argv[1]).resolve()
AAPT = ROOT / ".build-cache/aosp-tools/aapt2"
evidence = ROOT / "verification"
evidence.mkdir(exist_ok=True)
passed = 0


def check(name, condition):
    global passed
    if not condition:
        raise SystemExit("FAIL: " + name)
    passed += 1
    print("PASS: " + name)


def dump(file):
    return subprocess.check_output([str(AAPT), "dump", "xmltree", str(APK), "--file", file], text=True)


manifest = dump("AndroidManifest.xml")
(evidence / "apk-manifest.txt").write_text(manifest)
badging = subprocess.check_output([str(AAPT), "dump", "badging", str(APK)], text=True)
(evidence / "apk-badging.txt").write_text(badging)
with zipfile.ZipFile(APK) as archive:
    manifest_xml = decode(archive.read("AndroidManifest.xml"))
A = "{http://schemas.android.com/apk/res/android}"
ET.indent(manifest_xml)
(evidence / "apk-manifest.xml").write_text(ET.tostring(manifest_xml, encoding="unicode") + "\n")
app = manifest_xml.find("application")
sdk = manifest_xml.find("uses-sdk")
check("packaged APK requests zero permissions", not any(n.tag.startswith("uses-permission") for n in manifest_xml)
      and "uses-permission:" not in badging)
check("APK identity, minimum API 26 and target API 35", manifest_xml.get("package") == "dev.local.koto"
      and sdk.get(A + "minSdkVersion") == "26" and sdk.get(A + "targetSdkVersion") == "35")
check("APK version code and name match project version", manifest_xml.get(A + "versionCode") == str(VERSION_CODE)
      and manifest_xml.get(A + "versionName") == VERSION_NAME)
check("packaged APK is not debuggable", app.get(A + "debuggable") == "false")
check("packaged backup and cleartext flags disabled", app.get(A + "allowBackup") == "false"
      and app.get(A + "usesCleartextTraffic") == "false")
service = app.find("service")
check("packaged IME binding permission", service.get(A + "permission") == "android.permission.BIND_INPUT_METHOD"
      and service.get(A + "name") == ".KotoImeService" and service.get(A + "exported") == "true")
exported = {n.get(A + "name") for n in app if n.get(A + "exported") == "true"}
check("exactly two exported components, one internal activity", exported == {".SetupActivity", ".KotoImeService"}
      and app.find("activity[@android:name='.SettingsActivity']", {"android": A[1:-1]}).get(A + "exported") == "false")
check("no providers or receivers in packaged APK", not app.findall("provider") and not app.findall("receiver"))
domains = {"root", "file", "database", "sharedpref", "external", "device_root", "device_file", "device_database", "device_sharedpref"}
for index, filename in enumerate(("res/xml/backup_rules.xml", "res/xml/data_extraction_rules.xml")):
    content = dump(filename)
    (evidence / ("apk-backup-legacy.txt" if index == 0 else "apk-backup-modern.txt")).write_text(content)
    with zipfile.ZipFile(APK) as archive:
        tree = decode(archive.read(filename))
    sections = [tree] if index == 0 else [tree.find("cloud-backup"), tree.find("device-transfer")]
    for section in sections:
        check("packaged " + section.tag + " exclusions", {n.get("domain") for n in section if n.tag == "exclude"
              and n.get("path") == "."} == domains and not section.findall("include"))


def dex_strings(data):
    count, offset = struct.unpack_from("<II", data, 56)
    values = []
    for i in range(count):
        p = struct.unpack_from("<I", data, offset + 4 * i)[0]
        while data[p] & 0x80:
            p += 1
        p += 1
        end = data.index(0, p)
        values.append(data[p:end].decode("utf-8", errors="replace"))
    return values


with zipfile.ZipFile(APK) as archive:
    names = archive.namelist()
    check("no packaged native libraries, test classes, keys or extra executable jars", not any(
        name.startswith("lib/") or name.endswith((".jks", ".keystore", ".pem", ".p12", ".jar")) for name in names))
    for name in ("skk.tsv", "NOTICE.txt", "GPL-2.txt"):
        check("packaged " + name + " matches reviewed source", archive.read("assets/" + name)
              == (ROOT / "app/src/main/assets" / name).read_bytes())
    dex = archive.read("classes.dex")
    strings = dex_strings(dex)
    (evidence / "dex-strings.txt").write_text("\n".join(strings))
    forbidden = ("Ljava/net/", "Landroid/webkit/", "Landroid/speech/", "Landroid/content/ClipboardManager;",
                 "Landroid/util/Log;", "Lcom/google/firebase/", "Lokhttp", "Lretrofit", "Lio/sentry/",
                 "CoreTests", "FeatureTests", "ConnectionEditorHostTests", "CompositionCallbackHostTests", "LocalPreeditHostTests", "ImeSmokeInstrumentation", "SyntheticOnly42")
    check("DEX has no network, WebView, speech, logging, telemetry or test-data references", not any(
        fragment in value for value in strings for fragment in forbidden))
    check("DEX has no composing span creation reclamation or inferred-selection machinery",
          not any(value in {"setComposingText","setComposingRegion","setSelection"} or "EditorSelection" in value for value in strings))
    # The android-all compilation jar and build tools must never enter the APK.
    type_count, type_offset = struct.unpack_from("<II", dex, 64)
    types = [strings[struct.unpack_from("<I", dex, type_offset + 4 * i)[0]] for i in range(type_count)]
    class_count, class_offset = struct.unpack_from("<II", dex, 96)
    classes = [types[struct.unpack_from("<I", dex, class_offset + 32 * i)[0]] for i in range(class_count)]
    check("all defined DEX classes are this project's namespace", all(c.startswith("Ldev/local/koto/") for c in classes))
    check("APK has no legacy v1 signature files", not any(re.search(r"META-INF/.*\.(RSA|DSA|EC|SF)$", n) for n in names))
    entry = archive.getinfo("resources.arsc")
    with APK.open("rb") as source:
        source.seek(entry.header_offset + 26)
        filename_len, extra_len = struct.unpack("<HH", source.read(4))
    offset = entry.header_offset + 30 + filename_len + extra_len
    check("resource table is stored and aligned to four bytes", entry.compress_type == zipfile.ZIP_STORED and offset % 4 == 0)
print("PASS: " + str(passed) + " packaged APK checks; signature/install/runtime require separate verification")
print("APK SHA-256:", hashlib.sha256(APK.read_bytes()).hexdigest())
