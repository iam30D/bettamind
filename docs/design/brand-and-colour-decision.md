# Brand And Colour Decision

## Phase 2 source handling

The available source file is `brand/source/bettamind-logo-master.png`.
`brand/source/bettamind-logo-master.svg` is still absent, so Phase 2 uses the
approved PNG fallback without overwriting it.

The PNG is an RGB file with a baked checkerboard background. Generated assets
therefore use `scripts/generate_brand_assets.py` to mask bright low-saturation
background pixels, split the symbol from the wordmark, and preserve the visible
logo geometry.

If an SVG master is supplied later, regenerate platform assets from the SVG and
keep this PNG-derived set as Phase 2 history.

## Final palette

| Token | Hex | Use |
| --- | --- | --- |
| Primary | `#0E5A7A` | Brand blue, primary actions and app identity |
| Secondary | `#2F7D57` | Growth green and calm emphasis |
| Accent | `#A6C83A` | Limited positive accent |
| Background | `#F7FAF8` | Default light background and launcher backdrop |
| On background | `#12201B` | Primary text on light backgrounds |
| Surface variant | `#E4ECE6` | Quiet dividers, inactive states and chips |
| On surface variant | `#33413B` | Secondary text |
| Danger | `#B3261E` | Error state only |
| Dark background | `#0F1412` | Dark theme background |
| High contrast primary | `#00324A` | High contrast mode primary |

Representative WCAG contrast checks:

- White on Primary: 7.60:1.
- White on Secondary: 5.01:1.
- On background on Background: 16.01:1.
- On surface variant on Surface variant: 8.90:1.
- White on Danger: 6.54:1.
- Dark theme text on Dark background: 16.02:1.
- White on High contrast primary: 13.50:1.

The same ratios are enforced in
`shared/src/commonTest/kotlin/org/bettamind/shared/design/DesignFoundationTest.kt`.

## Generated assets

Generated from `brand/source/bettamind-logo-master.png`:

- `brand/generated/bettamind-lockup-transparent.png`
- `brand/generated/bettamind-mark-transparent.png`
- `brand/generated/bettamind-mark-primary.png`
- `brand/generated/bettamind-mark-white.png`
- `brand/generated/bettamind-store-master.png`
- `shared/src/commonMain/composeResources/drawable/bettamind_mark.png`
- `shared/src/commonMain/composeResources/drawable/bettamind_lockup.png`
- Android adaptive launcher assets under `androidApp/src/main/res/mipmap-anydpi-v26`
  and `androidApp/src/main/res/drawable-nodpi`.
- Android adaptive icon XML under `androidApp/src/main/res/mipmap-anydpi`.
- Android foreground, monochrome and notification icons under
  `androidApp/src/main/res/drawable*`.
- iOS AppIcon and brand image sets under
  `iosApp/iosApp/Assets.xcassets`.

## Usage rules

- Do not overwrite `brand/source/bettamind-logo-master.png`.
- Use the symbol-only crop for small app icons and notification glyphs.
- Use the full lockup for store, documentation and wider in-app placements.
- Keep at least 18% padding around platform app icons.
- Keep at least 24% padding around Android adaptive icon foregrounds.
- Preferred backgrounds are `#F7FAF8`, white, or dark theme surface colours.
- Minimum digital size: 32 px for the symbol, 160 px wide for the full lockup.
- Avoid placing the original full-colour logo on saturated green or blue
  backgrounds because the mark uses those same hues.
