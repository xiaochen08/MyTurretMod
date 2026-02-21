#!/usr/bin/env python3
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
EN_PATH = ROOT / "src" / "main" / "resources" / "assets" / "examplemod" / "lang" / "en_us.json"
ZH_PATH = ROOT / "src" / "main" / "resources" / "assets" / "examplemod" / "lang" / "zh_cn.json"

PRINTF_TOKEN_RE = re.compile(r"%(?:\\d+\\$)?[sdif]")
CJK_RE = re.compile(r"[\u4e00-\u9fff]")
MOJIBAKE_HINTS = ("\ufffd", "锟", "�")
BRITISH_SPELLINGS = ("colour", "centre", "favourite", "armour", "dialogue")


def load(path: Path) -> dict[str, str]:
    return json.loads(path.read_text(encoding="utf-8"))


def token_set(value: str) -> tuple[str, ...]:
    return tuple(PRINTF_TOKEN_RE.findall(value))


def main() -> int:
    en = load(EN_PATH)
    zh = load(ZH_PATH)

    errors: list[str] = []

    only_en = sorted(set(en) - set(zh))
    only_zh = sorted(set(zh) - set(en))
    if only_en:
        errors.append(f"Keys missing in zh_cn: {len(only_en)} (sample: {only_en[:10]})")
    if only_zh:
        errors.append(f"Keys missing in en_us: {len(only_zh)} (sample: {only_zh[:10]})")

    for key in sorted(set(en) & set(zh)):
        en_value = en[key]
        zh_value = zh[key]

        if not isinstance(en_value, str) or not isinstance(zh_value, str):
            errors.append(f"Non-string localization value: {key}")
            continue

        if any(h in en_value for h in MOJIBAKE_HINTS):
            errors.append(f"Possible mojibake in en_us: {key}")
        if any(h in zh_value for h in MOJIBAKE_HINTS):
            errors.append(f"Possible mojibake in zh_cn: {key}")
        if CJK_RE.search(en_value):
            errors.append(f"Unexpected CJK content in en_us: {key}")

        if token_set(en_value) != token_set(zh_value):
            errors.append(f"Placeholder mismatch: {key} | en={token_set(en_value)} zh={token_set(zh_value)}")

    en_joined = "\n".join(v for v in en.values() if isinstance(v, str)).lower()
    for british in BRITISH_SPELLINGS:
        if british in en_joined:
            errors.append(f"Found British spelling '{british}' in en_us.json")

    if errors:
        print("Localization verification failed:")
        for err in errors:
            print(f"- {err}")
        return 1

    print(f"Localization verification passed. Keys: en_us={len(en)}, zh_cn={len(zh)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
