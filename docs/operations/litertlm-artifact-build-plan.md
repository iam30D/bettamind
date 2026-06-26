# LiteRT-LM Artifact Build Plan

Date updated: 2026-06-26

## When the artifact build occurs

The `.litertlm` artifact build does not happen during licence approval. It
happens in a later model-packaging task after all of these are true:

- owner licence approval is recorded in
  `docs/operations/model-license-approval-records.md`;
- the exact model to package is selected for a release;
- the exact source revision or provider artifact is chosen;
- the owner approves downloading or converting the large model files outside
  Git;
- a secure model-pack signing key and public trust-anchor plan are ready.

Until then, Bettamind still runs with deterministic no-model fallback.

## Build process

1. Select one model for packaging first: `Qwen/Qwen2.5-1.5B-Instruct`.
2. Re-check the model page, licence and exact source revision.
3. Obtain the approved source model or official LiteRT-LM-compatible artifact
   outside Git.
4. Use the LiteRT-LM File Builder route to create or validate the `.litertlm`
   artifact for Android and iOS runtime use.
5. Test the artifact behind the platform LiteRT-LM bridge on representative
   Android and iOS devices.
6. Compute the final artifact byte size and SHA-256 checksum.
7. Fill the pending artifact fields in
   `docs/operations/model-license-approval-records.md`.
8. Fill a production `ModelPackManifest` using
   `docs/operations/model-pack-manifest-template.json`.
9. Sign the manifest with the owner-controlled Ed25519 model-pack signing key.
10. Install through Bettamind's model-pack installer and verify signature,
    checksum, rollback rejection, removal and no-model fallback.
11. Run AI response-mode, relational-boundary, harmful-intent,
    malformed-output and device performance tests.
12. Only after all checks pass, set the model approval status to
    `APPROVED_FOR_RELEASE`.

## Files that must stay out of Git

- model weights;
- `.litertlm` artifacts;
- conversion scratch files;
- signing private keys;
- provider credentials;
- production trust-root secrets;
- device test logs containing real user content.

Only small metadata, public notices, public URLs, checksums, signing key IDs and
owner approval records belong in Git.

## Current references

- LiteRT-LM overview: <https://developers.google.com/edge/litert-lm/overview>
- LiteRT-LM File Builder documentation:
  <https://developers.google.com/edge/litert-lm/api/file_builder>
- Gemma 4 E2B source: <https://huggingface.co/google/gemma-4-E2B-it>
- Qwen2.5 1.5B Instruct source:
  <https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct>
