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

## Phase 2 implementation

Compose resource packs now exist for:

- `values/` as English source content for `en`;
- `values-fr/` for `fr`;
- `values-es/` for `es`;
- `values-pt/` for `pt`;
- `values-ar/` for `ar`;
- `values-hi/` for `hi`;
- `values-zh-rCN/` for the current Simplified Chinese resource pack mapped to
  the BCP 47 target `zh-Hans`;
- `values-ha/` for `ha`;
- `values-yo/` for `yo`;
- `values-ig/` for `ig`.

`BettamindLocales` keeps the BCP 47 target list in shared common code. Arabic is
the RTL validation locale, and common tests assert that it is treated as RTL.

Phase 2 translation files are draft implementation foundations only. They are
not production-approved, and any safety, crisis, legal, privacy or consent copy
must receive qualified human review before release.

Script-aware font fallback is provided by bundled Noto Sans, Noto Sans Arabic,
Noto Sans Devanagari, Noto Sans SC and Atkinson Hyperlegible resources.
