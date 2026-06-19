# Model Licence Approval Records

Date updated: 2026-06-19

## Purpose

This file records owner approval before Bettamind packages or distributes an
optional local AI model artifact. Do not place signing private keys, access
tokens, provider account credentials, model weights or large model artifacts in
this file or anywhere in Git.

User installation consent does not replace publisher licence compliance. The
publishing entity must complete the relevant approval record before a model
artifact is signed, uploaded or offered to users.

## Current licence confirmation

As checked on 2026-06-19:

- `google/gemma-4-E2B-it` is listed as `apache-2.0` on Hugging Face and links
  to Google's Apache 2.0 Gemma licence page.
- `Qwen/Qwen2.5-1.5B-Instruct` is listed as `apache-2.0` on Hugging Face.

Re-check the exact model page and revision immediately before production
packaging, because model cards and licence metadata can change.

## How to complete approval

For each model to release:

1. Open the exact model page and licence page.
2. Sign in or accept provider terms if the provider requires it.
3. Record the publishing entity and the owner who approved release.
4. For licence-only approval before artifact packaging, set
   `owner_approval_status` to `APPROVED_LICENSE_ONLY_PENDING_ARTIFACT` and keep
   artifact-specific fields as `PENDING_ARTIFACT_PACKAGING`.
5. Record the exact model revision or commit used for the artifact when the
   final `.litertlm` file is created.
6. Record the final artifact filename, byte size and SHA-256 checksum.
7. Confirm redistribution, offline mobile use and target release countries.
8. Review third-party notices in `docs/legal/model-third-party-notices.md`.
9. Set `owner_approval_status` to `APPROVED_FOR_RELEASE` only after the final
   artifact fields, tests, notices and signed manifest are complete.

## Gemma 4 E2B approval record

```yaml
model_id: google/gemma-4-E2B-it
provider: Google
recommended_use: Primary optional local AI pack
runtime_id: litert-lm
license_spdx_id: Apache-2.0
model_card_url: https://ai.google.dev/gemma/docs/core/model_card_4
model_source_url: https://huggingface.co/google/gemma-4-E2B-it
license_url: https://ai.google.dev/gemma/docs/gemma_4_license
local_release_license_text: docs/legal/licenses/apache-2.0.txt
license_verified_on: 2026-06-19
owner_approval_status: APPROVED_LICENSE_ONLY_PENDING_ARTIFACT
publishing_entity: CORE-NOVANESS LIMITED
approved_by: OYINLOLA OLUSAYO
approval_date: 2026-06-19
provider_terms_account_or_record: No click-through acceptance shown. Apache-2.0 licence reviewed and approved by publishing entity.
owner_approval_statement: I have reviewed the Apache-2.0 licence for this model, approve use under my publishing entity, and understand artifact checksum/revision fields will be completed later.
source_revision_or_commit: PENDING_ARTIFACT_PACKAGING
artifact_source_url: PENDING_ARTIFACT_PACKAGING
converted_artifact_file_name: gemma-4-e2b-it.litertlm
artifact_size_bytes: PENDING_ARTIFACT_PACKAGING
artifact_sha256: PENDING_ARTIFACT_PACKAGING
redistribution_allowed: APPROVED_UNDER_APACHE_2_0_SUBJECT_TO_FINAL_ARTIFACT_RECORD
offline_mobile_use_allowed: APPROVED_UNDER_APACHE_2_0_SUBJECT_TO_FINAL_ARTIFACT_RECORD
target_release_countries_reviewed: PENDING_RELEASE_COUNTRY_REVIEW
third_party_notice_reviewed: DRAFT_NOTICE_REVIEWED_PENDING_FINAL_ARTIFACT
release_notes: Licence approved by CORE-NOVANESS LIMITED for future optional local model packaging. Final LiteRT-LM artifact revision, source URL, byte size, SHA-256 checksum, signed manifest and device tests remain pending.
```

## Qwen2.5 1.5B Instruct approval record

```yaml
model_id: Qwen/Qwen2.5-1.5B-Instruct
provider: Alibaba Cloud / Qwen
recommended_use: Smaller optional local AI fallback pack
runtime_id: litert-lm
license_spdx_id: Apache-2.0
model_card_url: https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct
model_source_url: https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct
license_url: https://www.apache.org/licenses/LICENSE-2.0
local_release_license_text: docs/legal/licenses/apache-2.0.txt
license_verified_on: 2026-06-19
owner_approval_status: APPROVED_LICENSE_ONLY_PENDING_ARTIFACT
publishing_entity: CORE-NOVANESS LIMITED
approved_by: OYINLOLA OLUSAYO
approval_date: 2026-06-19
provider_terms_account_or_record: No click-through acceptance shown. Apache-2.0 licence reviewed and approved by publishing entity.
owner_approval_statement: I have reviewed the Apache-2.0 licence for this model, approve use under my publishing entity, and understand artifact checksum/revision fields will be completed later.
source_revision_or_commit: PENDING_ARTIFACT_PACKAGING
artifact_source_url: PENDING_ARTIFACT_PACKAGING
converted_artifact_file_name: qwen2.5-1.5b-instruct.litertlm
artifact_size_bytes: PENDING_ARTIFACT_PACKAGING
artifact_sha256: PENDING_ARTIFACT_PACKAGING
redistribution_allowed: APPROVED_UNDER_APACHE_2_0_SUBJECT_TO_FINAL_ARTIFACT_RECORD
offline_mobile_use_allowed: APPROVED_UNDER_APACHE_2_0_SUBJECT_TO_FINAL_ARTIFACT_RECORD
target_release_countries_reviewed: PENDING_RELEASE_COUNTRY_REVIEW
third_party_notice_reviewed: DRAFT_NOTICE_REVIEWED_PENDING_FINAL_ARTIFACT
release_notes: Licence approved by CORE-NOVANESS LIMITED for future optional local model packaging. Final LiteRT-LM artifact revision, source URL, byte size, SHA-256 checksum, signed manifest and device tests remain pending.
```
