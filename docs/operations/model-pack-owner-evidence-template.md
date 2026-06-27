# Model Pack Owner Evidence Template

Date updated: 2026-06-27

Use this record for the first Qwen `.litertlm` release candidate. Do not
commit model weights, `.litertlm` files, signing private keys, provider
credentials, device logs with personal content or production package archives.

## Release candidate

```yaml
model_id: Qwen/Qwen2.5-1.5B-Instruct
runtime_id: litert-lm
release_intent: first_optional_local_ai_model_pack
owner_approval_status: APPROVED_ARTIFACT_PENDING_DEVICE_TESTS_AND_RELEASE_GATES
publishing_entity: CORE-NOVANESS LIMITED
approved_by: OYINLOLA OLUSAYO / CEO
approval_date: 2026-06-27
source_url: https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct
source_revision_or_commit: 19edb84c69a0212f29a6ef17ba0d6f278b6a1614
license_spdx_id: Apache-2.0
license_rechecked_on: 2026-06-27
converted_artifact_file_name: qwen2_5_1_5b_instruct_bettamind_v1.litertlm
artifact_storage_location_outside_git: C:\bettamind-model-release\release-qwen2.5-1.5b-v1
artifact_size_bytes: 1597931520
artifact_sha256: FAA60663B333290C1496C499828B21D3E3254A788CACD8CCE917CE0F761A2DC9
litertlm_inspection_result: C:\bettamind-model-release\evidence\qwen2_5_1_5b_litertlm_inspection.txt
signed_manifest_file_location_outside_git: C:\bettamind-model-release\release-qwen2.5-1.5b-v1\qwen2_5_1_5b_instruct_bettamind_v1.manifest.json
signed_manifest_sha256: 69F1E42B6FF9F67C362FEC21A283DA86726603391FA5EF07D27792F73029E324
detached_signature_sha256: 704937BDA6B9F68B1018562FDBFD58EEA12E2AEE46357FEDEB147800C77D3078
signing_key_id: bettamind-model-prod-2026-01
public_key_base64_der_spki: MCowBQYDK2VwAyEAGsgkjHlXsNaWfwbOzajfTImt5yC6nSGIEIVUL18EvKY=
public_key_fingerprint_sha256: 1B16EFA74603455514E92F78542EA5490FBD5D63291748CCE8650AFEAED01B0A
trust_anchor_app_file: shared/src/commonMain/kotlin/org/bettamind/shared/ai/ModelPackTrustPolicy.kt
trust_anchor_commit_sha: PENDING_COMMIT
revocation_record: PENDING_RELEASE_RECORD
rollback_plan_reviewed: PENDING_RELEASE_RECORD
third_party_notice_reviewed: DRAFT_NOTICE_INCLUDED_PENDING_RELEASE_REVIEW
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
must stay owner-controlled outside Git. The Qwen public trust anchor is now
recorded in `BettamindModelPackTrustPolicy.productionTrustAnchors`; production
release still requires the device evidence above, rollback/revocation review
and final owner release approval.
