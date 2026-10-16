#!/usr/bin/env python3
"""Run MkDocs in a repository-local virtual environment."""

import os
from pathlib import Path
import subprocess
import sys
import venv


ROOT = Path(__file__).resolve().parent.parent
ENV_DIR = ROOT / ".venv"
REQUIREMENTS = ROOT / "requirements.txt"


def main() -> int:
    if not ENV_DIR.exists():
        venv.EnvBuilder(with_pip=True).create(ENV_DIR)

    python = ENV_DIR / ("Scripts/python.exe" if os.name == "nt" else "bin/python")
    subprocess.check_call(
        [str(python), "-m", "pip", "install", "--disable-pip-version-check", "-r", str(REQUIREMENTS)]
    )
    return subprocess.call([str(python), "-m", "mkdocs", *sys.argv[1:]], cwd=ROOT)


if __name__ == "__main__":
    raise SystemExit(main())
