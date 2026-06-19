# Local AI Model Pack Release

Date updated: 2026-06-19

## Policy

Bettamind installs and runs with no AI model. A local AI model is optional,
replaceable and removable. Bettamind may recommend a compatible model pack, but
the user must approve installation before any model artifact is downloaded,
imported or activated. If the user declines, removes a model or lacks enough
storage, Bettamind continues through deterministic fallback.

The app owner must approve model licensing before a production model artifact
is packaged or distributed. User installation consent does not replace owner
licence compliance.

## Recommended packs

| Use | Model | Runtime | Approximate artifact size | Licence | Owner action |
| --- | --- | --- | --- | --- | --- |
| Primary optional pack | `google/gemma-4-E2B-it` | `litert-lm` | 2.58 GB | Apache-2.0 | Accept or confirm Gemma 4 licence terms before packaging |
| Smaller fallback | `Qwen/Qwen2.5-1.5B-Instruct` | `litert-lm` | 1.6 GB | Apache-2.0 | Confirm Qwen licence and notices before packaging |

Gemma 4 E2B is the default recommendation for standard or high-tier devices
with enough free storage. Qwen2.5 1.5B Instruct is the fallback recommendation
for low-tier or storage-limited devices when enough free storage remains. These
recommendations are not bundled and do not auto-install.

## Owner licence gate

Complete this gate before any production artifact is uploaded, signed or made
available to users:

1. Open the exact model source page.
2. Review and accept or confirm the model licence under the publishing entity.
3. Record the model ID, exact source URL, revision or commit, date accessed and
   licence identifier.
4. Save the licence text and any required attribution or notice text.
5. Confirm redistribution, mobile use, offline use and target-country release
   constraints.
6. Confirm the model is acceptable for Bettamind's adult, privacy-first,
   non-therapy and non-emergency positioning.
7. Record approval in the release checklist before packaging.

Current source pages:

- Gemma 4 model card: <https://ai.google.dev/gemma/docs/core/model_card_4>
- Gemma 4 licence: <https://ai.google.dev/gemma/docs/gemma_4_license>
- Gemma 4 E2B source: <https://huggingface.co/google/gemma-4-E2B-it>
- Qwen2.5 1.5B source: <https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct>
- Apache-2.0 licence text: <https://www.apache.org/licenses/LICENSE-2.0>

## Required files and records

For each released model pack, keep these records outside the Git repository
unless they are small metadata or documentation files:

- exact model artifact, preferably a LiteRT-LM-compatible `.litertlm` file;
- model card URL and archived revision identifier;
- licence text and notice or attribution text;
- SHA-256 checksum for the final artifact;
- artifact size in bytes;
- signed `ModelPackManifest`;
- Ed25519 signing key ID;
- public trust-anchor record approved for the app;
- revocation record and minimum accepted version policy;
- device-test report for Android and iOS;
- safety and relational-boundary evaluation report;
- user-facing install copy and third-party notices.

Never commit model weights, signing private keys, production trust-root secrets
or real user content.

## Packaging steps

1. Complete the owner licence gate.
2. Obtain the exact model artifact from the approved source.
3. Convert or package it into the approved LiteRT-LM format when needed.
4. Verify the model runs behind `LocalAiRuntime` on supported Android and iOS
   devices.
5. Compute the artifact SHA-256 checksum.
6. Fill the model-pack manifest from
   `docs/operations/model-pack-manifest-template.json`.
7. Sign the manifest with the owner-controlled Ed25519 model-pack signing key.
8. Upload the artifact and signed manifest to the approved distribution
   location.
9. Install through Bettamind's model-pack installer and verify signature,
   checksum, rollback rejection and removal.
10. Run AI response-mode tests, relational-boundary tests, harmful-intent
    tests, malformed-output tests and deterministic no-model fallback tests.
11. Run Android checks locally and Codemagic `ios-simulator-unsigned` for any
    shared, iOS, Gradle or workflow changes.
12. Update project memory and release notes with artifact version, checksum and
    owner approvals.

## User install experience

The app should keep the install flow short:

- explain that Bettamind works without the model;
- show model name, approximate size and local/offline behaviour;
- state that personal content does not leave the device because of this model;
- state that the model can be removed anytime;
- offer `Install` and `Not now`.

The user is approving optional installation, not taking over the publisher's
licence obligations.
