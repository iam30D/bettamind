# Bettamind Website

Static marketing, policy and support website for Bettamind, hosted at
`https://www.bettamind.com`.

## Stack

- Astro with TypeScript.
- Static output only.
- No server-side rendering.
- No database, authentication, analytics, ads, trackers or cookies.

## Local Setup

```powershell
npm install
npm run dev
```

Local dev server: `http://127.0.0.1:4321`

## Build

```powershell
npm run build
npm run preview
```

Static output directory: `dist`

## Verification

```powershell
npm run verify
```

The verification script builds the site and checks required routes, internal
links, sitemap output, public policy pages, localhost references, lorem ipsum,
false account-deletion wording and prohibited marketing claims.

## Cloudflare Pages

Suggested settings:

- Project name: `bettamind-website`
- Root directory: `apps/website`
- Build command: `npm ci && npm run verify`
- Output directory: `dist`
- Production branch: `main`
- Custom domain: `www.bettamind.com`
- Deploy command: leave blank for Cloudflare Pages Git deployments

No environment variables are required.

Cloudflare tokens, API keys and deployment secrets must not be stored in the
repository.

Do not set `npx wrangler deploy` as the Pages deploy command. That command
treats the static Astro site as a Worker and may auto-add a Cloudflare adapter.
Cloudflare Pages should deploy the static `dist` output directly. If a manual
CLI upload is needed later, use `wrangler pages deploy dist
--project-name=bettamind-website` after a successful build.

## Legal Page Updates

Before public release, review:

- `src/pages/privacy.astro`
- `src/pages/terms.astro`
- `src/pages/safety.astro`
- `src/pages/ai-transparency.astro`
- `src/pages/data-deletion.astro`
- `src/pages/legal.astro`

Keep these pages aligned with the shipped app, App Store privacy details,
Google Play Data Safety answers and any optional sync, export, AI model or
speech-pack release decisions.

## Brand Assets

Public website assets in `public/assets/` are optimized web copies generated
from the committed reusable assets in `brand/generated/`. Do not overwrite the
canonical source logo in `brand/source/`.
