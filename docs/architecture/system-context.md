# System Context

Bettamind is primarily a local mobile application.

```text
User
  |
  v
Bettamind Android/iOS app
  |
  +-- local encrypted storage (Phase 3)
  +-- optional local AI runtime (Phase 6)
  +-- optional encrypted exports (Phase 8)
  +-- optional backend over explicit consent (Phase 9)
```

The backend is not part of the core runtime requirement. It may later provide
signed manifests, professional verification, revocation lists, encrypted blob
storage and optional ciphertext-only sync.

Phase 1 creates only the build foundation and platform host projects.
