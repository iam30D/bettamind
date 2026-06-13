# Font Sources

Phase 2 bundles fonts locally so the mobile apps can start offline and without
runtime font downloads.

Downloaded on June 13, 2026 from the public Google Fonts repository:

| Font | Local path | Source |
| --- | --- | --- |
| Noto Sans Variable | `shared/src/commonMain/composeResources/font/noto_sans_variable.ttf` | `https://github.com/google/fonts/tree/main/ofl/notosans` |
| Noto Sans Arabic Variable | `shared/src/commonMain/composeResources/font/noto_sans_arabic_variable.ttf` | `https://github.com/google/fonts/tree/main/ofl/notosansarabic` |
| Noto Sans Devanagari Variable | `shared/src/commonMain/composeResources/font/noto_sans_devanagari_variable.ttf` | `https://github.com/google/fonts/tree/main/ofl/notosansdevanagari` |
| Noto Sans SC Variable | `shared/src/commonMain/composeResources/font/noto_sans_sc_variable.ttf` | `https://github.com/google/fonts/tree/main/ofl/notosanssc` |
| Atkinson Hyperlegible Regular | `shared/src/commonMain/composeResources/font/atkinson_hyperlegible_regular.ttf` | `https://github.com/google/fonts/tree/main/ofl/atkinsonhyperlegible` |
| Atkinson Hyperlegible Bold | `shared/src/commonMain/composeResources/font/atkinson_hyperlegible_bold.ttf` | `https://github.com/google/fonts/tree/main/ofl/atkinsonhyperlegible` |

Licence files are stored in `brand/licenses/fonts/`:

- `OFL-NotoSans.txt`
- `OFL-NotoSansArabic.txt`
- `OFL-NotoSansDevanagari.txt`
- `OFL-NotoSansSC.txt`
- `OFL-AtkinsonHyperlegible.txt`

Noto Sans is the default UI family with script-aware fallbacks. Atkinson
Hyperlegible is exposed as an accessibility-oriented display option in the
Phase 2 Settings placeholder.
