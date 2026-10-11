"""Single reviewed version source for Gradle, Maven-tool builds and deliverables."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
values = dict(line.split("=", 1) for line in (ROOT / "version.properties").read_text().splitlines() if line.strip())
VERSION_NAME = values["versionName"]
VERSION_CODE = int(values["versionCode"])
if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", VERSION_NAME) or VERSION_CODE <= 0:
    raise ValueError("Invalid project version")
