#!/usr/bin/env python3
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "src" / "main" / "java" / "com" / "example" / "examplemod"
OUT = ROOT / "docs" / "localization" / "HARDCODED_TEXT_AUDIT.md"

CJK_RE = re.compile(r"[\u4e00-\u9fff]")
STRING_RE = re.compile(r"\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"")


def main() -> int:
    rows: list[tuple[str, int, str]] = []
    for path in sorted(JAVA_ROOT.rglob("*.java")):
        text = path.read_text(encoding="utf-8", errors="ignore")
        for i, line in enumerate(text.splitlines(), start=1):
            if not CJK_RE.search(line):
                continue
            for m in STRING_RE.finditer(line):
                raw = m.group(1)
                if CJK_RE.search(raw):
                    rows.append((str(path.relative_to(ROOT)).replace("\\", "/"), i, raw[:100]))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    with OUT.open("w", encoding="utf-8") as f:
        f.write("# Hardcoded Text Audit\n\n")
        f.write(f"- Total literal CJK string hits: **{len(rows)}**\n\n")
        f.write("| File | Line | Snippet |\n")
        f.write("|---|---:|---|\n")
        for file, line, snippet in rows[:500]:
            snippet = snippet.replace("|", "\\|")
            f.write(f"| `{file}` | {line} | `{snippet}` |\n")

    print(f"Wrote {OUT} with {len(rows)} hits")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
