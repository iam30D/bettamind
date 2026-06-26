# Model Third-Party Notices

Date updated: 2026-06-26

## Status

This is a draft notice template for optional local AI model packs. It is not
production-complete until the owner has completed
`docs/operations/model-license-approval-records.md` for each model artifact
that will be distributed.

Do not add model weights, signing private keys, provider credentials or real
user content to this file.

## Notice template for app or release package

```text
Bettamind can optionally install local AI model packs. Bettamind works without
these models, and model installation requires user approval.

Optional local AI model: Qwen2.5 1.5B Instruct
Provider: Alibaba Cloud / Qwen
Model ID: Qwen/Qwen2.5-1.5B-Instruct
Licence: Apache License, Version 2.0
Source: https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct
Licence text: https://www.apache.org/licenses/LICENSE-2.0

Deferred optional local AI model: Gemma 4 E2B
Provider: Google
Model ID: google/gemma-4-E2B-it
Licence: Apache License, Version 2.0
Source: https://huggingface.co/google/gemma-4-E2B-it
Model card: https://ai.google.dev/gemma/docs/core/model_card_4
Licence text: https://ai.google.dev/gemma/docs/gemma_4_license
```

## Release checklist

Before using this notice in a release:

- confirm the exact model artifact and source revision;
- confirm the licence has not changed on the exact source page;
- include the full Apache License, Version 2.0 text in the released
  third-party notices or licence bundle from
  `docs/legal/licenses/apache-2.0.txt`;
- preserve provider names, model IDs, source URLs and licence URLs;
- add artifact checksum and version information to release records;
- remove any model entry that is not actually offered in the release.
