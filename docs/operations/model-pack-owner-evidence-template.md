# Model Pack Owner Evidence Template

Date updated: 2026-06-26

Use this template for the first Qwen `.litertlm` release candidate. Do not
commit model weights, `.litertlm` files, signing private keys, provider
credentials, device logs with personal content or production package archives.

## Release candidate

```yaml
model_id: Qwen/Qwen2.5-1.5B-Instruct
runtime_id: litert-lm
release_intent: first_optional_local_ai_model_pack
owner_approval_status: PENDING_FINAL_ARTIFACT
publishing_entity: CORE-NOVANESS LIMITED
approved_by: PENDING_OWNER
approval_date: PENDING_OWNER
source_url: https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct
source_revision_or_commit: PENDING_FINAL_ARTIFACT
license_spdx_id: Apache-2.0
license_rechecked_on: PENDING_FINAL_ARTIFACT
converted_artifact_file_name: qwen2.5-1.5b-instruct.litertlm
artifact_storage_location_outside_git: PENDING_FINAL_ARTIFACT
artifact_size_bytes: PENDING_FINAL_ARTIFACT
artifact_sha256: PENDING_FINAL_ARTIFACT
litertlm_inspection_result: PENDING_FINAL_ARTIFACT
signed_manifest_file_location_outside_git: PENDING_FINAL_ARTIFACT
signed_manifest_sha256: PENDING_FINAL_ARTIFACT
signing_key_id: PENDING_OWNER_KEY
public_key_base64: PENDING_OWNER_KEY
public_key_fingerprint_sha256: PENDING_OWNER_KEY
trust_anchor_app_file: shared/src/commonMain/kotlin/org/bettamind/shared/ai/ModelPackTrustPolicy.kt
trust_anchor_commit_sha: PENDING_TRUST_ANCHOR_COMMIT
revocation_record: PENDING_RELEASE_RECORD
rollback_plan_reviewed: PENDING_RELEASE_RECORD
third_party_notice_reviewed: PENDING_RELEASE_RECORD
```

## Required device evidence

Record each result with device model, OS version, app build number, model pack
version, date, tester and pass/fail summary.

```yaml
android_load_generate_remove:
  status: PENDING_DEVICE_TEST
  evidence_location: PENDING_DEVICE_TEST
ios_load_generate_remove:
  status: PENDING_TESTFLIGHT_OR_DEVICE_TEST
  evidence_location: PENDING_TESTFLIGHT_OR_DEVICE_TEST
low_storage_install_attempt:
  status: PENDING_DEVICE_TEST
  evidence_location: PENDING_DEVICE_TEST
airplane_mode_install_from_approved_local_source:
  status: PENDING_DEVICE_TEST
  evidence_location: PENDING_DEVICE_TEST
interrupted_download_or_import_resume:
  status: PENDING_DEVICE_TEST
  evidence_location: PENDING_DEVICE_TEST
battery_thermal_memory_observation:
  status: PENDING_DEVICE_TEST
  evidence_location: PENDING_DEVICE_TEST
startup_time_after_install:
  status: PENDING_DEVICE_TEST
  evidence_location: PENDING_DEVICE_TEST
deterministic_no_model_fallback_after_remove:
  status: PENDING_DEVICE_TEST
  evidence_location: PENDING_DEVICE_TEST
```

## Trust-anchor rule

Only the public Ed25519 key metadata belongs in the app. The private signing key
must stay owner-controlled outside Git. After the owner completes the key
record, add a real `ModelPackTrustAnchor` entry to
`BettamindModelPackTrustPolicy.productionTrustAnchors` and rerun release
verification.
