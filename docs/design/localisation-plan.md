# Localisation Plan

## Source locale

English (`en`) is the source locale.

## Initial target locales

- English (`en`)
- French (`fr`)
- Spanish (`es`)
- Portuguese (`pt`)
- Arabic (`ar`)
- Hindi (`hi`)
- Simplified Chinese (`zh-Hans`)
- Hausa (`ha`)
- Yoruba (`yo`)
- Igbo (`ig`)

## Requirements

- Use BCP 47 locale identifiers.
- Keep user-facing strings outside feature logic.
- Support RTL layout with Arabic validation.
- Use locale-aware dates, numbers and plurals.
- Use script-aware font fallback.
- Keep English fallback.
- Treat safety, crisis, legal, privacy and consent translations as drafts until
  qualified human review.

## Phase 1 handling

Phase 1 creates Compose resource foundations and a locale-tag contract only.
Full locale packs, fonts and RTL validation start in Phase 2.
