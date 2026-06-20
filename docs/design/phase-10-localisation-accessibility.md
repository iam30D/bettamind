# Phase 10 Localisation And Accessibility

Date: 2026-06-20

## Scope

Phase 10 completes the repository-level localisation and accessibility
foundation for the current Bettamind surfaces. It does not add speech, release
red-team signoff, TestFlight, store metadata, production model artifacts or
mandatory backend behavior.

The shared implementation lives in
`shared/src/commonMain/kotlin/org/bettamind/shared/accessibility/GlobalLocalizationAccessibility.kt`.

## Implemented Controls

- Target locale profiles cover English, French, Spanish, Portuguese, Arabic,
  Hindi, Simplified Chinese, Hausa, Yoruba and Igbo.
- Arabic is treated as RTL and mapped to the Arabic font fallback stack.
- Hindi maps to the Devanagari font fallback stack.
- Simplified Chinese maps to the Simplified Chinese font fallback stack.
- Locale formatting metadata defines date order, decimal/grouping separators
  and plural categories for the supported foundation.
- `LocalizationReadinessPolicy` checks resource-key completeness for every
  initial target locale.
- Safety-critical surfaces are classified for app lock, relational boundaries,
  harm safety, crisis/emergency, legal/consent/privacy, support bridge and
  export/sync copy.
- Safety-critical target translations require qualified human review before
  production readiness can be true.
- Source English review and target-locale qualified review are represented by
  explicit `TranslationReviewRecord` entries.
- `AccessibilityReadinessPolicy` models screen-reader labels, 48 dp minimum
  touch targets, large text up to 2.0x, reduced-motion static steps, simple
  wording mode and accessible typography fallback.
- Settings now exposes accessible typography, reduced motion and simple wording
  preferences with screen-reader state descriptions.
- `phaseTenCheck` runs the Windows-available Phase 10 verification path.

## Human Review Boundary

The target locale resource packs are complete at the key-coverage level, but
the non-English entries are still draft fallback text until qualified reviewers
approve each production locale. Bettamind must not treat safety, crisis, legal,
privacy or consent translations as production-approved only because they are
present in the repository.

Required owner records before production release:

- reviewer name or vendor;
- locale;
- reviewed string keys or reviewed file revision;
- review date;
- safety/legal/privacy/consent scope covered;
- approval outcome and required changes.

## Non-Goals

- No Phase 11 speech input or output.
- No Phase 12 release readiness signoff.
- No cloud AI.
- No model artifacts.
- No automatic model or translation downloads.
- No production assertion that non-English safety-critical copy has already
  been qualified-review approved.

## Verification Coverage

Common tests in
`shared/src/commonTest/kotlin/org/bettamind/shared/accessibility/GlobalLocalizationAccessibilityTest.kt`
cover locale profiles, RTL/script fallback, locale-aware formatting metadata,
resource/readiness logic, qualified-review gates, large text, reduced motion,
low-literacy mode and screen-reader required surfaces.

XML resource parity checks should continue to parse all Compose `strings.xml`
files and require every source string key in every target locale directory.
