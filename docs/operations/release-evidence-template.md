# Production Release Evidence Template

Date updated: 2026-06-26

Use this file as the owner-controlled release evidence record before production
submission. Keep screenshots, crash logs, IPA/AAB files, model artifacts,
signing keys and raw device logs outside Git unless an exact path is separately
approved.

## Scope

```yaml
release_scope: English-only, iOS-first and Android MVP
core_rules:
  offline_core: true
  account_required: false
  backend_required: false
  ai_required: false
  unencrypted_daily_storage_allowed: false
  automatic_model_download_allowed: false
  automatic_third_party_contact_allowed: false
production_locales:
  - en
non_english_locale_status: deferred_until_qualified_review
```

## App smoke evidence

```yaml
codemagic_ios_simulator_unsigned:
  status: PASSED
  commit_sha: PENDING_OWNER_RECORD
  run_url_or_id: PENDING_OWNER_RECORD
internal_testflight_smoke:
  status: PENDING_OWNER_TEST
  build_number: PENDING_OWNER_TEST
  device_model: PENDING_OWNER_TEST
  ios_version: PENDING_OWNER_TEST
  evidence_summary: PENDING_OWNER_TEST
android_physical_device_smoke:
  status: SKIPPED_FOR_NOW
  device_model: PENDING_OWNER_TEST
  android_version: PENDING_OWNER_TEST
  evidence_summary: PENDING_OWNER_TEST
```

## Required functional checks

```yaml
visible_ux_images:
  status: PENDING_TESTFLIGHT_SMOKE
  notes: Confirm brand mark, Today, Grow, Support and Settings render without scaffold copy.
encrypted_daily_records:
  status: PENDING_DEVICE_TEST
  notes: Confirm adult gate, encrypted save, app relaunch persistence, delete/export path and no unencrypted fallback.
reminders:
  status: PENDING_DEVICE_TEST
  notes: Confirm neutral preview, permission handling, quiet-hours/pause behavior and no personal lock-screen content.
calendar_handoff:
  status: PENDING_DEVICE_TEST
  notes: Confirm explicit user handoff and no broad calendar read.
speech:
  status: PENDING_DEVICE_TEST
  notes: Confirm explicit microphone permission, OS speech behavior, no raw audio retention and text fallback.
local_ai_model_pack:
  status: PENDING_DEVICE_TEST
  notes: Qwen artifact, app-compatible signed manifest and public trust anchor are prepared; confirm install, load, generate, remove, low-storage behavior, interrupted import/resume and deterministic fallback after removal.
```

## Store readiness

```yaml
store_metadata_source:
  status: BETTAMIND_WEBSITE_AVAILABLE_FOR_COPY_SOURCE
  url: https://bettamind.com
privacy_labels:
  status: PENDING_OWNER_CONFIRMATION
screenshots:
  status: PENDING_FINAL_SCREENSHOTS
  required_surfaces:
    - Today encrypted check-in and daily tools
    - Grow deterministic fallback and optional local AI status
    - Support voluntary local support assessment
    - Settings privacy, speech, model and release controls
support_url:
  status: PENDING_DEPLOYED_PUBLIC_URL_CONFIRMATION
data_deletion_url:
  status: PENDING_DEPLOYED_PUBLIC_URL_CONFIRMATION
safety_claim_review:
  status: PENDING_OWNER_REVIEW
```

## Release risk acceptance

```yaml
rollback_plan:
  status: PENDING_OWNER_APPROVAL
model_pack_revocation_plan:
  status: PENDING_OWNER_APPROVAL
known_skipped_items:
  android_physical_device_testing: SKIPPED_FOR_NOW_BY_OWNER
  non_english_qualified_review: DEFERRED_BY_ENGLISH_ONLY_SCOPE
final_release_decision:
  status: PENDING_OWNER_APPROVAL
  approved_by: PENDING_OWNER
  approval_date: PENDING_OWNER
```
