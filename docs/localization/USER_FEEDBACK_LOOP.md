# Localization Feedback Loop

## In-Game Feedback Command
- Recommended command channel: `/feedback locale <key> <issue>`
- Capture:
  - client language (`ClientLanguageState.currentLanguage()`),
  - screen/context,
  - translation key if known,
  - screenshot timestamp.

## Triage Workflow
1. Reproduce issue in `en_us` and `zh_cn`.
2. Verify key exists in both language files.
3. Update translation value.
4. Run:
   - `python scripts/verify_localization_system.py`
   - `python scripts/verify_zh_cn_manual_localization.py`
5. Add case notes to patch/PR.

## Severity
- High: blocked understanding, missing critical action text.
- Medium: awkward wording, inconsistent terminology.
- Low: punctuation, tone polish.
