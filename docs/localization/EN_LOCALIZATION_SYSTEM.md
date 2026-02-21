# English Localization System (en_US)

## Goals
- Auto-detect client language and switch mod text to `en_us` with no manual toggle.
- Keep `en_us` and `zh_cn` language packs structurally aligned.
- Provide safe fallback behavior when keys are missing.

## Runtime Design
- Minecraft language selection is the source of truth (`LanguageManager#getSelected()`).
- `ClientLanguageState` tracks the current language code and `isEnglishUs()` state.
- `ClientModEvents` refreshes language state:
  - once during client setup,
  - again on resource reload (language pack switch, `F3+T`, pack changes).
- UI code uses `Component.translatable(...)` so language switches happen natively without screen flash.

## Fallback Strategy
- Primary path: `Component.translatable(key)`.
- Optional defensive path: `ClientLanguageState.trOrEnglishFallback(key, "English fallback")`.
- Build-time parity checks prevent silent drift between `en_us` and `zh_cn`.

## Resource Structure
- `src/main/resources/assets/examplemod/lang/en_us.json`
- `src/main/resources/assets/examplemod/lang/zh_cn.json`

## Validation Pipeline
- `scripts/verify_localization_system.py`
  - checks key parity (`en_us` vs `zh_cn`),
  - checks `%s`/`%d` placeholder parity,
  - checks mojibake markers,
  - checks forbidden British spellings in `en_us`.
- `scripts/verify_zh_cn_manual_localization.py`
  - manual module focused integrity checks.
- Integrated into `processResources`:
  - `checkLocalizationSystem`
  - `checkZhCnManualLocalization`

## QA Checklist
- Launch in `en_us`: validate UI labels, tooltips, system messages, manual entries.
- Launch in `zh_cn`: validate no fallback key leaks.
- Switch language in-game and reload resources (`F3+T`) to verify seamless text refresh.
- Validate manual pages for overflow/truncation at common resolutions.

## Native Review Workflow
- Use `docs/localization/EN_REVIEW_CHECKLIST.md`.
- Require at least one American English reviewer sign-off for:
  - idiom quality,
  - tone consistency,
  - gameplay terminology correctness.
