#!/usr/bin/env python3
"""Set the real GitHub owner without connecting or publishing anything."""

import argparse
import json
import re
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("repository", help="GitHub owner/repository")
parser.add_argument("--maintainer", help="GitHub username; defaults to repository owner")
args = parser.parse_args()
if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9-]{0,38}/[A-Za-z0-9_.-]+", args.repository):
    parser.error("Use owner/repository, without a URL")
owner = args.maintainer or args.repository.split("/")[0]
if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9-]{0,38}", owner):
    parser.error("Invalid GitHub username")
root = Path(__file__).resolve().parents[1]
manifest = root / "custom_components/casanotify_tv/manifest.json"
data = json.loads(manifest.read_text())
url = "https://github.com/" + args.repository
data.update(documentation=url + "#readme", issue_tracker=url + "/issues", codeowners=["@" + owner])
manifest.write_text(json.dumps(data, indent=2) + "\n")
(root / "REPOSITORY_URL.txt").write_text(url + "\n")
print("Repository metadata configured. No remote publication was performed.")
