#!/usr/bin/env python3
"""Fail closed on this project's static privacy policy; not a security audit."""
import re
import xml.etree.ElementTree as ET
from pathlib import Path
from help_policy import help_checks, java_text_values

ROOT = Path(__file__).resolve().parents[1]
A = "{http://schemas.android.com/apk/res/android}"
passed = 0


def check(name, condition):
    global passed
    if not condition:
        raise SystemExit("FAIL: " + name)
    passed += 1
    print("PASS: " + name)


manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
check("manifest requests zero permissions", not any(n.tag.startswith("uses-permission") for n in manifest))
app = manifest.find("application")
check("application debugging disabled", app.get(A + "debuggable") == "false"
      and "debuggable false" in (ROOT / "app/build.gradle").read_text())
check("backup disabled and cleartext disabled", app.get(A + "allowBackup") == "false"
      and app.get(A + "usesCleartextTraffic") == "false")
check("backup rule resources are linked", app.get(A + "fullBackupContent") == "@xml/backup_rules"
      and app.get(A + "dataExtractionRules") == "@xml/data_extraction_rules")
service = app.find("service")
check("exported IME is protected by BIND_INPUT_METHOD", service is not None
      and service.get(A + "name") == ".KotoImeService"
      and service.get(A + "exported") == "true"
      and service.get(A + "permission") == "android.permission.BIND_INPUT_METHOD")
exported = {n.get(A + "name") for n in app if n.get(A + "exported") == "true"}
check("only launcher and protected IME are exported", exported == {".SetupActivity", ".KotoImeService"}
      and not app.findall("provider") and not app.findall("receiver"))
required = {"root", "file", "database", "sharedpref", "external", "device_root",
            "device_file", "device_database", "device_sharedpref"}
legacy = ET.parse(ROOT / "app/src/main/res/xml/backup_rules.xml").getroot()
modern = ET.parse(ROOT / "app/src/main/res/xml/data_extraction_rules.xml").getroot()
for name, section in [("legacy backup", legacy), ("cloud backup", modern.find("cloud-backup")),
                      ("device transfer", modern.find("device-transfer"))]:
    check(name + " excludes all nine domains", section is not None
          and {n.get("domain") for n in section if n.tag == "exclude" and n.get("path") == "."} == required
          and not section.findall("include"))

sources = {}
for p in (ROOT / "app/src/main/java").rglob("*.java"):
    # Remove comments, retain literals for outward-call/import checks.
    sources[p.name] = re.sub(r"/\*.*?\*/|//[^\n]*", "", p.read_text(), flags=re.S)
joined = "\n".join(sources.values())
blocked = [r"\b(?:Log|Timber)\s*\.", r"System\.(?:out|err)", r"printStackTrace\s*\(",
           r"\b(?:java\.net|okhttp|retrofit|firebase|analytics|crashlytics|sentry)\b",
           r"\b(?:WebView|SpeechRecognizer|RecognizerIntent|ClipboardManager|ContentResolver)\b",
           r"\b(?:sendBroadcast|startService|bindService|openFileOutput|FileOutputStream|getExternalFilesDir)\s*\("]
check("no runtime logs, network, speech, clipboard or upload calls", not any(re.search(p, joined, re.I) for p in blocked))
writes = re.findall(r"\.put(?:String|Int|Long|Boolean|Float|StringSet)\(\s*\"([^\"]+)\"", joined)
allowed_preferences={"row_height","haptics","flick_distance","incognito","learning_days","learning_cleanup_required"}
check("only six non-text controls and learning-policy preferences can be stored",set(writes)==allowed_preferences
      and not re.search(r"\.put(?:String|StringSet|Long|Float)\s*\(",joined))
file_users={name for name,source in sources.items() if re.search(r"\b(?:Files\.|FileChannel\.open)",source)}
check("learning file I/O is confined to the fixed-name storage adapter",file_users=={"LearningFileStorage.java"}
      and 'resolve("learning-v1.bin")' in sources["LearningFileStorage.java"]
      and 'resolve("learning-v1.tmp")' in sources["LearningFileStorage.java"]
      and "getNoBackupFilesDir().toPath()" in sources["LearningStore.java"])
check("learning defaults off and file operations use the background coordinator",
      'getInt("learning_days",0)' in sources["LearningStore.java"]
      and "Executors.newSingleThreadExecutor()" in sources["LearningStore.java"]
      and "worker.execute(this::drain)" in sources["LearningCoordinator.java"]
      and "storage." not in sources["KotoImeService.java"]
      and "storage." not in sources["SettingsActivity.java"])
check("no surrounding text or clipboard reads", not re.search(r"getTextBeforeCursor|getTextAfterCursor|getExtractedText|getSelectedText|getPrimaryClip", joined))
check("preedit stays local and no host span is created or reclaimed",
      not re.search(r"setComposingText|setComposingRegion|setSelection|EditorSelection",joined)
      and "boolean composing(" not in sources["InputSession.java"]
      and sources["InputSession.java"].count("editor.commit(")==3)
for name, condition in help_checks(java_text_values(sources["SettingsActivity.java"]), joined):
    check(name, condition)
outward = re.findall(r"\bnew Intent\(([^)]*)\)", joined)
check("intents limited to own settings and user-triggered system keyboard settings",
      sorted(outward) == ["Settings.ACTION_INPUT_METHOD_SETTINGS", "this, SettingsActivity.class"]
      and "ApplicationInfo.FLAG_SYSTEM" in sources["SetupActivity.java"]
      and "intent.setComponent" in sources["SetupActivity.java"])
build = (ROOT / "app/build.gradle").read_text()
check("no declared runtime Maven dependencies or native builds", not re.search(
      r"\b(?:implementation|api|runtimeOnly|externalNativeBuild)\b", build))
check("no private signing material in source tree", not any(
      p.suffix in {".jks", ".keystore", ".p12", ".pem"}
      for base in (ROOT / "app", ROOT / "scripts", ROOT / "third_party") for p in base.rglob("*")))
print("PASS: " + str(passed) + " static policy checks; merged APK and runtime are separate checks")
