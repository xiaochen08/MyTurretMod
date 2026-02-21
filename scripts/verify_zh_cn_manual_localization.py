#!/usr/bin/env python3
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
EN_PATH = ROOT / "src" / "main" / "resources" / "assets" / "examplemod" / "lang" / "en_us.json"
ZH_PATH = ROOT / "src" / "main" / "resources" / "assets" / "examplemod" / "lang" / "zh_cn.json"

CJK_RE = re.compile(r"[\u4e00-\u9fff]")
ASCII_WORD_RE = re.compile(r"[A-Za-z]{3,}")

ALLOWED_ASCII_ONLY = {
    "manual.examplemod.version",
}


def main() -> int:
    en = json.loads(EN_PATH.read_text(encoding="utf-8"))
    zh = json.loads(ZH_PATH.read_text(encoding="utf-8"))

    en_manual_keys = sorted(k for k in en.keys() if k.startswith("manual.examplemod."))
    errors: list[str] = []

    for key in en_manual_keys:
        if key not in zh:
            errors.append(f"Missing zh_cn key: {key}")
            continue

        value = zh[key]
        if not isinstance(value, str):
            errors.append(f"zh_cn key is not a string: {key}")
            continue

        if "?" in value:
            errors.append(f"Suspicious '?' found in zh_cn value: {key} => {value}")
        if "\ufffd" in value:
            errors.append(f"Replacement char found in zh_cn value: {key}")

        if key not in ALLOWED_ASCII_ONLY:
            has_cjk = bool(CJK_RE.search(value))
            has_english_word = bool(ASCII_WORD_RE.search(value))
            if has_english_word and not has_cjk:
                errors.append(f"Likely untranslated zh_cn value: {key} => {value}")

    if errors:
        print("zh_cn manual localization verification failed:")
        for err in errors:
            print(f"- {err}")
        return 1

    print(f"zh_cn manual localization verification passed. Checked {len(en_manual_keys)} keys.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
