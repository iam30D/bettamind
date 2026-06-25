# Bettamind Website Cloudflare Pages

## Purpose

The Bettamind public website supports App Store and Google Play review needs
for support, privacy, safety, AI transparency and data deletion URLs. It is a
static Astro site under `apps/website` and does not change the mobile app,
backend, AI, sync or safety-system runtime.

## Domain

- Production domain: `https://www.bettamind.com`
- Canonical host: `www.bettamind.com`
- Support email: `support@bettamind.com`

## Cloudflare Pages Settings

- Project name: `bettamind-website`
- Root directory: `apps/website`
- Build command: `npm ci && npm run verify`
- Output directory: `dist`
- Production branch: `main`
- Framework preset: Astro, or none with the command above
- Environment variables: none required

Do not store Cloudflare tokens, API keys or account credentials in the
repository.

## DNS Steps

1. Create the Cloudflare Pages project from the repository.
2. Set the root directory to `apps/website`.
3. Run the production build once from the production branch.
4. Add `www.bettamind.com` as the custom domain in Cloudflare Pages.
5. Follow Cloudflare's displayed DNS instruction for the `www` record.
6. If the apex domain `bettamind.com` is used, redirect it to
   `https://www.bettamind.com`.
7. Confirm HTTPS is active before using the URLs in store metadata.

## Security Headers and Redirects

The site ships Cloudflare Pages files:

- `apps/website/public/_headers`
- `apps/website/public/_redirects`

Headers include a restrictive Content Security Policy, content-type sniffing
protection, referrer policy, permissions policy and frame protections. The
redirect file maps `https://bettamind.com/*` to the canonical `www` host and
redirects `/home` to `/`.

## Public Store URLs

Use these public routes for store metadata when deployed:

- Support URL: `https://www.bettamind.com/support`
- Privacy Policy URL: `https://www.bettamind.com/privacy`
- Data Deletion URL: `https://www.bettamind.com/data-deletion`
- Safety URL: `https://www.bettamind.com/safety`
- AI Transparency URL: `https://www.bettamind.com/ai-transparency`
- Terms URL: `https://www.bettamind.com/terms`

## Policy Alignment

The website copy is written around current repository rules:

- Core app use does not require account creation.
- Core use is offline-first.
- AI is optional.
- Personal data is local and encrypted by default.
- Exports, support sharing and any future optional sync are user-directed.
- Bettamind is not therapy, medical diagnosis, emergency service, legal advice,
  financial advice or a romantic or sexual companion.

Before production release, verify App Store privacy details, Google Play Data
Safety answers and store listing claims match the shipped app. If optional
sync, account support, cloud AI, model packs or speech packs change, update the
website before store submission.

## Local Commands

From `apps/website`:

```powershell
npm ci
npm run verify
```

For local development:

```powershell
npm run dev
```

For production preview:

```powershell
npm run build
npm run preview
```
