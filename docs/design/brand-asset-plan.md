# Brand Asset Plan

Phase 1 did not generate final brand assets.

## Required source

The owner must provide one canonical source logo:

- `brand/source/bettamind-logo-master.svg`, preferred; or
- `brand/source/bettamind-logo-master.png`, fallback.

## Phase 2 work

Phase 2 must:

- inspect the canonical source logo;
- preserve geometry and recognisability;
- choose one final accessible Bettamind palette;
- derive logo colours from that palette;
- generate Android adaptive foreground, background, monochrome launcher and
  notification icons;
- generate a complete iOS AppIcon set;
- generate splash marks, compact in-app marks and store-ready masters;
- document safe area, minimum size, padding and approved backgrounds.

The source logo must not be overwritten.

## Phase 2 implementation

Phase 2 uses the available PNG fallback at
`brand/source/bettamind-logo-master.png`; the preferred SVG is not present yet.

Generated assets are created by `scripts/generate_brand_assets.py` and are
documented in `docs/design/brand-and-colour-decision.md`. The generated set
includes Android launcher, adaptive, monochrome and notification icons, a
complete iOS AppIcon asset catalog, shared Compose drawables, and store-ready
brand masters.
